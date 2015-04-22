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

import java.util.HashSet;
import java.util.Set;

/**
 * <p>A container class holding all the data within a suite from a
 * particular line number of their respective files. Intended to be
 * used as a proxy for run number. We can't use the actual run number,
 * because the Long Core software adds the position number to it for
 * discrete samples. We can't correct for that because it can't write
 * the position number to the DAT file.</p>
 * 
 * <p>This class is not currently used.</p>
 */

public class Line {

    private final int lineNumber;
    private final Set<Datum> data;
    // private Datum emptySlot;

    /** Creates a new Line with the specified line number. 
     * @param lineNumber the line number in the original data file
     */
    public Line(int lineNumber) {
        this.lineNumber = lineNumber;
        data = new HashSet<>();
    }

    /** Associates a Datum with this line. This indicates that the line number
     * which was passed to this Line's constructor is the same as the line
     * number on which the Datum occurred in the file from which it was read.
     * @param datum a datum to be associated with this line
     */
    public void add(Datum datum) {
        data.add(datum);
    }

    /** Returns the first Datum associated with this line which was a measurement
     * of an empty tray slot, not an actual sample. Returns null if no such Datum
     * exists.
     * @return the first Datum associated with this line which was a measurement
     * of an empty tray slot
     */
    public Datum getEmptySlot() {
        for (Datum d: data) {
            if (d.getSample().isEmptySlot()) return d;
        }
        return null;
    }

}
