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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

/**
 * An axis along which a magnetic moment measurement was made.
 * 
 * @author pont
 */

public enum MeasurementAxis {
    /** the x axis */
    X,
    /** the y axis */
    Y,
    /** the z axis */
    Z,
    /** the inverted z axis */
    MINUSZ,
    /** a virtual axis used in the modified Zijderveld plot, corresponding
     to the direction of the horizontal component of a data point.
     @see net.talvi.puffinplot.plots.ZPlot */
    H;
}
