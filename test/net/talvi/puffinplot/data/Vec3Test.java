/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot.data;

import static java.lang.Math.toRadians;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

/**
 *
 * @author pont
 */
public class Vec3Test {

    // randomly generated values, plus some special cases
    private static final double[][] testVectorsArray = {
        {0, 0, 0},
        {1, 0, 0},
        {0, 1, 0},
        {0, 0, 1},
        {0, 1, 1},
        {1, 0, 1},
        {1, 1, 0},
        {1, 1, 1},
        {-1, 0, 0},
        {0, -1, 0},
        {0, 0, -1},
        {0, -1, -1},
        {-1, 0, -1},
        {-1, -1, 0},
        {-1, -1, -1},
        {0, -1, 1},
        {-1, 0, 1},
        {-1, 1, 0},
        {0, 1, -1},
        {1, 0, -1},
        {1, -1, 0},
        {-1, 1, 1},
        {1, -1, 1},
        {1, 1, -1},
        {1, -1, -1},
        {-1, 1, -1},
        {-1, -1, 1},
        {-6.39, -4.29, +2.23},
        {+8.78, -7.91, +4.59},
        {+7.75, +0.17, +3.22},
        {-5.73, -0.52, +1.68},
        {+7.36, +0.52, -1.20},
        {+8.97, +6.52, -3.06},
        {-3.19, -0.56, -2.23},
        {-0.26, -0.85, -9.94},
        {+9.35, +3.79, -3.01},
        {-8.68, +8.25, +4.42},
        {-8.44, -8.17, +4.52},
        {+6.22, -9.45, +6.54},
        {-5.41, -3.25, -4.33},
        {+5.19, +5.72, -6.99},
        {-0.26, +3.62, +8.44},
        {-0.36, +8.71, -3.06},
        {+0.29, -8.48, +7.40},
        {-2.57, -6.26, -8.75},
        {+9.25, -7.69, +5.27},
        {+8.57, -8.47, -6.72},};
    private List<Vec3> testVectors;
    private List<Vec3> testUnitVectors;
    private final double delta = 1e-6;

    @Before
    public void setUp() {
        testVectors = Vec3Test.createTestVectors();
        testUnitVectors = testVectors.stream().
                filter(v -> !v.equals(Vec3.ORIGIN)).
                map(v -> v.normalize()).
                collect(Collectors.toList());
    }

    private static List<Vec3> createTestVectors() {
        final List<Vec3> result = new ArrayList<>(10);
        for (double[] xyz : testVectorsArray) {
            result.add(new Vec3(xyz[0], xyz[1], xyz[2]));
        }
        return result;
    }
    
    /**
     * Tests the {@link Vec3#addDecRad(double)} method.
     */
    @Test
    public void testAddDecRad() {
        final double mag = 1.9;
        final double tolerance = 0.0001;
        double[] angles = {-279, -201, -140, -50, -10, 0, 10, 40, 70, 100, 190, 359};
        for (double dec : angles) {
            for (double inc : angles) {
                Vec3 original = Vec3.fromPolarDegrees(mag, inc, dec);
                for (double offset : angles) {
                    Vec3 expected = Vec3.fromPolarDegrees(mag, inc, dec + offset);
                    Vec3 result = original.addDecRad(Math.toRadians(offset));
                    assertEquals(expected.x, result.x, tolerance);
                    assertEquals(expected.y, result.y, tolerance);
                    assertEquals(expected.z, result.z, tolerance);
                }
            }
        }
    }

    /**
     * Tests the {@link Vec3#addIncRad(double)} method.
     */
    @Test
    public void testAddIncRad() {
        final double mag = 1.0;
        final double tolerance = 0.0001;
        double[] decs = {-279, -201, -140, -50, -10, 0, 10, 40, 70, 100, 190, 359};
        double[] incs = {-80, -40, -10, 5, 20, 50, 80};
        double[] offsets = {-80, -50, -10, 5, 10, 40, 60, 85};
        for (double dec : decs) {
            for (double inc : incs) {
                Vec3 original = Vec3.fromPolarDegrees(mag, inc, dec);
                for (double offset : offsets) {
                    Vec3 expected = Vec3.fromPolarDegrees(mag, inc + offset, dec);
                    Vec3 result = original.addIncRad(Math.toRadians(offset));
                    String msg = String.format(Locale.ENGLISH,
                            "D %g I %g + %g", dec, inc, offset);
                    assertEquals(msg, expected.x, result.x, tolerance);
                    assertEquals(msg, expected.y, result.y, tolerance);
                    assertEquals(msg, expected.z, result.z, tolerance);
                }
            }
        }
    }

    @Test
    public void testEquatorPoint() {
        // A couple of simple tests
        testOneEquatorPoint(0, 1, 1, 0, 1, -1, 0, 1, 0);
        testOneEquatorPoint(1, 0, 1, 1, 0, -1, 1, 0, 0);

        // Unequal length vectors
        testOneEquatorPoint(0, 2, 5, 0, 1, -1, 0, 1, 0);

        // Point on equator
        testOneEquatorPoint(1, 0, 0, 1, 0, 1, 1, 0, 0);
        testOneEquatorPoint(11, 12, 13, 0, 1, 0, 0, 1, 0);
        
        testEquatorPointOpposite();
        testEquatorPointVertical();
        

        // Illegal arguments
        final Vec3[][] illegal = {
            {new Vec3(Double.NaN, 0, 0), Vec3.ORIGIN},
            {Vec3.ORIGIN, new Vec3(Double.NaN, 0, 0)},
            {Vec3.DOWN, Vec3.DOWN},
            {Vec3.DOWN.invert(), Vec3.DOWN.invert()}
        };

        for (Vec3[] illegalPair : illegal) {
            try {
                Vec3.equatorPoint(illegalPair[0], illegalPair[1]);
                assertTrue(false); // fail the test
            } catch (IllegalArgumentException e) {
                // expected behaviour -- do nothing
            }
        }
    }

    private void testOneEquatorPoint(double x0, double y0, double z0,
            double x1, double y1, double z1,
            double x2, double y2, double z2) {
        final Vec3 v0 = new Vec3(x0, y0, z0);
        final Vec3 v1 = new Vec3(x1, y1, z1);
        final Vec3 expected = new Vec3(x2, y2, z2);
        final Vec3 veq1 = Vec3.equatorPoint(v0, v1);
        final Vec3 veq2 = Vec3.equatorPoint(v1, v0);
        assertTrue(expected.equals(veq1, 1e-10));
        assertTrue(expected.equals(veq2, 1e-10));
    }
    
    private void testEquatorPointOpposite() {
        final Vec3 v0 = new Vec3(1, 1, 1);
        final Vec3 v1 = v0.invert();
        final Vec3 opposite = Vec3.equatorPoint(v0, v1);
        assertTrue(areCoplanar(v0, v1, opposite));
        assertEquals(0, opposite.z, delta);
    }
    
    private void testEquatorPointVertical() {
        final Vec3 result = Vec3.equatorPoint(Vec3.DOWN, Vec3.DOWN.invert());
        assertEquals(0, result.z, delta);
    }

    @Test
    public void testRot180() {
        final double delta = 1e-6;
        MeasurementAxis[] axes = {
            MeasurementAxis.X,
            MeasurementAxis.Y,
            MeasurementAxis.Z
        };

        for (Vec3 vec3 : testVectors) {
            // Test that double rotation about same axis
            // produces original vector
            for (MeasurementAxis axis : axes) {
                assertTrue(vec3.equals(vec3.rot180(axis).rot180(axis), delta));
            }

            // Test that rotating about all three axes produces
            // original vector
            assertTrue(vec3.equals(vec3.rot180(axes[0]).rot180(axes[1]).
                    rot180(axes[2]), delta));

            // Test that rotating about two axes is equivalent to
            // a rotation about the third axis
            for (int i = 0; i < 3; i++) {
                final Vec3 twoRots = vec3.rot180(axes[i]).
                        rot180(axes[(i + 1) % 3]);
                final Vec3 oneRot = vec3.rot180(axes[(i + 2) % 3]);
                assertTrue(twoRots.equals(oneRot, delta));
            }

            // Test that a rotation about an axis inverts the other
            // two axes, but leaves this one unchanged
            final Vec3 xrot = vec3.rot180(MeasurementAxis.X);
            assertEquals(vec3.x, xrot.x, delta);
            assertEquals(vec3.y, -xrot.y, delta);
            assertEquals(vec3.z, -xrot.z, delta);
            final Vec3 yrot = vec3.rot180(MeasurementAxis.Y);
            assertEquals(vec3.x, -yrot.x, delta);
            assertEquals(vec3.y, yrot.y, delta);
            assertEquals(vec3.z, -yrot.z, delta);
            final Vec3 zrot = vec3.rot180(MeasurementAxis.Z);
            assertEquals(vec3.x, -zrot.x, delta);
            assertEquals(vec3.y, -zrot.y, delta);
            assertEquals(vec3.z, zrot.z, delta);

            // Test that rotating about an "invalid" axis returns the
            // original vector.
            assertTrue(vec3.equals(vec3.rot180(MeasurementAxis.H), delta));
        }
    }

    @Test
    public void testInvert() {

        for (Vec3 vec3 : testVectors) {
            final Vec3 inverted = vec3.invert();
            // Test that the sum of a vector and its inversion is zero
            assertTrue(Vec3.ORIGIN.equals(vec3.plus(inverted), delta));

            // Test that the magnitude remains the same
            assertEquals(vec3.mag(), inverted.mag(), delta);

            // Test individual components inverted
            assertEquals(-vec3.x, inverted.x, delta);
            assertEquals(-vec3.y, inverted.y, delta);
            assertEquals(-vec3.z, inverted.z, delta);
        }
    }

    @Test
    public void testSum() {
        final double[] total = {0, 0, 0};

        for (double[] xyz : testVectorsArray) {
            for (int i = 0; i < 3; i++) {
                total[i] += xyz[i];
            }
        }

        final Vec3 sum = Vec3.sum(testVectors);

        assertEquals(total[0], sum.x, delta);
        assertEquals(total[1], sum.y, delta);
        assertEquals(total[2], sum.z, delta);

        Collection<Vec3> invertedVectors = testVectors.stream().
                map(x -> x.invert()).collect(Collectors.toList());

        assertTrue(sum.invert().equals(Vec3.sum(invertedVectors), delta));
    }

    @Test
    public void testPlus() {
        for (int i = 0; i < testVectors.size() - 1; i++) {
            final Vec3 v1 = testVectors.get(i);
            final Vec3 v2 = testVectors.get(i + 1);
            final Vec3 sum1 = v1.plus(v2);
            final Vec3 sum2 = v2.plus(v1);
            assertTrue(sum1.equals(sum2, delta));
            assertEquals(sum1.x, v1.x + v2.x, delta);
            assertEquals(sum1.y, v1.y + v2.y, delta);
            assertEquals(sum1.z, v1.z + v2.z, delta);
        }
    }

    @Test
    public void testTimes() {
        for (int i = 0; i < testVectors.size() - 1; i++) {
            final Vec3 v1 = testVectors.get(i);
            final Vec3 v2 = testVectors.get(i + 1);
            final Vec3 product1 = v1.times(v2);
            final Vec3 product2 = v2.times(v1);
            assertTrue(product1.equals(product2, delta));
            assertEquals(product1.x, v1.x * v2.x, delta);
            assertEquals(product1.y, v1.y * v2.y, delta);
            assertEquals(product1.z, v1.z * v2.z, delta);
        }
    }

    @Test
    public void testDivideByVector() {
        for (int i = 0; i < testVectors.size() - 1; i++) {
            final Vec3 v1 = testVectors.get(i);
            final Vec3 v2 = testVectors.get(i + 1);
            final Vec3 quotient = v1.divideBy(v2);
            assertEquals(quotient.x, v1.x / v2.x, delta);
            assertEquals(quotient.y, v1.y / v2.y, delta);
            assertEquals(quotient.z, v1.z / v2.z, delta);
        }
    }

    @Test
    public void testDivideByDouble() {
        final double[] divisors = {-9999, -1, 0.0021, 0, 1, 10, 17.9, 12345};
        for (Vec3 v : testVectors) {
            for (double d : divisors) {
                final Vec3 result = v.divideBy(d);
                assertEquals(v.x / d, result.x, delta);
                assertEquals(v.y / d, result.y, delta);
                assertEquals(v.z / d, result.z, delta);
            }
        }
    }

    @Test
    public void testIsWellFormed() {
        for (Vec3 v : testVectors) {
            assertTrue(v.isWellFormed());
        }

        final double nan = Double.NaN;
        final double inf = Double.POSITIVE_INFINITY;
        final double ninf = Double.NEGATIVE_INFINITY;
        final double[][] illFormed = {
            {nan, 0, 0},
            {0, nan, 0},
            {0, 0, nan},
            {inf, 0, 0},
            {0, inf, 0},
            {0, 0, inf},
            {ninf, 0, 0},
            {0, ninf, 0},
            {0, 0, ninf}
        };

        for (double[] xyz : illFormed) {
            assertFalse(new Vec3(xyz[0], xyz[1], xyz[2]).isWellFormed());
        }
    }

    @Test
    public void testSpherInterpolate() {

        for (int i = 0; i < testVectors.size() - 1; i++) {
            final Vec3 v0 = testVectors.get(i);
            final Vec3 v1 = testVectors.get(i + 1);
            if (Vec3.ORIGIN.equals(v0) || Vec3.ORIGIN.equals(v0)) {
                // a zero vector is invalid input
                continue;
            }

            final Vec3 normalDir = v0.cross(v1).normalize();

            final List<Vec3> steps = Vec3.spherInterpolate(v0, v1, 0.1);

            // Test that steps are all normalized.
            steps.forEach(step -> assertEquals(1, step.mag(), delta));

            // Test that steps are all in the right plane.
            steps.subList(1, steps.size()).forEach((step) -> {
                assertTrue(normalDir.equals(v0.cross(step).normalize(), delta));
            });

            // Test that steps are equally spaced.
            // We only run this loop to size-2 because currently
            // spherInterpolate seems to erroneously duplicate the last point.
            // TODO: update test to check for correct behaviour and 
            // fix spherInterpolate.
            final double firstAngle = steps.get(0).angleTo(steps.get(1));
            for (int j = 1; j < steps.size() - 2; j++) {
                final Vec3 step0 = steps.get(j);
                final Vec3 step1 = steps.get(j + 1);
                final double thisAngle = step0.angleTo(step1);
                assertEquals(firstAngle, thisAngle, delta);
            }
        }

        /*
         * Test a pathological pair of vectors. After normalization, the cross
         * product of these vectors will (thanks to floating-point rounding
         * errors) be slightly *larger* than one, producing NaNs if asin or acos
         * is applied. This test verifies that spherInterpolate is robust
         * against this problem. (These vectors came from a real-life data set,
         * so the problem is not merely theoretical.)
         */
        final Vec3 bad0 = new Vec3(0.48653711261156757,
                0.8724782534206357,
                0.04542395138773855);
        final Vec3 bad1 = new Vec3(0.4865371126115692,
                0.8724782534206347,
                0.045423951387740906);
        final List<Vec3> steps = Vec3.spherInterpolate(bad0, bad1, 0.1);
        steps.forEach(step -> {
            assertTrue(step.isWellFormed());
        });
    }
    
    @Test
    public void testEqualsAndHashCode() {
        final List<Vec3> otherTestVectors = Vec3Test.createTestVectors();
        
        final Object notAVec3 = new Object();
        
        for (int i=0; i<testVectors.size(); i++) {
            final Vec3 a = testVectors.get(i);
            final Vec3 b = otherTestVectors.get(i);
            assertNotEquals(a, notAVec3);
            assertEquals(a, a);
            assertEquals(b, b);
            assertEquals(a, b);
            assertEquals(b, a);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
    
    @Test
    public void testToString() {
        testVectors.forEach((vec3) -> {
            final String expected = String.format(Locale.ENGLISH,
                    "%.2f %.2f %.2f", vec3.x, vec3.y, vec3.z);
            assertEquals(expected, vec3.toString());
        });
    }
    
    @Test
    public void testGetComponent() {
        testVectors.forEach((vec3) -> {
            assertEquals(vec3.x, vec3.getComponent(MeasurementAxis.X), delta);
            assertEquals(vec3.y, vec3.getComponent(MeasurementAxis.Y), delta);
            assertEquals(vec3.z, vec3.getComponent(MeasurementAxis.Z), delta);
            assertEquals(-vec3.x, vec3.getComponent(MeasurementAxis.MINUSX), delta);
            assertEquals(-vec3.y, vec3.getComponent(MeasurementAxis.MINUSY), delta);
            assertEquals(-vec3.z, vec3.getComponent(MeasurementAxis.MINUSZ), delta);
            assertEquals(Math.sqrt(vec3.x*vec3.x+vec3.y*vec3.y),
                    vec3.getComponent(MeasurementAxis.H), delta);

            /* Test that every value of MeasurementAxis is covered.
             * At time of writing the explicit cases listed above cover all
             * values, but if new values are added to MeasurementAxis in
             * the future, this will ensure that Vec3 handles them.
             */
            for (MeasurementAxis axis: MeasurementAxis.values()) {
                vec3.getComponent(axis);
            }
        });
        /* At present, we're not testing the default case, because it's
         * unreachable with the code as compiled (but can't be removed
         * because the compiler requires it). Testing would be possible
         * using PowerMock for bytecode manipulation (see
         * https://stackoverflow.com/a/7233572/6947739 ) but I'm reluctant
         * to add PowerMock and Mockito to PuffinPlot's dependencies
         * just for this.
         */ 
    }
    
    @Test
    public void testGreatCirclePoints() {
        for (Vec3 normal: testUnitVectors) {
            final int[] pointCounts = {1, 2, 5, 10, 50};
            for (int pointCount: pointCounts) {
                final List<Vec3> openCircle =
                        normal.greatCirclePoints(pointCount, false);
                assertEquals(pointCount, openCircle.size());
                testOneCircle(normal, Math.PI/2, openCircle);
                
                final List<Vec3> closedCircle =
                        normal.greatCirclePoints(pointCount, true);
                assertEquals(pointCount+1, closedCircle.size());
                testOneCircle(normal, Math.PI/2, closedCircle);
                assertTrue(closedCircle.get(0).equals(
                        closedCircle.get(pointCount), delta));
            }
        }
    }
    

    @Test
    public void testMakeSmallCircle() {
        for (Vec3 v: testUnitVectors) {
            final double[] radii = {0, 0.1, 1, 2, 5, 10, 45, 89, 90};
            for (double radius: radii) {
                testOneCircle(v, Math.toRadians(radius),
                        v.makeSmallCircle(radius));
            }
        }
    }
    
    private void testOneCircle(Vec3 centre, double radius, List<Vec3> points) {
        // Every point should be normal to the defining vector.
        for (Vec3 point: points) {
            assertEquals(radius, Math.abs(centre.angleTo(point)), delta);
        }
        
        // Points should be spaced at equal angles.
        if (points.size() > 1) {
            final double angle = Math.abs(points.get(0).angleTo(points.get(1)));
            for (int i=1; i<points.size()-1; i++) {
                assertEquals(angle,
                        Math.abs(points.get(i).angleTo(points.get(i+1))),
                        delta);
            }
        }
    }
    
    @Test
    public void testInterpolateEquatorPoints() {
        final Random rnd = new Random(23);
        for (int i=0; i<20; i++) {
            final int nPoints = rnd.nextInt(20);
            Collections.shuffle(testUnitVectors, rnd);
            final List<Vec3> input = testUnitVectors.subList(0, 10);
            testInterpolateEquatorPoints(input);
        }
    }
    
    private void testInterpolateEquatorPoints(List<Vec3> input) {
        final List<List<Vec3>> sublists = Vec3.interpolateEquatorPoints(input);
        final List<Vec3> concatenated = new ArrayList<Vec3>(input.size());
        for (int i=0; i<sublists.size(); i++) {
            final List<Vec3> sublistWhole = new ArrayList(sublists.get(i));
            assertTrue(sublistWhole.size()>1);
            List<Vec3> sublistPart = sublistWhole;
            if (i > 0) {
                // not the first sublist: remove the first point,
                // then make sure it's on the equator and between its
                // neighbours
                final Vec3 firstPoint = sublistPart.remove(0);
                assertEquals(0, firstPoint.z, delta);
                final List<Vec3> previousList = sublists.get(i-1);
                assertTrue(areCoplanar(previousList.get(previousList.size()-2),
                        firstPoint, sublistPart.get(0)));
            }
            if (i < sublists.size() - 1) {
                // not the last sublist: remove the last point,
                // then make sure it's on the equator and between its
                // neighbours
                final Vec3 lastPoint =
                        sublistPart.remove(sublistPart.size() - 1);
                assertEquals(0, lastPoint.z, delta);
                final List<Vec3> nextList = sublists.get(i+1);
                assertTrue(areCoplanar(sublistPart.get(sublistPart.size()-1),
                        lastPoint, nextList.get(1)));
            }

            final long nHemispheres =
                    sublistPart.stream().map(v -> Math.signum(v.z)).
                            distinct().count();
            assertEquals(1, nHemispheres);
            concatenated.addAll(sublistPart);
        }
        assertEquals(input, concatenated);
    }
    
    @Test
    public void testNearestOnCircle() {
        for (Vec3 circle: testUnitVectors) {
            for (Vec3 point: testUnitVectors) {
                final Vec3 result = circle.nearestOnCircle(point);
                assertEquals(0, circle.dot(result), delta);
                assertTrue(areCoplanar(circle, point, result));
            }
        }
    }
    
    @Test
    public void testGetStrikeDeg() {
        final Random rnd = new Random(17);
        for (int i=0; i<20; i++) {
            double strike = rnd.nextDouble() * 360;
            double dip = rnd.nextDouble() * 90;
            if (rnd.nextInt(2) == 0) {
                /*
                 * 
                 */
                dip += 180;
            }
            /* Create a normal vector for a plane with the chosen dip
             * at a 0-degree strike. */
            final Vec3 unrotatedNormal = Vec3.fromPolarDegrees(1, dip-90, 90);
            assertEquals(0, unrotatedNormal.getStrikeDeg(), delta);
            /* Rotate it to match the chosen strike.         */
            final Vec3 rotatedNormal = unrotatedNormal.rotZ(toRadians(strike));
            assertEquals(strike, rotatedNormal.getStrikeDeg(), delta);
        }
    }
    
    private boolean areCoplanar(Vec3 v0, Vec3 v1, Vec3 v2) {
        final Vec3 cross = v0.cross(v1);
        final double dot = cross.dot(v2);
        return (Math.abs(dot) < delta);
    }

}
