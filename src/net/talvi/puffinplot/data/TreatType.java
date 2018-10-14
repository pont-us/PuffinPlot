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
package net.talvi.puffinplot.data;

/**
 * A type of treatment applied to a sample. The most common are alternating-field
 * and heating.
 * @author pont
 */
public enum TreatType {
    /** no treatment applied */
    NONE("No treatment", "", ""),
    /** static alternating-field treatment along three orthogonal axes */
    DEGAUSS_XYZ("3-axis degauss", "3-axis AF strength", "T"),
    /** static alternating-field treatment along one axis*/
    DEGAUSS_Z("Z-axis degauss", "Z-axis AF strength", "T"),
    /** anhysteretic remanent magnetization: alternating-field treatment with 
     a DC biasing field */
    ARM("Z-axis ARM", "AF strength", "T"), //AF usually more interesting than bias
    /** isothermal remanent magnetization: a pulsed non-alternating field */
    IRM("IRM", "IRM field", "T"),
    /** heating */
    THERMAL("Heating", "Temperature", "°C"),
    /** unknown treatment type */
    UNKNOWN("Unknown", "Unknown treatment", "?");

    // Human-friendly name for treatment
    private final String name;
    // The actual quantifiable `thing' applied (temperature or field)
    private final String treatment;
    // Unit name for the applied `thing'
    private final String unit;

    private TreatType(String name, String treatment, String unit) {
        this.name = name;
        this.treatment = treatment;
        this.unit = unit;
    }

    /** Returns a user-friendly name for this treatment.
     * @return a user-friendly name for this treatment */
    public String getNiceName() {
        return name;
    }
    
    /** Returns the axis label to use when plotting a graph involving this treatment.
     * @return the axis label to use when plotting a graph involving this treatment */
    public String getAxisLabel() {
        return treatment;
    }

    /** The units in which this treatment is quantified. 
     * @return the units in which this treatment is quantified */
    public String getUnit() {
        return unit;
    }
    
    /** @return {@code true} if this treatment involves application of
     * an alternating magnetic field */
    public boolean involvesAf() {
        return this==DEGAUSS_XYZ || this==DEGAUSS_Z || this==ARM;
    }
}
