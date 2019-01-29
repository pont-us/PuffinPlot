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
 *
 * @author pont
 */
class Jr6DataLine {

    private final static String regexXyz = "([-0-9. ]{6})";
    private final static String regex4digits = "([-0-9. ]{4})";
    private final static String fullRegexString =
            "(.{10})(.{8})" + regexXyz + regexXyz + regexXyz +
            regex4digits + regex4digits + regex4digits + regex4digits + 
            regex4digits + regex4digits + regex4digits +
            "(  3|  6|  9| 12)(  0| 90)(  3|  6|  9| 12)(  0| 90)" +
            regex4digits;
    private final static Pattern regexPattern = Pattern.compile(fullRegexString);

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

    private Jr6DataLine(String string) {
        final Matcher matcher = regexPattern.matcher(string);
        final boolean matches = matcher.matches();
        if (!matches) {
            throw new IllegalArgumentException("Malformed JR6 line");
        }
        name = matcher.group(1).trim();
        treatmentType = readTreatmentType(matcher.group(2));
        treatmentLevel = readTreatmentLevel(matcher.group(2));
        magnetization = readMagnetization(matcher);
        final IntFunction<Integer> readInt =
                (index) -> Integer.parseInt(matcher.group(index).trim());
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
        if ("NRM".equals(treatmentField.trim())) {
            return 0;
        }
        final String numericPart = treatmentField.trim().substring(1);
        return Integer.parseInt(numericPart);
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
    
    public static Jr6DataLine read(String string) {
        return new Jr6DataLine(string);
    }

    public String getName() {
        return name;
    }

    public TreatmentType getTreatmentType() {
        return treatmentType;
    }

    public int getTreatmentLevel() {
        return treatmentLevel;
    }

    public Vec3 getMagnetization() {
        return magnetization;
    }

    public int getAzimuth() {
        return azimuth;
    }

    public int getDip() {
        return dip;
    }

    public int getFoliationAzimuth() {
        return foliationAzimuth;
    }

    public int getFoliationDip() {
        return foliationDip;
    }

    public int getLineationTrend() {
        return lineationTrend;
    }

    public int getLineationPlunge() {
        return lineationPlunge;
    }

    public OrientationParameters getOrientationParameters() {
        return orientationParameters;
    }
    
    public VectorAndOrientations getVectorAndOrientations() {
        return new VectorAndOrientations(magnetization, azimuth, dip, foliationAzimuth, foliationDip);
    }

}
