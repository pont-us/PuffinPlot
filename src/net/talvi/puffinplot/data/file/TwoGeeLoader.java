package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

public class TwoGeeLoader extends AbstractFileLoader {

    private Vec3 sensorLengths = new Vec3(1, 1, 1);
    private Map<String,Integer> fields;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private static final int MAX_WARNINGS = 10;
    private File file;
    private LineNumberReader reader;

    public TwoGeeLoader(File file) {
        this.file = file;
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            addMessage(e.getMessage());
        } finally {
            try { reader.close(); } catch (IOException e2) {}
        }
    }

    private void readFile() throws IOException {
        String fileName = file.getName();
        String fieldsLine = reader.readLine();
        if (fieldsLine == null) {
            addMessage("%s is empty.", fileName);
            reader.close();
        } else {
            fields = new HashMap<String, Integer>();
            String[] fieldNames = fieldsLine.split(("\\t"));
            for (int i=0; i<fieldNames.length; i++)
                fields.put(fieldNames[i], i);
        }
        data = new LinkedList<Datum>();
        String line;
        while ((line = reader.readLine()) != null) {
            Datum d = readDatum(line, reader.getLineNumber());
            if (d != null) {
                if (d.isMagSusOnly()) {
                    /* The only way we can tie a mag. sus. value to a
                     * treatment step is by assuming it comes after
                     * the associated magnetic moment measurement.
                     * If the first line is mag. sus., we throw it away
                     * since we don't know the treatment step.
                     * 
                     */
                    if (data.size()>0) {
                        Datum dPrev = data.get(data.size() - 1);
                        if (!dPrev.isMagSusOnly()) {
                            dPrev.setMagSus(d.getMagSus());
                        } else {
                            data.add(d);
                        }
                    }
                } else {
                    data.add(d);
                }
            }
            if (messages.size() > MAX_WARNINGS) {
                addMessage("Too many errors in %s", fileName);
                break;
            }
        }
    }

    public void setSensorLengths(Vec3 v) {
        sensorLengths = v;
    }

    private Datum readDatum(String line, int lineNumber) {
        Datum d = null;
        if (emptyLine.matcher(line).matches()) return null;
        try {
            d = lineToDatum(line, lineNumber);
        } catch (IllegalArgumentException e) {
            addMessage("%s at line %d in file %s -- ignoring this line.",
                    e.getMessage(), lineNumber, file.getName());
        }
        return d;
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
        FieldReader r = new FieldReader(line);
        Datum d = new Datum();

        final MeasType measType = MeasType.fromString(r.getString("Meas. type"));

        if (fieldExists("Area")) d.setArea(r.getDouble("Area"));
        if (fieldExists("Volume")) d.setVolume(r.getDouble("Volume"));
        if (fieldExists("X corr") && ! r.getString("X corr").equals("NA")) {
            Vec3 moment = new Vec3(r.getDouble("X corr"),
                    r.getDouble("Y corr"),
                    r.getDouble("Z corr"));
            switch (measType) {
            case CONTINUOUS:
                moment = moment.divideBy(sensorLengths.times(d.getArea()));
                break;
            case DISCRETE:
                moment = moment.divideBy(d.getVolume());
                break;
            }
            d.setMoment(moment);
        }
        d.setMagSus(r.getDouble("MS corr"));
        if (fieldExists("Sample ID")) d.setSampleId(r.getString("Sample ID"));
        if (fieldExists("Depth")) d.setDepth(r.getString("Depth"));
        d.setMeasType(measType);
        d.setTreatType(TreatType.fromString(r.getString("Treatment Type")));
        d.setAfX(r.getDouble("AF X"));
        d.setAfY(r.getDouble("AF Y"));
        d.setAfZ(r.getDouble("AF Z"));
        d.setTemp(r.getDouble("Temp C"));
        d.setSampAz(r.getDouble("Sample Azimiuth")); // sic
        d.setSampDip(r.getDouble("Sample Dip"));
        d.setFormAz(r.getDouble("Formation Dip Azimuth"));
        d.setFormDip(r.getDouble("Formation Dip"));
        if (fieldExists("Mag Dev")) d.setMagDev(r.getDouble("Mag Dev"));
        return d;
    }
}
