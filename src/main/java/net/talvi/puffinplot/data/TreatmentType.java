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

/**
 * A type of treatment applied to a sample. The most common are alternating-field
 * and heating.
 * @author pont
 */
public enum TreatmentType {
    /**
     * No treatment applied.
     */
    NONE("No treatment", "", "", false),
    /**
     * Static alternating-field treatment along three orthogonal axes.
     */
    DEGAUSS_XYZ("3-axis degauss", "3-axis AF strength", "T", true),
    /**
     * Static alternating-field treatment along one axis.
     */
    DEGAUSS_Z("Z-axis degauss", "Z-axis AF strength", "T", true),
    /**
     * Anhysteretic remanent magnetization: alternating-field treatment with a
     * DC biasing field. The associated treatment level is assumed to be the
     * alternating-field strength rather than the bias field.
     */
    ARM("Z-axis ARM", "AF strength", "T", true),
    /**
     * Isothermal remanent magnetization: a pulsed non-alternating field.
     */
    IRM("IRM", "IRM field", "T", true),
    /**
     * Heating.
     */
    THERMAL("Heating", "Temperature", "°C", false),
    /**
     * Unknown treatment type.
     */
    UNKNOWN("Unknown", "Unknown treatment", "?", false);

    // Human-friendly name for treatment
    private final String name;
    /*
     * A description of what the treatment level measures (e.g. "temperature"
     * for thermal treatment).
     */
    private final String treatment;
    // Unit name for the applied `thing'
    private final String unit;
    // True if and only if this is a magnetic (e.g. AF, IRM) treatment
    private final boolean magnetic;

    private TreatmentType(String name, String treatment, String unit,
            boolean magnetic) {
        this.name = name;
        this.treatment = treatment;
        this.unit = unit;
        this.magnetic = magnetic;
    }

    /**
     * Returns a user-friendly name for this treatment.
     *
     * @return a user-friendly name for this treatment
     */
    public String getNiceName() {
        return name;
    }
    
    /**
     * Returns the axis label to use when plotting a graph involving this
     * treatment.
     *
     * @return the axis label to use when plotting a graph involving this
     * treatment
     */
    public String getAxisLabel() {
        return treatment;
    }

    /**
     * The units in which this treatment is quantified.
     *
     * @return the units in which this treatment is quantified
     */
    public String getUnit() {
        return unit;
    }
    
    /**
     * @return {@code true} if and only if this treatment involves application
     * of a magnetic field
     */
    public boolean isMagneticField() {
        return magnetic;
    }
}
