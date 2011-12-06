package net.talvi.puffinplot.data;

import com.sun.imageio.plugins.common.BitFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.PuffinUserException;
import net.talvi.puffinplot.data.file.AmsLoader;
import net.talvi.puffinplot.data.file.FileLoader;
import net.talvi.puffinplot.data.file.PplLoader;
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.data.file.ZplotLoader;

public class Suite {

    private List<Datum> data;
    private List<Site> sites;
    private File puffinFile;
    private List<Sample> samples = new ArrayList<Sample>(); // samples in order
    private LinkedHashMap<String, Sample> samplesById; // name or depth as appropriate
    private Map<Integer, Line> dataByLine;
    private int currentSampleIndex = 0;
    private MeasType measType;
    private String suiteName;
    private List<Sample> emptyTraySamples;
    private FisherValues suiteFisher;
    private List<String> loadWarnings;
    private boolean hasUnknownTreatType;
    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private CustomFields<String> customFlagNames;
    private CustomFields<String> customNoteNames;
    private List<KentParams> amsBootstrapParams;
    private List<KentParams> hextParams;

    public FisherValues getSuiteFisher() {
        return suiteFisher;
    }

    public List<String> getLoadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
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
    
    public void doFisherOnSites(Correction correction) {
        if (!getMeasType().isDiscrete()) {
            throw new UnsupportedOperationException("Only discrete suites "
                    + "can have sites.");
        }
        for (Site site: getSites()) site.doFisher(correction);
    }

    public List<FisherValues> getFishers() {
        List<FisherValues> result = new ArrayList<FisherValues>(getSites().size());
        for (Site site: getSites()) {
            if (site.getFisher() != null) result.add(site.getFisher());
        }
        return result;
    }

    public boolean isFilenameSet() {
        return getPuffinFile() != null;
    }

    public void save() throws PuffinUserException {
        if (getPuffinFile() != null) saveAs(getPuffinFile());
    }

    public void saveAs(File file) throws PuffinUserException {
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
            // csvWriter.close();
            fileWriter.write("\n");
            for (Sample sample: getSamples()) {
                List<String> lines = sample.toStrings();
                for (String line: lines) {
                    String w = String.format(Locale.ENGLISH, "SAMPLE\t%s\t%s\n",
                            sample.getNameOrDepth(), line);
                    fileWriter.write(w);
                }
            }
            for (Site site: getSites()) {
                List<String> lines = site.toStrings();
                for (String line: lines) {
                    String w = String.format(Locale.ENGLISH, "SITE\t%s\t%s\n",
                            site.getName(), line);
                    fileWriter.write(w);
                }
            }
            for (String line: toStrings()) {
                fileWriter.write(String.format(Locale.ENGLISH, "SUITE\t%s\n",
                        line));
            }
            fileWriter.close();
            puffinFile = file;
            suiteName = file.getName();
        } catch (IOException ex) {
            throw new PuffinUserException(ex);
        } finally {
            try {
                if (fileWriter != null) fileWriter.close();
                if (csvWriter != null) csvWriter.close();
            } catch (IOException ex) {
                throw new PuffinUserException(ex);
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
            s = new Sample(name, this);
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

    public void doSampleCalculations(Correction correction) {
        for (Sample sample: getSamples()) {
            sample.doPca(correction);
            sample.fitGreatCircle(correction);
            sample.calculateMagSusJump();
        }
    }

    public void doSiteCalculations(Correction correction) {
        final Set<Site> sitesDone = new HashSet<Site>();
        for (Sample sample : getSamples()) {
            final Site site = sample.getSite();
            if (site == null) continue;
            if (sitesDone.contains(site)) continue;
            site.doFisher(correction);
            site.doGreatCircle(correction);
            sitesDone.add(site);
        }
    }

    /**
     * Note that this may return an empty suite, in which case it is the
     * caller's responsibility to notice this and deal with it.
     * We can't just throw an exception if the suite's empty,
     * because then we lose the load warnings (which will probably explain
     * to the user *why* the suite's empty and are thus quite important).
     */
    public Suite(List<File> files, SensorLengths sensorLengths,
            TwoGeeLoader.Protocol protocol) throws IOException {
        assert(files.size() > 0);
        if (files.size() == 1) suiteName = files.get(0).getName();
        else suiteName = files.get(0).getParentFile().getName();
        files = expandDirs(files);
        final ArrayList<Datum> dataArray = new ArrayList<Datum>();
        data = dataArray;
        samplesById = new LinkedHashMap<String, Sample>();
        dataByLine = new HashMap<Integer, Line>();
        measType = MeasType.UNSET;
        loadWarnings = new ArrayList<String>();
        hasUnknownTreatType = false;
        final List<String> emptyStringList = Collections.emptyList();
        customFlagNames = new CustomFlagNames(emptyStringList);
        customNoteNames = new CustomNoteNames(emptyStringList);
        List<String> puffinLines = emptyStringList;
        sites = new ArrayList<Site>();

        for (File file: files) {
            if (!file.exists()) {
                loadWarnings.add(String.format("File \"%s\" does not exist.", file.getName()));
                continue;
            }
            if (!file.canRead()) {
                loadWarnings.add(String.format("File \"%s\" is unreadable.", file.getName()));
                continue;
            }
            FileType fileType = null;
            try {
                fileType = FileType.guess(file);
            } catch (IOException ex) {
                loadWarnings.add(String.format("Error guessing type of file \"%s\": %s",
                        file.getName(), ex.getLocalizedMessage()));
            }
            FileLoader loader = null;
            switch (fileType) {
            case TWOGEE:
            case PUFFINPLOT_OLD:
                TwoGeeLoader twoGeeLoader =
                        new TwoGeeLoader(file, protocol, sensorLengths.toVector());
                loader = twoGeeLoader;
                if (files.size()==1) puffinFile = file;
                break;
            case PUFFINPLOT_NEW:
                loader = new PplLoader(file);
                if (files.size()==1) puffinFile = file;
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
                puffinLines = loader.getExtraLines();
            }
        }
        setCurrentSampleIndex(0);
        if (hasUnknownTreatType)
            loadWarnings.add("One or more treatment types were not recognized.");
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

        processPuffinLines(puffinLines);
    }
    
    /**
     *  Intended to be called after instantiating a new Suite from a file.
     */
    public void doAllCalculations(Correction correction) {
        doSampleCalculations(correction);
        if (measType.isDiscrete()) {
            doSiteCalculations(correction);
        }
    }
    
    /**
     * Save calculations per-sample.
     */
    public void saveCalcsSample(File file, Correction correction) 
            throws PuffinUserException {
        CsvWriter writer = null;
        try {
            if (samples.isEmpty()) {
                throw new PuffinUserException("No calculations to save.");
            }

            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv("Suite", measType.getColumnHeader(),
                    "NRM intensity (A/m)",
                    "MS jump temp. (°C)",
                    PcaAnnotated.getHeaders(),
                    GreatCircle.getHeaders(), MDF.getHeaders());
            for (Sample sample: samples) {
                final PcaAnnotated pca = sample.getPca();
                final MDF mdf = sample.getMDF();
                final GreatCircle circle = sample.getGreatCircle();
                writer.writeCsv(getName(), sample.getNameOrDepth(),
                        String.format(Locale.ENGLISH, "%.4g", sample.getNRM()),
                        String.format(Locale.ENGLISH, "%.4g", sample.getMagSusJump()),
                        pca == null ? PcaAnnotated.getEmptyFields() : pca.toStrings(),
                        circle == null ? GreatCircle.getEmptyFields() : circle.toStrings(),
                        mdf == null ? MDF.getEmptyFields() : mdf.toStrings());
            }
        } catch (IOException ex) {
            throw new PuffinUserException(ex);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException ex) {
                throw new PuffinUserException(ex);
            }
        }
    }

    /*
     * Save [Fisher and great-circle] calculations per site. Only works for discrete.
     */
    public void saveCalcsSite(File file) throws PuffinUserException {
        CsvWriter writer = null;
        if (getSites() == null) {
            throw new PuffinUserException("No sites are defined.");
        }
        try {
            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv("site", FisherValues.getHeaders(), GreatCircles.getHeaders(),
                    Site.getGreatCircleLimitHeader());
            for (Site site: getSites()) {
                List<String> fisherCsv = (site.getFisher() == null)
                        ? FisherValues.getEmptyFields()
                        : site.getFisher().toStrings();
                List<String> gcCsv = (site.getGreatCircles() == null)
                        ? GreatCircles.getEmptyFields()
                        : site.getGreatCircles().toStrings();
                List<String> gcCsv2 = (site.getGreatCircles() == null)
                        ? Collections.nCopies(4, "")
                        : site.getGreatCircleLimitStrings();
                writer.writeCsv(site, fisherCsv, gcCsv, gcCsv2);
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
    public void saveCalcsSuite(File file) throws PuffinUserException {
        CsvWriter writer = null;
        try {
            if (suiteFisher == null) {
                throw new PuffinUserException("There are no calculations to save.");
            }
            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv(FisherValues.getHeaders());
            writer.writeCsv(suiteFisher.toStrings());
        } catch (IOException ex) {
           throw new PuffinUserException(ex);
        } finally {
            if (writer != null) {
                try { writer.close(); }
                catch (IOException e) { logger.warning(e.getLocalizedMessage()); }
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
        return Collections.unmodifiableList(samples);
    }

    public List<Datum> getData() {
        return Collections.unmodifiableList(data);
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

    public List<String> toStrings() {
        List<String> result = new ArrayList<String>();
        if (customFlagNames.size()>0) {
            result.add("CUSTOM_FLAG_NAMES\t"+customFlagNames.exportAsString());
        }
        if (customNoteNames.size()>0) {
            result.add("CUSTOM_NOTE_NAMES\t"+customNoteNames.exportAsString());
        }
        return result;
    }

    public void fromString(String string) {
        String[] parts = string.split("\t");
        if ("CUSTOM_FLAG_NAMES".equals(parts[0])) {
            customFlagNames = new CustomFlagNames(Arrays.asList(parts).subList(1, parts.length));
        } else if ("CUSTOM_NOTE_NAMES".equals(parts[0])) {
            customNoteNames = new CustomNoteNames(Arrays.asList(parts).subList(1, parts.length));
        }
    }

    public double getFormAz() {
        for (Sample s: samples) {
            final double v = s.getFormAz();
            if (!Double.isNaN(v)) return v;
        }
        return 0;
    }

    public double getFormDip() {
        for (Sample s: samples) {
            final double v = s.getFormDip();
            if (!Double.isNaN(v)) return v;
        }
        return 0;
    }

    public double getMagDev() {
        for (Sample s: samples) {
            final double v = s.getMagDev();
            if (!Double.isNaN(v)) return v;
        }
        return 0;
    }

    /**
     * Import AMS data from a whitespace-delimited file.
     * If directions==false, line format is k11 k22 k33 k12 k23 k13 
     * (tensor components) otherwise it's
     * inc1 dec1 inc2 dec2 inc3 dec3 (axis directions, decreasing magnitude).
     * If there's no sample in the suite from which to take the sample
     * and formation corrections, importAms will try to read them as
     * fields appended to the end of the line.
     */

    public void importAmsFromDelimitedFile(List<File> files, boolean directions)
            throws IOException {
        BufferedReader reader = null;
        directions = false;
        for (File file: files) {
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    final double[] v = new double[6];
                    Scanner s = new Scanner(line);
                    String name = s.next();
                    if (!containsSample(name)) {
                        insertNewSample(name);
                    }
                    Sample sample = getSampleByName(name);
                    for (int i = 0; i < 6; i++) v[i] = s.nextDouble();
                    if (directions) {
                        sample.setAmsDirections(v[0], v[1], v[2],
                                v[3], v[4], v[5]);
                    } else {
                        if (!sample.hasData()) {
                            sample.setCorrections(s.nextDouble(),
                                    s.nextDouble(), s.nextDouble(),
                                    s.nextDouble(), s.nextDouble());
                        }
                        sample.setAmsFromTensor(v[0], v[1], v[2],
                                v[3], v[4], v[5]);
                    }
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }

    public void importAmsFromAsc(List<File> files, boolean magneticNorth)
            throws IOException {
        List<AmsData> allData = new ArrayList<AmsData>();
        for (File file: files) {
            AmsLoader amsLoader = new AmsLoader(file);
            allData.addAll(amsLoader.readFile2());
        }
        for (AmsData ad: allData) {
            String name = ad.getName();
            if (ad.getfTest() < 3.9715) continue;
            if (!containsSample(name)) {
                insertNewSample(name);
            }
            Sample sample = getSampleByName(name);
            if (!sample.hasData()) {
                double azimuth = ad.getSampleAz();
                if (!magneticNorth) azimuth -= getMagDev();
                sample.setCorrections(azimuth, ad.getSampleDip(),
                        getFormAz(), getFormDip(), getMagDev());
            }
            double[] v = ad.getTensor();
            sample.setAmsFromTensor(v[0], v[1], v[2], v[3], v[4], v[5]);
            guessSites();
        }
    }

    public static List<FisherValues> doReversalTest(List<Suite> suites) {
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

    /**
     * @return the customFlagNames
     */
    public CustomFields<String> getCustomFlagNames() {
        return customFlagNames;
    }

    private void processPuffinLines(List<String> lines) {
        for (String line: lines) {
            String[] parts = line.split("\t");
            if ("SUITE".equals(parts[0])) {
                fromString(line.substring(6));
            } else if ("SAMPLE".equals(parts[0])) {
                String sampleId = parts[1];
                Sample sample = getSampleByName(sampleId);
                sample.fromString(line.substring(8+sampleId.length()));
            } else if ("SITE".equals(parts[0])) {
                Site site = getOrCreateSite(parts[1]);
                site.fromString(line.substring(6+parts[1].length()));
            }
        }
    }

    /**
     * @return the customNoteNames
     */
    public CustomFields<String> getCustomNoteNames() {
        return customNoteNames;
    }

    /**
     * @return the amsBootstrapParams
     */
    public List<KentParams> getAmsBootstrapParams() {
        return Collections.unmodifiableList(amsBootstrapParams);
    }

    public List<KentParams> getAmsHextParams() {
        return Collections.unmodifiableList(hextParams);
    }

    public void clearAmsCalculations() {
        amsBootstrapParams = null;
        hextParams = null;
    }

    public List<Site> getSites() {
        return Collections.unmodifiableList(sites);
    }

    public Site getSiteByName(String siteName) {
        for (Site site: sites) {
            if (siteName.equals(site.getName())) {
                return site;
            }
        }
        return null;
    }

    Site getOrCreateSite(String siteName) {
        Site site = getSiteByName(siteName);
        if (site==null) {
            site = new Site(siteName);
            sites.add(site);
        }
        return site;
    }

    public File getPuffinFile() {
        return puffinFile;
    }

    private class CustomFlagNames extends CustomFields<String> {
        public CustomFlagNames(List<String> list) {
            super(list);
        }
        @Override
        public void add(int position, String value) {
            super.add(position, value);
            for (Sample s: getSamples()) s.getCustomFlags().add(position, Boolean.FALSE);
        }
        @Override
        public void remove(int position) {
            super.remove(position);
            for (Sample s: getSamples()) s.getCustomFlags().remove(position);
        }
        @Override
        public void swapAdjacent(int position) {
            super.swapAdjacent(position);
            for (Sample s: getSamples()) s.getCustomFlags().swapAdjacent(position);
        }
    }

    private class CustomNoteNames extends CustomFields<String> {
        public CustomNoteNames(List<String> list) {
            super(list);
        }
        @Override
        public void add(int position, String value) {
            super.add(position, value);
            for (Sample s: getSamples()) s.getCustomNotes().add(position, "");
        }
        @Override
        public void remove(int position) {
            super.remove(position);
            for (Sample s: getSamples()) s.getCustomNotes().remove(position);
        }
        @Override
        public void swapAdjacent(int position) {
            super.swapAdjacent(position);
            for (Sample s: getSamples()) s.getCustomNotes().swapAdjacent(position);
        }
    }
        
    public static enum AmsCalcType { HEXT, BOOT, PARA_BOOT }; 

    public void doAmsStatistics(List<Sample> samples, AmsCalcType calcType,
            String scriptPath) throws IOException, IllegalArgumentException {
        List<Tensor> tensors = new ArrayList<Tensor>();
        for (Sample s: samples) {
            if (s.getAms() != null) tensors.add(s.getAms());
        }
        if (tensors.isEmpty()) {
            throw new IllegalArgumentException("No AMS data in specified samples.");
        } else if (tensors.size()<3) {
            throw new IllegalArgumentException("Too few samples with AMS data.");
        }
        switch (calcType) {
            case HEXT:
                hextParams = KentParams.calculateHext(tensors, scriptPath);
                break;
            case BOOT:
                amsBootstrapParams = KentParams.calculateBootstrap(tensors,
                        false, scriptPath);
                break;
            case PARA_BOOT:
                amsBootstrapParams = KentParams.calculateBootstrap(tensors,
                        true, scriptPath);
                break;
        }
    }
    
    private void removeEmptySites() {
        // ‘Iterator.remove is the only safe way to modify a collection 
        // during iteration’
        // -- http://docs.oracle.com/javase/tutorial/collections/interfaces/collection.html
        
        for (Iterator<Site> it = sites.iterator(); it.hasNext(); ) {
            if (it.next().isEmpty()) {
                it.remove();
            }
        }
    }
    
    public double getMinDepth() {
        if (!getMeasType().isContinuous()) return Double.NaN;
        double minimum = Double.POSITIVE_INFINITY;
        for (Sample s: getSamples()) {
            final double depth = s.getDepth();
            if (depth<minimum) {
                minimum = depth;
            }
        }
        return minimum;
    }
        
    public double getMaxDepth() {
        if (!getMeasType().isContinuous()) return Double.NaN;
        double maximum = Double.NEGATIVE_INFINITY;
        for (Sample s: getSamples()) {
            final double depth = s.getDepth();
            if (depth>maximum) {
                maximum = depth;
            }
        }
        return maximum;
    }
    
    public static interface SiteNamer {
        String siteName(Sample sample);
    }
    
    public void setSitesForSamples(Collection<Sample> samples, SiteNamer siteNamer) {
        for (Sample sample: samples) {
            final Site oldSite = sample.getSite();
            final Site newSite = getOrCreateSite(siteNamer.siteName(sample));
            if (oldSite != null) {
                oldSite.removeSample(sample);
            }
            sample.setSite(newSite);
            newSite.addSample(sample);
        }
        removeEmptySites();
    }
    
    public void setNamedSiteForSamples(Collection<Sample> samples,
            final String siteName) {
        setSitesForSamples(samples, new SiteNamer() {
            public String siteName(Sample sample) {
                return siteName;
            }
        });
    }
    
    public void setSiteNamesBySubstring(Collection<Sample> samples, final BitSet charMask) {
        setSitesForSamples(samples, new SiteNamer() {
            public String siteName(Sample sample) {
                final String sampleName = sample.getNameOrDepth();
                StringBuilder sb = new StringBuilder(sampleName.length());
                for (int i=0; i<sampleName.length(); i++) {
                    if (charMask.get(i)) {
                        sb.append(sampleName.substring(i, i+1));
                    }
                }
                return sb.toString();
            }
        });
    }
    
    public void setSiteNamesByDepth(Collection<Sample> samples, final double thickness) {
        setSitesForSamples(samples, new SiteNamer() {
            public String siteName(Sample sample) {
                double minDepth = getMinDepth();
                double relDepth = sample.getDepth() - minDepth;
                double slice = Math.floor(relDepth / thickness);
                String sliceName = String.format(Locale.ENGLISH, "%.2f", slice*thickness+minDepth);
                return sliceName;
            }
        });
    }
    
    public Sample insertNewSample(String id) {
        final Sample newSample = new Sample(id, this);
        int position = -1;
        do {
            position++;
        } while (position<samples.size() &&
                    samples.get(position).getNameOrDepth().
                    compareTo(newSample.getNameOrDepth()) < 0);
        samples.add(position, newSample);
        samplesById.put(newSample.getNameOrDepth(), newSample);
        return newSample;
    }

    public boolean containsSample(String id) {
        return samplesById.containsKey(id);
    }

    public void rescaleMagSus(double factor) {
        for (Datum d: data) {
            d.setMagSus(d.getMagSus() * factor);
        }
    }
}
