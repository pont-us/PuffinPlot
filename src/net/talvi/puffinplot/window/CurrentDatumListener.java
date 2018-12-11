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
package net.talvi.puffinplot.window;

import net.talvi.puffinplot.data.Datum;

/**
 * A listener for changes in the current datum (i.e. treatment step).
 * "Current" in this context means the one over which the mouse pointer
 * is hovering.
 * 
 * @author pont
 */
public interface CurrentDatumListener {
    
    /**
     * Called when the current datum changes, generally as a result of
     * the mouse pointer moving.
     * 
     * @param d the datum represented by the point currently under the mouse
     * pointer, or {@code null} if there is no datum-related point under the
     * mouse pointer.
     */
    public void datumChanged(Datum d);
}
