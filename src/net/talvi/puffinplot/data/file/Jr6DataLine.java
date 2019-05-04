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

import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.Vec3;

/**
 * A representation of the data contained in one line of a JR6 file.
 */
class Jr6DataLine {

    private final static String REGEX_XYZ = "([-0-9. ]{6})";
    private final static String REGEX_4_DIGITS = "([-0-9. ]{4})";
    private final static String FULL_REGEX_STRING =
            "(.{10})(.{8})" + REGEX_XYZ + REGEX_XYZ + REGEX_XYZ +
            REGEX_4_DIGITS + REGEX_4_DIGITS + REGEX_4_DIGITS + REGEX_4_DIGITS + 
            REGEX_4_DIGITS + REGEX_4_DIGITS + REGEX_4_DIGITS +
            "(  3|  6|  9| 12)(  0| 90)(  3|  6|  9| 12)(  0| 90)" +
            REGEX_4_DIGITS;
    private final static Pattern REGEX_PATTERN =
            Pattern.compile(FULL_REGEX_STRING);
    private final static Pattern INT_PATTERN =
            Pattern.compile("^[0-9]*$");

    private final String name;
    private final TreatmentType treatmentType;
    private final int treatmentLevel;
    private final Vec3 magnetization;
    private final int azimuth;
    private final int dip;
    private final int foliationAzimuth;
    private final int foliationDip;
    private final int lineationTrend;
    private final int lineationPlunge;
    private final OrientationParameters orientationParameters;

    private Jr6DataLine(String string, TreatmentType defaultTreatmentType) {
        final Matcher matcher = REGEX_PATTERN.matcher(string);
        final boolean matches = matcher.matches();
        if (!matches) {
            throw new IllegalArgumentException("Malformed JR6 line");
        }
        name = matcher.group(1).trim();
        final TreatmentType lineTreatmentType =
                readTreatmentType(matcher.group(2));
        treatmentType = lineTreatmentType == TreatmentType.UNKNOWN ?
                defaultTreatmentType : lineTreatmentType;
        treatmentLevel = readTreatmentLevel(matcher.group(2));
        magnetization = readMagnetization(matcher);
        final IntFunction<Integer> readInt =
                index -> Integer.parseInt(matcher.group(index).trim());
        azimuth = readInt.apply(7);
        dip = readInt.apply(8);
        foliationAzimuth = readInt.apply(9);
        foliationDip = readInt.apply(10);
        lineationTrend = readInt.apply(11);
        lineationPlunge = readInt.apply(12);
        orientationParameters = OrientationParameters.read(
                matcher.group(13), matcher.group(14),
                matcher.group(15), matcher.group(16));
    }
    
    private static TreatmentType readTreatmentType(String treatmentField) {
        final String trimmedField = treatmentField.trim();
        if (trimmedField.isEmpty()) {
            return TreatmentType.NONE;
        }
        if (representsInt(trimmedField)) {
            // No treatment code, but a valid number.
            return TreatmentType.UNKNOWN;
        }
        switch (treatmentField.substring(0, 1)) {
            case "N":
                return TreatmentType.NONE;
            case "T":
                return TreatmentType.THERMAL;
            case "A":
                return TreatmentType.DEGAUSS_XYZ;
            case "M":
                return TreatmentType.ARM;
            default:
                throw new IllegalArgumentException("Unknown treatment code \""
                        + treatmentField.substring(0, 1) + "\"");
        }
    }
    
    private static int readTreatmentLevel(String treatmentField) {
        final String trimmedField = treatmentField.trim();
        if (trimmedField.isEmpty() || "NRM".equals(trimmedField)) {
            return 0;
        } else if (representsInt(trimmedField)) {
            return Integer.parseInt(trimmedField);
        } else {
            final String numericPart = trimmedField.substring(1);
            return Integer.parseInt(numericPart);
        }
    }
    
    private static Vec3 readMagnetization(Matcher matcher) {
        final double scale = Math.pow(10,
                Integer.parseInt(matcher.group(6).trim()));
        return new Vec3(
                groupAsDouble(matcher, 3, scale),
                groupAsDouble(matcher, 4, scale),
                groupAsDouble(matcher, 5, scale));
    }
    
    private static double groupAsDouble(Matcher matcher, int index,
            double scale) {
        return Double.parseDouble(matcher.group(index).trim()) * scale;
    }
    
    private static boolean representsInt(String s) {
        return INT_PATTERN.matcher(s).matches();
    }

    /**
     * Read a line with a default treatment type; the supplied treatment type
     * is only used if none is specified in the line itself.
     * 
     * @param string a line from a JR6 file
     * @param defaultTreatmentType fallback treatment type to use if no
     *        treatment type is specified in the line
     * @return an object representing the data in the supplied line
     */
    public static Jr6DataLine read(String string,
            TreatmentType defaultTreatmentType) {
        return new Jr6DataLine(string, defaultTreatmentType);
    }

    /**
     * @return the sample name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the treatment type specified in the line, or (if there is none)
     * the default treatment type supplied to the constructor
     */
    public TreatmentType getTreatmentType() {
        return treatmentType;
    }

    /**
     * @return treatment level (°C or millitesla)
     */
    public int getTreatmentLevel() {
        return treatmentLevel;
    }

    /**
     * @return the magnetization vector
     */
    public Vec3 getMagnetization() {
        return magnetization;
    }

    /**
     * @return the sample azimuth in degrees
     */
    public int getAzimuth() {
        return azimuth;
    }

    /**
     * @return the sample dip in degrees
     */
    public int getDip() {
        return dip;
    }

    /**
     * @return the foliation azimuth in degrees
     */
    public int getFoliationAzimuth() {
        return foliationAzimuth;
    }

    /**
     * @return the foliation dip in degrees
     */
    public int getFoliationDip() {
        return foliationDip;
    }

    /**
     * @return the lineation trend in degrees
     */
    public int getLineationTrend() {
        return lineationTrend;
    }

    /**
     * @return the lineation plunge in degrees
     */
    public int getLineationPlunge() {
        return lineationPlunge;
    }

    /**
     * @return the orientation parameters defined in this line
     */
    public OrientationParameters getOrientationParameters() {
        return orientationParameters;
    }
    
    /**
     * @return the magnetization vector and sample and foliation orientations
     */
    public VectorAndOrientations getVectorAndOrientations() {
        return new VectorAndOrientations(magnetization, azimuth, dip,
                foliationAzimuth, foliationDip);
    }
}
