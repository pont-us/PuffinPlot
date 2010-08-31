package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

public class TwoGeeLoader extends AbstractFileLoader {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private Vec3 sensorLengths = new Vec3(1, 1, 1);
    private Map<String,Integer> fields;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private static final int MAX_WARNINGS = 10;
    private final File file;
    private LineNumberReader reader;
    private final Protocol protocol;
    private Set<String> requestedFields = new HashSet<String>();

    public enum Protocol {
        NORMAL,
        TRAY_NORMAL,
        NORMAL_TRAY,
        TRAY_NORMAL_YFLIP;
    }
    
    public TwoGeeLoader(File file, Protocol protocol, Vec3 sensorLengths) {
        this.file = file;
        this.protocol = protocol;
        setSensorLengths(sensorLengths);
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
            final Datum d = readDatum(line, reader.getLineNumber());
            // skip lines containing no data at all
            if (!d.hasMagSus() && !d.hasMagMoment()) continue;
            if (d != null) {
                if (d.isMagSusOnly()) {
                    /* The only way we can tie a mag. sus. value to a
                     * treatment step is by assuming it comes after
                     * the associated magnetic moment measurement.
                     * If the first line is mag. sus., we throw it away
                     * since we don't know the treatment step.
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
                    Datum tray, normal, yflip;
                    switch (protocol) {
                        case NORMAL:
                            data.add(d);
                            break;
                        case TRAY_NORMAL:
                            tray = d;
                            normal = readDatum(reader.readLine(), reader.getLineNumber());
                            data.add(combine2(tray, normal));
                            break;
                        case NORMAL_TRAY:
                            normal = d;
                            tray = readDatum(reader.readLine(), reader.getLineNumber());
                            data.add(combine2(tray, normal));
                            break;
                        case TRAY_NORMAL_YFLIP:
                            tray = d;
                            normal = readDatum(reader.readLine(), reader.getLineNumber());
                            /* We're using the two-position measurement protocol,
                             * so we will read three lines (tray, normal, y-flipped)
                             * and synthesize a Datum from them. */
                            yflip = readDatum(reader.readLine(), reader.getLineNumber());
                            data.add(combine3(tray, normal, yflip));
                    }
                }
            }
            if (messages.size() > MAX_WARNINGS) {
                addMessage("Too many errors in %s", fileName);
                break;
            }
        }
        correlateFields();
    }

    private Datum combine2(Datum tray, Datum normal) {
        /* Subtract a tray measurement from a sample measurement
         */

        final Vec3 trayV = tray.getMoment(Correction.NONE);
        final Vec3 normV = normal.getMoment(Correction.NONE);
        // The volume correction's already been applied on loading.

        // Just change the moment in the tray datum, retain all other
        // fields, and return the tray datum.
        tray.setMoment(normV.minus(trayV));
        return tray;
    }

    private Datum combine3(Datum tray, Datum normal, Datum reversed) {
        /* We'll keep the rest of the data from the first (tray)
         * measurement, and just poke in the magnetic moment vector
         * calculated from the three readings.
         */

        final Vec3 trayV = tray.getMoment(Correction.NONE);
        final Vec3 normV = normal.getMoment(Correction.NONE);
        final Vec3 revV = reversed.getMoment(Correction.NONE);

        // The volume correction's already been applied on loading.

        // calculate estimates of the true magnetic moment
        final Vec3 norm_tray = normV.minus(trayV);
        final Vec3 norm_rev = normV.minus(revV).divideBy(2);
        final Vec3 rev_tray = revV.invert().minus(trayV);

        // average the relevant estimates for each axis
        final Vec3 avg_x_z = Vec3.mean(norm_tray, norm_rev, rev_tray);
        final Vec3 avg_y = Vec3.mean(norm_tray, rev_tray.minus(normV));

        // combine the axis estimates into a single vector
        Vec3 avg = new Vec3(avg_x_z.x, avg_y.y, avg_x_z.z);
        tray.setMoment(avg);
        return tray;
    }

    /**
     * Cross-correlate which fields have been requested during loading
     * with which fields were present in the file, to produce a report
     * on which fields which were (1) in the file but not requested and
     * (2) requested but not found
     */
    private void correlateFields() {
        Set<String> fileFieldSet = new HashSet(fields.keySet());
        Set<String> notUsed = new HashSet(fileFieldSet);
        notUsed.removeAll(requestedFields);
        Set<String> notInFile = new HashSet(requestedFields);
        notInFile.removeAll(fileFieldSet);
        logger.info(String.format("Field headers in file %s\n" +
                "Not found in file: %s\nIn file but ignored: %s", file,
                Arrays.toString(notInFile.toArray()),
                Arrays.toString(notUsed.toArray())));
    }

    private void setSensorLengths(Vec3 v) {
        sensorLengths = v;
    }

    private Datum readDatum(String line, int lineNumber) {
        Datum d = null;
        if (line == null) {
            throw new IllegalArgumentException("null line in readDatum");
        }
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
        requestedFields.add(name);
        return fields.containsKey(name);
    }

    private class FieldReader {

        private String[] values;
        
        private class UnknownFieldException extends Exception {
            private final String fieldName;
            public UnknownFieldException(String fieldName) {
                this.fieldName = fieldName;
            }
            public String getFieldName() {
                return fieldName;
            }
        }

        FieldReader(String line) {
            values = line.split("\\t");
        }

        private double getDouble(String name, double defaultValue) {
            if (!fieldExists(name)) {
                return defaultValue;
            }
            String v = values[fields.get(name)];
            // catch the common case without using an expensive exception
            if ("NA".equals(v)) return Double.NaN;
            try { return Double.parseDouble(v); }
            catch (NumberFormatException e) { return Double.NaN; }
        }

        private int getInt(String name, int defaultValue) {
                        if (!fieldExists(name)) {
                return defaultValue;
            }
            String v = values[fields.get(name)];
            // catch the common case without using an expensive exception
            if ("NA".equals(v)) return 0;
            try { return Integer.parseInt(v); }
            catch (NumberFormatException e) { return 0; }
        }

        private String getString(String name, String defaultValue) {
                        if (!fieldExists(name)) {
                return defaultValue;
            }
            return values[fields.get(name)];
        }
    }

    private Datum lineToDatum(String line, int lineNumber) {
        FieldReader r = new FieldReader(line);
        Datum d = new Datum();

        final MeasType measType =
                MeasType.fromString(r.getString("Meas. type", "DISCRETE"));

        d.setArea(r.getDouble("Area", d.getArea()));
        d.setVolume(r.getDouble("Volume", d.getVolume()));
        if (fieldExists("X corr") &&
                !r.getString("X corr", "dummy").equals("NA")) {
            Vec3 moment = new Vec3(r.getDouble("X corr", 0),
                    r.getDouble("Y corr", 0),
                    r.getDouble("Z corr", 0));
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
        d.setMagSus(r.getDouble("MS corr", d.getMagSus()));
        d.setDiscreteId(r.getString("Sample ID", d.getDiscreteId()));
        if (fieldExists("Depth")) {
            if (measType == MeasType.DISCRETE) {
                // For a discrete measurement, "Depth" actually contains the
                // sum of the (meaningless) depth field and the slot number.
                // We assume that the depth field was set to 1 (TODO: make
                // this configurable) and use it to set the slot number.
                String depthString = r.getString("Depth", d.getDepth());
                int depth = Integer.parseInt(depthString);
                d.setSlotNumber(depth - 1);
            } else {
                d.setDepth(r.getString("Depth", d.getDepth()));
            }
        }
        d.setMeasType(measType);
        d.setTreatType(TreatType.fromString(r.getString("Treatment Type", "AF")));
        d.setAfX(r.getDouble("AF X", d.getAfX()));
        d.setAfY(r.getDouble("AF Y", d.getAfY()));
        d.setAfZ(r.getDouble("AF Z", d.getAfZ()));
        d.setTemp(r.getDouble("Temp C", d.getTemp()));
        d.setSampAz(r.getDouble("Sample Azimiuth", d.getSampAz())); // sic
        d.setSampDip(r.getDouble("Sample Dip", d.getSampDip()));
        d.setFormAz(r.getDouble("Formation Dip Azimuth", d.getFormAz()));
        d.setFormDip(r.getDouble("Formation Dip", d.getFormDip()));
        d.setMagDev(r.getDouble("Mag Dev", d.getMagDev()));
        if (fieldExists("Run #")) {
            /* 2G discrete files don't store the run number; they store the
             * sum of the run number and the slot number in the run number
             * field. The slot number is not explicitly stored; fortunately,
             * for discrete samples, the depth field contains the slot 
             * number plus (I think) whatever number was entered for "depth"
             * in the sample data table. So provided that this field is
             * always given the same value, we can produce a corrected run
             * number.
             */
            int runNumber = r.getInt("Run #", 0);
            if (measType == MeasType.DISCRETE && d.getSlotNumber() != -1) {
                runNumber -= d.getSlotNumber();
            }
            d.setRunNumber(runNumber);
        }
        if (fieldExists("Sample Timestamp"))
            d.setTimestamp(r.getString("Sample Timestamp", "UNKNOWN"));
        d.setXDrift(r.getDouble("X drift", d.getXDrift()));
        d.setYDrift(r.getDouble("Y drift", d.getYDrift()));
        d.setZDrift(r.getDouble("Z drift", d.getZDrift()));
        return d;
    }
}
