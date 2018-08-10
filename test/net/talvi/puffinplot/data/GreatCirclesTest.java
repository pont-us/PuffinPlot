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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
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
    
    /**
     * Test that the GreatCircles constructor accepts null for the
     * list of great circles.
     */
    @Test
    public void testNullCircleList() {
        final GreatCircles gcs = new GreatCircles(
                Collections.singletonList(Vec3.NORTH),
                null);
    }

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

    
    @Test
    public void testSimpleValidityConditions() {
        final GreatCircles gcs = new GreatCircles(
                Collections.singletonList(Vec3.NORTH),
                Collections.emptyList());
        assertTrue(Vec3.NORTH.equals(gcs.getMeanDirection(), 1e-10));
        assertTrue(gcs.isValid("true"));
        assertFalse(gcs.isValid("false"));
        assertFalse(gcs.isValid("42")); // non-boolean should return false
        assertFalse(gcs.isValid("]]]")); // syntax error should return false
    }
    
    /**
     * Test results of a three-circle intersection with no endpoints. This is
     * purely a characterization test: the expected data is generated from
     * GreatCircles itself.
     */
    @Test
    public void testThreeCircles() {
        final double[][][] circlePoints = {
            {{300, -1}, {310, 1}, {320, 2}},
            { {40,  1}, {30, -1},  {20, 1}},
            { {0, -40}, {0, -30}, {0, -20}}
        };
        final List<GreatCircle> circles = new ArrayList<>(circlePoints.length);
        for (double[][] pairs: circlePoints) {
            circles.add(new GreatCircle(vectorListFromDirections(pairs)));
        }
        final GreatCircles gcs = new GreatCircles(null,
                circles);
        System.out.println("> "+gcs.getMeanDirection().getDecDeg()+" "+
                gcs.getMeanDirection().getIncDeg());
        assertEquals(2.9965691442158913, gcs.getR(), 1e-12);
        assertEquals(57.10004805715773, gcs.getA95(), 1e-12);
        assertEquals(145.73623359977222, gcs.getK(), 1e-12);
        assertTrue(new Vec3(0.9979825583868029, -0.005052966985651441,
                0.06328728687796072).equals(gcs.getMeanDirection(), 1e-12));
        assertTrue(gcs.isA95Valid());
        assertTrue(gcs.isValid("a95 < 60 && k > 145"));
        assertFalse(gcs.isValid("a95 < 60 && k > 146"));
        /* Until GreatCircles is modified to take the validity expression
         * as a constructor argument rather than from the Preferences,
         * we have to check the toStrings result against a regular
         * expression, since the first field may be either N or Y.
         */
        assertTrue(Pattern.matches(
                "[NY],359[.]7099,3[.]6285,57[.]1000,145[.]7362,3,0,2[.]9966,3",
                String.join(",", gcs.toStrings())));
    }
    
    /**
     * Test results of a three-circle intersection with two endpoints. This is
     * purely a characterization test: the expected data is generated from
     * GreatCircles itself.
     */
    @Test
    public void testTwoEndpointsAndThreeCircles() {
        final double[][][] circlePoints = {
            {{300, -5}, {310, 1}, {320, 2}},
            { {40,  4}, {30, 0},  {20, 2}},
            { {3, -40}, {-1, -30}, {2, -20}}
        };
        final List<GreatCircle> circles = new ArrayList<>(circlePoints.length);
        for (double[][] pairs: circlePoints) {
            circles.add(new GreatCircle(vectorListFromDirections(pairs)));
        }

        double[][] endpoints = {{3, 2}, {-1, -1}};
        final GreatCircles gcs =
                new GreatCircles(vectorListFromDirections(endpoints),
                circles);
        System.out.println("> "+gcs.getMeanDirection().getDecDeg()+" "+
                gcs.getMeanDirection().getIncDeg());

        assertEquals(4.972711654793188, gcs.getR(), 1e-12);
        assertEquals(9.141439789331642, gcs.getA95(), 1e-12);
        assertEquals(91.614203098542, gcs.getK(), 1e-12);
        assertTrue(new Vec3(0.9980864323922312, 0.0011018874021068872,
                0.061824423318791295).equals(gcs.getMeanDirection(), 1e-12));
        assertTrue(gcs.isA95Valid());
        assertTrue(gcs.isValid("a95 < 10 && k > 90"));
        assertFalse(gcs.isValid("a95 < 9 && k > 90"));
    }
 
    private List<Vec3> vectorListFromDirections(double[][] pairs) {
        final List<Vec3> vecs = new ArrayList<>(pairs.length);
        for (double[] pair: pairs) {
            final Vec3 v = Vec3.fromPolarDegrees(1, pair[1], pair[0]);
            vecs.add(v);
        }
        return vecs;
    }

        
//    @Test
//    public void testNoConstraintsNoEndpoints() {
//        fail();
//    }
    
}
