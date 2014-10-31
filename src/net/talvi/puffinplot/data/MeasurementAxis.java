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

import net.talvi.puffinplot.plots.Direction;

/**
 * An axis along which a magnetic moment measurement was made.
 * 
 * @author pont
 */

public enum MeasurementAxis {
    /** the x axis */
    X(Direction.UP),
    /** the y axis */
    Y(Direction.RIGHT),
    /** the z axis */
    Z(null),
    /** the inverted X axis */
    MINUSX(Direction.DOWN),
    /** the inverted Y axis */
    MINUSY(Direction.LEFT),
    /** the inverted z axis */
    MINUSZ(null),
    /** a virtual axis used in the modified Zijderveld plot, corresponding
     to the direction of the horizontal component of a data point.
     @see net.talvi.puffinplot.plots.ZPlot */
    H(null);
    
    private MeasurementAxis opposite;
    private final Direction direction;
    
    static {
        X.opposite = MINUSX;
        Y.opposite = MINUSY;
        Z.opposite = MINUSZ;
        MINUSX.opposite = X;
        MINUSY.opposite = Y;
        MINUSZ.opposite = Z;
    }

    
    private MeasurementAxis(Direction direction) {
        this.direction = direction;
    }
    
    /**
     * Returns an axis pointing in the opposite direction to this axis.
     * 
     * @return the opposing axis
     */
    public MeasurementAxis opposite() {
        return opposite;
    }

    /**
     * Returns a compass (field) direction corresponding to this axis.
     * 
     * The returned direction should be interpreted as a compass
     * direction rather than a "paper" (up/down/left/right) direction.
     * The direction indicates the original field orientation corresponding
     * to this measurement axis.
     * 
     * This method is useful for determining labels on Zijderveld plots.
     * 
     * @return the compass direction for this axis
     */
    public Direction getDirection() {
        return direction;
    }
}
