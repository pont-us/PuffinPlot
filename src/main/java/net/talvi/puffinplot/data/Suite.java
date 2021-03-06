/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.talvi.puffinplot.PuffinUserException;
import net.talvi.puffinplot.data.file.AmsLoader;
import net.talvi.puffinplot.data.file.CaltechLoader;
import net.talvi.puffinplot.data.file.IapdLoader;
import net.talvi.puffinplot.data.file.Jr6Loader;
import net.talvi.puffinplot.data.file.LoadedData;
import net.talvi.puffinplot.data.file.PmdLoader;
import net.talvi.puffinplot.data.file.PplLoader;
import net.talvi.puffinplot.data.file.TabularFileLoader;
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.data.file.UcDavisLoader;
import net.talvi.puffinplot.data.file.ZplotLoader;
import net.talvi.puffinplot.data.file.FileLoader;

/**
 * A suite of data, containing a number of samples. This will usually correspond
 * to a section (for discrete studies) or core (for continuous studies).
 */
public final class Suite implements SampleGroup {

    private List<Site> sites = new ArrayList<>();
    private File puffinFile;
    private final List<Sample> samples = new ArrayList<>(); // samples in order
    private LinkedHashMap<String, Sample> samplesById =
            new LinkedHashMap<>(); // name or depth as appropriate
    private HashMap<Sample, Integer> indicesBySample; // maps sample to index
    private int currentSampleIndex = -1;
    private MeasurementType measurementType = MeasurementType.UNSET;
    private String name;
    private List<Sample> emptyTraySamples;
    private SuiteCalcs suiteCalcs;
    private boolean hasUnknownTreatType = false;
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    private CustomFlagNames customFlagNames =
            new CustomFlagNames(Collections.<String>emptyList());
    private CustomNoteNames customNoteNames =
            new CustomNoteNames(Collections.<String>emptyList());
    private List<KentParams> amsBootstrapParams = null;
    private List<KentParams> hextParams = null;
    private boolean saved = true;
    private Date creationDate;
    private FileType originalFileType = FileType.UNKNOWN;
    private static final DateFormat ISO_8601_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private Date modificationDate;
    private final String suiteCreator;
    private String fileCreator;
    private final Set<SavedListener> savedListenerSet = new HashSet<>();

    /**
     * Update this suit's internal index mapping samples to their indices
     * within the suite. This method is only public to make it usable from
     * testing code when constructing artificial suites; normal users of
     * the Suite API do not need to call it explicitly.
     * 
     */
    public void updateReverseIndex() {
        indicesBySample = new HashMap<>(getNumSamples());
        for (int i = 0; i < samples.size(); i++) {
            indicesBySample.put(samples.get(i), i);
        }
    }

    /**
     * Calculates Fisher statistics on all the calculated PCA directions for
     * samples within the suite. The Fisher parameters are stored in the suite
     * and can be retrieved with {@link #getSuiteMeans()}.
     *
     * @param selSamples samples for which to calculate means
     * @param selSites sites for which to calculate means
     */
    public void calculateSuiteMeans(List<Sample> selSamples,
            List<Site> selSites) {
        suiteCalcs = doCalculateSuiteMeans(selSamples, selSites);
    }
    
    private static SuiteCalcs doCalculateSuiteMeans(List<Sample> selSamples,
            List<Site> selSites) {
        final List<Vec3> sampleDirs = new ArrayList<>(selSamples.size());
        for (Sample sample : selSamples) {
            if (sample.getDirection() != null) {
                sampleDirs.add(sample.getDirection());
            }
        }
        final List<Vec3> siteDirs = new ArrayList<>(selSamples.size());
        for (Site site : selSites) {
            final FisherParams fp = site.getFisherParams();
            if (fp != null) {
                siteDirs.add(fp.getMeanDirection());
            }
        }
        final List<Vec3> sampleVgps = new ArrayList<>(selSamples.size());
        for (Sample sample : selSamples) {
            if (sample.getSite() != null &&
                    sample.getSite().getLocation() != null &&
                    sample.getDirection() != null &&
                    sample.getDirection().isFinite()) {
                final double alpha95 = 0; // arbitrary
                sampleVgps.add(VGP.calculate(sample.getDirection(), alpha95,
                        sample.getSite().getLocation()).getLocation().toVec3());
            }
        }
        final List<Vec3> siteVgps = new ArrayList<>(selSamples.size());
        for (Site site : selSites) {
            if (site.getVgp() != null) {
                siteVgps.add(site.getVgp().getLocation().toVec3());
            }
        }
        
        return new SuiteCalcs(
                SuiteCalcs.Means.calculate(siteDirs),
                SuiteCalcs.Means.calculate(sampleDirs),
                SuiteCalcs.Means.calculate(siteVgps),
                SuiteCalcs.Means.calculate(sampleVgps));
    }
    
    /**
     * Calculates and returns Fisher statistics on all the calculated PCA
     * directions for samples within supplied suites.
     *
     * @param  suites the suites on which to calculate statistics
     * @return        the results of the calculation
     */
    public static SuiteCalcs calculateMultiSuiteMeans(List<Suite> suites) {
        final List<Sample> selSamps = new ArrayList<>();
        final List<Site> selSites = new ArrayList<>();
        for (Suite suite : suites) {
            selSamps.addAll(suite.getSamples());
            selSites.addAll(suite.getSites());
        }
        return doCalculateSuiteMeans(selSamps, selSites);
    }
    
    /**
     * Performs a reversal test on a list of suites.
     *
     * @param suites the suites on which to perform the test.
     * @return a two-item list containing Fisher statistics for the normal and
     * reversed modes of the data in the suites, in that order
     */
    public static List<FisherValues> doReversalTest(List<Suite> suites) {
        final List<Vec3> normal = new ArrayList<>(),
                reversed = new ArrayList<>();
        for (Suite suite : suites) {
            for (Sample sample : suite.getSamples()) {
                final Vec3 vector = sample.getDirection();
                if (vector != null) {
                    (vector.z > 0 ? normal : reversed).add(vector);
                }
            }
        }
        return Arrays.asList(FisherValues.calculate(normal),
                FisherValues.calculate(reversed));
    }
    
    /**
     * Returns the Fisher parameters calculated on the entire suite.
     *
     * @return the Fisher parameters calculated on the entire suite
     */
    public SuiteCalcs getSuiteMeans() {
        return suiteCalcs;
    }

    /**
     * For each site in this suite, calculates Fisher statistics on the sample
     * PCA directions.
     *
     * @param correction the correction to apply to the magnetic moment
     * measurements when performing the PCA calculations
     * @see #getSiteFishers()
     * @see Site#calculateFisherStats(net.talvi.puffinplot.data.Correction)
     */
    public void calculateSiteFishers(Correction correction) {
        setSaved(false);
        for (Site site : getSites()) {
            site.calculateFisherStats(correction);
        }
    }

    /**
     * Returns the results of the per-site Fisher statistics calculated by
     * {@link #calculateSiteFishers(net.talvi.puffinplot.data.Correction)}.
     *
     * @return the results of previously calculated per-site Fisher statistics
     */
    public List<FisherValues> getSiteFishers() {
        final List<FisherValues> result = new ArrayList<>(getSites().size());
        for (Site site : getSites()) {
            if (site.getFisherValues() != null) {
                result.add(site.getFisherValues());
            }
        }
        return result;
    }

    /**
     * Reports whether a default PuffinPlot file is set for this suite.
     *
     * @return {@code true} if a default PuffinPlot file is set for this suite
     */
    public boolean isFilenameSet() {
        return getPuffinFile() != null;
    }

    /**
     * If a default PuffinPlot file is set for this suite, saves the suite data
     * to that file. If not, does nothing.
     *
     * @throws PuffinUserException if an error occurred while saving the data
     */
    public void save() throws PuffinUserException {
        if (getPuffinFile() != null) {
            saveAs(getPuffinFile());
        }
    }

    /**
     * Saves the data in this suite to a specified file. The specified file is
     * also set as the default PuffinPlot file for this suite.
     *
     * @param file the file to which to save the suite's data
     * @throws PuffinUserException if an error occurred while saving data
     */
    public void saveAs(File file)
            throws PuffinUserException {
        final List<String> fields = TreatmentParameter.getRealFieldStrings();

        try (OutputStream stream = new FileOutputStream(file);
                OutputStreamWriter writer =
                        new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                CsvWriter csvWriter = new CsvWriter(writer, "\t");) {
            
            writer.write("PuffinPlot file. Version 3\n");
            csvWriter.writeCsv(fields);

            for (Sample sample : getSamples()) {
                for (TreatmentStep treatmentStep : sample.getTreatmentSteps()) {
                    csvWriter.writeCsv(treatmentStep.toStrings());
                }
            }

            writer.write("\n");
            for (Sample sample : getSamples()) {
                List<String> lines = sample.toStrings();
                for (String line : lines) {
                    String w = String.format(Locale.ENGLISH, "SAMPLE\t%s\t%s\n",
                            sample.getNameOrDepth(), line);
                    writer.write(w);
                }
            }
            for (Site site : getSites()) {
                List<String> lines = site.toStrings();
                for (String line : lines) {
                    String w = String.format(Locale.ENGLISH, "SITE\t%s\t%s\n",
                            site.getName(), line);
                    writer.write(w);
                }
            }
            if (!saved) {
                modificationDate = new Date();
            }
            for (String line : toStrings()) {
                writer.write(String.format(Locale.ENGLISH, "SUITE\t%s\n",
                        line));
            }
            puffinFile = file;
            name = file.getName();
            setSaved(true);
        } catch (IOException ex) {
            throw new PuffinUserException(ex);
        }
    }

    /**
     * Adds a datum to the suite.
     *
     * If no corresponding sample exists, one is created. The measurement type
     * of the datum must be compatible with that of the suite -- that is, either
     * they must be the same, or the suite's measurement type must be
     * {@code UNSET}. In the latter case, the suite's measurement type will be
     * set to that of the supplied datum.
     *
     * @param step the datum to add
     * @throws IllegalArgumentException if d is null, or if the measurement type
     * of is invalid (i.e. it is null, UNSET, or incompatible with this suite's
     * measurement type)
     */
    public void addTreatmentStep(TreatmentStep step) {
        Objects.requireNonNull(step);
        Objects.requireNonNull(step.getMeasurementType());
        if (step.getMeasurementType() == MeasurementType.UNSET) {
            throw new IllegalArgumentException(
                    "Measurement type may not be UNSET");
        }
        if (measurementType == MeasurementType.UNSET) {
            measurementType = step.getMeasurementType();
        }
        if (step.getMeasurementType() != measurementType) {
            throw new IllegalArgumentException(String.format(
                    "Can't add a %s datum to a %s suite.",
                    step.getMeasurementType().getNiceName().toLowerCase(),
                    getMeasurementType().getNiceName().toLowerCase()));
        }
        if (step.getTreatmentType() == TreatmentType.UNKNOWN) {
            hasUnknownTreatType = true;
        }
        final String datumName = step.getIdOrDepth();
        Sample sample = samplesById.get(datumName);
        if (sample == null) {
            sample = new Sample(datumName, this);
            samplesById.put(datumName, sample);
            samples.add(sample);
        }
        step.setSuite(this);
        sample.addTreatmentStep(step);
    }

    private List<File> expandDirs(List<File> files) {
        final List<File> result = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                result.addAll(expandDirs(Arrays.asList(file.listFiles())));
            } else {
                result.add(file);
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Performs calculations for each sample in this suite. For each sample, PCA
     * and a great-circle fit are done, provided in each case that the sample
     * contains data points which are flagged to be used for the calculation in
     * question. The magnetic susceptibility jump temperature is also
     * calculated.
     *
     * @param correction the correction to apply to the magnetic moment data
     * when performing the calculations
     */
    public void doSampleCalculations(Correction correction) {
        setSaved(false);
        for (Sample sample : getSamples()) {
            sample.doPca(correction);
            sample.fitGreatCircle(correction);
            sample.calculateMagSusJump();
        }
    }

    /**
     * Calculates mean directions for all suitable sites in the suite.
     * For each site, a Fisher mean and a great-circle mean may
     * be calculated. The Fisher mean is only calculated for sites
     * with a sufficient number of PCA directions for samples. The great-circle
     * mean is only calculated for sites with a sufficient number of
     * great circles fitted to samples.
     * 
     * @param correction the correction to apply to the magnetic moment
     *     data when performing the calculations
     * @param greatCirclesValidityCondition an expression which is evaluated
     *     to determine whether a great-circles direction is considered valid
     *     (see {@link net.talvi.puffinplot.data.GreatCircles#instance(java.util.List, java.util.List, java.lang.String)}).
     */
    public void doSiteCalculations(Correction correction,
            String greatCirclesValidityCondition) {
        setSaved(false);
        // TODO we can use getSites for this now!
        final Set<Site> sitesDone = new HashSet<>();
        for (Sample sample : getSamples()) {
            final Site site = sample.getSite();
            if (site == null) {
                continue;
            }
            if (sitesDone.contains(site)) {
                continue;
            }
            site.calculateFisherStats(correction);
            site.calculateGreatCirclesDirection(correction,
                    greatCirclesValidityCondition);
            sitesDone.add(site);
        }
    }

    /**
     * Creates a new, empty suite.
     * 
     * @param creator a string identifying the program and version creating the
     *     suite
     */
    public Suite(String creator) {
        suiteCreator = creator;
        fileCreator = creator;
        name = "[Empty suite]";
        creationDate = new Date();
        modificationDate = new Date();
        /*
         * If we subsequently read a PuffinPlot file, file creator and creation
         * date will be overwritten by the stored values.
         */
    }
    
    /**
     * Convenience method for reading PuffinPlot files.This method is a wrapper
     * for the fully specified method {@link #readFiles(List, FileType,
     * Map)} which provides defaults for all the arguments
     * except for the list of file names.
     * <p>
     * The filetype is set to PUFFINPLOT_NEW.
     *
     * @param files files to read
     * @return a (possibly empty) list of messages generated during loading
     * @throws IOException if an exception occurs during file reading
     */
    public List<String> readFiles(List<File> files) throws IOException {
        return readFiles(files, FileType.PUFFINPLOT_NEW,
                Collections.emptyMap());
    }

    /**
     * Reads data into this suite from the specified files. After readFiles
     * returns, #getLoadWarnings() can be used to return a list of problems that
     * occurred during file reading.
     *
     * @param files the files from which to read the data (non-null, non-empty)
     * @param fileType type of the specified files
     * @param importOptions extra options passed to file importers
     * @return a (possibly empty) list of messages generated during loading
     * @throws IOException if an I/O error occurred while reading the files 
     */
    public List<String> readFiles(List<File> files, FileType fileType,
            Map<String, Object> importOptions) throws IOException {
        Objects.requireNonNull(files, "files may not be null");
        if (files.isEmpty()) {
            throw new IllegalArgumentException("File list must be non-empty.");
        }

        if (isEmpty()) { // only set the name if suite is empty
            if (files.size() == 1) {
                name = files.get(0).getName();
            } else {
                name = files.get(0).getParentFile().getName();
            }
        }
        
        /*
         * Remember whether the suite was initially empty -- this is needed to
         * correctly set the saved state later.
         */
        final boolean wasEmpty = isEmpty();
        final List<String> loadWarnings = new ArrayList<>();
        
        files = expandDirs(files);
        final ArrayList<TreatmentStep> tempDataList = new ArrayList<>();
        List<String> puffinLines = Collections.emptyList();

        /*
         * If fileType is PUFFINPLOT_NEW, originalFileType can be overwritten by
         * value specified in file.
         */
        originalFileType = fileType;
        
        for (File file: files) {
            if (!file.exists()) {
                loadWarnings.add(String.format(Locale.ENGLISH,
                        "File \"%s\" does not exist.", file.getName()));
                continue;
            }
            if (!file.canRead()) {
                loadWarnings.add(String.format(Locale.ENGLISH,
                        "File \"%s\" is unreadable.", file.getName()));
                continue;
            }

            FileLoader loader;
            final Map<String, Object> options = new HashMap<>(importOptions);
            LoadedData loadedData = null;
            switch (fileType) {
            case TWOGEE:
                loader = new TwoGeeLoader();
                loadedData = loader.readFile(file, options);
                break;
            case PUFFINPLOT_OLD:
                loader = new TwoGeeLoader();
                loadedData = loader.readFile(file, options);
                if (files.size() == 1) {
                    puffinFile = file;
                }
                break;
            case PUFFINPLOT_NEW:
                loader = new PplLoader();
                loadedData = loader.readFile(file, options);
                if (files.size() == 1) {
                    puffinFile = file;
                }
                break;
            case ZPLOT:
                loader = new ZplotLoader();
                loadedData = loader.readFile(file, options);
                break;
            case CALTECH:
                loader = new CaltechLoader();
                loadedData = loader.readFile(file, options);
                break;
            case IAPD:
                loader = new IapdLoader();
                loadedData = loader.readFile(file, options);
                break;
            case UCDAVIS:
                loader = new UcDavisLoader();
                loadedData = loader.readFile(file, options);
                break;
            case CUSTOM_TABULAR:
                loader = new TabularFileLoader();
                loadedData = loader.readFile(file, options);
                break;
            case PMD_ENKIN:
                loader = new PmdLoader();
                loadedData = loader.readFile(file, options);
                break;
            case JR6:
                loader = new Jr6Loader();
                loadedData = loader.readFile(file, options);
                break;
            case DIRECTIONS:
                return readDirectionalData(files); // NB: return, not break
            default:
                loadWarnings.add(String.format(Locale.ENGLISH,
                        "%s is of unknown file type.", file.getName()));
                break;
            }
            
            if (loadedData != null) {
                final List<TreatmentStep> loadedSteps =
                        loadedData.getTreatmentSteps();
                final Set<MeasurementType> measTypes =
                        TreatmentStep.collectMeasurementTypes(loadedSteps);
                
                boolean dataIsOk = true;
                if (measTypes.contains(MeasurementType.DISCRETE) &&
                        measTypes.contains((MeasurementType.CONTINUOUS))) {
                    /*
                     * The loaded file mixes measurement types. This should
                     * never happen in normal circumstances, but it's
                     * conceivable that, for example, a user concatenated
                     * incompatible files by mistake.
                     */
                    dataIsOk = false;
                    loadWarnings.add(String.format(Locale.ENGLISH,
                        "%s mixes discrete and continuous measurements.\n"
                                + "Ignoring this file.", file.getName()));
                    
                } else if (getMeasurementType().isActualMeasurement() &&
                        !measTypes.contains(getMeasurementType())) {
                    
                    final MeasurementType loadedType =
                            measTypes.contains(MeasurementType.CONTINUOUS) ?
                            MeasurementType.CONTINUOUS :
                            MeasurementType.DISCRETE;
                    dataIsOk = false;
                    loadWarnings.add(String.format(Locale.ENGLISH,
                            "%s contains %s measurements, \n"
                                    + "but the suite contains %s data.\n"
                                    + "Ignoring this file.", file.getName(),
                                    loadedType.getNiceName().toLowerCase(),
                                    getMeasurementType().getNiceName().
                                            toLowerCase()));
                }
                
                if (dataIsOk) {
                    tempDataList.ensureCapacity(tempDataList.size() +
                            loadedSteps.size());
                    for (TreatmentStep step : loadedSteps) {
                        // TODO: check for matching measurement type here
                        if (!step.ignoreOnLoading()) addTreatmentStep(step);
                    }
                    loadWarnings.addAll(loadedData.getMessages());
                    puffinLines = loadedData.getExtraLines();
                }
            }
        }
        
        setCurrentSampleIndex(0);
        if (hasUnknownTreatType) {
            loadWarnings.add(
                    "One or more treatment types were not recognized.");
        }
        if (measurementType.isDiscrete()) {
            emptyTraySamples = new ArrayList<>();
            int slot = 1;
            while (true) {
                final String slotId = "TRAY" + slot;
                if (!samplesById.containsKey(slotId)) {
                    break;
                }
                emptyTraySamples.add(samplesById.get(slotId));
                slot++;
            }
        }
        processPuffinLines(puffinLines);
        updateReverseIndex();
        
        /*
         * A suite isn't considered "unsaved" if it was empty before this file
         * was loaded.
         */
        setSaved(wasEmpty);
        return loadWarnings;
    }

    /**
     * Determines whether this suite is empty.
     * 
     * An empty suite is one that contains no demagnetization data.
     * 
     * @return {@code true} iff this suite is empty
     */
    public boolean isEmpty() {
        return samples.isEmpty();
    }
    
    /**
     * Reads sample direction data from text files.Text files should have no
     * header lines.
     *
     * Columns should be separated by commas, spaces, or tabs. Column 1 is
     * sample name, column 2 is declination in degrees, column 3 is inclination
     * in degrees.
     *
     * @param files files from which to read
     * @return a (possibly empty) list of messages generated during loading
     * @throws IOException if an error occurred while reading the files
     */
    public List<String> readDirectionalData(Collection<File> files)
            throws IOException {
        final List<String> loadWarnings = new ArrayList<>();
        for (File file : files) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    final String[] parts = line.split("[ \t,]+");
                    final String sampleName = parts[0];
                    final double dec = Double.parseDouble(parts[1]);
                    final double inc = Double.parseDouble(parts[2]);
                    final Vec3 v = Vec3.fromPolarDegrees(1., inc, dec);
                    final Sample sample = new Sample(sampleName, this);
                    sample.setImportedDirection(v);
                    addSample(sample, sampleName);
                }
            } catch (NumberFormatException exception) {
                loadWarnings.add(String.format("Unreadable data in file \"%s\"",
                        file.getName()));
            }
        }
        measurementType = MeasurementType.CONTINUOUS;
        for (Sample sample: samples) {
            try {
                Double.parseDouble(sample.getNameOrDepth());
            } catch (NumberFormatException ex) {
                measurementType = MeasurementType.DISCRETE;
                break;
            }
        }
        updateReverseIndex();
        return loadWarnings;
    }

    void addSample(Sample sample, final String sampleName) {
        samples.add(sample);
        samplesById.put(sampleName, sample);
    }
    
    /**
     * Performs all possible sample and site calculations. Intended to be called
     * after loading suite data from a file.
     *
     * @param correction the correction to apply to the magnetic moment data
     * when performing the calculations
     * @param greatCirclesValidityCondition an expression which is evaluated to
     * determine whether a great-circles direction is considered valid (see
     * {@link net.talvi.puffinplot.data.GreatCircles#instance(java.util.List, java.util.List, java.lang.String)}).
     */
    public void doAllCalculations(Correction correction,
            String greatCirclesValidityCondition) {
        setSaved(false);
        doSampleCalculations(correction);
        doSiteCalculations(correction, greatCirclesValidityCondition);
    }
    
    /**
     * Exports sample calculations to a specified file in CSV format.
     *
     * @param file the file to which to write the sample calculations
     * @throws PuffinUserException if an error occurred while writing the file
     */
    public void saveCalcsSample(File file) throws PuffinUserException {
        if (samples.isEmpty()) {
            throw new PuffinUserException("No samples in suite.");
        }
            
        try (FileWriter fw = new FileWriter(file);
                CsvWriter writer = new CsvWriter(fw)) {
            writer.writeCsv("Suite", measurementType.getColumnHeader(),
                    "NRM intensity (A/m)",
                    "MS jump temp. (degC)",
                    "Steps",
                    PcaAnnotated.getHeaders(),
                    GreatCircle.getHeaders(),
                    MedianDestructiveField.getHeaders(),
                    FisherValues.getHeaders(),
                    Tensor.getHeaders(),
                    customFlagNames.toStrings(),
                    customNoteNames.toStrings(),
                    "Initial MS");
            for (Sample sample: samples) {
                final PcaAnnotated pca = sample.getPcaAnnotated();
                final MedianDestructiveField mdf = sample.getMdf();
                final GreatCircle circle = sample.getGreatCircle();
                final FisherValues fisher = sample.getFisherValues();
                final Tensor ams = sample.getAms();
                writer.writeCsv(getName(), sample.getNameOrDepth(),
                        String.format(Locale.ENGLISH, "%.4g", sample.getNrm()),
                        String.format(Locale.ENGLISH, "%.4g",
                                sample.getMagSusJump()),
                        sample.getTreatmentSteps().size(),
                        pca == null ? PcaAnnotated.getEmptyFields() :
                                pca.toStrings(),
                        circle == null ? GreatCircle.getEmptyFields() :
                                circle.toStrings(),
                        mdf == null ? MedianDestructiveField.getEmptyFields() :
                                mdf.toStrings(),
                        fisher == null ? FisherValues.getEmptyFields() :
                                fisher.toStrings(),
                        ams == null ? Tensor.getEmptyFields() :
                                ams.toStrings(),
                        sample.getCustomFlags().toStrings(),
                        sample.getCustomNotes().toStrings(),
                        sample.getTreatmentSteps().isEmpty() ? ""
                            : String.format(Locale.ENGLISH, "%.4g",
                                sample.getTreatmentStepByIndex(0).getMagSus())
                );
            }
        } catch (IOException ex) {
            throw new PuffinUserException(ex);
        }
    }

    /**
     * Exports site calculations to a specified file in CSV format.
     *
     * @param file the file to which to write the site calculations
     * @throws PuffinUserException if an error occurred while writing the file
     */
    public void saveCalcsSite(File file) throws PuffinUserException {
        if (getSites().isEmpty()) {
            // A null check here would be dead code: sites is never null.
            throw new PuffinUserException("No sites are defined.");
        }
        try (FileWriter fw = new FileWriter(file);
                CsvWriter writer = new CsvWriter(fw)) {
            writer.writeCsv("Site", "Samples",
                    FisherValues.getHeaders(), GreatCircles.getHeaders(),
                    Site.getGreatCircleLimitHeader(),
                    Location.getHeaders(), VGP.getHeaders());
            for (Site site: getSites()) {
                final List<String> fisherCsv = (site.getFisherValues() == null)
                        ? FisherValues.getEmptyFields()
                        : site.getFisherValues().toStrings();
                final List<String> gcCsv = (site.getGreatCircles() == null)
                        ? GreatCircles.getEmptyFields()
                        : site.getGreatCircles().toStrings();
                final List<String> gcCsv2 = (site.getGreatCircles() == null)
                        ? Collections.nCopies(4, "")
                        : site.getGreatCircleLimitStrings();
                final List<String> locCsv = (site.getLocation()== null)
                        ? Location.getEmptyFields()
                        : site.getLocation().toStrings();
                final List<String> vgpCsv = (site.getVgp()== null)
                        ? VGP.getEmptyFields()
                        : site.getVgp().toStrings();                
                writer.writeCsv(site,
                        Integer.toString(site.getSamples().size()),
                        fisherCsv, gcCsv, gcCsv2, locCsv, vgpCsv);
            }
        } catch (IOException ex) {
           throw new PuffinUserException(ex);
        }
    }

    /**
     * Saves the Fisher mean direction for the whole suite to a file in CSV
     * format
     *
     * @param file the file to which to write the mean direction
     * @throws PuffinUserException if an error occurred while writing the file
     */
    public void saveCalcsSuite(File file) throws PuffinUserException {
        if (suiteCalcs == null) {
            throw new PuffinUserException("There are no calculations to save.");
        }
        try (FileWriter fw = new FileWriter(file);
                CsvWriter writer = new CsvWriter(fw)) {
            writer.writeCsv(SuiteCalcs.getHeaders());
            for (List<String> line: suiteCalcs.toStrings()) {
                writer.writeCsv(line);
            }
        } catch (IOException ex) {
           throw new PuffinUserException(ex);
        }
    }

    /**
     * Returns a sample from this suite with the specified name, or {@code null}
     * if no such sample exists.
     *
     * @param name a sample name
     * @return a sample from this suite with the specified name, or {@code null}
     * if no such sample exists
     */
    public Sample getSampleByName(String name) {
        return samplesById.get(name);
    }

    /**
     * Returns the index defining the current sample.
     *
     * @return the index defining the current sample
     */
    public int getCurrentSampleIndex() {
        return currentSampleIndex;
    }
    
    /**
     * Sets the index defining the current sample.
     *
     * @param value the index defining the current sample
     */
    public void setCurrentSampleIndex(int value) {
        currentSampleIndex = value;
    }

    /**
     * Returns the current sample
     *
     * @return the current sample
     */
    public Sample getCurrentSample() {
        return getSampleByIndex(getCurrentSampleIndex());
    }
 
    /**
     * Returns all the samples in this suite.
     *
     * @return all the samples in this suite
     */
    @Override
    public List<Sample> getSamples() {
        return Collections.unmodifiableList(samples);
    }

    /**
     * Returns the measurement type of this suite (discrete or continuous)
     *
     * @return the measurement type of this suite (discrete or continuous)
     */
    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    /**
     * Returns the name of this suite.
     *
     * @return the name of this suite
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the number of samples in this suite.
     *
     * @return the number of samples in this suite
     */
    public int getNumSamples() {
        return samplesById.size();
    }

    /**
     * Returns the sample with the specified index.
     *
     * If the suite contains no samples, or the index is -1, returns null.
     *
     * @param i an index number for a sample
     * @return the sample with the specified index
     */
    public Sample getSampleByIndex(int i) {
        if (getSamples().isEmpty() || i == -1) {
            return null;
        } else {
            return samples.get(i);
        }
    }

    /**
     * Returns the index of a specified sample within this suite.
     *
     * @param sample a sample in the suite
     * @return the index of the sample, or {@code -1} if not in this suite
     */
    public int getIndexBySample(Sample sample) {
        final Integer index = indicesBySample.get(sample);
        return index == null ? -1 : index;
    }
    
    /**
     * Returns the name of this suite.
     *
     * @return the name of this suite
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * Returns strings representing data about this suite. Note that this does
     * not include data at the level of sites, samples, or treatment steps.
     *
     * @return strings representing data about this suite
     */
    public List<String> toStrings() {
        final List<String> result = new ArrayList<>();
        result.add("MEASUREMENT_TYPE\t" + getMeasurementType().name());
        if (customFlagNames.size()>0) {
            result.add("CUSTOM_FLAG_NAMES\t"+customFlagNames.exportAsString());
        }
        if (customNoteNames.size()>0) {
            result.add("CUSTOM_NOTE_NAMES\t"+customNoteNames.exportAsString());
        }
        result.add("CREATION_DATE\t" + ISO_8601_FORMAT.format(creationDate));
        result.add("MODIFICATION_DATE\t" +
                ISO_8601_FORMAT.format(modificationDate));
        if (originalFileType != null) {
            result.add("ORIGINAL_FILE_TYPE\t" + originalFileType.name());
        }
        result.add("ORIGINAL_CREATOR_PROGRAM\t" + fileCreator);
        result.add("SAVED_BY_PROGRAM\t" + suiteCreator);
        return result;
    }

    /**
     * Sets suite data from a string. The string must be in the format of one of
     * the strings produced by the {@link #toStrings()} method.
     *
     * @param string a string from which to read suite data
     */
    public void fromString(String string) {
        final String[] parts = string.split("\t");
        if (null != parts[0]) {
            switch (parts[0]) {
                case "MEASUREMENT_TYPE":
                    setMeasurementType(MeasurementType.valueOf(parts[1]));
                    break;
                case "CUSTOM_FLAG_NAMES":
                    customFlagNames = new CustomFlagNames(
                            Arrays.asList(parts).subList(1, parts.length));
                    break;
                case "CUSTOM_NOTE_NAMES":
                    customNoteNames = new CustomNoteNames(
                            Arrays.asList(parts).subList(1, parts.length));
                    break;
                case "CREATION_DATE":
                    try {
                        creationDate = ISO_8601_FORMAT.parse(parts[1]);
                    } catch (ParseException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                    break;
                case "MODIFICATION_DATE":
                    try {
                        modificationDate = ISO_8601_FORMAT.parse(parts[1]);
                    } catch (ParseException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                    break;
                case "ORIGINAL_FILE_TYPE":
                    originalFileType = FileType.valueOf(parts[1]);
                    break;
                case "ORIGINAL_CREATOR_PROGRAM":
                    fileCreator = parts[1];
                    break;
                case "SAVED_BY_PROGRAM":
                    /*
                     * There's no need to store the value of this field --
                     * it will be overwritten in any case if the file is saved.
                     */
                    LOGGER.log(Level.INFO, "File saved by: {0}", parts[1]);
                    break;
            }
        }
    }
    
    private void setMeasurementType(MeasurementType measurementType) {
        this.measurementType = measurementType;
        for (Sample sample : getSamples()) {
            for (TreatmentStep treatmentStep : sample.getTreatmentSteps()) {
                treatmentStep.setMeasurementType(measurementType);
            }
        }
    }

    /**
     * Imports site locations from a CSV file.
     * 
     * File format: CSV, no header line. Column 1 contains the site name, column
     * 2 the latitude (degrees north from equator, negative for southern
     * hemisphere), and column 3 the longitude (degrees east of Greenwich).
     *
     * @param file the file from which to read site locations
     * @throws IOException if an error occurred while reading the file
     */
    public void importLocations(File file) throws IOException {
        try (BufferedReader reader =
                   new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] parts = line.split(" *, *");
                final String siteName = parts[0];
                final Site site = getSiteByName(siteName);
                if (site != null) {
                    final double latitude = Double.parseDouble(parts[1]);
                    final double longitude = Double.parseDouble(parts[2]);
                    site.setLocation(Location.fromDegrees(latitude, longitude));
                }
            }
        }
    }

    /**
     * Imports AMS data from a whitespace-delimited file.
     * If {@code directions==false}, line format is k11 k22 k33 k12 k23 k13 
     * (tensor components) otherwise it's
     * inc1 dec1 inc2 dec2 inc3 dec3 (axis directions, decreasing magnitude).
     * If there's no sample in the suite from which to take the sample
     * and formation corrections, importAmsWithDialog will try to read them as
     * fields appended to the end of the line.
     * <p>
     * <em>Not fully tested -- use with caution.</em>
     * Not currently accessible from the PuffinPlot GUI.
     * 
     * @param files the files from which to read the data
     * @param directions {@code true} to read axis directions,
     * {@code false} to read tensor components
     * @throws IOException if there was an I/O error reading the files 
     */
    public void importAmsFromDelimitedFile(List<File> files, boolean directions)
            throws IOException {
        setSaved(false);
        BufferedReader reader = null;
        for (File file: files) {
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    final double[] v = new double[6];
                    final Scanner sc = new Scanner(line);
                    sc.useLocale(Locale.ENGLISH);
                    String sampleName = sc.next();
                    if (!containsSample(sampleName)) {
                        insertNewSample(sampleName);
                    }
                    final Sample sample = getSampleByName(sampleName);
                    for (int i = 0; i < 6; i++) v[i] = sc.nextDouble();
                    if (directions) {
                        sample.setAmsDirections(v[0], v[1], v[2],
                                v[3], v[4], v[5]);
                    } else {
                        if (!sample.hasTreatmentSteps()) {
                            sample.setCorrections(sc.nextDouble(),
                                    sc.nextDouble(), sc.nextDouble(),
                                    sc.nextDouble(), sc.nextDouble());
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

    /**
     * Imports AMS data from ASC files in the format produced by Agico's SAFYR
     * and SUSAR programs.
     * 
     * Note that the {@code overwriteSampleCorrection} and {@code
     * overwriteFormationCorrection} arguments only have an effect when
     * importing into an existing sample; if a new sample is created for
     * AMS data, the orientations in the ASC file will always be used.
     *
     * @param files the ASC files to read (non-null)
     * @param magneticNorth {@code true} if the sample and formation dip
     * azimuths in the file
     * are relative to magnetic north; {@code false} if they are relative to
     * geographic north
     * @param overwriteSampleCorrection If importing to an existing sample,
     * overwrite the sample's sample correction with the values specified
     * in the ASC file. Otherwise, retain the values already set for the
     * sample.
     * @param overwriteFormationCorrection  If importing to an existing sample,
     * overwrite the sample's formation correction with the values specified
     * in the ASC file. Otherwise, retain the values already set for the
     * sample.
     * @throws IOException if an I/O error occurred while reading the file
     * @throws NullPointerException if {@code files} is null
     */
    public void importAmsFromAsc(List<File> files, boolean magneticNorth,
            boolean overwriteSampleCorrection,
            boolean overwriteFormationCorrection)
            throws IOException {
        Objects.requireNonNull(files);
        setSaved(false);
        final List<AmsData> allData = new ArrayList<>();
        for (File file : files) {
            final AmsLoader amsLoader = new AmsLoader(file);
            allData.addAll(amsLoader.readFile());
        }
        for (AmsData amsData : allData) {
            final String sampleName = amsData.getName();
            if (amsData.getfTest() < 3.9715) {
                continue;
            }
            if (!containsSample(sampleName)) {
                insertNewSample(sampleName);
            }
            final Sample sample = getSampleByName(sampleName);
            if (sample.hasTreatmentSteps()) {
                /*
                 * Overwrite sample and formation corrections if appropriate
                 * parameters passed.
                 */
                if (overwriteSampleCorrection) {
                    sample.setCorrections(
                            amsData.getSampleAz(), amsData.getSampleDip(),
                            sample.getFormAz(), sample.getFormDip(),
                            sample.getMagDev());
                }
                if (overwriteFormationCorrection) {
                    sample.setCorrections(
                            sample.getSampAz(), sample.getSampDip(),
                            amsData.getFormAz(), amsData.getFormDip(),
                            sample.getMagDev());
                }
            } else { // No demagnetization data in sample
                double sampleAzimuth = amsData.getSampleAz();
                double formationAzimuth = amsData.getFormAz();
                if (!magneticNorth) {
                    /*
                     * Since there's no existing data for this sample from
                     * which to determine the magnetic deviation, we
                     * take it from the another sample in the suite, on the
                     * assumption that it will be the same throughout the
                     * suite.
                     */
                    sampleAzimuth -= getFirstValidMagneticDeviation();
                    formationAzimuth -= getFirstValidMagneticDeviation();
                }
                sample.setCorrections(
                        sampleAzimuth, amsData.getSampleDip(),
                        formationAzimuth, amsData.getFormDip(),
                        getFirstValidMagneticDeviation());
            }
            final double[] v = amsData.getTensor();
            sample.setAmsFromTensor(v[0], v[1], v[2], v[3], v[4], v[5]);
        }
        updateReverseIndex();
    }

    /**
     * @return the first valid (i.e. finite) magnetic deviation for any sample
     * in this suite, or 0 if there are none
     */
    private double getFirstValidMagneticDeviation() {
        for (Sample sample : samples) {
            final double v = sample.getMagDev();
            if (!Double.isNaN(v)) {
                return v;
            }
        }
        return 0;
    }

    /**
     * Exports a subset of this suite's data to multiple files, one file per
     * sample. The files are in a tab-delimited text format.
     *
     * @param directory the directory in which to create the files (non-null)
     * @param fields the fields to export (non-null)
     * 
     * @throws NullPointerException if either argument is null
     */
    public void exportToFiles(File directory, List<TreatmentParameter> fields) {
        Objects.requireNonNull(directory);
        Objects.requireNonNull(fields);
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                LOGGER.warning(String.format(Locale.ENGLISH,
                        "exportToFiles: %s is not a directory",
                        directory.toString()));
                return;
            }
        } else {
            if (!directory.mkdirs()) {
                LOGGER.warning(String.format(Locale.ENGLISH,
                        "exportToFiles: couldn't create %s",
                        directory.toString()));
                return;
            }
        }
        for (Sample s : getSamples()) {
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
                LOGGER.log(Level.WARNING,
                        "exportToFiles: exception writing file.", e);
            } finally {
                try {
                    if (fw != null) {
                        fw.close();
                    }
                } catch (IOException e2) {
                    LOGGER.log(Level.WARNING, 
                            "exportToFiles: exception closing file.", e2);
                }
            }
        }
    }

    /**
     * Returns the names (titles) of the custom flags for this suite.
     *
     * @return the names (titles) of the custom flags for this suite
     */
    public CustomFields<String> getCustomFlagNames() {
        return customFlagNames;
    }

    private void processPuffinLines(List<String> lines) {
        Objects.requireNonNull(lines);
        for (String line : lines) {
            final String[] parts = line.split("\t");
            switch (parts[0]) {
                case "SUITE":
                    fromString(line.substring(6));
                    break;
                case "SAMPLE":
                    final String sampleId = parts[1];
                    Sample sample = getSampleByName(sampleId);
                    /*
                     * We create a sample here if returned sample is null, to
                     * deal with suites which have samples with no associated
                     * TreatmentStep lines.
                     */
                    if (sample == null) {
                        sample = new Sample(sampleId, this);
                        samplesById.put(sampleId, sample);
                        samples.add(sample);
                    }
                    sample.fromString(line.substring(8 + sampleId.length()));
                    break;
                case "SITE":
                    final Site site = getOrCreateSite(parts[1]);
                    site.fromString(line.substring(6+parts[1].length()));
                    break;
            }
        }
    }

    /**
     * Returns the names (titles) of the custom notes for this suite.
     *
     * @return the names (titles) of the custom notes for this suite
     */
    public CustomFields<String> getCustomNoteNames() {
        return customNoteNames;
    }

    /**
     * Returns the parameters of the last AMS bootstrap statistics (if any)
     * calculated on this suite's data.
     *
     * @return the parameters of the last AMS bootstrap statistics (if any)
     * calculated on this suite's data
     */
    public List<KentParams> getAmsBootstrapParams() {
        return amsBootstrapParams;
    }

    /**
     * Returns the parameters of the last AMS Hext statistics (if any)
     * calculated on this suite's data.
     *
     * @return the parameters of the last AMS Hext statistics (if any)
     * calculated on this suite's data
     */
    public List<KentParams> getAmsHextParams() {
        return hextParams;
    }

    /**
     * Clears any AMS calculations on this suite
     */
    public void clearAmsCalculations() {
        setSaved(false);
        amsBootstrapParams = null;
        hextParams = null;
    }

    /**
     * Returns the sites within this suite. If there are no sites, the returned
     * list will be empty, but it will never be null.
     *
     * @return the sites within this suite
     */
    public List<Site> getSites() {
        return Collections.unmodifiableList(sites);
    }

    /**
     * Returns a site with the given name, or {@code null} if this suite
     * contains no such site.
     *
     * @param siteName a site name (non-null)
     * @return a site with the given name, or {@code null} if this suite
     * contains no such site
     * 
     * @throws NullPointerException if {@code siteName} is null
     */
    public Site getSiteByName(String siteName) {
        Objects.requireNonNull(siteName);
        for (Site site : sites) {
            if (siteName.equals(site.getName())) {
                return site;
            }
        }
        return null;
    }

    Site getOrCreateSite(String siteName) {
        Site site = getSiteByName(siteName);
        if (site == null) {
            site = new Site(siteName);
            sites.add(site);
        }
        return site;
    }

    /**
     * Returns the name of the PuffinPlot file associated with this suite, if
     * any.
     *
     * @return the name of the PuffinPlot file associated with this suite, if
     * any
     */
    public File getPuffinFile() {
        return puffinFile;
    }

    /**
     * @return true if the suite has not been modified since it was last saved
     */
    public boolean isSaved() {
        return saved;
    }

    /**
     * Sets this suite's "saved" flag, which records whether the suite
     * has been saved since its last modification. If this flag is
     * false, the suite is "modified", and data would be lost by closing it
     * without saving.
     * 
     * @param saved the saved state to set
     */
    public void setSaved(boolean saved) {
        if (this.saved != saved) {
            this.saved = saved;
            for (SavedListener savedListener : savedListenerSet) {
                savedListener.savedStateChanged(saved);
            }
        }
    }

    /**
     * Returns a string identifying the program and version which created this
     * suite.
     *
     * @return the creator
     */
    public String getCreator() {
        return suiteCreator;
    }

    /**
     * Return the samples with a given discrete ID. For discrete suites,
     * there will usually be at most one sample with a given discrete ID.
     * For continuous suites, an entire core section may be returned.
     * 
     * @param id a discrete ID (non-null)
     * @return a list of the samples in this Suite with the specified ID
     * 
     * @throws NullPointerException if {@code id} is null
     */
    public List<Sample> getSamplesByDiscreteId(String id) {
        Objects.requireNonNull(id);
        return getSamples().stream().filter(s -> id.equals(s.getDiscreteId())).
                collect(Collectors.toList());
    }

    /**
     * Rotate the declination of magnetic moment data in this suite according to
     * the discrete ID of the sample.
     * 
     * @param rotations a map from discrete sample IDs to rotation angles
     * (clockwise in degrees)
     */
    public void rotateSamplesByDiscreteId(Map<String, Double> rotations) {
        for (Sample sample : getSamples()) {
            final String discreteId = sample.getDiscreteId();
            if (rotations.containsKey(discreteId)) {
                final double rotationAngle = rotations.get(discreteId);
                for (TreatmentStep treatmentStep : sample.getTreatmentSteps()) {
                    treatmentStep.setMoment(treatmentStep.getMoment().
                            rotZ(Math.toRadians(rotationAngle)));
                }
            }
        }
    }
    
    /**
     * Align the declinations of core sections in this suite.It is assumed that
     * the suite is continuous; the discrete IDs of samples are interpreted as
     * core section identifiers.
     *
     * @param margin the number of samples to average at the end of each section
     * to determine the declination
     * @param targetDeclination the declination to which to align the core
     * @param targetType the core declination to align with the target
     *
     * @see CoreSections#alignSections(int, double, net.talvi.puffinplot.data.CoreSections.TargetDeclinationType) 
     */
    public void alignSectionDeclinations(int margin, double targetDeclination,
            CoreSections.TargetDeclinationType targetType) {
        final CoreSections coreSections =
                CoreSections.fromSampleListByDiscreteId(getSamples());
        coreSections.alignSections(margin, targetDeclination,
                CoreSections.TargetDeclinationType.TOP);
    }
    
    /**
     * Report whether samples near the ends of core sections have an
     * associated direction, e.g. a sample direction calculated by PCA.
     * 
     * @param margin the number of samples to regard as being "near" the
     *     end of a core section
     * @return {@code true} if and only if all samples near the end of
     *     a core section have an associated direction
     * 
     * @see CoreSections#areSectionEndDirectionsDefined(int) 
     */
    public boolean areSectionEndDirectionsDefined(int margin) {
        final CoreSections coreSections =
                CoreSections.fromSampleListByDiscreteId(getSamples());
        return coreSections.areSectionEndDirectionsDefined(margin);
    }
    
    /**
     * Within each of the supplied samples, merges any TreatmentStep objects
     * which have the same treatment type and treatment level.
     *
     * @param samplesToMerge samples containing the TreatmentStep objects to
     * merge (where possible)
     */
    public void mergeDuplicateTreatmentSteps(
            Collection<Sample> samplesToMerge) {
        samplesToMerge.forEach(Sample::mergeDuplicateTreatmentSteps);
    }

    /**
     * Within the supplied collection of samples, any two or more samples which
     * have the same depth or discrete ID will be merged into a single sample.
     * The merged sample will contain all the distinct treatment steps from the
     * union of the duplicate samples. If any of the individual merged samples
     * contains any duplicate treatment steps, the duplicate treatment steps
     * will also be merged.
     * 
     * This should seldom be needed in practice since the suite creation and
     * reading code doesn't create new samples with the same names/depths as
     * existing ones. But the PuffinPlot application is forever acquiring new
     * ways to edit suites and samples (and of course scripting the API can
     * result in all sorts of weirdness), so this is a useful way to restore
     * order in the event that a suite does get into a funny state.
     *
     * @param samples the collection of samples within which to merge duplicates
     *                (non-null)
     * 
     * @throws NullPointerException is {@code samples} is null
     */
    public void mergeDuplicateSamples(Collection<Sample> samples) {
        Objects.requireNonNull(samples);
        Map<String, List<Sample>> duplicateGroups = samples.stream().
                collect(Collectors.groupingBy(Sample::getNameOrDepth));
        
        for (String id : duplicateGroups.keySet()) {
            final List<Sample> sampleGroup = duplicateGroups.get(id);
            /*
             * The sample group might only contain one sample, but there's
             * no need to make a special case for that eventuality.
             */
            Sample.mergeSamples(sampleGroup);
            removeSamples(sampleGroup.subList(1, sampleGroup.size()));
        }
    }

    private class CustomFlagNames extends CustomFields<String> {
        public CustomFlagNames(List<String> list) {
            super(list);
        }
        @Override
        public void add(int position, String value) {
            super.add(position, value);
            for (Sample s : getSamples()) {
                s.getCustomFlags().add(position, Boolean.FALSE);
            }
        }
        @Override
        public void remove(int position) {
            super.remove(position);
            for (Sample s : getSamples()) {
                s.getCustomFlags().remove(position);
            }
        }
        @Override
        public void swapAdjacent(int position) {
            super.swapAdjacent(position);
            for (Sample s : getSamples()) {
                s.getCustomFlags().swapAdjacent(position);
            }
        }
    }

    private class CustomNoteNames extends CustomFields<String> {
        public CustomNoteNames(List<String> list) {
            super(list);
        }
        @Override
        public void add(int position, String value) {
            super.add(position, value);
            for (Sample s : getSamples()) {
                s.getCustomNotes().add(position, "");
            }
        }
        @Override
        public void remove(int position) {
            super.remove(position);
            for (Sample s : getSamples()) {
                s.getCustomNotes().remove(position);
            }
        }
        @Override
        public void swapAdjacent(int position) {
            super.swapAdjacent(position);
            for (Sample s : getSamples()) {
                s.getCustomNotes().swapAdjacent(position);
            }
        }
    }
    
    /**
     * Calculates and stores AMS statistics using an external script.
     *
     * @param samples the samples on which to calculate statistics
     * @param calcType the type of AMS calculation to perform
     * @param scriptPath the filesystem path of the script which will perform
     * the calculation
     * @throws IOException if there was an error running the script or reading
     * its output
     * @throws IllegalArgumentException if the samples contain insufficient AMS
     * data
     */
    public void calculateAmsStatistics(List<Sample> samples,
            AmsCalculationType calcType, String scriptPath)
            throws IOException, IllegalArgumentException {
        /*
         * It may not be immediately obvious why this should be an instance
         * method of Suite. In fact the only reason for this is that it stores
         * its results in Suite. This is probably OK. The main deficiency of the
         * current model for AMS data is that it only allows one set of data to
         * be stored at a time. However, even if we improve that to allow
         * multiple sets of AMS data, the Suite is the natural place to store
         * them (since the sets of samples for AMS calculations isn't
         * necessarily tied to a single Site).
         */
        setSaved(false);
        final List<Tensor> tensors = new ArrayList<>();
        for (Sample s : samples) {
            if (s.getAms() != null) tensors.add(s.getAms());
        }
        if (tensors.isEmpty()) {
            throw new IllegalArgumentException(
                    "No AMS data in specified samples.");
        } else if (tensors.size()<3) {
            throw new IllegalArgumentException(
                    "Too few samples with AMS data.");
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
        /*
         * ‘Iterator.remove is the only safe way to modify a collection during
         * iteration’ --
         * http://docs.oracle.com/javase/tutorial/collections/interfaces/collection.html
         */
        for (Iterator<Site> it = sites.iterator(); it.hasNext(); ) {
            if (it.next().isEmpty()) {
                it.remove();
            }
        }
    }
    
    /**
     * For a continuous suite, returns the minimum depth of a sample within the
     * suite.
     *
     * @return the minimum depth of a sample within the suite
     */
    public double getMinDepth() {
        if (!getMeasurementType().isContinuous()) {
            return Double.NaN;
        }
        double minimum = Double.POSITIVE_INFINITY;
        for (Sample sample : getSamples()) {
            final double depth = sample.getDepth();
            if (depth < minimum) {
                minimum = depth;
            }
        }
        return minimum;
    }
        
    /**
     * For a continuous suite, returns the maximum depth of a sample within the
     * suite.
     *
     * @return the maximum depth of a sample within the suite
     */
    public double getMaxDepth() {
        if (!getMeasurementType().isContinuous()) {
            return Double.NaN;
        }
        double maximum = Double.NEGATIVE_INFINITY;
        for (Sample sample : getSamples()) {
            final double depth = sample.getDepth();
            if (depth > maximum) {
                maximum = depth;
            }
        }
        return maximum;
    }
    
    /**
     * Clears sites for specified samples within this suite. The site for
     * each specified sample will be set to {@code null}, and any sites with
     * no associated samples will be removed from this suite's list of sites.
     * 
     * @param samples samples for which to clear sites
     */
    public void clearSites(Collection<Sample> samples) {
        setSaved(false);
        samples.forEach(sample -> sample.setSite(null));
        rebuildSiteListFromSamples();
    }
    
    private void rebuildSiteListFromSamples() {
        /*
         * The distinct() filter is stable, so this produces sites in the order
         * in which they're first encountered in the sample list. Usually we
         * expect sites to consist of contiguous sequences of samples, in which
         * case the ordering of any two sites S1 and S2 will correspond to the
         * ordering of any two samples s1 and s2 such that s1 ∈ S1 and s2 ∈ S2.
         * (More loosely: sites will be ordered like their samples.)
         */
        sites = getSamples().stream().map(sample -> sample.getSite())
                .filter(Objects::nonNull).distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * A SiteNamer turns a sample name into a site name. It is used to
     * automatically define site names for a number of samples according to a
     * pre-programmed scheme.
     */
    @FunctionalInterface
    public static interface SiteNamer {
        /**
         * Determines a site name from a sample name.
         *
         * @param sample the name of a sample
         * @return the name of a site which should contain the sample with the
         * specified name
         */
        String siteName(Sample sample);
    }
    
    /**
     * Sets sites for supplied samples according to a supplied site namer. Where
     * a site with the required name exists, it will be used; otherwise a new
     * site with the required name will be created.
     *
     * @param samples the samples for which to set sites
     * @param siteNamer the site namer which will produce the site names
     */
    public void setSitesForSamples(Collection<Sample> samples,
            SiteNamer siteNamer) {
        setSaved(false);
        for (Sample sample : samples) {
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
    
    /**
     * Explicitly sets a site for the specified samples. If a site with the
     * requested name exists, it will be used; otherwise a new site with that
     * name will be created.
     *
     * @param samples the samples for which to set the site
     * @param siteName the name of the site into which to put the samples
     */
    public void setNamedSiteForSamples(Collection<Sample> samples,
            final String siteName) {
        setSaved(false);
        setSitesForSamples(samples, sample -> siteName);
    }
    
    /**
     * Sets site names for samples according to chosen characters from the
     * sample names. The caller supplies a bit-set; for each sample name, the
     * site name is determined by taking the characters of the sample name for
     * which the corresponding bit is set.
     *
     * @param samples the samples for which to set sites
     * @param charMask the mask determining which characters to use for the site
     * name
     */
    public void setSiteNamesBySubstring(Collection<Sample> samples,
            final BitSet charMask) {
        setSaved(false);
        setSitesForSamples(samples, new SiteNamer() {
            @Override
            public String siteName(Sample sample) {
                final String sampleName = sample.getNameOrDepth();
                final StringBuilder sb = new StringBuilder(sampleName.length());
                for (int i = 0; i < sampleName.length(); i++) {
                    if (charMask.get(i)) {
                        sb.append(sampleName.substring(i, i + 1));
                    }
                }
                return sb.toString();
            }
        });
    }
    
    /**
     * Sets site names for a continuous suite according to the depth of the
     * samples. A thickness is specified to the method, and the suite is divided
     * into sites of that thickness. Each site is named for the shallowest depth
     * within it.
     *
     * @param samples the samples for which to set site names
     * @param thickness the thickness of each site
     */
    public void setSiteNamesByDepth(Collection<Sample> samples,
            final double thickness) {
        setSaved(false);
        setSitesForSamples(samples, new SiteNamer() {
            @Override
            public String siteName(Sample sample) {
                final double minDepth = getMinDepth();
                final double relDepth = sample.getDepth() - minDepth;
                final double slice = Math.floor(relDepth / thickness);
                final String sliceName = String.format(Locale.ENGLISH, "%.2f",
                        slice * thickness + minDepth);
                return sliceName;
            }
        });
    }
    
    /**
     * Creates a new sample and adds it to this suite. The sample's position is
     * determined by its name. Provided that the suite is sorted by sample name
     * (or depth), the sample will be inserted at its correct position according
     * to that sorting. Note that the reverse (sample-to-index) map is not
     * updated; this must be done manually with a call to updateReverseIndex().
     *
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

    /**
     * Determine whether this suite contain a same with a specified identifier
     * (name).
     *
     * @param id a sample identifier
     * @return {@code true} if this suite contains a sample with the specified
     * identifier
     */
    public boolean containsSample(String id) {
        return samplesById.containsKey(id);
    }

    /**
     * Multiplies all magnetic susceptibility measurements in this suite by the
     * specified factor.
     *
     * @param factor a factor by which to multiply all the magnetic
     * susceptibility measurements in this suite
     */
    public void rescaleMagSus(double factor) {
        setSaved(false);
        for (Sample sample : samples) {
            for (TreatmentStep treatmentStep : sample.getTreatmentSteps()) {
                treatmentStep.setMagSus(treatmentStep.getMagSus() * factor);
            }
        }
    }
    
    /**
     * Sorts this suite's samples in ascending order of depth
     */
    public void sortSamplesByDepth() {
        Collections.sort(samples, (Sample arg0, Sample arg1) ->
                Double.compare(arg0.getDepth(), arg1.getDepth()));
        updateReverseIndex();
    }
    
    /**
     * Retains the samples between the specified depths in the suite,
     * and removes all samples outside this range.
     * 
     * @param minDepth Minimum depth of samples to retain
     * @param maxDepth Maximum depth of samples to retain
     */
    public void removeSamplesOutsideDepthRange(
            double minDepth, double maxDepth) {
        final Set<Sample> samplesToRemove = samples.stream().
                filter(s -> s.getDepth()< minDepth || s.getDepth() > maxDepth).
                collect(Collectors.toSet());
        removeSamples(samplesToRemove);
    }

    /**
     * For every sample in the supplied collection: remove the sample from
     * this suite if any of its treatment steps has the specified treatment
     * type.
     * 
     * @param removableSamples samples to consider for removal (must be 
     *   within this suite)
     * @param treatmentType the treatment type that selects which samples
     *   should be removed.
     */
    public void removeSamplesByTreatmentType(
            Collection<Sample> removableSamples, TreatmentType treatmentType) {
        final Set<Sample> samplesToRemove = removableSamples.stream().
                filter(s -> s.getTreatmentSteps().stream().
                        anyMatch(d -> d.getTreatmentType() == treatmentType)).
                collect(Collectors.toSet());
        removeSamples(samplesToRemove);
    }
    
    /**
     * Removes the specified samples from this suite.
     *
     * @param samplesToRemove the samples to remove
     */
    public void removeSamples(Collection<Sample> samplesToRemove) {
        samples.removeAll(samplesToRemove);
        samplesToRemove.forEach(s -> samplesById.remove(s.getNameOrDepth()));
        ensureCurrentSampleIndexValid();
        updateReverseIndex();
        setSaved(false);
    }
    
    private void ensureCurrentSampleIndexValid() {
        currentSampleIndex = Math.max(0,
                Math.min(getCurrentSampleIndex(), getNumSamples() - 1));
    }
    
    /**
     * This exception indicates that a supplied argument did not contain
     * an expected sample name.
     */
    public class MissingSampleNameException extends Exception {
        /**
         * Creates a new {@code MissingSampleNameException} with the
         * supplied message.
         * 
         * @param message a message giving details about the exception
         */
        public MissingSampleNameException(String message) {
            super(message);
        }
    }
    
    /**
     * Converts this suite from a discrete suite to a continuous suite
     * using a supplied mapping from sample names to depths.
     * 
     * @param nameToDepth a map containing a depth string for each sample name
     * 
     * @throws MissingSampleNameException if the map does not contain
     * all the sample names in this suite as keys
     */
    public void convertDiscreteToContinuous(Map<String,String> nameToDepth)
            throws MissingSampleNameException {
        if (getMeasurementType() != MeasurementType.DISCRETE) {
            throw new IllegalStateException("convertDiscreteToContinuous "
                    + "can only be called on a discrete Suite.");
        }
        Objects.requireNonNull(nameToDepth, "nameToDepth must be non-null");
        if (!getSamples().stream().allMatch(
                s -> nameToDepth.containsKey(s.getNameOrDepth()))) {
            throw new MissingSampleNameException(
                    "Missing sample name key(s) in nameToDepth");
        }
        setMeasurementType(MeasurementType.CONTINUOUS);
        samplesById = new LinkedHashMap<>();
        for (Sample sample : getSamples()) {
            final String newSampleName =
                    nameToDepth.get(sample.getNameOrDepth());
            sample.setNameOrDepth(newSampleName);
            samplesById.put(newSampleName, sample);
        }
        updateReverseIndex();
        sortSamplesByDepth();
    }
    
    /**
     * Converts a discrete suite to a continuous suite, using a file to provide
     * the mapping from sample names to depths.
     * 
     * @param file a text file consisting of lines in the format
     *   {@literal <sample-name>,<sample-depth>}
     * @throws IOException if there was a problem reading the file
     * @throws MissingSampleNameException if there are samples in this suite
     *   which are not listed in the specified file
     */
    public void convertDiscreteToContinuous(File file)
            throws IOException, MissingSampleNameException {
        final Map<String,String> nameToDepth = new HashMap<>();
        try (BufferedReader reader =
                   new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] parts = line.split(" *, *");
                nameToDepth.put(parts[0], parts[1]);
            }
        }
        convertDiscreteToContinuous(nameToDepth);
    }
    
    /**
     * A listener interface for modifications to the Suite's save state.
     * 
     * This interface is for components that need to be notified when
     * the Suite's save state changes (i.e. when it is modified or saved).
     */
    public static interface SavedListener {
        
        /**
         * This method is called when the Suite's save state changes.
         * 
         * @param newState the new saved state of the Suite (true is saved,
         * false is modified)
         */
        public void savedStateChanged(boolean newState);
    }
    
    /**
     * Add a listener for the Suite's save state
     * 
     * @param savedListener the new listener
     */
    public void addSavedListener(SavedListener savedListener) {
        savedListenerSet.add(savedListener);
    }
    
    /**
     * Remove a listener for the Suite's save state
     * 
     * @param savedListener the listener to remove
     */
    public void removeSavedListener(SavedListener savedListener) {
        savedListenerSet.remove(savedListener);
    }
}
