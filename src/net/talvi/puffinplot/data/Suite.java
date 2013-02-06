/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.PuffinUserException;
import net.talvi.puffinplot.data.file.*;

/** A suite of data, containing a number of samples.
 * This will usually correspond to a section (for discrete studies)
 * or core (for continuous studies).
 * 
 * @author pont
 */
public final class Suite {

    private List<Datum> data;
    private List<Site> sites;
    private File puffinFile;
    private List<Sample> samples = new ArrayList<Sample>(); // samples in order
    private LinkedHashMap<String, Sample> samplesById; // name or depth as appropriate
    private HashMap<Sample, Integer> indicesBySample; // maps sample to index
    private Map<Integer, Line> dataByLine;
    private int currentSampleIndex = 0;
    private MeasType measType;
    private String suiteName;
    private List<Sample> emptyTraySamples;
    private SuiteCalcs suiteCalcs;
    private List<String> loadWarnings;
    private boolean hasUnknownTreatType;
    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private CustomFields<String> customFlagNames;
    private CustomFields<String> customNoteNames;
    private List<KentParams> amsBootstrapParams = null;
    private List<KentParams> hextParams= null;

    /** Get the list of warnings produced when data was being loaded from
     * one or more files.
     * @return the list of warnings produced when data was being loaded from
     * one or more files */
    public List<String> getLoadWarnings() {
        return Collections.unmodifiableList(loadWarnings);
    }

    private void updateReverseIndex() {
        indicesBySample = new HashMap<Sample, Integer>(getNumSamples());
        for (int i=0; i<samples.size(); i++) {
            indicesBySample.put(samples.get(i), i);
        }
    }
    
    private static SuiteCalcs calculateSuiteMeans(List<Sample> selSamps,
            List<Site> selSites) {
        List<Vec3> sampleDirs = new ArrayList<Vec3>(selSamps.size());
        for (Sample sample: selSamps) {
            PcaValues pca = sample.getPcaValues();
            if (pca != null) {
                sampleDirs.add(pca.getDirection());
            }
        }
        List<Vec3> siteDirs = new ArrayList<Vec3>(selSamps.size());
        for (Site site: selSites) {
            FisherParams fp = site.getMeanDirection();
            if (fp != null) {
                siteDirs.add(fp.getMeanDirection());
            }
        }
        return new SuiteCalcs(
                SuiteCalcs.Means.calculate(siteDirs),
                SuiteCalcs.Means.calculate(sampleDirs));
    }
    
    /** Calculates Fisher statistics on all the calculated PCA 
     * directions for samples within the suite. The Fisher parameters
     * are stored in the suite and can be retrieved with
     * {@link #getSuiteMeans()}. */
    public void calculateSuiteMeans() {
        final List<Sample> selSamps = PuffinApp.getInstance().getSelectedSamples();
        final List<Site> selSites = PuffinApp.getInstance().getSelectedSites();
        suiteCalcs = calculateSuiteMeans(selSamps, selSites);
    }

    /** Calculates and returns Fisher statistics on all the calculated PCA 
     * directions for samples within supplied suite. The Fisher parameters
     * are stored in the suite and can be retrieved with
     * {@link #getSuiteMeans()}.
     * @param suites the suites on which to calculate statistics
     * @return the results of the calculation
     */
    public static SuiteCalcs calculateMultiSuiteMeans(List<Suite> suites) {
        final List<Sample> selSamps = new ArrayList<Sample>();
        final List<Site> selSites = new ArrayList<Site>();
        for (Suite suite: suites) {
            selSamps.addAll(suite.getSamples());
            selSites.addAll(suite.getSites());
        }
        return calculateSuiteMeans(selSamps, selSites);
    }
    
    /** Performs a reversal test on a list of suites. 
     * @param suites the suites on which to perform the test.
     * @return a two-item list containing Fisher statistics for the
     * normal and reversed modes of the data in the suites, in that order
     */
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
    
    /** Returns the Fisher parameters calculated on the entire suite. 
     * @return the Fisher parameters calculated on the entire suite */
    public SuiteCalcs getSuiteMeans() {
        return suiteCalcs;
    }

    /** For each site in this suite, calculates Fisher statistics on the
     * sample PCA directions.
     * @param correction the correction to apply to the magnetic moment
     * measurements when performing the PCA calculations
     * @see #getSiteFishers()
     * @see Site#doFisher(net.talvi.puffinplot.data.Correction)
     */
    public void calculateSiteFishers(Correction correction) {
        for (Site site: getSites()) site.doFisher(correction);
    }

    /** Returns the results of the per-site Fisher statistics calculated 
     * by {@link #calculateSiteFishers(net.talvi.puffinplot.data.Correction)}.
     * @return the results of previously calculated per-site Fisher statistics
     */
    public List<FisherValues> getSiteFishers() {
        List<FisherValues> result = new ArrayList<FisherValues>(getSites().size());
        for (Site site: getSites()) {
            if (site.getFisher() != null) result.add(site.getFisher());
        }
        return result;
    }

    /** Reports whether a default PuffinPlot file is set for this suite. 
     * @return {@code true} if a default PuffinPlot file is set for this suite */
    public boolean isFilenameSet() {
        return getPuffinFile() != null;
    }

    /** If a default PuffinPlot file is set for this suite, saves the suite data
     * to that file. If not, does nothing.
     * @throws PuffinUserException if an error occurred while saving the data
     */
    public void save() throws PuffinUserException {
        if (getPuffinFile() != null) saveAs(getPuffinFile());
    }

    /** Saves the data in this suite to a specified file. The specified
     * file is also set as the default PuffinPlot file for this suite.
     * 
     * @param file the file to which to save the suite's data
     * @throws PuffinUserException if an error occurred while saving data
     */
    public void saveAs(File file) throws PuffinUserException {
        List<String> fields = DatumField.getRealFieldHeadings();

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

    /* Part of the old empty-slot correction code. Not currently used,
     * but leaving in for now. */
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

    /** Performs calculations for each sample in this suite. For each sample,
     * PCA and a great-circle fit are done, provided in each case that
     * the sample contains data points which are flagged to be used for the
     * calculation in question. The magnetic susceptibility jump temperature
     * is also calculated.
     * 
     * @param correction the correction to apply to the magnetic moment
     * data when performing the calculations
     */
    public void doSampleCalculations(Correction correction) {
        for (Sample sample: getSamples()) {
            sample.doPca(correction);
            sample.fitGreatCircle(correction);
            sample.calculateMagSusJump();
        }
    }

    /** Calculate mean directions for all suitable sites in the suite.
     * For each site, a Fisher mean and a great-circle mean may
     * be calculated. The Fisher mean is only calculated for sites
     * with a sufficient number of PCA directions for samples. The great-circle
     * mean is only calculated for sites with a sufficient number of
     * great circles fitted to samples.
     * 
     * @param correction the correction to apply to the magnetic moment
     * data when performing the calculations
     */
    public void doSiteCalculations(Correction correction) {
        // TODO we can use getSites for this now!
        final Set<Site> sitesDone = new HashSet<Site>();
        for (Sample sample : getSamples()) {
            final Site site = sample.getSite();
            if (site == null) continue;
            if (sitesDone.contains(site)) continue;
            site.doFisher(correction);
            site.calculateGreatCirclesDirection(correction);
            sitesDone.add(site);
        }
    }

    /**
     * <p>Creates a new suite from the specified files.
     * The is a convenience method for
     * {@link Suite(List, SensorLengths, TwoGeeLoader.Protocol)}
     * using the default sensor lengths (1, 1, 1), protocol 
     * ({@code NORMAL}), and Cartesian (X/Y/Z) magnetic moment fields.
     * </p>
     * 
     * @param files the files from which to load the data
     * @throws IOException if an I/O error occurred while reading the files 
     */
    public Suite(List<File> files) throws IOException {
            this(files, SensorLengths.fromPresetName("1:1:1"),
                    TwoGeeLoader.Protocol.NORMAL, false, null);
    }
    
    /**
     * <p>Creates a new suite from the specified files.</p>
     * 
     * <p>Note that this may return an empty suite, in which case it is the
     * caller's responsibility to notice this and deal with it.
     * We can't just throw an exception if the suite's empty,
     * because then we lose the load warnings (which will probably explain
     * to the user <i>why</i> the suite's empty and are thus quite important).</p>
     * 
     * @param files the files from which to load the data
     * @param sensorLengths for 2G long core files only: the effective lengths 
     * of the magnetometer's SQUID sensors,
     * used to correct Cartesian magnetic moment data
     * @param protocol for 2G files only: the measurement protocol used
     * @param usePolarMoment for 2G files only: use polar (dec/inc/int)
     * data fields instead of Cartesian ones (X/Y/Z) to determine magnetic moment
     * @param format explicitly specified file format (null to automatically
     * guess between 2G, PuffinPlot, and Zplot).
     * @throws IOException if an I/O error occurred while reading the files 
     */
    public Suite(List<File> files, SensorLengths sensorLengths,
            TwoGeeLoader.Protocol protocol, boolean usePolarMoment,
            FileFormat format) throws IOException {
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
            if (format != null) {
                // explicit format specified: use the custom format loader
                fileType = FileType.CUSTOM_TABULAR;
            } else {
                // no format specified: gues the file type
                try {
                    fileType = FileType.guess(file);
                } catch (IOException ex) {
                    loadWarnings.add(String.format("Error guessing type of file \"%s\": %s",
                            file.getName(), ex.getLocalizedMessage()));
                }
            }
            FileLoader loader = null;
            switch (fileType) {
            case TWOGEE:
            case PUFFINPLOT_OLD:
                TwoGeeLoader twoGeeLoader =
                        new TwoGeeLoader(file, protocol,
                        sensorLengths.toVector(), usePolarMoment);
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
            case CUSTOM_TABULAR:
                loader = new TabularFileLoader(file, format);
                break;
            default:
                loadWarnings.add(String.format("%s is of unknown file type.", file.getName()));
                break;
            }
            if (loader != null) {
                dataArray.ensureCapacity(dataArray.size() + loader.getData().size());
                for (Datum d: loader.getData()) {
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
        }
        processPuffinLines(puffinLines);
        updateReverseIndex();
    }
    
    /** Performs all possible sample and site calculations.
     *  Intended to be called after instantiating a new Suite from a file.
     * @param correction the correction to apply to the magnetic moment
     * data when performing the calculations
     */
    public void doAllCalculations(Correction correction) {
        doSampleCalculations(correction);
        if (measType.isDiscrete()) {
            doSiteCalculations(correction);
        }
    }
    
    /** Exports sample calculations to a specified file in CSV format.
     * @param file the file to which to write the sample calculations
     * @throws PuffinUserException if an error occurred while writing the file
     */
    public void saveCalcsSample(File file) throws PuffinUserException {
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
                    GreatCircle.getHeaders(),
                    MedianDestructiveField.getHeaders(),
                    Tensor.getHeaders());
            for (Sample sample: samples) {
                final PcaAnnotated pca = sample.getPcaAnnotated();
                final MedianDestructiveField mdf = sample.getMdf();
                final GreatCircle circle = sample.getGreatCircle();
                final Tensor ams = sample.getAms();
                writer.writeCsv(getName(), sample.getNameOrDepth(),
                        String.format(Locale.ENGLISH, "%.4g", sample.getNrm()),
                        String.format(Locale.ENGLISH, "%.4g", sample.getMagSusJump()),
                        pca == null ? PcaAnnotated.getEmptyFields() : pca.toStrings(),
                        circle == null ? GreatCircle.getEmptyFields() : circle.toStrings(),
                        mdf == null ? MedianDestructiveField.getEmptyFields() : mdf.toStrings(),
                        ams == null ? Tensor.getEmptyFields() : ams.toStrings());
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

    /** Exports site calculations to a specified file in CSV format.
     * @param file the file to which to write the site calculations
     * @throws PuffinUserException if an error occurred while writing the file
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

    /** Saves the Fisher mean direction for the whole suite to a file in CSV format
     * @param file the file to which to write the mean direction
     * @throws PuffinUserException if an error occurred while writing the file
     */
    public void saveCalcsSuite(File file) throws PuffinUserException {
        CsvWriter writer = null;
        try {
            if (suiteCalcs == null) {
                throw new PuffinUserException("There are no calculations to save.");
            }
            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv(SuiteCalcs.getHeaders());
            for (List<String> line: suiteCalcs.toStrings()) {
                writer.writeCsv(line);
            }
        } catch (IOException ex) {
           throw new PuffinUserException(ex);
        } finally {
            if (writer != null) {
                try { writer.close(); }
                catch (IOException e) { logger.warning(e.getLocalizedMessage()); }
            }
        }
    }

    /** Returns a sample from this suite with the specified name,
     * or {@code null} if no such sample exists. 
     * @param name a sample name
     * @return a sample from this suite with the specified name,
     * or {@code null} if no such sample exists
     */
    public Sample getSampleByName(String name) {
        return samplesById.get(name);
    }

    /** Returns the index defining the current sample. 
     * @return the index defining the current sample */
    public int getCurrentSampleIndex() {
        return currentSampleIndex;
    }
    
    /** Sets the index defining the current sample. 
     * @param value the index defining the current sample */
    public void setCurrentSampleIndex(int value) {
        currentSampleIndex = value;
    }

    /** Returns the current sample 
     * @return the current sample */
    public Sample getCurrentSample() {
        return getSampleByIndex(getCurrentSampleIndex());
    }
 
    /** Returns all the samples in this suite.
     * @return all the samples in this suite */
    public List<Sample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    /** Returns all the data points in this suite.
     * @return all the data points in this suite */
    public List<Datum> getData() {
        return Collections.unmodifiableList(data);
    }

    /** Returns the measurement type of this suite (discrete or continuous) 
     * @return the measurement type of this suite (discrete or continuous) */
    public MeasType getMeasType() {
        return measType;
    }

    /** Returns the name of this suite.
     * @return the name of this suite */
    public String getName() {
        return suiteName;
    }

    /** Returns the number of samples in this suite.
     * @return the number of samples in this suite */
    public int getNumSamples() {
        return samplesById.size();
    }

    /** Returns the sample with the specified index. 
     * @param i an index number for a sample
     * @return the sample with the specified index */
    public Sample getSampleByIndex(int i) {
        return samples.get(i);
    }

    /** Returns the index of a specified sample within this suite.
     * @param sample a sample in the suite
     * @return the index of the sample, or {@code -1} if not in this suite */
    public int getIndexBySample(Sample sample) {
        final Integer index =  indicesBySample.get(sample);
        return index==null ? -1 : index;
    }
    
    /** Returns the tray correction for the specified data point.
     * Not currently used.
     * @param d a data point representing a magnetic measurement
     * @return a data point containing the magnetic moment of the
     * section of the tray on which the measurement was made
     */
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

    /** Returns the name of this suite. 
     * @return the name of this suite */
    @Override
    public String toString() {
        return getName();
    }

    /** Returns strings representing data about this suite.
     * Note that this does not include data at the level of sites,
     * samples, or treatment steps.
     * @return strings representing data about this suite
     */
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

    /** Sets suite data from a string. The string must be in the format
     * of one of the strings produced by the {@link #toStrings()} method.
     * @param string a string from which to read suite data
     */
    public void fromString(String string) {
        String[] parts = string.split("\t");
        if ("CUSTOM_FLAG_NAMES".equals(parts[0])) {
            customFlagNames = new CustomFlagNames(Arrays.asList(parts).subList(1, parts.length));
        } else if ("CUSTOM_NOTE_NAMES".equals(parts[0])) {
            customNoteNames = new CustomNoteNames(Arrays.asList(parts).subList(1, parts.length));
        }
    }

    private double getFormAz() {
        for (Sample s: samples) {
            final double v = s.getFormAz();
            if (!Double.isNaN(v)) return v;
        }
        return 0;
    }

    private double getFormDip() {
        for (Sample s: samples) {
            final double v = s.getFormDip();
            if (!Double.isNaN(v)) return v;
        }
        return 0;
    }

    private double getMagDev() {
        for (Sample s: samples) {
            final double v = s.getMagDev();
            if (!Double.isNaN(v)) return v;
        }
        return 0;
    }

    /**
     * <p>Imports AMS data from a whitespace-delimited file.
     * If {@code directions==false}, line format is k11 k22 k33 k12 k23 k13 
     * (tensor components) otherwise it's
     * inc1 dec1 inc2 dec2 inc3 dec3 (axis directions, decreasing magnitude).
     * If there's no sample in the suite from which to take the sample
     * and formation corrections, importAmsWithDialog will try to read them as
     * fields appended to the end of the line.</p>
     * 
     * <p>Not currently used in PuffinPlot.</p>
     * 
     * @param files the files from which to read the data
     * @param directions {@code true} to read axis directions,
     * {@code false} to read tensor components
     * @throws IOException if there was an I/O error reading the files 
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
        updateReverseIndex();
    }

    /** Imports AMS data from ASC files in the format produced by
     * Agico's SAFYR program.
     * 
     * @param files the ASC files to read
     * @param magneticNorth {@code true} if the sample dip azimuths 
     * in the file are relative to magnetic north; {@code false} if
     * they are relative to geographic north
     * @throws IOException if an I/O error occurred while reading the file
     */
    public void importAmsFromAsc(List<File> files, boolean magneticNorth)
            throws IOException {
        List<AmsData> allData = new ArrayList<AmsData>();
        for (File file: files) {
            AmsLoader amsLoader = new AmsLoader(file);
            allData.addAll(amsLoader.readFile());
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
        }
        updateReverseIndex();
    }

    /** Exports a subset of this suite's data to multiple files, one file
     * per sample. The files are in a tab-delimited text format.
     * @param directory the directory in which to create the files
     * @param fields the fields to export
     */
    public void exportToFiles(File directory, List<DatumField> fields) {
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
            final File outFile = new File(directory, s.getNameOrDepth());
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

    /** Returns the names (titles) of the custom flags for this suite.
     * @return the names (titles) of the custom flags for this suite  */
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

    /** Returns the names (titles) of the custom notes for this suite.
     * @return the names (titles) of the custom notes for this suite  */
    public CustomFields<String> getCustomNoteNames() {
        return customNoteNames;
    }

    /** Returns the parameters of the last AMS bootstrap statistics
     * (if any) calculated on this suite's data.
     * @return the parameters of the last AMS bootstrap statistics
     * (if any) calculated on this suite's data */
    public List<KentParams> getAmsBootstrapParams() {
        return amsBootstrapParams;
    }

    /** Returns the parameters of the last AMS Hext statistics
     * (if any) calculated on this suite's data.
     * @return the parameters of the last AMS Hext statistics
     * (if any) calculated on this suite's data */
    public List<KentParams> getAmsHextParams() {
        return hextParams;
    }

    /** Clears any AMS calculations on this suite */
    public void clearAmsCalculations() {
        amsBootstrapParams = null;
        hextParams = null;
    }

    /** Returns the sites within this suite. 
     * @return the sites within this suite */
    public List<Site> getSites() {
        return Collections.unmodifiableList(sites);
    }

    /** Returns a site with the given name, or {@code null} if
     * this suite contains no such site.
     * @param siteName a site name
     * @return a site with the given name, or {@code null} if
     * this suite contains no such site
     */
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

    /** Returns the name of the PuffinPlot file associated with this suite, if any. 
     * @return the name of the PuffinPlot file associated with this suite, if any */
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
        
    /** The type of a statistical calculation on AMS tensors. */
    public static enum AmsCalcType {
        /** Hext statistics*/
        HEXT,
        /** nonparametric bootstrap statistics */
        BOOT,
        /** parametric bootstrap statistics */
        PARA_BOOT }; 

    
    /** Calculates and stores AMS statistics using an external script.
     * 
     * @param samples the samples on which to calculate statistics
     * @param calcType the type of AMS calculation to perform
     * @param scriptPath the filesystem path of the script which will perform the calculation
     * @throws IOException if there was an error running the script or reading its output
     * @throws IllegalArgumentException if the samples contain insufficient AMS data
     */
    public void calculateAmsStatistics(List<Sample> samples, AmsCalcType calcType,
            String scriptPath) throws IOException, IllegalArgumentException {
        /* It may not be immediately obvious why this should be an instance
         * method of Suite. In fact the only reason for this is that it
         * stores its results in Suite. This is probably OK. The main deficiency
         * of the current model for AMS data is that it only allows one set of
         * data to be stored at a time. However, even if we improve that to
         * allow multiple sets of AMS data, the Suite is the natural place
         * to store them (since the sets of samples for AMS calculations
         * isn't necessarily tied to a single Site).
         */
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
    
    /**
     * For a continuous suite, returns the minimum depth of a sample
     * within the suite. 
     * @return the minimum depth of a sample within the suite */
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
        
    /** For a continuous suite, returns the maximum depth of a sample within the suite. 
     * @return the maximum depth of a sample within the suite */
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
    
    /** Clears all sites for this suite.
     */
    public void clearSites() {
        sites = new ArrayList<Site>();
        for (Sample s: samples) {
            s.setSite(null);
        }
    }
    
    /** A SiteNamer turns a sample name into a site name. It is used
     * to automatically define site names for a number of samples according
     * to a pre-programmed scheme.
     */
    public static interface SiteNamer {
        /** Determines a site name from a sample name. 
         * @param sample the name of a sample
         * @return the name of a site which should contain the sample with the
         * specified name
         */
        String siteName(Sample sample);
    }
    
    /** Sets sites for supplied samples according to a supplied site namer.
     * Where a site with the required name exists, it will be used; 
     * otherwise a new site with the required name will be created.
     * @param samples the samples for which to set sites
     * @param siteNamer the site namer which will produce the site names
     */
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
    
    /** Explicitly sets a site for the specified samples.
     * If a site with the requested name exists, it will be used;
     * otherwise a new site with that name will be created.
     * @param samples the samples for which to set the site
     * @param siteName the name of the site into which to put the samples
     */
    public void setNamedSiteForSamples(Collection<Sample> samples,
            final String siteName) {
        setSitesForSamples(samples, new SiteNamer() {
            @Override
            public String siteName(Sample sample) {
                return siteName;
            }
        });
    }
    
    /** Sets site names for samples according to chosen characters from the
     * sample names. The caller supplies a bit-set; for each sample name,
     * the site name is determined by taking the characters of the sample
     * name for which the corresponding bit is set.
     * 
     * @param samples the samples for which to set sites
     * @param charMask the mask determining which characters to use for the site name
     */
    public void setSiteNamesBySubstring(Collection<Sample> samples, final BitSet charMask) {
        setSitesForSamples(samples, new SiteNamer() {
            @Override
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
    
    /** Sets site names for a continuous suite according to the depth of the samples.
     * A thickness is specified to the method, and the suite is divided into
     * sites of that thickness. Each site is named for the shallowest depth
     * within it.
     * @param samples the samples for which to set site names
     * @param thickness the thickness of each site */
    public void setSiteNamesByDepth(Collection<Sample> samples, final double thickness) {
        setSitesForSamples(samples, new SiteNamer() {
            @Override
            public String siteName(Sample sample) {
                double minDepth = getMinDepth();
                double relDepth = sample.getDepth() - minDepth;
                double slice = Math.floor(relDepth / thickness);
                String sliceName = String.format(Locale.ENGLISH, "%.2f", slice*thickness+minDepth);
                return sliceName;
            }
        });
    }
    
    /** Creates a new sample and adds it to this suite.
     * The sample's position is determined by
     * its name. Provided that the suite is sorted by sample name (or depth),
     * the sample will be inserted at its correct position according to 
     * that sorting. Note that the reverse (sample-to-index) map is not
     * updated; this must be done manually with a call to
     * updateReverseIndex().
     * @param id the identifier or name of the new sample
     * @return a new sample with the supplied identifier
     */
    private Sample insertNewSample(String id) {
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

    /** Determine whether this suite contain a same with a specified
     * identifier (name).
     * @param id a sample identifier
     * @return {@code true} if this suite contains a sample with the
     * specified identifier
     */
    public boolean containsSample(String id) {
        return samplesById.containsKey(id);
    }

    /** Multiplies all magnetic susceptibility measurements in this
     * suite by the specified factor.
     * @param factor a factor by which to multiply all the magnetic
     * susceptibility measurements in this suite
     */
    public void rescaleMagSus(double factor) {
        for (Datum d: data) {
            d.setMagSus(d.getMagSus() * factor);
        }
    }
}
