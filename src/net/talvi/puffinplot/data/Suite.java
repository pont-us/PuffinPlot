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
            PcaValues pca = sample.getPca();
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
            String site = sample.getName().substring(0, 2);
            PcaValues pca = sample.getPca();
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
            if (measType == MeasType.UNSET)
                measType = d.getMeasType();
            if (d.getMeasType() != measType) {
                throw new IllegalArgumentException("Can't mix long core and discrete measurements.");
            }
            switch (measType) {
                case CONTINUOUS:
                    addDatumLongcore(d, depthSet);
                    break;
                case DISCRETE:
                    addDatumDiscrete(d, nameSet);
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
    
    /*
     * Note that this may return an empty suite, in which case various things
     * can break. We can't just throw an exception if the suite's empty,
     * because then we lose the load warnings (which will probably explain
     * to the user *why* the suite's empty and are thus quite important). 
     * 
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
            LineNumberReader reader = null;
            fileTypeSwitch: switch (fileType) {
            case PUFFINPLOT:
            case TWOGEE: 
                try {
                    reader = new LineNumberReader(new FileReader(file));
                    String fieldsLine = reader.readLine();
                    if (fieldsLine == null) {
                        loadWarnings.add(file.getName()+" is empty.");
                        break;
                    }
                    Fields fields = new Fields(fieldsLine);
                    if (fields.areAllUnknown()) {
                        loadWarnings.add(file.getName() +
                                " doesn't look like a 2G file. " +
                                "Ignoring it.");
                    } else {
                        while ((line = reader.readLine()) != null) {
                            final int lineNum = reader.getLineNumber();
                            try {
                                addLine2G(line, lineNum, fields.fields,
                                        depthSet, nameSet);
                            } catch (IllegalArgumentException e) {
                                loadWarnings.add(e.getMessage() +
                                        " at line " + lineNum +
                                        " in file " + file.getName() +
                                        " -- ignoring this line.");
                                if (++warningsThisFile > MAX_WARNINGS_PER_FILE) {
                                    loadWarnings.add("Too many errors in " +
                                            file.getName() +
                                            "-- aborting load at line " +
                                            lineNum);
                                    break;
                                }
                            }
                        }
                    }
                    if (fields.unknown.size() > 0) {
                        loadWarnings.add(
                                "I didn't recognize the following field names,\n" +
                                "so I'm ignoring them:\n" +
                                fields.unknown);
                    }
                } finally {
                    if (reader != null) reader.close();
                }
                break;

            case ZPLOT:
                try {
                reader = new LineNumberReader(new FileReader(file));
                for (int i=0; i<6; i++) reader.readLine();     // skip the header fields
                String[] headers = whitespace.split(reader.readLine());
                
                if (headers.length != 7) {
                    loadWarnings.add("Wrong number of header fields in Zplot file "+file.getName()+
                            ": expected 7, got "+headers.length);
                    reader.close();
                    break;
                }
                String[] expectedHeaders = 
                {"Sample", "Project", "Demag", "Declin", "Inclin", "Intens", "Operation"};
                for (int i=0; i<expectedHeaders.length; i++) {
                    if (!expectedHeaders[i].equals(headers[i])) {
                        loadWarnings.add("Unknown header field "+headers[i]+" in file "+
                                file.getName()+" -- aborting load.");
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
                loadWarnings.add("I don't recognize the file\""+file+"\", so I'm ignoring it.");
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
        Writer writer = null;
        try {
            ArrayList<Sample> samples = new ArrayList<Sample>(getNumSamples());
            if (measType == MeasType.CONTINUOUS) {
                for (double depth: depths)
                    samples.add(getSampleByDepth(depth));
            } else {
                for (String name: names)
                    samples.add(getSampleByName(name));
            }
            if (samples.size()==0) {
                PuffinApp.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }

            writer = new FileWriter(file);
            writer.write(
                    (measType == MeasType.CONTINUOUS ? "depth" : "sample") +
                    "," + FisherValues.getHeader(",") +
                    "," + PcaValues.getHeader(",") + "\n");
            for (Sample sample: samples) {
                PcaValues pca = sample.getPca();
                String pcaCsv = pca==null ? ",,," : pca.toLine(",");
                FisherValues fish = sample.getFisher();
                String fishCsv = fish==null ? ",,," : fish.toLine(",");
                String sampleId = (measType == MeasType.CONTINUOUS) ?
                    String.format("%f", sample.getDepth()) :
                    sample.getName();
                writer.write(sampleId+","+fishCsv+","+pcaCsv+"\n");
            }
            
        } catch (IOException ex) {
            PuffinApp.errorDialog("Error saving file", ex.getMessage());
        } finally {
                try {
                    if (writer != null) writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(Suite.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
    }
    
    /*
     * Save [Fisher] calculations per site. Only works for discrete.
     */
    public void saveCalcsSite(File file) {
        Writer writer = null;
        try {
            if (siteFishers==null || siteFishers.size() == 0) {
                PuffinApp.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }
            // write them to a file
            writer = new FileWriter(file);
            writer.write("site,"+FisherValues.getHeader(",")+"\n");
            for (FisherForSite f: siteFishers) {
                writer.write(f.site + "," + f.fisher.toLine(",")+"\n");
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
        Writer writer = null;
        try {
            if (suiteFisher == null) {
                PuffinApp.errorDialog("Error saving calculations",
                        "No calculations to save!");
                return;
            }
            writer = new FileWriter(file);
            writer.write(FisherValues.getHeader(",")+"\n");
            writer.write(suiteFisher.toLine(",")+"\n");

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
