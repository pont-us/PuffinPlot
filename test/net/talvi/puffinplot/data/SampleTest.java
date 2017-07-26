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

import java.util.BitSet;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class SampleTest {
    
    public SampleTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of selectByTreatmentLevelRange method, of class Sample.
     */
    @Test
    public void testSelectByTreatmentLevelRange() {
        System.out.println("selectByTreatmentLevelRange");
        
        final Sample s = new Sample("Test sample", null);
        
        for (int i=0; i<10; i++) {
            final Datum d = new Datum(10-i, 20-i, 30-i);
            d.setTreatType(TreatType.DEGAUSS_XYZ);
            d.setAfX(i);
            d.setAfY(i);
            d.setAfZ(i);
            s.addDatum(d);
        }
        
        final double[] mins = {2.5, 5.5, 10.5, Double.NEGATIVE_INFINITY};
        final double[] maxs = {7.5, 6.5, 0, Double.POSITIVE_INFINITY};
        
        for (int i=0; i<mins.length; i++) {
        s.selectByTreatmentLevelRange(mins[i], maxs[i]);
        
        for (Datum d: s.getData()) {
            final boolean shouldBeSelected =
                    d.getTreatmentLevel() >= mins[i] &&
                    d.getTreatmentLevel() <= maxs[i];
            assertEquals(d.isSelected(), shouldBeSelected);
        }
        }
        
        s.selectByTreatmentLevelRange(Double.NaN, Double.NaN);
        // Result is undefined so we don't test it, but we're making sure
        // that calling with NaNs doesn't produce an exception.
    }
   
}
