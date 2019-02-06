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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentStepField;
import net.talvi.puffinplot.data.Vec3;

/**
 * Loader for the old UC Davis format. This format has only one line per
 * position (depth). The (x,y,z) and (d,i,i) values for each successive
 * treatment level are concatenated along this single line. The mapping between
 * column index and parameter/level is defined in the header line.
 *
 * @author pont
 */
public class UcDavisLoader extends AbstractFileLoader {

    private LineNumberReader reader;
    private final File file;
    private final Map<Object, Object> importOptions;

    private void readFile() throws IOException {

        final String headerLine = reader.readLine();
        if (headerLine == null) {
            addMessage("%s is empty", file.getName());
            return;
        }

        /**
         * First we loop through the fields in the header line and populate two
         * structures for subsequent use: a sorted set of treatment levels, and
         * a hash table mapping (parameter, level) pairs to column indices.
         */
        final SortedSet<Integer> levels = new TreeSet();
        final Map<ColumnDef, Integer> fieldMap = new HashMap<>();
        final String[] headers = headerLine.trim().split("\t");
        for (int i = 1; i < headers.length; i++) {
            final String header = headers[i];
            final ColumnDef cd = ColumnDef.fromHeader(header);
            levels.add(cd.treatmentLevel);
            fieldMap.put(cd, i);
        }

        String line;
        while ((line = reader.readLine()) != null) {
            if ("".equals(line.trim())) {
                continue; // skip blank lines
            }
            final String[] partStrings = line.split("\t");
            final List<Double> parts = Stream.of(partStrings).
                    map(Double::valueOf).collect(Collectors.toList());

            /**
             * Loop through the treatment levels defined in the header line,
             * using the field map to locate the column for each parameter/level
             * pair.
             */
            for (Integer level : levels) {
                final TreatmentStep step = new TreatmentStep();

                /**
                 * The file contains both Cartesian and polar data, for each
                 * depth/level pair, but they don't represent the same vector!
                 * There's no documentation on this file format, so it's not
                 * clear why this is. In newer 2G files, the polar data tends to
                 * have more corrections applied (e.g. for effective sensor
                 * length), so for now I'm ignoring the Cartesian values and
                 * initializing the datum using the polar vector.
                 */
                step.setMoment(Vec3.fromPolarDegrees(
                        parts.get(fieldMap.get(new ColumnDef(level,
                                TreatmentStepField.VIRT_MAGNETIZATION))),
                        parts.get(fieldMap.get(new ColumnDef(level,
                                TreatmentStepField.VIRT_INCLINATION))),
                        parts.get(fieldMap.get(new ColumnDef(level,
                                TreatmentStepField.VIRT_DECLINATION)))));
                step.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
                final double levelTesla = level / 1000.;
                step.setAfX(levelTesla);
                step.setAfY(levelTesla);
                step.setAfZ(levelTesla);
                step.setMeasurementType(MeasurementType.CONTINUOUS);
                step.setDepth(partStrings[0]);
                addTreatmentStep(step);
            }
        }
    }

    /**
     * Each column in a UC Davis file is associated with a (hopefully) unique
     * tuplet of parameter (e.g. X magnitude, declination) and treatment level.
     * The tuplets are defined in the header line. This class is a container for
     * those two values, and is used as a key for a hash table which maps a
     * (parameter, level) tuplet to a column index.
     *
     */
    private static class ColumnDef {

        public final Integer treatmentLevel;
        public final TreatmentStepField parameter;
        private final static Pattern HEADER_PATTERN
                = Pattern.compile("([^(]+)[(](\\d+)[)]");

        private ColumnDef(Integer treatmentLevel, TreatmentStepField parameter) {
            this.treatmentLevel = treatmentLevel;
            this.parameter = parameter;
        }

        /**
         * Determine a parameter from a header field specifier.
         *
         * @param s parameter specifier from the UC Davis file
         * @return the parameter type specified by the string
         */
        private static TreatmentStepField stringToField(String s) {
            switch (s) {
                case "X":
                    return TreatmentStepField.X_MOMENT;
                case "Y":
                    return TreatmentStepField.Y_MOMENT;
                case "Z":
                    return TreatmentStepField.Z_MOMENT;
                case "D":
                    return TreatmentStepField.VIRT_DECLINATION;
                case "I":
                    return TreatmentStepField.VIRT_INCLINATION;
                case "J":
                    return TreatmentStepField.VIRT_MAGNETIZATION;
                default:
                    return null;
            }
        }

        /**
         * Create a column definition from a header field string.
         *
         * @param header specifier string from the header line
         * @return column definition specified by the given string
         */
        public static ColumnDef fromHeader(String header) {
            final Matcher matcher = HEADER_PATTERN.matcher(header);
            matcher.matches();
            final TreatmentStepField field = stringToField(matcher.group(1));
            return new ColumnDef(Integer.parseInt(matcher.group(2)),
                    field);
        }

        /**
         * It's vital to override the hashCode function so that identical column
         * definitions have identical hash codes. We can then retrieve values
         * from the field map using newly constructed ColumnDef instances.
         *
         * @return
         */
        @Override
        public int hashCode() {
            return Objects.hash(treatmentLevel, parameter);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ColumnDef other = (ColumnDef) obj;
            if (!Objects.equals(this.treatmentLevel, other.treatmentLevel)) {
                return false;
            }
            if (this.parameter != other.parameter) {
                return false;
            }
            return true;
        }
    }

    /**
     * Create a loader for the old UC Davis file format.
     *
     * @param file file to load
     * @param importOptions import options (currently ignored)
     */
    public UcDavisLoader(File file, Map<Object, Object> importOptions) {

        treatmentSteps = new LinkedList<>();
        this.importOptions = importOptions;
        this.file = file;
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            messages.add("Error reading \"" + file.getName() + "\"");
            messages.add(e.getMessage());
        }

    }
}
