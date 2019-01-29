/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.TreatmentType;
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
            "^(.....)%s %s %s %s %s %s %s %s ([ 0-9]\\d[.]\\d) (.*)$",
            NUM_REGEX_1, NUM_REGEX_1, NUM_REGEX_1, NUM_REGEX_1,
            NUM_REGEX_2, NUM_REGEX_3, NUM_REGEX_2, NUM_REGEX_3);
    private static final Pattern LINE_REGEX = Pattern.compile(LINE_REGEX_STRING);
    public final TreatmentType treatmentType;
    public final int treatmentLevel;
    public final Vec3 moment;
    public final double magnetization;
    public final double sampleCorrectedDeclination;
    public final double sampleCorrectedInclination;
    public final double formationCorrectedDeclination;
    public final double formationCorrectedInclination;
    public final double alpha95;
    public final String comment;
    
    private static class PmdTreatment {
        /* Format 1: treatment type before number */
        private static final Pattern REGEX_1 =
                Pattern.compile("([HMT])(\\d\\d\\d) ");
        
        /* Format 2: treatment type after number */
        private static final Pattern REGEX_2 =
                Pattern.compile("(\\d{1,3})M(T ?)? ");
        
        /* Format 3: thermal treatment with "°C" suffix.
         * I've only seen this format in PMD files exported by Remasoft 3.
         * The regular expression also allows "░" for the degree symbol,
         * since this is produced if an ISO-8859-1 file is read using the
         * CP437 encoding. See comment in PmdLoader class for more details.
         */
        private static final Pattern REGEX_3 =
                Pattern.compile("(\\d{1,3})[░°]C {0,2}");

        public final TreatmentType type;
        public final int level;
        
        public PmdTreatment(TreatmentType type, int level) {
            this.type = type;
            this.level = level;
        }
        
        public static PmdTreatment parse(String s) {
            if ("NRM  ".equals(s)) {
                return new PmdTreatment(TreatmentType.NONE, 0);
            }
            final Matcher matcher1 = REGEX_1.matcher(s);
            if (matcher1.matches()) {
                final String typeCode = matcher1.group(1);
                TreatmentType type;
                switch (typeCode) {
                    case "H":
                    case "M":
                        type = TreatmentType.DEGAUSS_XYZ;
                        break;
                    case "T":
                        type = TreatmentType.THERMAL;
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                return new PmdTreatment(type,
                        Integer.parseInt(matcher1.group(2)));
            }
            final Matcher matcher2 = REGEX_2.matcher(s);
            if (matcher2.matches()) {
                return new PmdTreatment(TreatmentType.DEGAUSS_XYZ,
                        Integer.parseInt(matcher2.group(1)));
            }
            final Matcher matcher3 = REGEX_3.matcher(s);
            if (matcher3.matches()) {
                return new PmdTreatment(TreatmentType.THERMAL,
                        Integer.parseInt(matcher3.group(1)));                
            }
            throw new IllegalArgumentException();
        }
    }
    
    public PmdDataLine(TreatmentType treatmentType, int treatmentLevel,
                       double[] d, String comment) {
        this.treatmentType = treatmentType;
        this.treatmentLevel = treatmentLevel;
        this.moment = new Vec3(d[0], d[1], d[2]);
        this.magnetization = d[3];
        this.sampleCorrectedDeclination = d[4];
        this.sampleCorrectedInclination = d[5];
        this.formationCorrectedDeclination = d[6];
        this.formationCorrectedInclination = d[7];
        this.alpha95 = d[8];
        this.comment = comment;
    }
    
    public static PmdDataLine read(String line) {
        final Matcher matcher = LINE_REGEX.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }
        final double[] d = new double[9];
        for (int i=0; i<9; i++) {
            d[i] = Double.parseDouble(matcher.group(i+2));
        }
        final PmdTreatment treatment = PmdTreatment.parse(matcher.group(1));
        return new PmdDataLine(treatment.type, treatment.level, d,
                matcher.group(11));
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof PmdDataLine)) {
            return false;
        }
        final PmdDataLine pdl = (PmdDataLine) o;
        return treatmentType == pdl.treatmentType &&
                treatmentLevel == pdl.treatmentLevel &&
                moment.equals(pdl.moment) &&
                magnetization == pdl.magnetization &&
                sampleCorrectedDeclination == pdl.sampleCorrectedDeclination &&
                sampleCorrectedInclination == pdl.sampleCorrectedInclination &&
                formationCorrectedDeclination == pdl.formationCorrectedDeclination &&
                alpha95 == pdl.alpha95 &&
                comment.equals(pdl.comment);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.treatmentType);
        hash = 83 * hash + this.treatmentLevel;
        hash = 83 * hash + Objects.hashCode(this.moment);
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.magnetization) ^ (Double.doubleToLongBits(this.magnetization) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.sampleCorrectedDeclination) ^ (Double.doubleToLongBits(this.sampleCorrectedDeclination) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.sampleCorrectedInclination) ^ (Double.doubleToLongBits(this.sampleCorrectedInclination) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.formationCorrectedDeclination) ^ (Double.doubleToLongBits(this.formationCorrectedDeclination) >>> 32));
        hash = 83 * hash + (int) (Double.doubleToLongBits(this.alpha95) ^ (Double.doubleToLongBits(this.alpha95) >>> 32));
        hash = 83 * hash + Objects.hashCode(this.comment);
        return hash;
    }
    
}
