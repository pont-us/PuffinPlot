package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;

public class TwoGeeLoader implements FileLoader {

    // Note that sensors may have negative effective lengths, depending
    // on how they're mounted. These are the absolute values, and some
    // may be negated when calculating magnetization vectors for long cores.
    // (See the constructor for details.)
    private static final double sensorLengthX = 4.628,
     sensorLengthY = 4.404,
     sensorLengthZ = 6.280;

    private final LineNumberReader reader;
    private Map<String,Integer> fields;
    private Datum nextDatum;
    private LoadingStatus status;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private MeasType measType;
    private final String fileName;
    private List<String> loadWarnings = new LinkedList<String>();
    private static final int MAX_WARNINGS = 10;

    public TwoGeeLoader(File file) throws IOException {
        fileName = file.getName();
        reader = new LineNumberReader(new FileReader(file));
        status = LoadingStatus.IN_PROGRESS;
        String fieldsLine = reader.readLine();
        if (fieldsLine == null) {
            addWarning("%s is empty.", fileName);
            reader.close();
            status = LoadingStatus.COMPLETE;
        } else {
            fields = new HashMap<String, Integer>();
            String[] fieldNames = fieldsLine.split(("\\t"));
            for (int i=0; i<fieldNames.length; i++)
                fields.put(fieldNames[i], i);
        }
        readNextDatum();
    }

    private void readNextDatum() {
        // TODO: skip malformed lines instead of ending up with nextLine == null
        try {
            String line;
            do {
                line = reader.readLine();
            } while (line != null && emptyLine.matcher(line).matches());
            if (line == null) {
                status = LoadingStatus.COMPLETE;
                reader.close();
            } else {
                final int lineNum = reader.getLineNumber();
                try {
                    nextDatum = lineToDatum(line, lineNum);
                } catch (IllegalArgumentException e) {
                    addWarning("%s at line %d in file %s -- " + "ignoring this line.",
                            e.getMessage(), lineNum, fileName);
                    if (loadWarnings.size() > MAX_WARNINGS) {
                        addWarning("Too many errors in %s -- " + "aborting load at line %d",
                                fileName, lineNum);
                        reader.close();
                        status = LoadingStatus.ABORTED;
                    }
                }
            }
        } catch (IOException ex) {
            addWarning("Error reading %s: %s", fileName, ex.getLocalizedMessage());
        }
    }

    public Datum getNext() {
        Datum d = nextDatum;
        readNextDatum();
        return d;
    }

    public LoadingStatus getStatus() {
        return status;
    }

    public List<String> getMessages() {
        return Collections.emptyList();
    }

    private void addWarning(String s, Object... args) {
        loadWarnings.add(String.format(s, args));
    }

    private boolean fieldExists(String name) {
        return fields.containsKey(name);
    }

    private class FieldReader {

        private String[] values;

        FieldReader(String line) {
            values = line.split("\\t");
        }

        private double getDouble(String name) {
            String v = values[fields.get(name)];
            // catch the common case without using an expensive exception
            if ("NA".equals(v)) return Double.NaN;
            try { return Double.parseDouble(v); }
            catch (NumberFormatException e) { return Double.NaN; }
        }

        private String getString(String name) {
            return values[fields.get(name)];
        }
    }

    private Datum lineToDatum(String line, int lineNumber) {
        final boolean oldSquid =
                PuffinApp.getInstance().getPrefs().isUseOldSquidOrientations();
        FieldReader r = new FieldReader(line);
        Datum d = new Datum(r.getDouble("X corr"),
                r.getDouble("Y corr"),
                r.getDouble("Z corr"));
        if (fieldExists("Sample ID")) d.setSampleId(r.getString("Sample ID"));
        if (fieldExists("Depth")) d.setDepth(r.getString("Depth"));
        d.setMeasType(MeasType.fromString(r.getString("Meas. type")));
        d.setTreatType(TreatType.fromString(r.getString("Treatment Type")));
        d.setAfX(r.getDouble("AF X"));
        d.setAfY(r.getDouble("AF Y"));
        d.setAfZ(r.getDouble("AF Z"));
        d.setTemp(r.getDouble("Temp C"));
        d.setSampAz(r.getDouble("Sample Azimiuth")); // sic
        d.setSampDip(r.getDouble("Sample Dip"));
        d.setFormAz(r.getDouble("Formation Dip Azimuth")); // sic
        d.setFormDip(r.getDouble("Formation Dip"));
        d.setMagDev(r.getDouble("Mag Dev"));
        return d;
    }
}
