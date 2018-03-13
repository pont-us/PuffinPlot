/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

/**
 * A line of data (corresponding to a treatment step) in a PMD file.
 */
class PmdDataLine {
    
    private static final String NUM_REGEX_1 =
            "([- +]\\d[.]\\d\\dE[-+]\\d\\d)";
    private static final String NUM_REGEX_2 = "([ 0-9][ 0-9][ 0-9][.][0-9])";
    private static final String NUM_REGEX_3 = "([-0-9 ][-0-9 ][-0-9 ][.][0-9])";
    private static final String LINE_REGEX_STRING = String.format(
            "^(....) %s %s %s %s %s %s %s %s ([ 0-9]\\d[.]\\d) (.*)$",
            NUM_REGEX_1, NUM_REGEX_1, NUM_REGEX_1, NUM_REGEX_1,
            NUM_REGEX_2, NUM_REGEX_3, NUM_REGEX_2, NUM_REGEX_3);
    private static final Pattern LINE_REGEX = Pattern.compile(LINE_REGEX_STRING);
    public final TreatType treatmentType;
    public final int treatmentLevel;
    public final Vec3 magnetization;
    
    private static class PmdTreatment {
        private static final Pattern REGEX_1 =
                Pattern.compile("([HMT])(\\d\\d\\d)");
        private static final Pattern REGEX_2 =
                Pattern.compile("(\\d{1,3})M(T ?)?");

        public final TreatType type;
        public final int level;
        
        public PmdTreatment(TreatType type, int level) {
            this.type = type;
            this.level = level;
        }
        
        public static PmdTreatment parse(String s) {
            final Matcher matcher1 = REGEX_1.matcher(s);
            if (matcher1.matches()) {
                final String typeCode = matcher1.group(1);
                TreatType type;
                switch (typeCode) {
                    case "H":
                    case "M":
                        type = TreatType.DEGAUSS_XYZ;
                        break;
                    case "T":
                        type = TreatType.THERMAL;
                    default:
                        throw new IllegalArgumentException();
                }
                return new PmdTreatment(type,
                        Integer.parseInt(matcher1.group(2)));
            }
            final Matcher matcher2 = REGEX_2.matcher(s);
            if (matcher2.matches()) {
                return new PmdTreatment(TreatType.DEGAUSS_XYZ,
                        Integer.parseInt(matcher2.group(1)));
            }
            throw new IllegalArgumentException();
        }
    }
    
    public PmdDataLine(TreatType treatmentType, int treatmentLevel,
            Vec3 magnetization) {
        this.treatmentType = treatmentType;
        this.treatmentLevel = treatmentLevel;
        this.magnetization = magnetization;
        
    }
    
    public static PmdDataLine read(String line) {
        final Matcher matcher = LINE_REGEX.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        final PmdTreatment treatment = PmdTreatment.parse(matcher.group(1));
        return new PmdDataLine(treatment.type, treatment.level,
                new Vec3(Double.parseDouble(matcher.group(2)),
                        Double.parseDouble(matcher.group(3)),
                        Double.parseDouble(matcher.group(4))
        ));
    }
    
}
