package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

public class TwoGeeLoader extends AbstractFileLoader {

    private Vec3 sensorLengths = new Vec3(1, 1, 1);
    private Map<String,Integer> fields;
    private static final Pattern emptyLine = Pattern.compile("^\\s*$");
    private static final int MAX_WARNINGS = 10;
    private final File file;
    private LineNumberReader reader;
    private final boolean twoPosition;

    public TwoGeeLoader(File file, boolean twoPosition) {
        this.file = file;
        this.twoPosition = twoPosition;
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
                    if (twoPosition) {
                    /* We're using the two-position measurement protocol,
                     * so we will read three lines (tray, normal, y-flipped)
                     * and synthesize a Datum from them. */
                    final Datum tray = d;
                    final Datum normal = readDatum(reader.readLine(), reader.getLineNumber());
                    final Datum reversed = readDatum(reader.readLine(), reader.getLineNumber());
                    data.add(combine(tray, normal, reversed));
                    } else {
                        data.add(d);
                    }
                }
            }
            if (messages.size() > MAX_WARNINGS) {
                addMessage("Too many errors in %s", fileName);
                break;
            }
        }
    }

    private Datum combine(Datum tray, Datum normal, Datum reversed) {
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


        private int getInt(String name) {
            String v = values[fields.get(name)];
            // catch the common case without using an expensive exception
            if ("NA".equals(v)) return 0;
            try { return Integer.parseInt(v); }
            catch (NumberFormatException e) { return 0; }
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
        if (fieldExists("Sample ID")) d.setDiscreteId(r.getString("Sample ID"));
        if (fieldExists("Depth")) {
            if (measType == MeasType.DISCRETE) {
                // For a discrete measurement, "Depth" actually contains the
                // sum of the (meaningless) depth field and the slot number.
                // We assume that the depth field was set to 1 (TODO: make
                // this configurable) and use it to set the slot number.
                d.setSlotNumber((int) (r.getDouble("Depth") - 1.0));
            } else {
                d.setDepth(r.getString("Depth"));
            }
        }
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
            int runNumber = r.getInt("Run #");
            if (measType == MeasType.DISCRETE && d.getSlotNumber() != -1) {
                runNumber -= d.getSlotNumber();
            }
            d.setRunNumber(runNumber);
        }
        if (fieldExists("Sample Timestamp"))
            d.setTimestamp(r.getString("Sample Timestamp"));
        if (fieldExists("X drift")) d.setXDrift(r.getDouble("X drift"));
        if (fieldExists("Y drift")) d.setYDrift(r.getDouble("Y drift"));
        if (fieldExists("Z drift")) d.setZDrift(r.getDouble("Z drift"));
        return d;
    }
}
