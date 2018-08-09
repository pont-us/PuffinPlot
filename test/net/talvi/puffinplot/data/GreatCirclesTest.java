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
        final GreatCircle gc =new GreatCircle(Arrays.asList(
                new Vec3[] {Vec3.NORTH, Vec3.EAST}));
        new GreatCircles(Collections.emptyList(), Collections.singletonList(gc));
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

//    @Test
//    public void testGetCircles() {
//        System.out.println("getCircles");
//        GreatCircles instance = null;
//        List<GreatCircle> expResult = null;
//        List<GreatCircle> result = instance.getCircles();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//
//    @Test
//    public void testGetMeanDirection() {
//        System.out.println("getMeanDirection");
//        GreatCircles instance = null;
//        Vec3 expResult = null;
//        Vec3 result = instance.getMeanDirection();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//
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
//    @Test
//    public void testGetEmptyFields() {
//        System.out.println("getEmptyFields");
//        List<String> expResult = null;
//        List<String> result = GreatCircles.getEmptyFields();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
//    }
//
//    @Test
//    public void testGetHeaders() {
//        System.out.println("getHeaders");
//        List<String> expResult = null;
//        List<String> result = GreatCircles.getHeaders();
//        assertEquals(expResult, result);
//        fail("The test case is a prototype.");
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
