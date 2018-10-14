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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import java.util.HashMap;
import java.util.Map;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

class TwoGeeHelper {

    private static final Map<String, TreatType> treatTypeMap =
            new HashMap<>();
    private static final Map<String, MeasType> measTypeMap =
            new HashMap<>();

    private static final String[] treatTypeMappings = {
        "NONE", "none",
        "DEGAUSS_XYZ", "degauss x, y, & z",
        "DEGAUSS_Z", "degauss z",
        "ARM", "degauss z - arm axial",
        "IRM", "irm",
        "THERMAL", "thermal demag",
        "UNKNOWN", "Unknown treatment"};

    private static final String[] measTypeMappings = {
        "DISCRETE", "sample/discrete",
        "CONTINUOUS", "sample/continuous",
        "NONE", "na"};

    static {
        for (int i=0; i<treatTypeMappings.length; i+=2) {
            treatTypeMap.put(treatTypeMappings[i+1],
                    TreatType.valueOf(treatTypeMappings[i]));
        }
        for (int i = 0; i < measTypeMappings.length; i += 2) {
            measTypeMap.put(measTypeMappings[i+1],
                    MeasType.valueOf(measTypeMappings[i]));
        }
    }

    /**
     * Sometimes people preprocess data with Excel, which helpfully
     * wraps field names in quotation marks. This peels them off again.
     */
    private static String normalizeString(String s) {
        s = s.toLowerCase();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length()-1);
        return s;
    }

    static TreatType treatTypeFromString(String treatType) {
        TreatType t = treatTypeMap.get(normalizeString(treatType));
        return t != null ? t : TreatType.UNKNOWN;
    }

    static MeasType measTypeFromString(String measType) {
        MeasType t = measTypeMap.get(normalizeString(measType));
        return t != null ? t : MeasType.UNKNOWN;
    }

    /**
     * Convert an intensity of magnetization in Gauss to an equivalent
     * SI moment per unit volume in A/m. See Butler, p. 13.
     *
     * @param gauss
     * @return
     */
    static double gaussToAm(double gauss) {
        return gauss * 1000;
    }

    /**
     * Convert an magnetization vector in Gauss to an equivalent
     * SI moment per unit volume vector in A/m. See Butler, p. 13.
     *
     * @param gauss
     * @return
     */
    static Vec3 gaussToAm(Vec3 gauss) {
        return gauss.times(1000);
    }

    /**
     * Convert a magnetic field strength in Oersted to an equivalent
     * SI magnetic induction in Tesla. See Butler, p. 13.
     *
     * @param oersted
     * @return
     */
    static double oerstedToTesla(double oersted) {
        return oersted / 10000;
    }

}
