package net.talvi.puffinplot.data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
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
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.talvi.puffinplot.FileType;
import net.talvi.puffinplot.PuffinApp;

public class Suite implements Iterable<Datum> {

    private List<Datum> data;
    private File[] files;
    private Double[] depths = {};
    private String[] names = {};
    private Map<Double, Sample> samplesByDepth;
    private Map<String, Sample> samplesByName;
    private Map<Integer, Line> dataByLine;
    private int currentDepthIndex = 0;
    private MeasType measType;
    private String currentName;
    private String suiteName;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private static final Pattern whitespace = Pattern.compile("\\s+");
    private List<String> loadWarnings = new LinkedList<String>();
    private List<FisherForSite> siteFishers;
    private FisherValues suiteFisher;
    final private static String[] ZPLOT_HEADERS = {"Sample", "Project", "Demag", "Declin", "Inclin", "Intens", "Operation"};

    public Iterator<Datum> iterator() {
        return data.iterator();
    }

    public List<String> getLoadWarnings() {
        return loadWarnings;
    }

    public FisherValues getSuiteFisher() {
        return suiteFisher;
    }

    private static class Fields {
        List<TwoGeeField> fields;
        List<String> unknown;

        Fields(String header) {
            fields = new LinkedList<TwoGeeField>();
            unknown = new LinkedList<String>();
            Scanner scanner = new Scanner(header);
            scanner.useDelimiter(Pattern.compile("\\t")); // might have spaces within fields
            while (scanner.hasNext()) {
                String name = scanner.next();
                TwoGeeField field = TwoGeeField.getByHeader(name);
                fields.add(field);
                if (field == TwoGeeField.UNKNOWN) unknown.add(name);
            }
        }

        public boolean areAllUnknown() {
            for (TwoGeeField field: fields)
                if (field != TwoGeeField.UNKNOWN)
                    return false;
            return true;
        }
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
        Sample[] samples = PuffinApp.getInstance().getSelectedSamples();
        List<PcaValues> pcas = new ArrayList<PcaValues>(samples.length);

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

    public void save(File file) {
        List<TwoGeeField> fields = new LinkedList(Arrays.asList(TwoGeeField.values()));
        fields.remove(TwoGeeField.UNKNOWN);

        Writer writer = null;
        try {
            writer = new FileWriter(file);

            StringBuilder header = new StringBuilder();
            for (TwoGeeField field : fields) {
                header.append(field.getHeading());
                header.append("\t");
            }
            header.deleteCharAt(header.length()-1);
            header.append("\n");
            writer.write(header.toString());

            for (Sample sample : getSamples()) {
                for (Datum datum : sample.getData()) {
                    StringBuilder line = new StringBuilder();
                    for (TwoGeeField field : fields) {
                        line.append(datum.getValue(field).toString());
                        line.append("\t");
                    }
                    line.deleteCharAt(line.length() - 1);
                    line.append("\n");
                    writer.write(line.toString());
                }
            }
        } catch (IOException e) {
            PuffinApp.errorDialog("Error saving file", e.getLocalizedMessage());
        } finally {
            if (writer != null)  {
                try { writer.close(); }
                catch (IOException e) {
                    PuffinApp.errorDialog("Error closing saved file", e.getLocalizedMessage());
                }
            }
        }
    }

    private Line getLineContainer(int lineNumber) {
        if (!dataByLine.containsKey(lineNumber))
            dataByLine.put(lineNumber, new Line(lineNumber));
        return dataByLine.get(lineNumber);
    }

    private void addDatumLongcore(Datum d, Set<Double> depthSet) {
        if (!d.ignoreOnLoading()) {
            data.add(d);
            Sample s = samplesByDepth.get(d.getDepth());
            if (s == null) {
                s = new Sample(d.getDepth());
                samplesByDepth.put(d.getDepth(), s);
            }
            s.addDatum(d);
            depthSet.add(d.getDepth());
        }
    }

    private void addDatumDiscrete(Datum d, Set<String> nameSet) {
        if (!d.ignoreOnLoading()) {
            data.add(d);
            String name = d.getSampleId();
            Sample s = samplesByName.get(name);
            if (s == null) {
                s = new Sample(name);
                samplesByName.put(name, s);
            }
            s.addDatum(d);
            nameSet.add(name);
        }
    }

    private void addLine2G(String line, int lineNumber, List<TwoGeeField> fields,
            Set<Double> depthSet, Set<String> nameSet) {
        final boolean oldSquid = PuffinApp.getInstance().getPrefs().isUseOldSquidOrientations();
        if (!emptyLine.matcher(line).matches()) {
            Datum d = new Datum(line, fields, getLineContainer(lineNumber), oldSquid);
            if (d.getMeasType() != MeasType.NONE) {
                if (measType == MeasType.UNSET)
                    measType = d.getMeasType();
                if (d.getMeasType() != measType) {
                    throw new IllegalArgumentException
                            ("Can't mix long core and discrete measurements.");
                }
            }
            switch (measType) {
                case CONTINUOUS:
                    addDatumLongcore(d, depthSet);
                    break;
                case DISCRETE:
                    addDatumDiscrete(d, nameSet);
                    break;
                case NONE:
                    // This is a treatment step with no measurement, so there will
                    // be no data.
                    break;
                default:
                    throw new IllegalArgumentException("Unknown measurement type.");
            }
        }
    }

    private void addLineZplot(String line, Set<Double> depthSet, Set<String> nameSet) {
        Datum d = new Datum(line);
        if (measType == MeasType.UNSET) measType = d.getMeasType();
        if (d.getMeasType() != measType) {
            throw new Error("Can't mix long core and discrete measurements.");
        }
        switch (measType) {
        case CONTINUOUS: addDatumLongcore(d, depthSet); break;
        case DISCRETE: addDatumDiscrete(d, nameSet); break;
        default: throw new Error("Unknown measurement type.");
        }
    }

    private List<File> expandDirs(File[] files) {
        List<File> result = new LinkedList<File>();
        for (File file: files) {
            if (file.isDirectory()) result.addAll(expandDirs(file.listFiles()));
            else result.add(file);
        }
        return result;
    }

    private void addWarning(String s, Object... args) {
        loadWarnings.add(String.format(s, args));
    }

    /*
     * Note that this may return an empty suite, in which case various things
     * can break. We can't just throw an exception if the suite's empty,
     * because then we lose the load warnings (which will probably explain
     * to the user *why* the suite's empty and are thus quite important).
     **/
    public Suite(File[] files) throws IOException {
        assert(files.length > 0);
        if (files.length == 1) suiteName = files[0].getName();
        else suiteName = files[0].getParentFile().getName();
        files = expandDirs(files).toArray(new File[] {});
        this.files = files;
        data = new ArrayList<Datum>();
        samplesByDepth = new HashMap<Double, Sample>();
        samplesByName = new HashMap<String, Sample>();
        dataByLine = new HashMap<Integer, Line>();
        measType = MeasType.UNSET;
        String line;
        TreeSet<Double> depthSet = new TreeSet<Double>();
        TreeSet<String> nameSet = new TreeSet<String>();
        final int MAX_WARNINGS_PER_FILE = 3;

        for (File file: files) {
            int warningsThisFile = 0;
            FileType fileType = FileType.guessFromName(file);
            final String fileName = file.getName();
            LineNumberReader reader = null;
            fileTypeSwitch: switch (fileType) {
            case PUFFINPLOT:
            case TWOGEE:
                try {
                    reader = new LineNumberReader(new FileReader(file));
                    String fieldsLine = reader.readLine();
                    if (fieldsLine == null) {
                        addWarning("%s is empty.", fileName);
                        reader.close();
                        break;
                    }
                    Fields fields = new Fields(fieldsLine);
                    if (fields.areAllUnknown()) {
                        addWarning("%s doesn't look like a 2G or PPL file. " +
                                "Ignoring it.");
                        reader.close();
                        break;
                    }

                    while ((line = reader.readLine()) != null) {
                        final int lineNum = reader.getLineNumber();
                        try {
                            addLine2G(line, lineNum, fields.fields,
                                    depthSet, nameSet);
                        } catch (IllegalArgumentException e) {
                            addWarning("%s at line %d in file %s -- " +
                                    "ignoring this line.", e.getMessage(),
                                    lineNum, fileName);
                            if (++warningsThisFile > MAX_WARNINGS_PER_FILE) {
                                addWarning("Too many errors in %s -- " +
                                        "aborting load at line %d",
                                        fileName, lineNum);
                                break;
                            }
                        }
                    }
                    
                    if (fields.unknown.size() > 0) {
                        addWarning("I didn't recognize the following field " +
                                "names,\nso I'm ignoring them:\n" +
                                fields.unknown);
                    }
                } finally {
                    if (reader != null) reader.close();
                }
                break;

            case ZPLOT:
                try {
                    reader = new LineNumberReader(new FileReader(file));
                    // Check first line for magic string
                    if (!reader.readLine().startsWith("File Name:")) {
                        addWarning("Ignoring unrecognized file %s", fileName);
                        reader.close();
                        break fileTypeSwitch;
                    }
                    // skip remaining header fields
                    for (int i = 0; i < 5; i++) reader.readLine();
                    String headerLine = reader.readLine();
                    if (headerLine == null) {
                        addWarning("Ignoring malformed ZPlot file %s", file.getName());
                        reader.close();
                        break fileTypeSwitch;
                    }
                    String[] headers = whitespace.split(reader.readLine());

                    if (headers.length != 7) {
                        addWarning("Wrong number of header fields in Zplot file %s:" +
                                ": expected 7, got %s", fileName, headers.length);
                        reader.close();
                        break fileTypeSwitch;
                    }
                    for (int i = 0; i < ZPLOT_HEADERS.length; i++) {
                        if (!ZPLOT_HEADERS[i].equals(headers[i])) {
                            addWarning("Unknown header field %s in file %s " +
                                    " -- aborting load.", headers[i], fileName);
                            reader.close();
                            break fileTypeSwitch;
                        }
                    }

                    while ((line = reader.readLine()) != null)
                        addLineZplot(line, depthSet, nameSet);
                } finally {
                    if (reader != null) reader.close();
                }
                break;

            case UNKNOWN:
                addWarning("I don't recognize the file %s, so I'm ignoring it.",
                        fileName);
                break;

            }
        }

        loadWarnings = Collections.unmodifiableList(loadWarnings);
        depths = depthSet.toArray(depths);
        names = nameSet.toArray(names);
        setCurrentDepthIndex(0);
        for (Sample s: getSamples()) s.doPca();
    }

    /*
     * Save calculations per-sample.
     */
    public void saveCalcsSample(File file) {
        CsvWriter writer = null;
        try {
            List<Sample> samples = getSamplesOrdered();
            if (samples.size()==0) {
                PuffinApp.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }

            writer = new CsvWriter(new FileWriter(file));
            writer.writeCsv(measType.getColumnHeader(),
                    FisherValues.getHeaders(), PcaAnnotated.getHeaders());
            for (Sample sample: samples) {
                PcaAnnotated pca = sample.getPca();
                FisherValues fish = sample.getFisher();
                writer.writeCsv(sample.getNameOrDepth(),
                        fish == null ? FisherValues.getEmptyFields() : fish.toStrings(),
                        pca == null ? PcaAnnotated.getEmptyFields() : pca.toStrings());
            }

        } catch (IOException ex) {
            PuffinApp.errorDialog("Error saving file", ex.getMessage());
        } finally {
                try {
                    if (writer != null) writer.close();
                } catch (IOException ex) {
                    PuffinApp.errorDialog("Error closing file", ex.getLocalizedMessage());
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
                PuffinApp.errorDialog("Error saving calculations",
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
                PuffinApp.errorDialog("Error saving calculations",
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

    public Sample getSampleByDepth(double depth) {
        return samplesByDepth.get(depth);
    }

    public Sample getSampleByName(String name) {
        return samplesByName.get(name);
    }

    public double getCurrentDepth() {
        return depths[getCurrentDepthIndex()];
    }

    public void setCurrentDepthIndex(int value) {
        currentDepthIndex = value;
    }

    public int getCurrentDepthIndex() {
        return currentDepthIndex;
    }

    public Sample getCurrentSample() {
        switch (measType) {
        case CONTINUOUS: return getSampleByDepth(getCurrentDepth());
        case DISCRETE: return getSampleByName(currentName);
        default: throw new RuntimeException("Unknown measurement type.");
        }
    }

    public Collection<Sample> getSamples() {
        return measType == MeasType.CONTINUOUS ?
            samplesByDepth.values() : samplesByName.values();
    }

    public List<Sample> getSamplesOrdered() {
        ArrayList<Sample> samples = new ArrayList<Sample>(getNumSamples());
        if (measType == MeasType.CONTINUOUS) {
            for (double depth : depths) samples.add(getSampleByDepth(depth));
        } else {
            for (String name : names) samples.add(getSampleByName(name));
        }
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
        if (measType == MeasType.CONTINUOUS) return depths.length;
        else return samplesByName.size();
    }

    public String getCurrentName() {
        return currentName;
    }

    public void setCurrentName(String currentName) {
        this.currentName = currentName;
    }

    public String[] getNameArray() {
        return names;
    }

    @Override
    public String toString() {
        return getName();
    }
}
