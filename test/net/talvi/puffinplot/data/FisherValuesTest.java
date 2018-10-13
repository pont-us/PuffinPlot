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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.talvi.puffinplot.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

public class FisherValuesTest {
    
    /**
     * Test results of FisherValues.calculate on predefined lists of
     * pseudorandom vectors, checking output against hard-coded values
     * precalculated by PmagPy. This method also tests the parameter getters,
     * since they are used to retrieve the calculated values.
     */
    @Test
    public void testCalculateWithPseudorandomValues() {
        /* The value of the random seed is important. It has to match the
         * seed used to generate the data from which the reference values 
         * were calculated.
         */
        final Random rnd = new Random(23);
        final List<Vec3> testVectors = new ArrayList<>(100);
        for (int i=0; i<100; i++) {
            testVectors.add(new Vec3(rnd.nextDouble()-0.5,
                    rnd.nextDouble()-0.5,
                    rnd.nextDouble()-0.5).normalize());
        }
        
        /* TODO Generate some more plausible test data: these are completely
         * random directions, so they all have very low k and high a95
         * compared to real data sets.
         */
        
        // Generated by PmagPy's gofish.py. csd not currently used.
        final double[][] expectedResults = {
            // dec  inc     N       r    k    a95  csd (circular std. dev.)
            { 47.0, -18.9,  5, 1.4668, 1.1, 180.0, 76.1},
            {327.1,  61.7,  6, 1.1605, 1.0, 180.0, 79.7},
            {147.2, -60.4,  7, 3.1063, 1.5,  79.1, 65.3},
            { 17.1,   1.8,  8, 2.8420, 1.4,  88.2, 69.5},
            {241.3, -44.0,  9, 4.2818, 1.7,  60.0, 62.2},
            {  6.8,  30.2, 10, 2.6890, 1.2, 180.0, 73.0},
            { 75.5, -32.0, 11, 3.4213, 1.3,  76.9, 70.5},
            {234.3,  55.3, 12, 2.3751, 1.1, 180.0, 75.8},
            {309.4,  12.6, 13, 2.4438, 1.1, 180.0, 76.0},
            {346.3,  61.7, 14, 5.1583, 1.5,  56.2, 66.8},
        };
        
        /* Step through the input list, slicing out chunks of increasing
         * length from 5 to 14 and calculating Fisher stats on each one.
         */
        for (int startPos = 0, sublistLength=5, resultIndex=0;
                sublistLength<15;
                startPos += sublistLength++, resultIndex++) {
            final List<Vec3> sublist =
                    testVectors.subList(startPos, startPos+sublistLength);
            final FisherValues fv = FisherValues.calculate(sublist);
            for (int i=0; i<sublist.size(); i++) {
                assertTrue(sublist.get(i).
                        equals(fv.getDirections().get(i), 1e-6));
            }
            
            assertEquals(expectedResults[resultIndex][0],
                    fv.getMeanDirection().getDecDeg(), 0.1);
            assertEquals(expectedResults[resultIndex][1],
                    fv.getMeanDirection().getIncDeg(), 0.1);
            assertEquals(expectedResults[resultIndex][2], fv.getN(), 0);
            assertEquals(expectedResults[resultIndex][3], fv.getR(), 0.0001);
            assertEquals(expectedResults[resultIndex][4], fv.getK(), 0.1);
            final double expectedA95 = expectedResults[resultIndex][5];
            if (expectedA95 < 180) {
                assertEquals(expectedA95, fv.getA95(), 0.1);
            } else {
                /* There's a discrepancy here between PmagPy and
                 * PuffinPlot: PmagPy sets any a95 greater than
                 * 90 to 180. I'm not sure that this is justified
                 * (neither Butler nor Borradaile mention it, and
                 * paleomagnetism.org doesn't implement it) so for
                 * now I'm keeping PuffinPlot's implementation as is
                 * and using a looser test for the 90 <= a95 <= 180
                 * case. PmagPy also returns 180 for "impossible"
                 * a95s (i.e. where the argument to acos is <-1),
                 * whereas PuffinPlot indicates this with NaN.
                 */
                assertTrue(fv.getA95() >= 90 || Double.isNaN(fv.getA95()));
            }
            assertEquals(Double.isFinite(fv.getA95()), fv.isA95Valid());
        }
    }
    
    /**
     * Test that calling calculate on an empty list produces a null reference.
     * The API should probably be changed to produce a null <i>object</i>
     * instead (seems cleaner and more intuitive) but for now this is
     * the defined behaviour.
     */
    @Test
    public void testCalculateOnEmptyList() {
        assertNull(FisherValues.calculate(Collections.emptyList()));
    }

    /**
     * Test that each string in the toStrings output has the value defined
     * by the corresponding header string.
     */
    @Test
    public void testToStringsInstance() {
        final FisherValues fv = createFisherValues();
        final List<String> headers = FisherValues.getHeaders();
        final List<String> stringValues = fv.toStrings();
        for (int i=0; i<headers.size(); i++) {
            final String header =
                    headers.get(i).toLowerCase().replace("fisher ", "");
            double expectedValue = -1e6;
            if (header.contains("dec")) {
                expectedValue = fv.getMeanDirection().getDecDeg();
            } else if (header.contains("inc")) {
                expectedValue = fv.getMeanDirection().getIncDeg();
            } else if (header.contains("a95")) {
                expectedValue = fv.getA95();
            } else if (header.contains("k")) {
                expectedValue = fv.getK();
            } else if (header.contains("n")) {
                expectedValue = fv.getN();
            } else if (header.contains("r")) {
                expectedValue = fv.getR();
            } else {
                fail("Unknown header string "+header);
            }
            final String value = stringValues.get(i);
            assertEquals(expectedValue, Double.parseDouble(value), 0.001);
        }
    }

    /**
     * Test that the static version of the toStrings method matches the
     * toStrings instance method for non-nulls, and returns empty strings
     * on null.
     */
    @Test
    public void testToStringsStatic() {
        assertEquals(FisherValues.getEmptyFields(),
                FisherValues.toStrings(null));
        final FisherValues fv = createFisherValues();
        assertEquals(fv.toStrings(), FisherValues.toStrings(fv));
    }

    /**
     * The string representation isn't formally defined, but it should
     * at least contain all the strings in the multi-string representation,
     * so we check that each element of the toStrings() output is
     * present in the toString() output.
     */
    @Test
    public void testToString() {
        final FisherValues fv = createFisherValues();
        final String result = fv.toString();
        final List<String> parts = fv.toStrings();
        assertTrue(parts.stream().allMatch(part -> result.contains(part)));
    }

    /**
     * Check that the parameter header strings are as expected, and that
     * they only contain printable ASCII characters (important for
     * importing into tools with poor support for non-ASCII character
     * sets).
     */
    @Test
    public void testGetHeaders() {
        final String expected =
                "Fisher dec. (deg),Fisher inc. (deg)," +
                "Fisher a95 (deg),Fisher k,Fisher nDirs,Fisher R";
        final String headers = String.join(",", FisherValues.getHeaders());
        
        assertEquals(expected, headers);
        assertTrue(TestUtils.isPrintableAscii(headers));
    }

    /**
     * Test that getEmptyFields returns a list of empty strings equal in
     * length to the header strings list.
     */
    @Test
    public void testGetEmptyFields() {
        final List<String> emptyFields = FisherValues.getEmptyFields();
        final List<String> headers = FisherValues.getHeaders();
        assertEquals(headers.size(), emptyFields.size());
        assertTrue(emptyFields.stream().allMatch(field -> "".equals(field)));
    }
    
    private static FisherValues createFisherValues() {
        return FisherValues.calculate(Arrays.asList(new Vec3[] {
            new Vec3(1,1,1), new Vec3(1,1,2), new Vec3(1,2,1),
            new Vec3(1,2,2), new Vec3(0,1,1), new Vec3(0,2,1),
        }));
    }
}
