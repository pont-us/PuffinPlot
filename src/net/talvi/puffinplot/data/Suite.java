package net.talvi.puffinplot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.talvi.puffinplot.FileType;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.file.FileLoader;
import net.talvi.puffinplot.data.file.LoadingStatus;
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.data.file.ZplotLoader;

public class Suite implements Iterable<Datum> {

    private List<Datum> data;
    private final List<File> inputFiles;
    private File puffinFile;
    private String[] names = {};
    private Map<String, Sample> samplesByName;
    private Map<Integer, Line> dataByLine;
    private int currentSampleIndex = 0;
    private MeasType measType;
    private String currentSampleName;
    private String suiteName;
    private List<FisherForSite> siteFishers;
    private FisherValues suiteFisher;
    final private PuffinApp app;
    private List<String> loadWarnings;
    private static final Vec3 SENSOR_LENGTHS_OLD =
            new Vec3(-4.628, 4.404, -6.280);
    private static final Vec3 SENSOR_LENGTHS_NEW =
            new Vec3(4.628, -4.404, -6.280);

    public Iterator<Datum> iterator() {
        return data.iterator();
    }

    public FisherValues getSuiteFisher() {
        return suiteFisher;
    }

    public List<String> getLoadWarnings() {
        return loadWarnings;
    }

    private static class FisherForSite {
        String site;
        FisherValues fisher;

        public FisherForSite(String site, FisherValues fisher) {
            this.site = site;
            this.fisher = fisher;
        }
    }

    public void doFisherOnSuite() {
        List<Sample> samples = PuffinApp.getInstance().getSelectedSamples();
        List<PcaValues> pcas = new ArrayList<PcaValues>(samples.size());

        for (Sample sample: samples) {
            PcaValues pca = sample.getPcaValues();
            if (pca != null) pcas.add(pca);
        }

        List<Vec3> directions = new ArrayList<Vec3>(pcas.size());
        for (PcaValues pca: pcas) directions.add(pca.getDirection());

        suiteFisher = FisherValues.calculate(directions);
    }

    public void doFisherOnSites() {
        Map<String, Set<PcaValues>> sitePcas =
                new LinkedHashMap<String, Set<PcaValues>>();

        // Chuck PCA values into buckets
        for (Sample sample : PuffinApp.getInstance().getSelectedSamples()) {
            String site = sample.getSiteId();
            PcaValues pca = sample.getPcaValues();
            if (pca != null) {
                if (!sitePcas.containsKey(site))
                    sitePcas.put(site, new HashSet<PcaValues>());
                sitePcas.get(site).add(pca);
            }
        }

        siteFishers = new ArrayList<FisherForSite>(sitePcas.size());
        // Go through them doing Fisher calculations
        for (Map.Entry<String, Set<PcaValues>> entry: sitePcas.entrySet()) {
            Collection<Vec3> directions =
                    new ArrayList<Vec3>(entry.getValue().size());
            for (PcaValues pca: entry.getValue())
                directions.add(pca.getDirection());
            siteFishers.add(new FisherForSite(entry.getKey(),
                    FisherValues.calculate(directions)));
        }

    }

    public List<FisherValues> getFishers() {
        if (siteFishers==null) return null;
        List<FisherValues> result = new ArrayList<FisherValues>(siteFishers.size());
        for (FisherForSite f: siteFishers) result.add(f.fisher);
        return result;
    }

    public boolean isFilenameSet() {
        return puffinFile != null;
    }

    public void save() {
        if (puffinFile != null) saveAs(puffinFile);
    }

    public void saveAs(File file) {
        List<DatumField> fields = new LinkedList(Arrays.asList(DatumField.values()));
        fields.remove(DatumField.UNKNOWN);

        Writer writer = null;
        try {
            writer = new FileWriter(file);

            StringBuilder header = new StringBuilder();
            for (DatumField field : fields) {
                header.append(field.getHeading());
                header.append("\t");
            }
            header.deleteCharAt(header.length()-1);
            header.append("\n");
            writer.write(header.toString());

            for (Sample sample : getSamples()) {
                for (Datum datum : sample.getData()) {
                    StringBuilder line = new StringBuilder();
                    for (DatumField field : fields) {
                        //line.append(datum.getValue(field).toString());
                        line.append("\t");
                    }
                    line.deleteCharAt(line.length() - 1);
                    line.append("\n");
                    writer.write(line.toString());
                }
            }
            writer.close();
            puffinFile = file;
            suiteName = file.getName();
            app.getRecentFiles().add(Collections.singletonList(file));
            app.getMainWindow().getMainMenuBar().updateRecentFiles();
        } catch (IOException e) {
            app.errorDialog("Error saving file", e.getLocalizedMessage());
        } finally {
            if (writer != null)  {
                try { writer.close(); }
                catch (IOException e) {
                    app.errorDialog("Error closing saved file", e.getLocalizedMessage());
                }
            }
        }
    }

    private Line getLineContainer(int lineNumber) {
        if (!dataByLine.containsKey(lineNumber))
            dataByLine.put(lineNumber, new Line(lineNumber));
        return dataByLine.get(lineNumber);
    }

    private void addDatum(Datum d, Set<String> nameSet) {
        if (measType == MeasType.UNSET) measType = d.getMeasType();
        if (d.getMeasType() != measType) {
            throw new Error("Can't mix long core and discrete measurements.");
        }
        if (!d.ignoreOnLoading()) {
            data.add(d);
            String name = d.getSampleIdOrDepth();
            Sample s = samplesByName.get(name);
            if (s == null) {
                s = new Sample(name);
                samplesByName.put(name, s);
            }
            s.addDatum(d);
            nameSet.add(name);
        }
    }

    private List<File> expandDirs(List<File> files) {
        List<File> result = new LinkedList<File>();
        for (File file: files) {
            if (file.isDirectory())
                result.addAll(expandDirs(Arrays.asList(file.listFiles())));
            else result.add(file);
        }
        return result;
    }

    /*
     * Note that this may return an empty suite, in which case it is the
     * caller's responsibility to notice this and deal with it.
     * We can't just throw an exception if the suite's empty,
     * because then we lose the load warnings (which will probably explain
     * to the user *why* the suite's empty and are thus quite important).
     **/
    public Suite(List<File> files) throws IOException {
        app = PuffinApp.getInstance();
        assert(files.size() > 0);
        if (files.size() == 1) suiteName = files.get(0).getName();
        else suiteName = files.get(0).getParentFile().getName();
        files = expandDirs(files);
        this.inputFiles = files;
        data = new ArrayList<Datum>();
        samplesByName = new LinkedHashMap<String, Sample>();
        dataByLine = new HashMap<Integer, Line>();
        measType = MeasType.UNSET;
        TreeSet<Double> depthSet = new TreeSet<Double>();
        TreeSet<String> nameSet = new TreeSet<String>();

        for (File file: files) {
            FileType fileType = FileType.guessFromName(file);
            FileLoader loader = null;
            switch (fileType) {
            case TWOGEE:
            case PUFFINPLOT:
                TwoGeeLoader twoGeeLoader = new TwoGeeLoader(file);
                twoGeeLoader.setSensorLengths(SENSOR_LENGTHS_NEW);
                loader = twoGeeLoader;
                break;
            case ZPLOT:
                loader = new ZplotLoader(file);
                break;
            }
            while (loader.getStatus() == LoadingStatus.IN_PROGRESS) {
                addDatum(loader.getNext(), nameSet);
            }
                    
        loadWarnings.addAll(loader.getMessages());
        names = nameSet.toArray(names);
        setCurrentSampleIndex(0);
        for (Sample s: getSamples()) s.doPca();
        if (files.size() == 1 &&
                FileType.guessFromName(files.get(0)) == FileType.PUFFINPLOT &&
                getNumSamples() > 0) {
            app.getRecentFiles().add(files);
            app.getMainWindow().getMainMenuBar().updateRecentFiles();
            puffinFile = files.get(0);
            }
        }
    }
    
    /*
     * Save calculations per-sample.
     */
    public void saveCalcsSample(File file) {
        CsvWriter writer = null;
        try {
            List<Sample> samples = getSamplesOrdered();
            if (samples.size()==0) {
                app.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }

            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv(measType.getColumnHeader(),
                    FisherValues.getHeaders(), PcaAnnotated.getHeaders(),
                    MDF.getHeaders());
            for (Sample sample: samples) {
                PcaAnnotated pca = sample.getPca();
                FisherValues fish = sample.getFisher();
                MDF mdf = sample.getMidpoint();
                writer.writeCsv(sample.getNameOrDepth(),
                        fish == null ? FisherValues.getEmptyFields() : fish.toStrings(),
                        pca == null ? PcaAnnotated.getEmptyFields() : pca.toStrings(),
                        mdf == null ? MDF.getEmptyFields() : mdf.toStrings());
            }
        } catch (IOException ex) {
            app.errorDialog("Error saving file", ex.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException ex) {
                app.errorDialog("Error closing file", ex.getLocalizedMessage());
            }
        }
    }

    /*
     * Save [Fisher] calculations per site. Only works for discrete.
     */
    public void saveCalcsSite(File file) {
        CsvWriter writer = null;
        try {
            if (siteFishers==null || siteFishers.size() == 0) {
                app.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }

            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv("site", FisherValues.getHeaders());
            for (FisherForSite f: siteFishers) {
                writer.writeCsv(f.site, f.fisher.toStrings());
            }
        } catch (IOException ex) {
           throw new Error(ex);
        } finally {
            if (writer != null) {
                try { writer.close(); }
                catch (IOException e) { throw new Error(e); }
            }
        }
    }

    /*
     * Save a single Fisher calculation for the suite.
     */
    public void saveCalcsSuite(File file) {
        CsvWriter writer = null;
        try {
            if (suiteFisher == null) {
                app.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }
            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv(FisherValues.getHeaders());
            writer.writeCsv(suiteFisher.toStrings());
        } catch (IOException ex) {
           throw new Error(ex);
        } finally {
            if (writer != null) {
                try { writer.close(); }
                catch (IOException e) { throw new Error(e); }
            }
        }
    }

    public Sample getSampleByName(String name) {
        return samplesByName.get(name);
    }

    public void setCurrentSampleIndex(int value) {
        currentSampleIndex = value;
    }

    public int getCurrentSampleIndex() {
        return currentSampleIndex;
    }

    public Sample getCurrentSample() {
        return getSampleByName(names[getCurrentSampleIndex()]);
    }

    public Collection<Sample> getSamples() {
        return samplesByName.values();
    }

    public List<Sample> getSamplesOrdered() {
        ArrayList<Sample> samples = new ArrayList<Sample>(getNumSamples());
            for (String name : names) samples.add(getSampleByName(name));
        return samples;
    }

    public List<Datum> getData() {
        return data;
    }

    public MeasType getMeasType() {
        return measType;
    }

    public String getName() {
        return suiteName;
    }

    public int getNumSamples() {
        return samplesByName.size();
    }

    public String getCurrentName() {
        return currentSampleName;
    }

    public void setCurrentName(String currentName) {
        this.currentSampleName = currentName;
    }

    public String[] getNameArray() {
        return names;
    }

    public void applySelectionToAll(Sample sample) {
        for (Sample mySample: getSamples())
            mySample.copySelectionFrom(sample);
    }

    public Sample getSampleByIndex(int i) {
        return samplesByName.get(names[i]);
    }

    @Override
    public String toString() {
        return getName();
    }
}
