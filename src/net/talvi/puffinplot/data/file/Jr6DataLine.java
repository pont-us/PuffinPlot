/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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

/**
 *
 * @author pont
 */
class Jr6DataLine {

    private final String name;
    private TreatType treatmentType;
    
    private final static String regexXyz = "([-0-9. ]{6})";
    private final static String regex4digits = "([-0-9. ]{4})";
    private final static String fullRegexString =
            "(.{10})(.{8})" + regexXyz + regexXyz + regexXyz +
            regex4digits + regex4digits + regex4digits + regex4digits + 
            regex4digits + regex4digits + regex4digits +
            "(  3|  6|  9| 12)(  0| 90)(  3|  6|  9| 12)(  0| 90)" +
            regex4digits;
    private final static Pattern regexPattern = Pattern.compile(fullRegexString);

    private Jr6DataLine(String string) {
        final Matcher matcher = regexPattern.matcher(string);
        final boolean matches = matcher.matches();
        name = matcher.group(1).trim();
        treatmentType = readTreatmentType(matcher.group(2));
    }
    
    private static TreatType readTreatmentType(String treatmentField) {
        switch (treatmentField.substring(0, 1)) {
            case "N":
                return TreatType.NONE;
            case "T":
                return TreatType.THERMAL;
            case "A":
                return TreatType.DEGAUSS_XYZ;
            case "M":
                return TreatType.ARM;
            default:
                throw new IllegalArgumentException("Unknown treatment code \""
                        + treatmentField.substring(0, 1) + "\"");
        }
    }
    
    static Jr6DataLine read(String string) {
        return new Jr6DataLine(string);
    }

    public String getName() {
        return name;
    }

    public TreatType getTreatmentType() {
        return treatmentType;
    }
    
}
