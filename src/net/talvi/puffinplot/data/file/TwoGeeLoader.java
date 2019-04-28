/* This file is part of PuffinPlot, a program for palaeomagnetic
 * treatmentSteps plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.ArmAxis;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;

import static net.talvi.puffinplot.data.file.TwoGeeHelper.gaussToAm;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.measTypeFromString;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.oerstedToTesla;
import static net.talvi.puffinplot.data.file.TwoGeeHelper.treatTypeFromString;

/**
 * A loader for the DAT files produced by the Long Core software supplied with
 * 2G Enterprises magnetometers.
 *
 * @author pont
 */
public class TwoGeeLoader implements FileLoader {

    private static final Logger LOGGER
            = Logger.getLogger("net.talvi.puffinplot");
    private Vec3 sensorLengths = new Vec3(1, 1, 1);
    private Map<String, Integer> fields;
    private static final Pattern EMPTY_LINE = Pattern.compile("^\\s*$");
    private static final int MAX_WARNINGS = 10;
    private File file;
    private LineNumberReader reader;
    private Protocol protocol;
    private Set<String> requestedFields = new HashSet<>();
    private boolean usePolarMoment; // use d/i/i rather than x/y/z fields

    /**
     * A measurement protocol. A protocol defines the order in which sample
     * measurements and empty-tray measurements are taken, and the sample
     * orientations during sample measurements.
     */
    public static enum Protocol {
        /**
         * just a sample measurement
         */
        NORMAL,
        
        /**
         * tray measurement then sample measurement
         */
        TRAY_NORMAL,
        
        /**
         * sample measurement then tray measurement
         */
        NORMAL_TRAY,
        
        /**
         * tray, then sample, then sample flipped around Y axis
         */
        TRAY_NORMAL_YFLIP,
        
        /**
         * single initial tray measurement, then sample measurements
         */
        TRAY_FIRST,
        
        /**
         * as TRAY_NORMAL but only use first tray correction
         */
        TRAY_NORMAL_IGNORE;
    }

    /**
     * Creates a new 2G loader using the supplied parameters.
     *
     * @param file the file to read
     */
    @Override
    public LoadedData readFile(File file, Map<Object, Object> options) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(options);
        this.file = file;
        this.protocol = (Protocol) options.get("protocol");
        this.sensorLengths = (Vec3) options.get("sensor_lengths");
        this.usePolarMoment = (Boolean) options.get("use_polar_moment");
        setSensorLengths(sensorLengths);
        try (LineNumberReader r = new LineNumberReader(new FileReader(file))) {
            reader = r;
            return readFile();
        } catch (IOException exception) {
            return new EmptyLoadedData("Error reading file: "
                    + exception.getMessage());
        }
    }

    private LoadedData readFile() throws IOException {
        LOGGER.log(Level.INFO, "Reading 2G file {0}.", file.toString());
        final SimpleLoadedData loadedData = new SimpleLoadedData();
        final String fileName = file.getName();
        final String fieldsLine = reader.readLine();
        if (fieldsLine == null) {
            loadedData.addMessage("%s is empty.", fileName);
            return loadedData; // The caller should close the reader for us.
        } else {
            fields = new HashMap<>();
            final String[] fieldNames = fieldsLine.split(("\\t"));
            for (int i = 0; i < fieldNames.length; i++) {
                fields.put(fieldNames[i], i);
            }
        }
        final List<TreatmentStep> treatmentSteps = new ArrayList<>();
        // trayMoment is only used for TRAY_FIRST and TRAY_NORMAL_IGNORE.
        Vec3 trayMoment = null;
        String line;
        while ((line = reader.readLine()) != null) {
            final TreatmentStep step =
                    readTreatmentStep(line, reader.getLineNumber(),
                            loadedData);
            // skip lines containing no data at all
            if (step == null || (!step.hasMagSus() && !step.hasMagMoment())) {
                continue;
            }
            // if the first line only is tray data, save it
            if (protocol == Protocol.TRAY_FIRST
                    && reader.getLineNumber() == 2) {
                trayMoment = step.getMoment(Correction.NONE);
                continue;
            }
            if (step.isMagSusOnly()) {
                /*
                 * The only way we can tie a mag. sus. value to a treatment step
                 * is by assuming it comes after the associated magnetic moment
                 * measurement. If the first line is MS only, or if the previous
                 * datum is also MS only, then there's no moment measurement to
                 * attach it to, and it gets its own datum.
                 */
                if (treatmentSteps.size() > 0) {
                    final TreatmentStep previousStep
                            = treatmentSteps.get(treatmentSteps.size() - 1);
                    if (!previousStep.isMagSusOnly()) {
                        previousStep.setMagSus(step.getMagSus());
                    } else {
                        treatmentSteps.add(step);
                    }
                } else {
                    treatmentSteps.add(step);
                }
            } else {
                TreatmentStep tray, normal, yflip;
                TreatmentStep combined = null;
                switch (protocol) {
                    case NORMAL:
                        combined = step;
                        break;
                    case TRAY_NORMAL:
                        tray = step;
                        normal = readTreatmentStep(reader.readLine(),
                                reader.getLineNumber(),
                                loadedData);
                        combined = combine2(tray, normal, true);
                        break;
                    case TRAY_NORMAL_IGNORE:
                        tray = step;
                        normal = readTreatmentStep(reader.readLine(),
                                reader.getLineNumber(),
                                loadedData);
                        if (trayMoment == null) {
                            trayMoment = tray.getMoment(Correction.NONE);
                        }
                        normal.setMoment(normal.getMoment(Correction.NONE).
                                minus(trayMoment));
                        combined = normal;
                        break;
                    case NORMAL_TRAY:
                        normal = step;
                        tray = readTreatmentStep(reader.readLine(),
                                reader.getLineNumber(),
                                loadedData);
                        combined = combine2(tray, normal, false);
                        break;
                    case TRAY_NORMAL_YFLIP:
                        tray = step;
                        normal = readTreatmentStep(reader.readLine(),
                                reader.getLineNumber(),
                                loadedData);
                        /*
                         * We're using the two-position measurement protocol, so
                         * we will read three lines (tray, normal, y-flipped)
                         * and synthesize a TreatmentStep from them.
                         */
                        yflip = readTreatmentStep(reader.readLine(),
                                reader.getLineNumber(),
                                loadedData);
                        combined = combine3(tray, normal, yflip);
                        break;
                    case TRAY_FIRST:
                        /*
                         * We can't use combine2 as we want the rest of the data
                         * from d, not the tray measurement.
                         */
                        final Vec3 normV = step.getMoment(Correction.NONE);
                        step.setMoment(normV.minus(trayMoment));
                        combined = step;
                        break;
                }
                if (combined == null) {
                    break;
                } else {
                    treatmentSteps.add(combined);
                }
            }
            if (loadedData.getMessages().size() > MAX_WARNINGS) {
                loadedData.addMessage("Too many errors in %s -- aborting.",
                        fileName);
                break;
            }
        }
        correlateFields();
        loadedData.setTreatmentSteps(treatmentSteps);
        return loadedData;
    }

    /**
     * Subtracts a tray measurement from a sample measurement.
     */
    private TreatmentStep combine2(TreatmentStep tray, TreatmentStep normal,
            boolean useTrayData) {
        if (tray == null || normal == null) {
            return null;
        }
        final Vec3 trayV = tray.getMoment(Correction.NONE);
        final Vec3 normV = normal.getMoment(Correction.NONE);
        // The volume correction's already been applied on loading.
        final TreatmentStep result = useTrayData ? tray : normal;
        result.setMoment(normV.minus(trayV));
        return result;
    }

    private static Vec3 vectorMean(Vec3... vectors) {
        return Vec3.mean(Arrays.asList(vectors));
    }

    private TreatmentStep combine3(TreatmentStep tray, TreatmentStep normal,
            TreatmentStep reversed) {
        if (tray == null || normal == null || reversed == null) {
            return null;
        }

        /*
         * We will keep the data from the first (tray) measurement, except that
         * we poke in a new value for the magnetic moment vector, calculated
         * from the three readings. The volume correction has already been
         * applied.
         */
        
        // 1. Get the three moment measurements from the treatment steps.
        final Vec3 trayV = tray.getMoment(Correction.NONE);
        final Vec3 normV = normal.getMoment(Correction.NONE);
        // revV has the X and Z axes flipped, but Y axis the same.
        final Vec3 revV = reversed.getMoment(Correction.NONE);

        // 2. Calculate estimates of the true magnetic moment.
        
        // norm_tray: tray-corrected normal measurement
        final Vec3 norm_tray = normV.minus(trayV);
        // norm_rev: average x and z from normal/reverse; y =~ 0
        final Vec3 norm_rev = normV.minus(revV).divideBy(2);
        // rev_tray: tray-corrected reverse measurement
        final Vec3 rev_tray = revV.minus(trayV);

        // 3. Average the relevant estimates for each axis.
        
        // for x and z, average tray-corr. norm, tc rev, and norm/rev average
        final Vec3 avg_x_z = vectorMean(norm_tray, norm_rev, rev_tray.invert());
        // for y, average tray-corrected normal and reversed
        // (since 'reversed' doesn't flip the Y axis)
        final Vec3 avg_y = vectorMean(norm_tray, rev_tray);

        // 4. Combine the individual axis estimates into a single vector.
        final Vec3 average = new Vec3(avg_x_z.x, avg_y.y, avg_x_z.z);
        tray.setMoment(average);
        return tray;
    }

    /**
     * Cross-correlate which fields have been requested during loading with
     * which fields were present in the file, to produce a report on which
     * fields which were (1) in the file but not requested and (2) requested but
     * not found
     */
    private void correlateFields() {
        final Set<String> fileFieldSet = new HashSet<>(fields.keySet());
        final Set<String> notUsed = new HashSet<>(fileFieldSet);
        notUsed.removeAll(requestedFields);
        final Set<String> notInFile = new HashSet<>(requestedFields);
        notInFile.removeAll(fileFieldSet);
        LOGGER.info(String.format(Locale.ENGLISH,
                "Field headers in file %s\n"
                + "Not found in file: %s\nIn file but ignored: %s", file,
                Arrays.toString(notInFile.toArray()),
                Arrays.toString(notUsed.toArray())));
    }

    private void setSensorLengths(Vec3 v) {
        sensorLengths = v;
    }

    private TreatmentStep readTreatmentStep(String line, int lineNumber,
            SimpleLoadedData loadedData) {
        if (line == null) {
            loadedData.addMessage("File ended unexpectedly at line %d -- "
                    + "is 2G Protocol correctly set?",
                    lineNumber);
            return null;
        }
        if (EMPTY_LINE.matcher(line).matches()) {
            return null;
        }
        try {
            return lineToTreatmentStep(line, lineNumber);
        } catch (IllegalArgumentException e) {
            loadedData.addMessage(
                    "%s at line %d in file %s -- ignoring this line.",
                    e.getMessage(), lineNumber, file.getName());
            return null;
        }
    }

    private boolean fieldExists(String name) {
        requestedFields.add(name);
        return fields.containsKey(name);
    }

    private class FieldReader {

        private final String[] values;

        FieldReader(String line) {
            values = line.split("\\t");
        }

        private double getDouble(String name, double defaultValue) {
            if (!fieldExists(name)) {
                return defaultValue;
            }
            String v = values[fields.get(name)];
            // Catch the common case without using an expensive exception.
            if ("NA".equals(v)) {
                return Double.NaN;
            }
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException e) {
                return Double.NaN;
            }
        }

        private boolean hasDouble(String name) {
            if (!fieldExists(name)) {
                return false;
            }
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
            final String value = values[fields.get(name)];
            // Catch the common case without using an expensive exception.
            if ("NA".equals(value)) {
                return 0;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return 0;
            }
        }

        private String getString(String name, String defaultValue) {
            if (!fieldExists(name)) {
                return defaultValue;
            }
            return values[fields.get(name)];
        }
    }

    private boolean fieldExistsAndIsValid(String fieldName, FieldReader r) {
        return fieldExists(fieldName)
                && !r.getString(fieldName, "dummy").equals("NA");
    }

    private Vec3 readMagnetizationVector(FieldReader r) {
        Vec3 result = null;
        if (usePolarMoment) {
            if (fieldExistsAndIsValid("Declination: Unrotated", r)) {
                result = Vec3.fromPolarDegrees(r.getDouble("Intensity", 0),
                        r.getDouble("Inclination: Unrotated", 0),
                        r.getDouble("Declination: Unrotated", 0));
            }
        } else {
            if (fieldExistsAndIsValid("X corr", r)) {
                result = new Vec3(r.getDouble("X corr", 0),
                        r.getDouble("Y corr", 0),
                        r.getDouble("Z corr", 0));
            } else if (fieldExistsAndIsValid("X intensity", r)) {
                result = new Vec3(r.getDouble("X intensity", 0),
                        r.getDouble("Y intensity", 0),
                        r.getDouble("Z intensity", 0));
            } else if (fieldExistsAndIsValid("X mean", r)) {
                result = new Vec3(r.getDouble("X mean", 0),
                        r.getDouble("Y mean", 0),
                        r.getDouble("Z mean", 0));
            }
        }
        return result;
    }

    private TreatmentStep lineToTreatmentStep(String line, int lineNumber) {
        final FieldReader r = new FieldReader(line);

        // The TreatmentStep will be initialized with a default volume and area.
        final TreatmentStep step = new TreatmentStep();

        final MeasurementType measurementType;
        if (fieldExists("Meas. type")) {
            measurementType = measTypeFromString(
                    r.getString("Meas. type", "sample/continuous"));
        } else if (fieldExists("Depth")) {
            measurementType = MeasurementType.CONTINUOUS;
        } else if (fieldExists("Position") && !fieldExists("Sample ID")) {
            measurementType = MeasurementType.CONTINUOUS;
        } else {
            measurementType = MeasurementType.DISCRETE;
        }

        /*
         * Default values for area and volume are hard-coded in the
         * TreatmentStep class, and will be used here if nothing is specified in
         * the file.
         */
        step.setArea(r.getDouble("Area", step.getArea()));
        step.setVolume(r.getDouble("Volume", step.getVolume()));

        final Vec3 momentGaussCm3 = readMagnetizationVector(r);
        if (momentGaussCm3 != null) {
            /*
             * we have a raw magnetic moment in gauss * cm^3 (a.k.a. emu). First
             * we divide it by the sample volume in cm^3 to get a magnetization
             * in gauss.
             */
            final Vec3 magnetizationGauss;
            switch (measurementType) {
                case CONTINUOUS:
                    if (usePolarMoment) {
                        /*
                         * Polar moments in 2G files are already corrected for
                         * sensor length and cross-sectional core area (and are
                         * thus not actually moments but magnetizations).
                         */
                        magnetizationGauss = momentGaussCm3;
                    } else {
                        magnetizationGauss = momentGaussCm3.
                                divideBy(sensorLengths.times(step.getArea()));
                    }
                    break;
                case DISCRETE:
                    if (usePolarMoment) {
                        /*
                         * Polar moments in 2G files are already corrected for
                         * sample volume (and are thus not actually moments but
                         * magnetizations).
                         */
                        magnetizationGauss = momentGaussCm3;
                    } else {
                        magnetizationGauss
                                = momentGaussCm3.divideBy(step.getVolume());
                    }
                    break;
                default:
                    magnetizationGauss = Vec3.ORIGIN;
            }
            /*
             * To avoid unpleasant 4pi factors, we follow common palaeomagnetic
             * practice and don't convert the cgs magnetization to an SI
             * magnetization (which would be in Tesla). Instead we convert it to
             * an equivalent magnetic dipole moment per unit volume, expressed
             * in A/m.
             */
            final Vec3 momentPerUnitVolumeAm = gaussToAm(magnetizationGauss);
            step.setMoment(momentPerUnitVolumeAm);
        }
        step.setMagSus(r.getDouble("MS corr", step.getMagSus()));
        step.setDiscreteId(r.getString("Sample ID", step.getDiscreteId()));
        if (fieldExists("Depth")) {
            if (measurementType == MeasurementType.DISCRETE) {
                final int USER_SPECIFIED_DEPTH = 1;
                /*
                 * For a discrete measurement, "Depth" actually contains the sum
                 * of the (meaningless) user-specified data table depth field
                 * and the slot number. We assume that the data table depth
                 * field was set to 1 (TODO: make this configurable) and use
                 * this depth value to set the slot number.
                 */
                final String depthString =
                        r.getString("Depth", step.getDepth());
                /*
                 * The depth value is represented as a float (it has a trailing
                 * .0 even if integral); if the depth field in the data table
                 * were a non-integral value, this presumably would also be one.
                 * Since I just want the slot number and can't think of a case
                 * where a non-integral depth would be useful in a discrete
                 * file, I'm going to discard the fractional part here.
                 */
                int depth = (int) Double.parseDouble(depthString);
                step.setSlotNumber(depth - USER_SPECIFIED_DEPTH);
            } else /*
             * assume continuous measurement
             */ {
                step.setDepth(r.getString("Depth", step.getDepth()));
            }
        }

        // Sometimes, continuous files use "Position" for the depth field
        if (measurementType.isContinuous()
                && !fieldExists("Depth") && fieldExists("Position")) {
            step.setDepth(r.getString("Position", step.getDepth()));
        }
        step.setMeasurementType(measurementType);

        if (fieldExists("Treatment Type")) {
            step.setTreatmentType(treatTypeFromString(
                    r.getString("Treatment Type", "degauss z")));
        } else if (fieldExistsAndIsValid("ARM Gauss", r)) {
            step.setTreatmentType(TreatmentType.ARM);
        } else if (fieldExistsAndIsValid("IRM Gauss", r)) {
            step.setTreatmentType(TreatmentType.IRM);
        } else if (fieldExistsAndIsValid("AF X", r)) {
            step.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
        } else if (fieldExistsAndIsValid("AF Z", r)) {
            step.setTreatmentType(TreatmentType.DEGAUSS_Z);
        } else if (fieldExistsAndIsValid("Temp C", r)) {
            step.setTreatmentType(TreatmentType.THERMAL);
        } else {
            step.setTreatmentType(TreatmentType.DEGAUSS_Z);
        }

        if (r.hasDouble("AF X")) {
            step.setAfX(oerstedToTesla(r.getDouble("AF X", Double.NaN)));
        }
        if (r.hasDouble("AF Y")) {
            step.setAfY(oerstedToTesla(r.getDouble("AF Y", Double.NaN)));
        }
        if (r.hasDouble("AF Z")) {
            step.setAfZ(oerstedToTesla(r.getDouble("AF Z", Double.NaN)));
        }
        if (r.hasDouble("IRM Gauss")) {
            // Yes, they say Gauss, but I think they mean Oersted.
            step.setIrmField(
                    oerstedToTesla(r.getDouble("IRM Gauss", Double.NaN)));
        }
        if (r.hasDouble("ARM Gauss")) {
            // Yes, they say Gauss, but I think they mean Oersted.
            step.setIrmField(
                    oerstedToTesla(r.getDouble("ARM Gauss", Double.NaN)));
        }
        step.setArmAxis(ArmAxis.fromString(r.getString("ARM axis", "UNKNOWN")));
        // TODO better default ARM axis
        step.setTemperature(r.getDouble("Temp C", step.getTemperature()));
        step.setArmField(r.getDouble("ARM Gauss", step.getArmField()));
        step.setSampAz(r.getDouble("Sample Azimiuth", step.getSampAz())); // sic
        step.setSampDip(r.getDouble("Sample Dip", step.getSampDip()));
        step.setFormAz(r.getDouble("Formation Dip Azimuth", step.getFormAz()));
        step.setFormDip(r.getDouble("Formation Dip", step.getFormDip()));
        step.setMagDev(r.getDouble("Mag Dev", step.getMagDev()));
        if (fieldExists("Run #")) {
            /*
             * 2G discrete files don't store the run number; they store the sum
             * of the run number and the slot number in the run number field.
             * The slot number is not explicitly stored; fortunately, for
             * discrete samples, the depth field contains the slot number plus
             * (I think) whatever number was entered for "depth" in the sample
             * data table. So provided that this field is always given the same
             * value, we can produce a corrected run number.
             */
            int runNumber = r.getInt("Run #", 0);
            if (measurementType == MeasurementType.DISCRETE
                    && step.getSlotNumber() != -1) {
                runNumber -= step.getSlotNumber();
            }
            step.setRunNumber(runNumber);
        }
        if (fieldExists("Sample Timestamp")) {
            step.setTimestamp(r.getString("Sample Timestamp", "UNKNOWN"));
        }
        step.setXDrift(r.getDouble("X drift", step.getXDrift()));
        step.setYDrift(r.getDouble("Y drift", step.getYDrift()));
        step.setZDrift(r.getDouble("Z drift", step.getZDrift()));
        return step;
    }
}
