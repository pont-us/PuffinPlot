package net.talvi.puffinplot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Point;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TwoGeeField;

public class Suite implements Iterable<Datum> {

    private List<Datum> data;
    private File[] files;
    private Double[] depths = {};
    private String[] names = {};
    private Map<Double, Sample> samplesByDepth;
    private Map<String, Sample> samplesByName;
    private int currentDepthIndex = 0;
    private MeasType measType;
    private String currentName;
    private String suiteName;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private static final Pattern whitespace = Pattern.compile("\\s+");
    private List<String> loadWarnings = new LinkedList<String>();

    public Iterator<Datum> iterator() {
        return data.iterator();
    }

    public List<String> getLoadWarnings() {
        return loadWarnings;
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
    
    private void addDatumLongcore(Datum d, Set<Double> depthSet) {
        if (!d.isMagSus()) {
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
        if (!d.isMagSus()) {
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
    
    private void addLine2G(String line, List<TwoGeeField> fields,
            Set<Double> depthSet, Set<String> nameSet) {
        if (!emptyLine.matcher(line).matches()) {
            Datum d = new Datum(line, fields);
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
        measType = MeasType.UNSET;
        String line;
        TreeSet<Double> depthSet = new TreeSet<Double>();
        TreeSet<String> nameSet = new TreeSet<String>();
        final int MAX_WARNINGS_PER_FILE = 3;
        
        // TODO: throw exceptions when loading fails, or when no samples in file!
        
        for (File file: files) {
            int warningsThisFile = 0;
            FileType fileType = FileType.guessFromName(file);
            LineNumberReader reader = null;
            fileTypeSwitch: switch (fileType) {
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
                            try {
                                addLine2G(line, fields.fields, depthSet, nameSet);
                            } catch (IllegalArgumentException e) {
                                loadWarnings.add(e.getMessage() +
                                        " at line " + reader.getLineNumber() +
                                        " in file " + file.getName() +
                                        " -- ignoring this line.");
                                if (++warningsThisFile > MAX_WARNINGS_PER_FILE) {
                                    loadWarnings.add("Too many errors in " +
                                            file.getName() +
                                            "-- aborting load at line " +
                                            reader.getLineNumber());
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

            case PUFFINPLOT:
                loadWarnings.add("Can't load PuffinPlot files yet: ignoring "+file.getName());
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
    }

    public void saveCalculations(File file) {
        Writer writer = null;
        try {
            writer = new FileWriter(file);
            ArrayList<Sample> samples = new ArrayList<Sample>(getNumSamples());
            if (measType == MeasType.CONTINUOUS) {
                for (double depth: depths)
                    samples.add(getSampleByDepth(depth));
            } else {
                for (String name: names)
                    samples.add(getSampleByName(name));
            }

            writer.write(measType == MeasType.CONTINUOUS ?
                "\"Depth\", " : "\"Sample\", ");
            writer.write("\"Fisher inc.\", \"Fisher dec.\", \"a95\", \"k\", "+
                    "\"PCA inc.\", \"PCA dec.\", \"MAD1\", \"MAD3\"\n");
            for (Sample sample: samples) {
                String fishCsv = ",,,";
                String pcaCsv = ",,,";
                PcaValues pca = sample.getPca();
                if (pca != null) {
                    pcaCsv = String.format("%.1f, %.1f, %.1f, %.1f",
                            pca.getIncDegrees(), pca.getDecDegrees(),
                            pca.getMad1(), pca.getMad3());
                }
                FisherValues fish = sample.getFisher();
                if (fish != null) {
                    Point dir = fish.getMeanDirection();
                    fishCsv = String.format("%.1f, %.1f, %.1f, %.1f",
                            dir.decDegrees(), dir.incDegrees(),
                            fish.getA95(), fish.getK());
                }
                String sampleId = (measType == MeasType.CONTINUOUS) ?
                    String.format("%f", sample.getDepth()) :
                    sample.getName();
                writer.write(sampleId+", "+fishCsv+", "+pcaCsv+"\n");
            }
            
        } catch (IOException ex) {
            PuffinApp.errorDialog("Error saving file", ex.getMessage());
        } finally {
                try {
                    if (writer != null) writer.close();
                } catch (IOException ex) {
                    Logger.getLogger(PuffinActions.class.getName()).log(Level.SEVERE, null, ex);
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
