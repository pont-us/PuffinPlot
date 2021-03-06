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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.regex.Pattern;

/** The type of measurement which was performed on a sample or set of samples.
 * 
 * @author pont
 */
public enum MeasurementType {
    /** measurement was on a discrete sample */
    DISCRETE("Discrete", "discrete", "Sample"),
    /** measurement was on a continuous long core or u-channel */
    CONTINUOUS("Continuous", "continuous", "Depth"),
    /** a measurement run was recorded, but no measurement was actually made */
    NONE("None", "^na$", "no measurement type"),
    /** this value has not yet been set */
    UNSET("UNSET", "$a", "UNSET"), // $a should never match -- http://stackoverflow.com/a/1723225
    /** the measurement type data could not be interpreted */
    UNKNOWN("UNKNOWN", "$a", "UNKNOWN");
    
    private final String columnHeader;
    private final Pattern namePattern;
    private final String niceName;
    
    private MeasurementType(String niceName, String namePattern, String columnHeader) {
        this.niceName = niceName;
        this.namePattern = Pattern.compile(namePattern,
                Pattern.CASE_INSENSITIVE);
        this.columnHeader = columnHeader;
    }
    
    private boolean matches(String name) {
        return namePattern.matcher(name).find();
    }
    
    /** Creates a measurement type from a string representation. 
     * @param string a string representation of a measurement type
     * @return the corresponding measurement type
     */
    public static MeasurementType fromString(String string) {
        for (MeasurementType mt: MeasurementType.values()) {
            if (mt.matches(string)) {
                return mt;
            }
        }
        return UNKNOWN;
    }

    /** Returns a suitable column header for sample identifiers.
     * This will be something like ‘sample name’ for discrete
     * measurements and ‘depth’ for continuous measurements.
     * @return a suitable column header for sample identifiers
     */
    public String getColumnHeader() {
        return columnHeader;
    }

    /** Returns {@code true} if this field corresponds to an actual measurement.
     * This is the case if the field is {@code DISCRETE} or {@code CONTINUOUS}
     * rather than one of the fields indicating a non-existent or unknown
     * measurement type.
     * @return {@code true} if this field corresponds to an actual measurement
     */
    public boolean isActualMeasurement() {
        return (this != NONE && this != UNSET && this != UNKNOWN);
    }

    /** Returns {@code true} if this field is {@code DISCRETE}. Convenience method. 
     * @return {@code true} if this field is {@code DISCRETE} */
    public boolean isDiscrete() {
        return this == DISCRETE;
    }

    /** Returns {@code true} if this field is {@code CONTINUOUS}. Convenience method. 
     * @return {@code true} if this field is {@code CONTINUOUS} */
    public boolean isContinuous() {
        return this == CONTINUOUS;
    }

    /**
     * @return a name suitable for display to a user
     */
    public String getNiceName() {
        return niceName;
    }

}
