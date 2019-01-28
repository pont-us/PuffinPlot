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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.talvi.puffinplot.TestUtils;
import org.junit.Test;

import static java.lang.Math.PI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class GreatCirclesTest {
    
    private final double[][] mcfaddenCircles = {
        // Eigenvector (pole)  Constraint 1  Constraint 2
        //   p      q       r      D      I      D      I
        {0.263, 0.664, -0.700, 180.0, -20.6, 260.0, -45.0}, // KB31A1
        {0.794, 0.335, -0.507, 141.0, -38.7, 200.0, -59.5}, // KB31A4
        {0.692, 0.358, -0.627, 134.7, -20.3, 196.6, -50.7}, // KB31B1
        {0.593, 0.663, -0.457, 148.0, -18.4, 210.0, -61.6}, // KB31C2
        {0.637, 0.476, -0.606, 190.0, -49.5, 270.0, -38.1}, // KB31D1
        {0.628, 0.524, -0.576, 157.0, -32.9, 215.0, -54.7}  // KB31D2
    };
    
    private final double[][] mcfaddenEndpoints = {
        //   D      I
        {146.6, -52.6}, // KB31A3
        {195.5, -57.9}  // KB31B3
    };
    
    /**
     * Test that we get an IllegalArgumentException when supplying no
     * endpoints or circles.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNoData() {
        GreatCircles.instance(emptyList(), emptyList(), "true");
    }
    
    /**
     * Test that we get an IllegalArgumentException when supplying no
     * endpoints and only one circle.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithInsufficientData() {
        final GreatCircle gc = GreatCircle.fromBestFit(Arrays.asList(
                new Vec3[] {Vec3.NORTH, Vec3.EAST}));
        GreatCircles.instance(emptyList(), singletonList(gc), "true");
    }
    
    
    @Test
    public void testGetM() {
        final GreatCircles gcs = GreatCircles.instance(
                singletonList(Vec3.NORTH), emptyList(), "true");
        assertEquals(1, gcs.getM());
    }

    @Test
    public void testGetN() {
        final GreatCircles gcs = GreatCircles.instance(
                singletonList(Vec3.NORTH), emptyList(), "true");
        assertEquals(0, gcs.getN());
    }

    @Test
    public void testGetCircles() {
        final GreatCircle gc = GreatCircle.fromBestFit(Arrays.asList(
                new Vec3[] {Vec3.NORTH, Vec3.EAST}));
        final GreatCircles gcs =GreatCircles.instance(singletonList(Vec3.EAST),
                singletonList(gc), "true");
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
        final GreatCircles gcs = GreatCircles.instance(
                singletonList(Vec3.NORTH), emptyList(), "true");
        assertTrue(Vec3.NORTH.equals(gcs.getMeanDirection(), 1e-10));
    }
    
    /**
     * Test that the GreatCircles constructor accepts null for the
     * list of great circles.
     */
    @Test
    public void testNullCircleList() {
        final GreatCircles gcs = GreatCircles.instance(
                singletonList(Vec3.NORTH), null, "true");
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
        final List<Vec3> points = singletonList(Vec3.NORTH);
        final List<GreatCircle> circles = emptyList();
        assertTrue(GreatCircles.instance(points, circles, "true").isValid());
        assertFalse(GreatCircles.instance(points, circles, "false").isValid());
        // non-boolean should return false
        assertFalse(GreatCircles.instance(points, circles, "42").isValid());
         // syntax error should return false
        assertFalse(GreatCircles.instance(points, circles, "]]]").isValid());
    }
    
    /**
     * Test results of a three-circle intersection with no endpoints. This is a
     * characterization test: the expected results were generated from
     * GreatCircles itself. The code that generated the expected results also
     * passes the non-constrained tests based on the McFadden & McElhinny (1988)
     * example data, providing indirect verification of this test's reliability.
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
            circles.add(GreatCircle.fromBestFit(vectorListFromDirections(pairs)));
        }
        final GreatCircles gcs = GreatCircles.instance(null,
                circles, "true");
        final GreatCircles gcsWithValidCondition = GreatCircles.instance(null,
                circles, "a95 < 60 && k > 145");
        final GreatCircles gcsWithInvalidCondition = GreatCircles.instance(null,
                circles, "a95 < 60 && k > 146");
        
        assertEquals(2.9965691442158913, gcs.getR(), 1e-12);
        assertEquals(57.10004805715773, gcs.getA95(), 1e-12);
        assertEquals(145.73623359977222, gcs.getK(), 1e-12);
        assertTrue(new Vec3(0.9979825583868029, -0.005052966985651441,
                0.06328728687796072).equals(gcs.getMeanDirection(), 1e-12));
        assertTrue(gcs.isA95Valid());
        assertTrue(gcs.isValid());
        assertTrue(gcsWithValidCondition.isValid());
        assertFalse(gcsWithInvalidCondition.isValid());
        /* Until GreatCircles is modified to take the validity expression
         * as a constructor argument rather than from the Preferences,
         * we have to check the toStrings result against a regular
         * expression, since the first field may be either N or Y.
         */
        assertTrue(Pattern.matches(
                "Y,359[.]7099,3[.]6285,57[.]1000,145[.]7362,3,0,2[.]9966,3",
                String.join(",", gcs.toStrings())));
        assertTrue(Pattern.matches(
                "N,359[.]7099,3[.]6285,57[.]1000,145[.]7362,3,0,2[.]9966,3",
                String.join(",", gcsWithInvalidCondition.toStrings())));
        
    }
    
    /**
     * Test results of a three-circle intersection with no endpoints. This is a
     * characterization test: the expected results were generated from
     * GreatCircles itself. The code that generated the expected results also
     * passes the non-constrained tests based on the McFadden & McElhinny (1988)
     * example data, providing indirect verification of this test's reliability.
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
            circles.add(GreatCircle.fromBestFit(vectorListFromDirections(pairs)));
        }

        double[][] endpoints = {{3, 2}, {-1, -1}};
        final GreatCircles gcs =
                GreatCircles.instance(vectorListFromDirections(endpoints),
                circles, "true");

        assertEquals(4.972711654793188, gcs.getR(), 1e-12);
        assertEquals(9.141439789331642, gcs.getA95(), 1e-12);
        assertEquals(91.614203098542, gcs.getK(), 1e-12);
        assertTrue(new Vec3(0.9980864323922312, 0.0011018874021068872,
                0.061824423318791295).equals(gcs.getMeanDirection(), 1e-12));
        assertTrue(gcs.isA95Valid());
        assertTrue(gcs.isValid());
    }
 
    private List<Vec3> vectorListFromDirections(double[][] pairs) {
        final List<Vec3> vecs = new ArrayList<>(pairs.length);
        for (double[] pair: pairs) {
            final Vec3 v = Vec3.fromPolarDegrees(1, pair[1], pair[0]);
            vecs.add(v);
        }
        return vecs;
    }
    
    /**
     * Test calculation results against example in McFadden & McElhinny
     * (1988): circles and endpoints, but no sector constraints.
     */
    @Test
    public void testWithoutConstraintsWithEndpoints() {
        final List<Vec3> endpoints =
                vectorListFromDirections(mcfaddenEndpoints);
        final List<GreatCircle> circles = new ArrayList<>();
        for (double[] fields: mcfaddenCircles) {
            circles.add(GreatCircle.fromPole(new Vec3(fields[0], fields[1], fields[2])));
        }
        final GreatCircles gcs = GreatCircles.instance(endpoints, circles, "true");

        assertEquals(183.1, gcs.getMeanDirection().getDecDeg(), 0.1);
        assertEquals(-51.2, gcs.getMeanDirection().getIncDeg(), 0.1);
        assertEquals(8, gcs.getM() + gcs.getN());
        assertEquals(7.8336, gcs.getR(), 0.0001);
        assertEquals(24.032, gcs.getK(), 0.001);
        assertEquals(12.5, gcs.getA95(), 0.1);
    }
    
    /**
     * Test calculation results against example in McFadden & McElhinny
     * (1988): circles only, no endpoints or sector constraints.
     */
    @Test
    public void testWithoutConstraintsWithoutEndpoints() {
        final List<GreatCircle> circles = new ArrayList<>();
        for (double[] fields: mcfaddenCircles) {
            circles.add(GreatCircle.fromPole(new Vec3(fields[0], fields[1], fields[2])));
        }
        final GreatCircles gcs = GreatCircles.instance(null, circles, "true");
        
        /*
         * There's a slight complication in checking the direction: since we
         * don't have the data from the original points to which the circles
         * were fitted, there are two equally valid and opposite solutions.
         * (M&M 1988 presumably used the point data in picking a starting
         * point for the iteration, but they only published the great circle
         * poles.) Thus the test here confirms that we have either M&M's
         * direction or its inverse. The other parameters (k, R, etc.) are
         * the same for either solution.
         */
        final Vec3 actualDirection = gcs.getMeanDirection();
        final Vec3 expectedDirection = Vec3.fromPolarDegrees(1, -50.5, 246.8);
        final Vec3 directionToCheck =
                Math.abs(actualDirection.angleTo(expectedDirection)) < PI / 2 ?
                actualDirection :
                actualDirection.invert();
        
        assertTrue(Math.abs(expectedDirection.angleTo(directionToCheck)) <=
                Math.toRadians(0.1));
        assertEquals(6, gcs.getM() + gcs.getN());
        assertEquals(5.9735, gcs.getR(), 0.0001);
        assertEquals(75.592, gcs.getK(), 0.005);
        assertEquals(10.1, gcs.getA95(), 0.1);
    }
    
    /**
     * A pathological case: test with three mutually orthogonal circles as
     * input. This will not produce a physically meaningful result, but the test
     * checks that the algorithm won't hang or crash, and that the resulting
     * alpha-95 value is not finite.
     */
    @Test
    public void testOrthogonalCircles() {
        final Vec3[] polesArray = new Vec3[] {
            Vec3.NORTH, Vec3.EAST, Vec3.DOWN};
        List<GreatCircle> gcList = Arrays.stream(polesArray).
                map((pole) -> GreatCircle.fromPole(pole)).
                collect(Collectors.toList());
        final GreatCircles gcs = GreatCircles.instance(null, gcList, "true");
        assertFalse(gcs.isA95Valid());
    }
}
