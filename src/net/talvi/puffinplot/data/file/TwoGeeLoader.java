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
import net.talvi.puffinplot.data.ArmAxis;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.Vec3;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.*;

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
        NORMAL, // just a sample measurement
        TRAY_NORMAL, // tray measurement then sample measurement
        NORMAL_TRAY, // sample measurement then tray measurement
        TRAY_NORMAL_YFLIP, // tray, sample, sample flipped around Y axis
        TRAY_FIRST; // single tray measurement, then sample measurements
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
        logger.log(Level.INFO,
                "Reading 2G file {0}.", file.toString());
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
        Vec3 trayMoment = null; // only used for TRAY_FIRST
        String line;
        while ((line = reader.readLine()) != null) {
            final Datum d = readDatum(line, reader.getLineNumber());
            // skip lines containing no data at all
            if (d == null || (!d.hasMagSus() && !d.hasMagMoment())) continue;
            // if the first line only is tray data, save it
            if (protocol == Protocol.TRAY_FIRST && reader.getLineNumber() == 2) {
                trayMoment = d.getMoment(Correction.NONE);
                continue;
            }
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
                            data.add(combine2(tray, normal, true));
                            break;
                        case NORMAL_TRAY:
                            normal = d;
                            tray = readDatum(reader.readLine(), reader.getLineNumber());
                            data.add(combine2(tray, normal, false));
                            break;
                        case TRAY_NORMAL_YFLIP:
                            tray = d;
                            normal = readDatum(reader.readLine(), reader.getLineNumber());
                            /* We're using the two-position measurement protocol,
                             * so we will read three lines (tray, normal, y-flipped)
                             * and synthesize a Datum from them. */
                            yflip = readDatum(reader.readLine(), reader.getLineNumber());
                            data.add(combine3(tray, normal, yflip));
                            break;
                        case TRAY_FIRST:
                            // can't use combine2 as we want the rest of the
                            // data from d, not the tray measurement
                            final Vec3 normV = d.getMoment(Correction.NONE);
                            d.setMoment(normV.minus(trayMoment));
                            data.add(d);
                            break;
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

    private Datum combine2(Datum tray, Datum normal, boolean useTrayData) {
        /* Subtract a tray measurement from a sample measurement
         */
        final Vec3 trayV = tray.getMoment(Correction.NONE);
        final Vec3 normV = normal.getMoment(Correction.NONE);
        // The volume correction's already been applied on loading.
        Datum result = useTrayData ? tray : normal;
        result.setMoment(normV.minus(trayV));
        return result;
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

        private boolean hasDouble(String name) {
            if (!fieldExists(name)) return false;
            try {
                Double.parseDouble(values[fields.get(name)]);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
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
                measTypeFromString(r.getString("Meas. type", "SAMPLE/DISCRETE"));

        d.setArea(r.getDouble("Area", d.getArea()));
        d.setVolume(r.getDouble("Volume", d.getVolume()));
        if (fieldExists("X corr") &&
                !r.getString("X corr", "dummy").equals("NA")) {
            final Vec3 momentGaussCm3 = new Vec3(r.getDouble("X corr", 0),
                    r.getDouble("Y corr", 0),
                    r.getDouble("Z corr", 0));
            // we have a raw magnetic moment in gauss * cm^3.
            // First we divide it by the sample volume in cm^3 to get a
            // magnetization in gauss.
            final Vec3 magnetizationGauss;
            switch (measType) {
            case CONTINUOUS:
                magnetizationGauss = momentGaussCm3.divideBy(sensorLengths.times(d.getArea()));
                break;
            case DISCRETE:
                magnetizationGauss = momentGaussCm3.divideBy(d.getVolume());
                break;
            default:
                magnetizationGauss = Vec3.ORIGIN;
            }
            // To avoid unpleasant 4pi factors, we follow common
            // palaeomagnetic practice and don't convert the cgs
            // magnetization to an SI magnetization (which would be
            // in Tesla). Instead we convert it to an equivalent
            // magnetic dipole moment per unit volume, expressed
            // in A/m.
            Vec3 momentPerUnitVolumeAm = gaussToAm(magnetizationGauss);
            d.setMoment(momentPerUnitVolumeAm);
        }
        d.setMagSus(r.getDouble("MS corr", d.getMagSus()));
        d.setDiscreteId(r.getString("Sample ID", d.getDiscreteId()));
        if (fieldExists("Depth")) {
            if (measType == MeasType.DISCRETE) {
                final int USER_SPECIFIED_DEPTH = 1;
                // For a discrete measurement, "Depth" actually contains the
                // sum of the (meaningless) user-specified data table depth
                // field and the slot number.
                // We assume that the data table depth field was set to 1
                // (TODO: make this configurable) and use this depth value
                // to set the slot number.
                String depthString = r.getString("Depth", d.getDepth());
                // The depth value is represented as a float (it has
                // a trailing .0 even if integral); if the depth field
                // in the data table were a non-integral value, this
                // presumably would also be one. Since I just want the
                // slot number and can't think of a case where a non-
                // integral depth would be useful in a discrete file,
                // I'm going to discard the fractional part here.
                int depth = (int) Double.parseDouble(depthString);
                d.setSlotNumber(depth - USER_SPECIFIED_DEPTH);
            } else /* assume continuous measurement */ {
                d.setDepth(r.getString("Depth", d.getDepth()));
            }
        }
        d.setMeasType(measType);
        d.setTreatType(treatTypeFromString(r.getString("Treatment Type", "AF")));
        if (r.hasDouble("AF X")) {
            d.setAfX(oerstedToTesla(r.getDouble("AF X", Double.NaN)));
        }
        if (r.hasDouble("AF Y")) {
            d.setAfY(oerstedToTesla(r.getDouble("AF Y", Double.NaN)));
        }
        if (r.hasDouble("AF Z")) {
            d.setAfZ(oerstedToTesla(r.getDouble("AF Z", Double.NaN)));
        }
        if (r.hasDouble("IRM Gauss")) {
            // Yes, they say Gauss, but I think they mean Oersted.
            d.setIrmField(oerstedToTesla(r.getDouble("IRM Gauss", Double.NaN)));
        }
        if (r.hasDouble("ARM Gauss")) {
            // Yes, they say Gauss, but I think they mean Oersted.
            d.setIrmField(oerstedToTesla(r.getDouble("aRM Gauss", Double.NaN)));
        }
        d.setArmAxis(ArmAxis.fromString(r.getString("ARM axis", "UNKNOWN")));
        // TODO better default ARM axis
        d.setTemp(r.getDouble("Temp C", d.getTemp()));
        d.setArmField(r.getDouble("ARM Gauss", d.getArmField()));
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
