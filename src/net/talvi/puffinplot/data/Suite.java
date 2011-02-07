package net.talvi.puffinplot.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.PuffinPrefs;
import net.talvi.puffinplot.data.file.FileLoader;
import net.talvi.puffinplot.data.file.PplLoader;
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.data.file.ZplotLoader;

public class Suite {

    private List<Datum> data;
    private List<Site> sites;
    private final List<File> inputFiles;
    private File puffinFile;
    private List<Sample> samples = new ArrayList<Sample>(); // samples in order
    private Map<String, Sample> samplesById; // name or depth as appropriate
    private Map<Integer, Line> dataByLine;
    private int currentSampleIndex = 0;
    private MeasType measType;
    private String suiteName;
    private List<Sample> emptyTraySamples;
    private FisherValues suiteFisher;
    final private PuffinApp app;
    private List<String> loadWarnings;
    private boolean hasUnknownTreatType;
    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");

    public FisherValues getSuiteFisher() {
        return suiteFisher;
    }

    public List<String> getLoadWarnings() {
        return loadWarnings;
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

    private void guessSites() {
        Map<String, List<Sample>> siteMap =
                new LinkedHashMap<String, List<Sample>>();
        for (Sample sample : samples) {
            String siteName = sample.getSiteId();
            if (!siteMap.containsKey(siteName))
                siteMap.put(siteName, new LinkedList<Sample>());
            siteMap.get(siteName).add(sample);
        }
        sites = new ArrayList<Site>(siteMap.size());
        for (Entry<String, List<Sample>> entry: siteMap.entrySet()) {
            List<Sample> siteSamples = entry.getValue();
            Site site = new Site(entry.getKey(), siteSamples);
            sites.add(site);
            for (Sample s: siteSamples) s.setSite(site);
        }
    }
    
    public void doFisherOnSites() {
        if (!getMeasType().isDiscrete())
            throw new UnsupportedOperationException("Only discrete suites can have sites.");
        for (Site site: sites) site.doFisher();
    }

    public List<FisherValues> getFishers() {
        List<FisherValues> result = new ArrayList<FisherValues>(sites.size());
        for (Site site: sites) {
            if (site.getFisher() != null) result.add(site.getFisher());
        }
        return result;
    }

    public boolean isFilenameSet() {
        return puffinFile != null;
    }

    public void save() {
        if (puffinFile != null) saveAs(puffinFile);
    }

    public void saveAs(File file) {
        List<String> fields = DatumField.getRealFieldHeaders();

        FileWriter fileWriter = null;
        CsvWriter csvWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write("PuffinPlot file. Version 3\n");
            csvWriter = new CsvWriter(fileWriter, "\t");
            csvWriter.writeCsv(fields);

            for (Sample sample : getSamples()) {
                for (Datum datum : sample.getData()) {
                    csvWriter.writeCsv(datum.toStrings());
                }
            }
            csvWriter.close();
            puffinFile = file;
            suiteName = file.getName();
            app.getRecentFiles().add(Collections.singletonList(file));
            app.getMainWindow().getMainMenuBar().updateRecentFiles();
        } catch (IOException e) {
            app.errorDialog("Error saving file", e.getLocalizedMessage());
        } finally {
            try {
                if (fileWriter != null) fileWriter.close();
                if (csvWriter != null) csvWriter.close();
            } catch (IOException e) {
                app.errorDialog("Error closing saved file", e.getLocalizedMessage());
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
        List<File> result = new ArrayList<File>();
        for (File file: files) {
            if (file.isDirectory())
                result.addAll(expandDirs(Arrays.asList(file.listFiles())));
            else result.add(file);
        }
        Collections.sort(result);
        return result;
    }

    public void doSampleCalculations() {
        for (Sample sample : getSamples()) {
            sample.calculateFisher();
            sample.doPca();
            sample.fitGreatCircle();
        }
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
        final PuffinPrefs prefs = app.getPrefs();
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
            case PUFFINPLOT_OLD:
                TwoGeeLoader twoGeeLoader =
                        new TwoGeeLoader(file, prefs.get2gProtocol(),
                        prefs.getSensorLengths().toVector());
                loader = twoGeeLoader;
                break;
            case PUFFINPLOT_NEW:
                loader = new PplLoader(file);
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
                FileType.guess(files.get(0)) == FileType.PUFFINPLOT_OLD &&
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
            guessSites(); // sites aren't saved yet so we just re-guess on load
        }
        doSampleCalculations();
    }
    
    /*
     * Save calculations per-sample.
     */
    public void saveCalcsSample(File file, Correction correction) {
        CsvWriter writer = null;
        try {
            if (samples.isEmpty()) {
                app.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }

            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv("Suite", measType.getColumnHeader(), "NRM intensity",
                    FisherValues.getHeaders(), PcaAnnotated.getHeaders(),
                    MDF.getHeaders());
            for (Sample sample: samples) {
                PcaAnnotated pca = sample.getPca();
                FisherValues fish = sample.getFisher();
                MDF mdf = sample.getMDF();
                writer.writeCsv(getName(), sample.getNameOrDepth(),
                        String.format("%.4g", sample.getNRM(correction)),
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
     * Save [Fisher and great-circle] calculations per site. Only works for discrete.
     */
    public void saveCalcsSite(File file) {
        CsvWriter writer = null;
        if (sites == null) {
            app.errorDialog("Error saving file", "No sites are defined.");
            return;
        }
        try {
            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv("site", FisherValues.getHeaders(), GreatCircles.getHeaders());
            for (Site site: sites) {
                List<String> fisherCsv = (site.getFisher() == null)
                        ? FisherValues.getEmptyFields()
                        : site.getFisher().toStrings();
                List<String> gcCsv = (site.getGreatCircles() == null)
                        ? GreatCircles.getEmptyFields()
                        : site.getGreatCircles().toStrings();
                writer.writeCsv(site, fisherCsv, gcCsv);
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

    /**
     *  Import AMS data from a file. If directions==false, line format is
     *  k11 k22 k33 k12 k23 k13  (tensor components)
     * otherwise it's
     * inc1 dec1 inc2 dec2 inc3 dec3 (axis directions, decreasing magnitude)
     */

    public void importAms(List<File> files, boolean directions)
            throws IOException {
        BufferedReader reader = null;
        for (File file: files) {
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    final double[] v = new double[6];
                    Scanner s = new Scanner(line);
                    String name = s.next();
                    Sample sample = getSampleByName(name);
                    for (int i = 0; i < 6; i++) v[i] = s.nextDouble();
                    if (sample != null) {
                        if (directions) {
                            sample.setAmsDirections(v[0], v[1], v[2],
                                    v[3], v[4], v[5]);
                        } else {
                            sample.setAmsFromTensor(v[0], v[1], v[2],
                                    v[3], v[4], v[5]);
                        }
                    }
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    public static List<FisherValues> doReversalsTest(List<Suite> suites) {
        List<Vec3> normal = new ArrayList<Vec3>(), reversed = new ArrayList<Vec3>();
        for (Suite suite: suites) {
            for (Sample sample: suite.getSamples()) {
                PcaValues pca = sample.getPcaValues();
                if (pca != null) {
                 (pca.getDirection().z > 0 ? normal : reversed).add(pca.getDirection());
                }
            }
        }
        FisherValues fisherNormal = FisherValues.calculate(normal);
        FisherValues fisherReversed = FisherValues.calculate(reversed);
        return Arrays.asList(fisherNormal, fisherReversed);
    }

    public void exportToFiles(File directory, Collection<DatumField> fields) {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                logger.info(String.format("exportToFiles: %s is not a directory",
                        directory.toString()));
                return;
            }
        } else {
            if (!directory.mkdirs()) {
                logger.info(String.format("exportToFiles: couldn't create %s",
                        directory.toString()));
                return;
            }
        }
        for (Sample s: getSamples()) {
            final List<String> lines = s.exportFields(fields);
            final File outFile = new File(directory, s.getSampleId());
            FileWriter fw = null;
            try {
                fw = new FileWriter(outFile);
                for (String line : lines) {
                    fw.write(line);
                    fw.write("\n");
                }
            } catch (IOException e) {
                logger.log(Level.WARNING,
                        "exportToFiles: exception writing file.", e);
            } finally {
                try {
                    if (fw != null) fw.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING, 
                            "exportToFiles: exception closing file.", e2);
                }
            }
        }
    }

}
