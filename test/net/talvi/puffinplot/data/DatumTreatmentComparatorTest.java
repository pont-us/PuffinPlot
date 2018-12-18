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

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class DatumTreatmentComparatorTest {

    final DatumTreatmentComparator comparator =
                new DatumTreatmentComparator();
    
    
    /**
     * Test that comparisons of different treatment types produce 0.
     */
    @Test
    public void testCompareDifferentTreatmentTypes() {
        final Datum d0 = new Datum();
        d0.setTreatType(TreatType.DEGAUSS_Z);
        d0.setAfZ(100);
        final Datum d1 = new Datum();
        d1.setTreatType(TreatType.THERMAL);
        d1.setTemp(50);
        assertEquals(0, comparator.compare(d0, d1));
        assertEquals(0, comparator.compare(d1, d0));
    }
    
    @Test
    public void testCompareThermal() {
        final List<Datum> data = new ArrayList<>(3);
        for (int i=0; i<3; i++) {
            final Datum d = new Datum();
            d.setTreatType(TreatType.THERMAL);
            d.setTemp(i*100);
            data.add(d);
        }
        for (int i=0; i<3; i++) {
            assertEquals(0, comparator.compare(data.get(i), data.get(i)));
        }
        assertTrue(comparator.compare(data.get(0), data.get(1)) < 0);
        assertTrue(comparator.compare(data.get(1), data.get(0)) > 0);
        assertTrue(comparator.compare(data.get(1), data.get(2)) < 0);
        assertTrue(comparator.compare(data.get(2), data.get(1)) > 0);
    }
}
