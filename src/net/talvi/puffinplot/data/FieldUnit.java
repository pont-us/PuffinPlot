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
package net.talvi.puffinplot.data;

/**
 * This enum represents a unit in which magnetic field strength can be measured.
 * It is intended for use in the tabular file importer.
 *
 * @author pont
 */
public enum FieldUnit {
    /** tesla (T) */
    TESLA(1),
    /** millitesla (mT) */
    MILLITESLA(0.001),
    /** gauss (G) */
    GAUSS(1e-4),
    /** kilogauss (kG) */
    KILOGAUSS(0.1)
    ;
    
    private final double factor;

    private FieldUnit(double factor) {
        this.factor = factor;
    }
    
    /**
     * @return the number by which a value expressed in this unit must be 
     * multiplied, in order to produce a value expressed in tesla
     */
    public double getFactorForTesla() {
        return factor;
    }
}
