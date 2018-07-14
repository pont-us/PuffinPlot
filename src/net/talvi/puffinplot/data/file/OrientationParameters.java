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

import java.util.Objects;

/**
 *
 * @author pont
 */
class OrientationParameters {
    
    public enum AzimuthParameter {
        A3, A6, A9, A12;
        
        public static AzimuthParameter read(String s) {
            final String t = s.trim();
            switch (t) {
                case "3": return A3;
                case "6": return A6;
                case "9": return A9;
                case "12": return A12;
                default:
                    throw new IllegalArgumentException(
                            "Unknown azimuthal parameter string \"" + s + "\"");
            }
        }
    }
    
    public enum DipParameter {
        D0, D90;
        public static DipParameter read(String s) {
            final String t = s.trim();
            switch (t) {
                case "0": return D0;
                case "90": return D90;
                default:
                    throw new IllegalArgumentException(
                            "Unknown dip parameter string \"" + s + "\"");
            }
        }
    }
    
    public final AzimuthParameter p1;
    public final DipParameter p2;
    public final AzimuthParameter p3;
    public final DipParameter p4;
    
    public OrientationParameters(AzimuthParameter p1,
            DipParameter p2, AzimuthParameter p3, DipParameter p4) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }
    
    public static OrientationParameters read(
            String p1, String p2, String p3, String p4) {
        return new OrientationParameters(
                AzimuthParameter.read(p1), DipParameter.read(p2),
                AzimuthParameter.read(p3), DipParameter.read(p4));
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof OrientationParameters) {
            final OrientationParameters o = (OrientationParameters) other;
            return p1 == o.p1 && p2 == o.p2 && p3 == o.p3 && p4 == o.p4;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.p1);
        hash = 37 * hash + Objects.hashCode(this.p2);
        hash = 37 * hash + Objects.hashCode(this.p3);
        hash = 37 * hash + Objects.hashCode(this.p4);
        return hash;
    }
}
