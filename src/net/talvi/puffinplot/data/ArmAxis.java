/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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
package net.talvi.puffinplot.data;

/**
 * ArmAxis represents the axis along which an ARM 
 * (anhysteretic remanent magnetization) field has been applied.
 * 
 * @author pont
 */
public enum ArmAxis {
    /** an ARM axis lying along the axis of the magnetometer */
    AXIAL,
    /** no ARM was applied */
    NONE,
    /** the ARM axis is unknown, or cannot be represented by this enum */
    UNKNOWN;
    
    /**
     * <p>Creates an {@link ArmAxis} from the supplied string. The values
     * produced are as follows:</p>
     * 
     * <table>
     * <caption>ArmAxis values</caption>
     * <tr><th>Input</th><th>Result</th></tr>
     * <tr><td>{@code AXIAL}</td><td>{@code AXIAL}</td></tr>
     * <tr><td>{@code NONE}</td><td>{@code NONE}</td></tr>
     * <tr><td>{@code NA}</td><td>{@code NONE}</td></tr>
     * <tr><td>[any other string]</td><td>{@code UNKNOWN}</td></tr>
     * </table>
     * 
     * @param name a string specifying the ARM axis
     * @return the ARM axis specified by the supplied string
     */
    public static ArmAxis fromString(String name) {
        switch (name) {
            case "AXIAL":
                return AXIAL;
            case "NONE":
            case "NA":
                return NONE;
            default:
                return UNKNOWN;
        }
    }
}
