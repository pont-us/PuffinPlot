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
public class AralevTest {
    
    public AralevTest() {
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

    private static double[][] inputs = {
        {39, 28, 43, 33, 7, -25, 2, -16, 10, 15, 39, -3, -84, -72, 14, -5, -41, 47, -16, 43},
        {86, 86.5, 86.1, 87, 87.5, 87.2, 86.7, 86.7, 87.2, 87.1, 89, 89.9},
        {-86, -86.5, -86.1, -87, -87.5, -87.2, -86.7, -86.7, -87.2, -87.1, -89, -89.9}
    };
    
    private static double[][] outputs = {
        // ainc    ak     t63    a95    ierr
        {  8.565, 1.78808, 61.967, 36.253, 0},
        { 87.495, 2602.30,  1.584,   .851, 0},
        {-87.495, 2602.30,  1.584,   .851, 0}
    };
    
    // We want to check to a set number of significant figures.
    // We convert this to decimal places for JUnit.
    private static double delta(double expected, int sigfigs) {
        final int oom = (int) Math.ceil(Math.log10(Math.abs(expected)));
        final int dp = sigfigs - oom;
        return Math.pow(10, -dp);
    }
    
    /**
     * Test of calculate method, of class Aralev.
     */
    @Test
    public void testARALEV() {
        System.out.println("ARALEV");
        
        for (int i=0; i<inputs.length; i++) {
        final double[] testData = {};
            final List<Double> testDataList = DoubleStream.of(inputs[i]).mapToObj(Double::valueOf).
                    collect(Collectors.toList());

            final Aralev result = Aralev.calculate(testDataList);
            // 20   8.565   1.78807      61.967  36.253  0
            
            final double[] correct = outputs[i];
            assertEquals(correct[0], result.getAinc(), delta(correct[0], 4));
            assertEquals(correct[1], result.getAk(), delta(correct[1], 6));
            assertEquals(correct[2], result.getT63(), 0.001);
            assertEquals(correct[3], result.getA95(), 0.001);
            assertEquals((int) correct[4], result.getIerr());
        }
    }
}
