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

import java.util.Comparator;

/**
 * <p>This class implements a specific type of comparison for datum objects.
 * It compares the magnetic moment measurements by looking only at one of the
 * orthogonal components of the magnetic moment. It also allows a correction
 * (for e.g. sample orientation) to be applied before the comparison.</p>
 * 
 * <p>The intended use for this class is to determine the maximum extent of 
 * an orthogonal projection of a set of data (e.g. in a Zijderveld plot)
 * in order to scale the plot appropriately.</p>
 * 
 * @author pont
 */
public class DatumComparator implements Comparator<Datum> {

    private final MeasurementAxis axis;
    private final Correction corr;

    /**
     * Creates a comparator which will compare magnetic moments along
     * the specified axis after applying the specified correction.
     * 
     * @param axis
     * @param corr 
     */
    public DatumComparator(MeasurementAxis axis, Correction corr) {
        this.axis = axis;
        this.corr = corr;
        // TODO make defensive copy of correction,
        // or make correction class immutable
    }

    /**
     * Compares the magnetic moments of the supplied datum objects
     * using the criteria specified in the constructor.
     * 
     * @param datum1 the first datum to be compared
     * @param datum2 the second datum to be compared
     * @return the result of the comparison (less than 0, 0, or greater
     * than 0, according as the value from the first datum is less than,
     * equal to, or greater than the corresponding value from the second datum)
     * @see java.lang.Double#compare(double, double) 
     */
    public int compare(Datum datum1, Datum datum2) {
        // TODO ensure that the moment is non-null.
        return Double.compare(datum1.getMoment(corr).getComponent(axis),
                datum2.getMoment(corr).getComponent(axis));
    }
}
