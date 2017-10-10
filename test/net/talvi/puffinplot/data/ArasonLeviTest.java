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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import net.talvi.puffinplot.data.ArasonLevi.ArithMean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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
        {-86, -86.5, -86.1, -87, -87.5, -87.2, -86.7, -86.7, -87.2, -87.1, -89, -89.9},
        {40.0, 41.0, 42.0, 43.0, 44.0, 45.0, 46.0, -1.0},
        {1.0},
        {56.72, -71.16, 38.46, 82.34, -79.92, 6.70, 83.26, -44.58, 22.96, 62.48},
        {41.46, -13.07, 23.30, -85.24, -55.97, -82.16, -11.73, 30.60, -26.05, 74.17},
        {-41.73, -13.53, -77.12, 64.31, 30.28, 54.24, -64.10, 11.15, -35.95, 68.74}
        
    };

    private static final double[][] outputs = {
        // ainc    ak     t63    a95    ierr  n   arithmean invvar studt63 studa95
        {8.565, 1.78808, 61.967, 36.253, 0, 20, 2.900, 2.36982, 35.929, 17.419},
        {87.495, 2602.30, 1.584, .851, 0, 12, 87.242, 2528.55, 1.123, 0.724},
        {-87.495, 2602.30, 1.584, .851, 0, 12, -87.242, 2528.55, 1.123, 0.724},
        {39.530, 15.0558, 20.939, 14.751, 0, 8, 37.500, 13.3447, 15.936, 13.115},
        {1.0, -1.0, 105.070, 154.158, 1, 1, 1.0, -1.0, 0, 0},
        {90.0, 0.681026, 85.898, 0.000, 0, 10, 15.726, 0.872842, 61.184, 43.868},
        {-90.00000000 , 0.349559  ,    95.173 ,  0.000, 0 ,10 ,-10.469 ,  1.14787    ,  53.353 , 38.253},
        { 0.0 ,  0.0   ,  105.070,   0.0, 0 ,10 , -0.371 ,  1.13131   ,   53.742  ,38.532}
        
        
    };

    // We want to check to a set number of significant figures.
    // We convert this to decimal places for JUnit.
    private static double delta(double expected, int sigfigs) {
        final int oom = (int) Math.ceil(Math.log10(Math.abs(expected)));
        final int dp = sigfigs - oom;
        return Math.pow(10, -dp);
    }

    /**
     * Tests calculate method of class ArasonLevi and ArasonLevi.ArithMean.
     */
    @Test
    public void testCalculate() {

        System.out.println("ARALEV");

        for (int i = 0; i < inputs.length; i++) {
            final List<Double> inputData = DoubleStream.of(inputs[i]).
                    mapToObj(Double::valueOf).
                    collect(Collectors.toList());

            final ArasonLevi result = ArasonLevi.calculate(inputData);
            final ArithMean mean = ArasonLevi.ArithMean.calculate(inputData);

            final double[] correct = outputs[i];
            assertEquals(inputData.size(), result.getN());
            assertEquals(correct[0], result.getMeanInc(), delta(correct[0], 4));
            assertEquals(correct[1], result.getKappa(), delta(correct[1], 6));
            assertEquals(correct[2], result.getT63(), 0.001);
            assertEquals(correct[3], result.getA95(), 0.001);
            assertEquals(inputData.size(), mean.getN());
            assertEquals(correct[6], mean.getMeanInc(), 0.001);
            assertEquals(correct[7], mean.getKappa(), delta(correct[7], 3));
            /*
             * The reference data is generated from the original Fortran
             * implementation of the Arason-Levi algorithm, which also includes
             * code for arithmetic mean calculation with t-value-based θ63 and
             * α95 estimates. The Fortran code interpolates a look-up table to
             * get these estimates. This can produce some fairly inaccurate θ63
             * values compared with the Java Commons Math implemented used by
             * PuffinPlot, which is why we only check this value to 1 s.f.
             * (Fortunately the table has a column of values for p=0.95, so
             * reference α95 values are more accurate and we can use 3 s.f.
             * there.)
             */
            assertEquals(correct[8], mean.getT63(), delta(correct[8], 1));
            assertEquals(correct[9], mean.getA95(), delta(correct[9], 3));
            assertEquals((int) correct[4], result.getErrorCode());
        }
    }
    
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    
    @Test
    public void testArasonLeviEmptyInputException() {
        exception.expect(IllegalArgumentException.class);
        ArasonLevi.calculate(Collections.emptyList());
    }
    
    @Test
    public void testArithMeanEmptyInputException() {
        exception.expect(IllegalArgumentException.class);
        ArithMean.calculate(Collections.emptyList());
    }
    
    @Test
    public void testInclinationTooLargeException() {
        exception.expect(IllegalArgumentException.class);
        ArasonLevi.calculate(Arrays.asList(1., -2., 3., 91., 4., -5., 6.));
    }

    @Test
    public void testInclinationTooSmallException() {
        exception.expect(IllegalArgumentException.class);
        ArasonLevi.calculate(Arrays.asList(1., -2., 3., -91., 4., -5., 6.));
    }
    
    @Test
    public void testArasonLeviIdenticalInclinations() {
        final double inc = 12;
        final ArasonLevi al = ArasonLevi.calculate(Arrays.asList(inc, inc, inc, inc, inc));
        assertEquals(0, al.getErrorCode());
        assertEquals(5, al.getN());
        assertEquals(inc, al.getMeanInc(), 0.00001);
        assertEquals(1e10, al.getKappa(), 1);
        assertEquals(0, al.getT63(), 0.00001);
        assertEquals(0, al.getA95(), 0.00001);
    }

//    @Test
//    public void findGoodInputs() {
//        Random rnd = new Random(11);
//        for (int i=0; i<1000; i++) {
//            final DoubleStream ds = rnd.doubles(-90, 90);
//            final List<Double> values =
//                    ds.limit(10).boxed().collect(Collectors.toList());
//            final ArasonLevi al = ArasonLevi.calculate(values);
//        }
//    }
    
    /**
     * Tests the implementation of the modified Bessel function in 
     * ArasonLevi. The class is already tested as part of calculate.
     * This extra test method exists to test it for negative inputs,
     * which isn't possible via calculate. All the functions that Bessel
     * calculates are either even or odd, so the values for negative
     * inputs are simply checked against the corresponding positive
     * inputs -- obviously thoroughly insufficient on its own, but
     * OK in conjunction with the full test of the
     * ArasonLevi.calculate method.
     * 
     * Since Bessel is a private class, it's instantiated via reflection.
     * 
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException 
     */
    @Test
    public void testBessel() throws SecurityException, NoSuchMethodException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchFieldException {
        final Class<?>[] innerClassArray = ArasonLevi.class.getDeclaredClasses();
        final Stream<Class> innerClassStream = Arrays.stream(innerClassArray);
        final Class besselClass = innerClassStream.
                filter(x -> "net.talvi.puffinplot.data.ArasonLevi$Bessel".
                        equals(x.getName())).
                findFirst().get();
        final Method calculate = 
                besselClass.getDeclaredMethod("calculate", double.class);
        final double[] values = {0, 1, 2, 3, 4, 5};
        for (double x: values) {
            final Object pos = calculate.invoke(null, 10);
            final Object neg = calculate.invoke(null, -10);
            checkBesselFieldsEqual(pos, neg);
        }
    }
    
    private void checkBesselFieldsEqual(Object expected, Object actual) throws
            IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException {
        final String[] fieldNames = {"bi0e", "bi1e", "bi1i0"};
        for (String fieldName: fieldNames) {
            double expValue = expected.getClass().
                    getDeclaredField(fieldName).
                    getDouble(expected);
            final double actValue = actual.getClass().
                    getDeclaredField(fieldName).
                    getDouble(actual);
            if (!"bi0e".equals(fieldName)) {
                expValue = -expValue;
            }
            assertEquals(expValue, actValue, 0.000001);
        }
    }
    
    
    
    // TODO: test more "abnormal" inputs: single value, all values identical.
    // TODO: Test illegal and abnormal inputs for ArasonLevi.ArithMean
}
