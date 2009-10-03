package net.talvi.puffinplot.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.file.FileLoader;
import net.talvi.puffinplot.data.file.Ppl2Loader;
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.data.file.ZplotLoader;

public class Suite {

    private List<Datum> data;
    private final List<File> inputFiles;
    private File puffinFile;
    private List<Sample> samples = new ArrayList<Sample>(); // samples in order
    private Map<String, Sample> samplesById; // name or depth as appropriate
    private Map<Integer, Line> dataByLine;
    private int currentSampleIndex = 0;
    private MeasType measType;
    private String suiteName;
    private List<FisherForSite> siteFishers;
    private List<Sample> emptyTraySamples;
    private FisherValues suiteFisher;
    final private PuffinApp app;
    private List<String> loadWarnings;
    private static final Vec3 SENSOR_LENGTHS_OLD =
            new Vec3(-4.628, 4.404, -6.280);
    private static final Vec3 SENSOR_LENGTHS_NEW =
            new Vec3(4.628, -4.404, -6.280);
    private boolean hasUnknownTreatType;

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
        List<Sample> selected = PuffinApp.getInstance().getSelectedSamples();
        List<PcaValues> pcas = new ArrayList<PcaValues>(selected.size());
        for (Sample sample: selected) {
            PcaValues pca = sample.getPcaValues();
            if (pca != null) pcas.add(pca);
        }
        List<Vec3> directions = new ArrayList<Vec3>(pcas.size());
        for (PcaValues pca: pcas) directions.add(pca.getDirection());
        suiteFisher = FisherValues.calculate(directions);
    }

    public void doFisherOnSites() {
        if (!getMeasType().isDiscrete())
            throw new UnsupportedOperationException("Only discrete suites can have sites.");
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
        List<String> fields = Datum.getFieldNames();

        CsvWriter writer = null;
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write("PuffinPlot file. Version 2\n");
            writer = new CsvWriter(fileWriter, "\t");
            writer.writeCsv(fields);

            for (Sample sample : getSamples()) {
                for (Datum datum : sample.getData()) {
                    writer.writeCsv(datum.toStrings());
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

    private void addDatum(Datum d) {
        if (measType == MeasType.UNSET) measType = d.getMeasType();
        if (d.getMeasType() != measType) {
            throw new Error("Can't mix long core and discrete measurements.");
        }
        data.add(d);
        if (d.getTreatType() == TreatType.UNKNOWN) hasUnknownTreatType = true;
        String name = d.getIdOrDepth();
        Sample s = samplesById.get(name);
        if (s == null) {
            s = new Sample(name);
            samplesById.put(name, s);
            samples.add(s);
        }
        d.setSuite(this);
        s.addDatum(d);
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

    /**
     * Note that this may return an empty suite, in which case it is the
     * caller's responsibility to notice this and deal with it.
     * We can't just throw an exception if the suite's empty,
     * because then we lose the load warnings (which will probably explain
     * to the user *why* the suite's empty and are thus quite important).
     */
    public Suite(List<File> files) throws IOException {
        app = PuffinApp.getInstance();
        assert(files.size() > 0);
        if (files.size() == 1) suiteName = files.get(0).getName();
        else suiteName = files.get(0).getParentFile().getName();
        files = expandDirs(files);
        this.inputFiles = files;
        final ArrayList dataArray = new ArrayList<Datum>();
        data = dataArray;
        samplesById = new LinkedHashMap<String, Sample>();
        dataByLine = new HashMap<Integer, Line>();
        measType = MeasType.UNSET;
        loadWarnings = new ArrayList<String>();
        hasUnknownTreatType = false;

        for (File file: files) {
            if (!file.exists()) {
                loadWarnings.add(String.format("File \"%s\" does not exist.", file.getName()));
                continue;
            }
            if (!file.canRead()) {
                loadWarnings.add(String.format("File \"%s\" is unreadable.", file.getName()));
                continue;
            }
            final FileType fileType = FileType.guess(file);
            FileLoader loader = null;
            switch (fileType) {
            case TWOGEE:
            case PUFFINPLOT_1:
                TwoGeeLoader twoGeeLoader = new TwoGeeLoader(file);
                twoGeeLoader.setSensorLengths(SENSOR_LENGTHS_NEW);
                loader = twoGeeLoader;
                break;
            case PUFFINPLOT_2:
                loader = new Ppl2Loader(file);
                break;
            case ZPLOT:
                loader = new ZplotLoader(file);
                break;
            default:
                loadWarnings.add(String.format("%s is of unknown file type.", file.getName()));
                break;
            }
            if (loader != null) {
                dataArray.ensureCapacity(dataArray.size() + loader.getData().size());
                for (Datum d : loader.getData()) {
                    if (!d.ignoreOnLoading()) addDatum(d);
                }
                loadWarnings.addAll(loader.getMessages());
            }
        }
        setCurrentSampleIndex(0);
        if (hasUnknownTreatType)
            loadWarnings.add("One or more treatment types were not recognized.");
        for (Sample s : getSamples()) s.doPca();
        if (files.size() == 1 &&
                FileType.guess(files.get(0)) == FileType.PUFFINPLOT_1 &&
                getNumSamples() > 0) {
            app.getRecentFiles().add(files);
            app.getMainWindow().getMainMenuBar().updateRecentFiles();
            puffinFile = files.get(0);
        }
        if (measType.isDiscrete()) {
            emptyTraySamples = new ArrayList<Sample>();
            int slot = 1;
            while (true) {
                String slotId = "TRAY" + slot;
                if (!samplesById.containsKey(slotId)) break;
                emptyTraySamples.add(samplesById.get(slotId));
                slot++;
            }
        }
    }
    
    /*
     * Save calculations per-sample.
     */
    public void saveCalcsSample(File file) {
        CsvWriter writer = null;
        try {
            if (samples.size()==0) {
                app.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }

            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv(measType.getColumnHeader(), "NRM intensity",
                    FisherValues.getHeaders(), PcaAnnotated.getHeaders(),
                    MDF.getHeaders());
            for (Sample sample: samples) {
                PcaAnnotated pca = sample.getPca();
                FisherValues fish = sample.getFisher();
                MDF mdf = sample.getMDF();
                writer.writeCsv(sample.getNameOrDepth(),
                        String.format("%.4g", sample.getNRM()),
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
        return samplesById.get(name);
    }

    public void setCurrentSampleIndex(int value) {
        currentSampleIndex = value;
    }

    public int getCurrentSampleIndex() {
        return currentSampleIndex;
    }

    public Sample getCurrentSample() {
        return getSampleByIndex(getCurrentSampleIndex());
    }

    public List<Sample> getSamples() {
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
        return samplesById.size();
    }

    public void applySelectionToAll(Sample sample) {
        for (Sample mySample: getSamples())
            mySample.copySelectionFrom(sample);
    }

    public Sample getSampleByIndex(int i) {
        return samples.get(i);
    }

    public Datum getTrayCorrection(Datum d) {
        Sample s = d.getSample();
        int slot = s.getSlotNumber();
        if (emptyTraySamples != null && emptyTraySamples.size()>slot) {
            Sample empty = emptyTraySamples.get(slot);
            return empty.getDatumByRunNumber(d.getRunNumber());
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
