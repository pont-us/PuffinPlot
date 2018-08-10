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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.talvi.puffinplot.TestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class GreatCirclesTest {
    
    /**
     * Test that we get an IllegalArgumentException when supplying no
     * endpoints or circles.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNoData() {
        new GreatCircles(Collections.emptyList(), Collections.emptyList());
    }
    
    /**
     * Test that we get an IllegalArgumentException when supplying no
     * endpoints and only one circle.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithInsufficientData() {
        final GreatCircle gc = new GreatCircle(Arrays.asList(
                new Vec3[] {Vec3.NORTH, Vec3.EAST}));
        new GreatCircles(Collections.emptyList(),
                Collections.singletonList(gc));
    }
    
    
    @Test
    public void testGetM() {
        final GreatCircles gcs = new GreatCircles(
                Collections.singletonList(Vec3.NORTH),
                Collections.emptyList());
        assertEquals(1, gcs.getM());
    }

    @Test
    public void testGetN() {
        final GreatCircles gcs = new GreatCircles(
                Collections.singletonList(Vec3.NORTH),
                Collections.emptyList());
        assertEquals(0, gcs.getN());
    }

    @Test
    public void testGetCircles() {
        final GreatCircle gc = new GreatCircle(Arrays.asList(
                new Vec3[] {Vec3.NORTH, Vec3.EAST}));
        final GreatCircles gcs =new GreatCircles(
                Collections.singletonList(Vec3.EAST),
                Collections.singletonList(gc));
        final List<GreatCircle> actual = gcs.getCircles();
        assertEquals(1, actual.size());
        final GreatCircle actualCircle = actual.get(0);
        assertTrue(gc.getPole().equals(actualCircle.getPole(), 1e-10));
        for (int i=0; i<actualCircle.getPoints().size(); i++) {
            assertTrue(gc.getPoints().get(i).
                    equals(actualCircle.getPoints().get(i), 1e-10));
        }
    }

    @Test
    public void testGetMeanDirection() {
        final GreatCircles gcs = new GreatCircles(
                Collections.singletonList(Vec3.NORTH),
                Collections.emptyList());
        assertTrue(Vec3.NORTH.equals(gcs.getMeanDirection(), 1e-10));
    }

//    @Test
//    public void testToStrings() {
//        System.out.println("toStrings");
//        GreatCircles instance = null;
//        List<String> expResult = null;
//        List<String> result = instance.toStrings();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//
    @Test
    public void testGetEmptyFields() {
        assertEquals(GreatCircles.getHeaders().size(),
                GreatCircles.getEmptyFields().size());
        assertTrue(GreatCircles.getEmptyFields().stream().
                allMatch(field -> "".equals(field)));
    }

    @Test
    public void testGetHeaders() {
        final String expected = "GC valid,GC dec. (deg),GC inc. (deg)," +
                "GC a95 (deg),GC k,GC N,GC M,GC R,GC min points";
        final String actual = String.join(",", GreatCircles.getHeaders());
        assertEquals(expected, actual);
        assertTrue(TestUtils.isPrintableAscii(actual));
    }
    
//    @Test
//    public void testNoConstraintsNoEndpoints() {
//        fail();
//    }
    
//
//    @Test
//    public void testIsValid() {
//        System.out.println("isValid");
//        GreatCircles instance = null;
//        boolean expResult = false;
//        boolean result = instance.isValid();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//
//    @Test
//    public void testGetA95() {
//        System.out.println("getA95");
//        GreatCircles instance = null;
//        double expResult = 0.0;
//        double result = instance.getA95();
//        assertEquals(expResult, result, 0.0);
//        fail("The test case is a prototype.");
//    }
//
//    @Test
//    public void testIsA95Valid() {
//        System.out.println("isA95Valid");
//        GreatCircles instance = null;
//        boolean expResult = false;
//        boolean result = instance.isA95Valid();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//
//    @Test
//    public void testGetK() {
//        System.out.println("getK");
//        GreatCircles instance = null;
//        double expResult = 0.0;
//        double result = instance.getK();
//        assertEquals(expResult, result, 0.0);
//        fail("The test case is a prototype.");
//    }
//
//    @Test
//    public void testGetR() {
//        System.out.println("getR");
//        GreatCircles instance = null;
//        double expResult = 0.0;
//        double result = instance.getR();
//        assertEquals(expResult, result, 0.0);
//        fail("The test case is a prototype.");
//    }
    
}
