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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import net.talvi.puffinplot.data.ArasonLevi.Mean;
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
public class ArasonLeviTest {
    
    public ArasonLeviTest() {
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

    private static final double[][] inputs = {
        {39, 28, 43, 33, 7, -25, 2, -16, 10, 15, 39, -3, -84, -72, 14, -5, -41, 47, -16, 43},
        {86, 86.5, 86.1, 87, 87.5, 87.2, 86.7, 86.7, 87.2, 87.1, 89, 89.9},
        {-86, -86.5, -86.1, -87, -87.5, -87.2, -86.7, -86.7, -87.2, -87.1, -89, -89.9}
    };
    
    private static final double[][] outputs = {
        // ainc    ak     t63    a95    ierr  n   arithmean invvar studt63 studa95
        {  8.565, 1.78808, 61.967, 36.253, 0, 20,   2.900, 2.36982, 35.929, 17.419},
        { 87.495, 2602.30,  1.584,   .851, 0, 12,  87.242, 2528.55,  1.123, 0.724},
        {-87.495, 2602.30,  1.584,   .851, 0, 12, -87.242, 2528.55,  1.123, 0.724}
    };
    
    // We want to check to a set number of significant figures.
    // We convert this to decimal places for JUnit.
    private static double delta(double expected, int sigfigs) {
        final int oom = (int) Math.ceil(Math.log10(Math.abs(expected)));
        final int dp = sigfigs - oom;
        return Math.pow(10, -dp);
    }
    
    /**
     * Tests calculate method of class ArasonLevi.
     */
    @Test
    public void testCalculate() {
        
        // TODO: test "bad" inputs (zero-length arrays etc.)
        
        System.out.println("ARALEV");
        
        for (int i=0; i<inputs.length; i++) {
            final List<Double> testDataList = DoubleStream.of(inputs[i]).mapToObj(Double::valueOf).
                    collect(Collectors.toList());

            final ArasonLevi result = ArasonLevi.calculate(inputs[i]);
            final Mean mean = ArasonLevi.Mean.calculate(inputs[i]);
            
            final double[] correct = outputs[i];
            assertEquals(correct[0], result.getMlMeanInc(), delta(correct[0], 4));
            assertEquals(correct[1], result.getMlKappa(), delta(correct[1], 6));
            assertEquals(correct[2], result.getMlT63(), 0.001);
            assertEquals(correct[3], result.getMlA95(), 0.001);
            assertEquals(correct[6], mean.getMeanInclination(), 0.001);
            assertEquals(correct[7], mean.getInverseVariance(), delta(correct[7], 3));
            
            // The reference data is generated from the original Fortran
            // implementation of the Arason-Levi algorithm, which interpolates
            // a look-up table to get the θ63 and α95 estimates. This can 
            // produce some fairly inaccurate θ63 values, which is why we
            // only check this value to 1 s.f. (Fortunately the table has a 
            // column of values for p=0.95, so reference α95 values are more
            // accurate and we can use 3 s.f. there.)
            assertEquals(correct[8], mean.getT63(), delta(correct[8], 1));
            
            assertEquals(correct[9], mean.getA95(), delta(correct[9], 3));
            assertEquals((int) correct[4], result.getErrorCode());
        }
    }
}
