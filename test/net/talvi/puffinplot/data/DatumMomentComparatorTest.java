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
package net.talvi.puffinplot.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class DatumMomentComparatorTest {
    
    @Test
    public void testCompare() {
        assertTrue(makeComparator(MeasurementAxis.X).
                compare(new Datum(2, 9, 7), new Datum(1, 0, -2)) > 0);
        assertTrue(makeComparator(MeasurementAxis.X).
                compare(new Datum(2, 9, 7), new Datum(3, 0, -2)) < 0);
        assertTrue(makeComparator(MeasurementAxis.Y).
                compare(new Datum(2, 9, 7), new Datum(1, 0, -2)) > 0);
        assertTrue(makeComparator(MeasurementAxis.Y).
                compare(new Datum(2, -9, 7), new Datum(3, 0, -2)) < 0);
        assertTrue(makeComparator(MeasurementAxis.Z).
                compare(new Datum(2, 9, 7), new Datum(1, 0, -2)) > 0);
        assertTrue(makeComparator(MeasurementAxis.Z).
                compare(new Datum(2, -9, -7), new Datum(3, 0, -2)) < 0);
    }

    private static DatumMomentComparator makeComparator(MeasurementAxis axis) {
        return new DatumMomentComparator(axis, Correction.NONE);
    }
    
}
