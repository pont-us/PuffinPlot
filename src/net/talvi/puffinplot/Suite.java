package net.talvi.puffinplot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
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
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import javax.management.ImmutableDescriptor;
import javax.swing.JOptionPane;

import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatType;
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
    private static final Pattern whitespacePattern =
            Pattern.compile("^\\w*$");
    private List<String> loadWarnings = new LinkedList<String>();

    public Iterator<Datum> iterator() {
        return data.iterator();
    }

    public List<String> getLoadWarnings() {
        return loadWarnings;
    }
    
    private class Fields {
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
        if (!whitespacePattern.matcher(line).matches()) {
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
        if (measType == MeasType.UNSET) measType = MeasType.CONTINUOUS;
        Datum d = new Datum(line, measType, TreatType.DEGAUSS);
        // XXX must ask or guess measurement/treatment type here
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
        
        // TODO: throw exceptions when loading fails, or when no samples in file!
        
        for (File file: files) {
            FileType fileType = FileType.guessFromName(file);
            switch (fileType) {
            case TWOGEE: {
                LineNumberReader reader = new LineNumberReader(new FileReader(file));
                Fields fields = new Fields(reader.readLine());
                if (fields.areAllUnknown()) {
                    loadWarnings.add(file.getName() + " doesn't look like a 2G file. " +
                            "Ignoring it.");
                } else {
                try {
                while ((line = reader.readLine()) != null) 
                    addLine2G(line, fields.fields, depthSet, nameSet);
                } catch (IllegalArgumentException e) {
                    loadWarnings.add(e.getMessage()+
                            " at line "+reader.getLineNumber()+
                            " in file "+file.getName()+" -- ignoring this line.");
                }
                reader.close();
                if (fields.unknown.size() > 0) {
                    loadWarnings.add(
                            "I didn't recognize the following field names,\n"+
                            "so I'm ignoring them:\n"+
                            fields.unknown);
                }
                }
                break;
            }

            case ZPLOT: {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                for (int i=0; i<7; i++) reader.readLine();     // skip the header fields
                while ((line = reader.readLine()) != null)
                    addLineZplot(line, depthSet, nameSet);
                reader.close();
                break; }

            case PUFFINPLOT:
                loadWarnings.add("Can't load PuffinPlot files yet: ignoring "+file.getName());
                break;
                
            case UNKNOWN:
                loadWarnings.add("I don't recognize the file\""+file+"\", so I'm ignoring it.");
                break;
                
            }
        }
        
        if (depthSet.size()==0 && nameSet.size()==0) {
            throw new IOException("There was no data in the selected files.");
        }
        loadWarnings = Collections.unmodifiableList(loadWarnings);
        depths = depthSet.toArray(depths);
        names = nameSet.toArray(names);
        setCurrentDepthIndex(0);
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

}
