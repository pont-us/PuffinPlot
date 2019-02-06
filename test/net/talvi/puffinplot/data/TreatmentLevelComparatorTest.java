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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class TreatmentLevelComparatorTest {

    final TreatmentLevelComparator comparator =
                new TreatmentLevelComparator();
    
    /**
     * Test that comparisons of different treatment types compare the treatment
     * levels (even when it doesn't make sense physically).
     */
    @Test
    public void testCompareDifferentTreatmentTypes() {
        final TreatmentStep d0 = new TreatmentStep();
        d0.setTreatmentType(TreatmentType.DEGAUSS_Z);
        d0.setAfZ(100);
        final TreatmentStep d1 = new TreatmentStep();
        d1.setTreatmentType(TreatmentType.THERMAL);
        d1.setTemp(50);
        assertTrue(comparator.compare(d0, d1) > 0);
        assertTrue(comparator.compare(d1, d0) < 0);
    }
    
    @Test
    public void testCompareThermal() {
        final List<TreatmentStep> data = new ArrayList<>(3);
        for (int i=0; i<3; i++) {
            final TreatmentStep d = new TreatmentStep();
            d.setTreatmentType(TreatmentType.THERMAL);
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
