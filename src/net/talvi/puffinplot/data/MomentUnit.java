/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
 * <p>This enum represents a unit in which magnetic moment per
 * unit volume can be measured. It is intended for use in
 * the tabular file importer.</p>
 * 
 * @author pont
 */
public enum MomentUnit {
    
    /** amperes per metre (A/m) */
    AM(1),
    /** milliamperes per metre (mA/m) */
    MILLIAM(0.001);
    
    private final double factor;
    
    private MomentUnit(double factor) {
      this.factor = factor;
    }

    /**
     * @return the number by which a value measured in this unit should
     * be multiplied in order to produce a value measured in A/m.
     */
    public double getFactorForAm() {
        return factor;
    }
    
}
