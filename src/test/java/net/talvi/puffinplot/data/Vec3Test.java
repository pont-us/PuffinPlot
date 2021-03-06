/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import Jama.Matrix;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        testOneEquatorPoint(0, 0, 1, 0, 1, -1, 0, 1, 0);

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

        for (Vec3[] illegalPair: illegal) {
            try {
                Vec3.equatorPoint(illegalPair[0], illegalPair[1]);
                fail();
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
            assertTrue(v.isFinite());
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
            assertFalse(new Vec3(xyz[0], xyz[1], xyz[2]).isFinite());
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
            assertTrue(step.isFinite());
        });
    }
    
    @Test (expected = NullPointerException.class)
    public void testSpherInterpolateNullFirstVector() {
        Vec3.spherInterpolate(null, Vec3.EAST, 0.1);
    }
    
    @Test (expected = NullPointerException.class)
    public void testSpherInterpolateNullSecondVector() {
        Vec3.spherInterpolate(Vec3.EAST, null, 0.1);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateNonFiniteFirstVector() {
        Vec3.spherInterpolate(new Vec3(1, 0, Double.NaN), Vec3.EAST, 0.1);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateNonFiniteSecondVector() {
        Vec3.spherInterpolate(Vec3.EAST, new Vec3(1, 0, Double.NaN), 0.1);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateZeroFirstVector() {
        Vec3.spherInterpolate(Vec3.ORIGIN, Vec3.EAST, 0.1);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateZeroSecondVector() {
        Vec3.spherInterpolate(Vec3.EAST, Vec3.ORIGIN, 0.1);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateStepSizeNonFinite() {
        Vec3.spherInterpolate(Vec3.EAST, Vec3.NORTH, Double.NEGATIVE_INFINITY);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateStepSizeTooSmall() {
        Vec3.spherInterpolate(Vec3.EAST, Vec3.NORTH, -100);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpolateStepSizeTooLarge() {
        Vec3.spherInterpolate(Vec3.EAST, Vec3.NORTH, 100);
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
    public void testEqualsWithPrecision() {
        final double precision = 1e-20;
        for (int i=0; i<testVectors.size(); i++) {
            for (int j=0; j<testVectors.size(); j++) {
                final Vec3 v0 = testVectors.get(i);
                final Vec3 v1 = testVectors.get(j);
                if (i==j) {
                    assertTrue(v0.equals(v1, precision));
                } else {
                    assertFalse(v0.equals(v1, precision));
                }
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void equalsThrowsExceptionForNonFinitePrecision() {
        Vec3.ORIGIN.equals(Vec3.ORIGIN, Double.NaN);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void equalsThrowsExceptionForNegativePrecision() {
        Vec3.ORIGIN.equals(Vec3.ORIGIN, -0.1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void equalsThrowsExceptionForNonFiniteOtherVector() {
        Vec3.ORIGIN.equals(new Vec3(Double.NaN, 0, 0), 0.1);
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
                /* Randomly flip the vector by 180 degrees. getStrikeDeg
                 * should always take the upward-pointing direction,
                 * even if the vector's pointing down.
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
    
    @Test
    public void testToCustomString() {
        // test prefix, postfix, and separators
        assertEquals("BEFORE 0.00 BETWEEN 0.00 BETWEEN 0.00 AFTER",
                Vec3.ORIGIN.toCustomString("BEFORE ", " AFTER", " BETWEEN ",
                        2, false));
        
        /* Test scale. The scale is chosen based on the total
         * magnitude of the vector, which is why (5.5e7, 5.5e7, 5.5e7)
         * comes out as "<55.0 55.0 55.0>e6" (since sqrt(5.5^2 * 3) < 10)
         * but (6e7, 6e7, 6e7) comes out as "<6.0 6.0 6.0>e7"
         * (since sqrt(6^2 * 3) > 10).
         */ 
        assertEquals("<9.9 9.9 9.9>e6",
                new Vec3(9.9e6, 9.9e6, 9.9e6).
                        toCustomString("<", ">", " ", 1, true));
        assertEquals("<10.000 10.000 10.000>e6",
                new Vec3(1e7, 1e7, 1e7).toCustomString("<", ">", " ", 3, true));
        assertEquals("<11.0 11.0 11.0>e6",
                new Vec3(1.1e7, 1.1e7, 1.1e7).
                        toCustomString("<", ">", " ", 1, true));
        assertEquals("<55.0 55.0 55.0>e6",
                new Vec3(5.5e7, 5.5e7, 5.5e7).
                        toCustomString("<", ">", " ", 1, true));
        assertEquals("<6.0 6.0 6.0>e7",
                new Vec3(6e7, 6e7, 6e7).
                        toCustomString("<", ">", " ", 1, true));
        assertEquals("[1.200, 3.400, 5.600]e-5",
                new Vec3(1.2e-5, 3.4e-5, 5.6e-5).
                        toCustomString("[", "]", ", ", 3, true));
        
        // test decimal places
        final Vec3 dpTest =
                new Vec3(11.2222222222, 11.2222222222, 11.2222222222);
        for (int dp=0; dp<10; dp++) {
            final String s = dpTest.toCustomString("", "", " ", dp, false);
            final long numberOf2s = s.chars().filter((x) -> x == '2').count();
            assertEquals(3*dp, numberOf2s);
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void makeEllipseThrowsExceptionForNull() {
        Vec3.makeEllipse(null);
    }
    
    @Test
    public void testMakeEllipse() {
        // The only parameters used are etaMag, zetaMag, inc, dec, etaDec, and etaInc
        // Data from Campbell Island
        final String[] paramStrings = {
            "0.33546 0.00026    46.7     5.7    10.5   315.0    14.1    12.6   163.0    74.1",
            "0.33274 0.00018   297.0    73.5    10.2    48.2     6.4    32.4   139.9    15.0",
            "0.33180 0.00024   138.3    15.4    12.0    47.6     2.4    46.9   311.6    67.7",
            "0.33449 0.00012    77.5     4.5    11.2   167.8     2.7    27.0   274.5    80.7",
            "0.33319 0.00011   333.0    72.5    10.9   233.4     3.4    16.6   142.3    17.2",
            "0.33232 0.00010   168.9    16.9     7.2    75.4    11.8    16.0   312.4    68.9",
            "0.33423 0.00010   134.7     3.5    17.2    44.7     1.9    38.6   303.7    80.1",
            "0.33322 0.00010   235.4    71.9    12.2   138.0     2.5    23.6    47.2    17.9",
            "0.33255 0.00010    43.6    17.8     9.5   133.9     0.8    25.7   226.1    70.5"
        };
        
        for (String paramString: paramStrings) {
            final KentParams kp = new KentParams(paramString);
            final List<Vec3> ellipse = Vec3.makeEllipse(kp);
            testOneEllipse(kp, ellipse);
        }
    }

    private void testOneEllipse(KentParams kp, final List<Vec3> ellipse) {
        final Vec3 expectedMean = Vec3.fromPolarDegrees(1,
                kp.getMean().getIncDeg(), kp.getMean().getDecDeg());
        final Vec3 actualMean = Vec3.meanDirection(ellipse);
        // We don't expect extreme accuracy for the mean, since the points
        // are fairly spaced out. 
        assertTrue(expectedMean.equals(actualMean, 1e-2));
        
        // Check that angular distances from edge to centre all in range, and
        // that they inflect exactly four times.
        double prevAngleToCentre = -1;
        int prevDirectionOfRadiusChange = 0; // increasing or decreasing
        int nInflections = 0; // number of changes between increase/decrease
        
        // Determine major and minor axes. According to /Essentials of
        // Paleomagnetism/, zeta should be the major axis, but in practice
        // bootams.py and s_hext.py sometimes return an ellipse with
        // zeta < eta, so we need to check for this.
        double minorAxisAngle, majorAxisAngle;
        Vec3 minorAxisDir, majorAxisDir;
        if (kp.getEtaMag() > kp.getZetaMag()) {
            minorAxisAngle = kp.getZetaMag();
            minorAxisDir = kp.getZetaDir();
            majorAxisAngle = kp.getEtaMag();
            majorAxisDir = kp.getEtaDir();
        } else {
            minorAxisAngle = kp.getEtaMag();
            minorAxisDir = kp.getEtaDir();
            majorAxisAngle = kp.getZetaMag();
            majorAxisDir = kp.getZetaDir();            
        }
        double minAngle = 1e10; // minimum angular distance to centre
        double maxAngle = -1; // maximum angular distance to centre
        Vec3 minAngleVector = null; // vector which gives minAngle
        Vec3 maxAngleVector = null; // vector which gives maxAngle
        for (Vec3 v: ellipse) {
            final double angle = Math.abs(actualMean.angleTo(v));
            // We allow a 0.01-radian margin of error.
            assertTrue(angle > minorAxisAngle - 0.01);
            assertTrue(angle < majorAxisAngle + 0.01);
            if (prevAngleToCentre > -1) {
                int direction = angle > prevAngleToCentre ? 1 : -1;
                if (direction != prevDirectionOfRadiusChange) {
                    nInflections++;
                }
                prevDirectionOfRadiusChange = direction;
            }
            prevAngleToCentre = angle;
            
            if (angle < minAngle) {
                minAngle = angle;
                minAngleVector = v;
            }
            if (angle > maxAngle) {
                maxAngle = angle;
                maxAngleVector = v;
            }
        }
        // Check that the ellipse is ellipse-shaped
        assertEquals(nInflections, 4);
        
        // Check that the major and minor axes point in the right directions
        assertTrue(areRoughlyCoplanar(actualMean, kp.getZetaDir(),
                maxAngleVector, 0.01));
        assertTrue(areRoughlyCoplanar(actualMean, kp.getEtaDir(),
                minAngleVector, 0.01));
        
        // Smoothness: check that angle between adjacent ellipse segments is
        // not too large.
        Vec3 prevDiff = null;
        for (int i=0; i<ellipse.size()-1; i++) {
            final Vec3 diff = ellipse.get(i+1).minus(ellipse.get(i)).normalize();
            if (prevDiff != null) {
                final double angle = diff.angleTo(prevDiff);
                assertTrue(Math.abs(angle) < 0.25);
            }
            prevDiff = diff;
        }
    }
    

    @Test
    public void testSpherInterpDir() {
        for (Vec3 from: testUnitVectors) {
            for (Vec3 to: testUnitVectors) {
                for (Vec3 direction: testUnitVectors) {
                    final double step = 0.05;
                    final List<Vec3> arc =
                            Vec3.spherInterpDir(from, to, direction, step);
                    testOneSpherInterpDir(from, to, direction, step, arc);
                }
            }
        }
    }
    
    private void testOneSpherInterpDir(Vec3 from, Vec3 to, Vec3 direction,
            double step, List<Vec3> arc) {
        if (from.equals(to)) {
            assertTrue(from.equals(arc.get(0)));
            return;
        }
        assertTrue(from.equals(arc.get(0), delta));
        assertTrue(to.equals(arc.get(arc.size()-1), delta));
        assertTrue(areCoplanar(arc, 0.02));
        
        // check requested spacing
        if (from.angleTo(to) >= step) {
            int repeated = 0;
            for (int i=1; i<arc.size()-1; i++) {
                final double angle = Math.abs(arc.get(i-1).angleTo(arc.get(i)));
                if (angle < delta) {
                    repeated++;
                } else {
                    /* We put a very loose constraint on the spacing.
                     * The arc may be cobbled together from sub-arcs,
                     * each of which may have to fiddle the step value
                     * if it doesn't divide the sub-arc length exactly.
                     * We're just sanity checking here.
                     */
                    assertEquals(step, angle, step*0.9);
                }
            }
            assertTrue(repeated==0 || repeated==4);
        }
        
        // Check that the direction is correct.
        final double largestDot = largestDotProduct(direction, arc);
        final double largestInverseDot =
                largestDotProduct(direction.invert(), arc);
                
        assertTrue(largestDot + delta >= largestInverseDot);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpDirStepSizeNonFinite() {
        Vec3.spherInterpDir(Vec3.EAST, Vec3.NORTH, Vec3.DOWN, Double.NEGATIVE_INFINITY);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpDirStepSizeTooSmall() {
        Vec3.spherInterpDir(Vec3.EAST, Vec3.NORTH, Vec3.DOWN, -100);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSpherInterpDirStepSizeTooLarge() {
        Vec3.spherInterpDir(Vec3.EAST, Vec3.NORTH, Vec3.DOWN, 100);
    }
    
    private double largestDotProduct(Vec3 reference, List<Vec3> vectors) {
        double max = -1;
        for (int i=0; i<vectors.size(); i++) {
            final Vec3 v = vectors.get(i);
            final double dot = reference.dot(v);
            if (dot > max) {
                max = dot;
            }

        }
        return max;
    }
    
    @Test
    public void testGetDipDeg() {
        for (Vec3 v: testVectors) {
            double incRad = atan2(v.z, sqrt(v.x*v.x + v.y*v.y));
            if (incRad > 0) {
                incRad = -incRad;
            }
            final double dipRad = incRad + Math.PI / 2;
            final double dipDeg = Math.toDegrees(dipRad);
            assertEquals(dipDeg, v.getDipDeg(), delta);
        }
    }
    
    @Test
    public void testCorrectForm() {
        final double[] azimuths =
            {0, 0.1, 1, 2, 5, 10, 15, 19, 45, 90,
                135, 137, 180, 181, 269, 270, 359};
        final double[] dips = {0, 0.1, 1, 2, 5, 10, 45, 49, 90};
        for (double azimuth: azimuths) {
            for (double dip: dips) {
                for (Vec3 v: testVectors) {
                    testCorrectFormInversion(azimuth, dip, v);
                    testCorrectFormVsManual(azimuth, dip, v);
                }
            }
        }
    }

    @Test
    public void testOTensor() {
        final Random rnd = new Random(99);
        for (int test=0; test<10; test++) {
            final Vec3 v = new Vec3(rnd.nextDouble()*100-50,
                    rnd.nextDouble()*100-50,
                    rnd.nextDouble()*100-50);
            final Matrix tensor = v.oTensor();
            for (int i=0; i<3; i++) {
                for (int j=0; j<3; j++) {
                    assertEquals(
                            getVectorComponent(v, i) * getVectorComponent(v, j),
                            tensor.get(i, j),
                            1e-6);
                }
            }
        }
    }
    
    @Test
    public void testRotZ() {
        assertTrue(Vec3.EAST.equals(Vec3.NORTH.rotZ(Math.PI/2), delta));
        assertTrue(Vec3.NORTH.equals(Vec3.EAST.rotZ(3*Math.PI/2), delta));
        assertTrue(Vec3.ORIGIN.equals(Vec3.ORIGIN.rotZ(Math.PI/3), delta));
        assertTrue(Vec3.DOWN.equals(Vec3.DOWN.rotZ(1), delta));
        final Random rnd = new Random(99);
        for (Vec3 v: testVectors) {
            final double angle = (rnd.nextDouble() * 4 - 2) * Math.PI;
            assertTrue(v.equals(v.rotZ(angle).rotZ(-angle), delta));
        }
    }
    
    @Test
    public void testNormalize() {
        for (Vec3 v: testVectors) {
            if (Vec3.ORIGIN.equals(v)) {
                continue;
            }
            final Vec3 normalized = v.normalize();
            assertEquals(1, normalized.mag(), delta);
            assertEquals(v.getDecRad(), normalized.getDecRad(), delta);
            assertEquals(v.getIncRad(), normalized.getIncRad(), delta);
        }
    }

    /**
     * The result is undefined and thus unchecked, but we want to check that no
     * exception is thrown.
     */
    @Test
    public void testNormalizeZero() {
        Vec3.ORIGIN.normalize();
    }
    
    /**
     * The result is undefined and thus unchecked, but we want to check that no
     * exception is thrown.
     */
    @Test
    public void testNormalizeNaN() {
        new Vec3(Double.NaN, 1, 1).normalize();
    }
    
    /**
     * The result is undefined and thus unchecked, but we want to check that no
     * exception is thrown.
     */
    @Test
    public void testNormalizeInfinite() {
        new Vec3(Double.POSITIVE_INFINITY, 1, 1).normalize();
    }
    
    @Test(expected = NullPointerException.class)
    public void distanceThrowsExceptionForNull() {
        Vec3.ORIGIN.distance(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void distanceThrowsExceptionForNonFinite() {
        Vec3.ORIGIN.distance(new Vec3(Double.NaN, 0, 0));
    }
    
    private static double getVectorComponent(Vec3 v, int i) {
        switch (i) {
            case 0: return v.x;
            case 1: return v.y;
            case 2: return v.z;
            default: throw new IllegalArgumentException();
        }
    }
    
    private void testCorrectFormInversion(double azimuth, double dip, Vec3 v) {
        final Vec3 tilted = v.correctForm(toRadians(azimuth),
                toRadians(dip));
        final Vec3 detilted = tilted.
                correctForm(toRadians((180 + azimuth) % 360),
                        toRadians(dip));
        assertTrue(v.equals(detilted, delta));
    }
    
    private void testCorrectFormVsManual(double azimuth, double dip, Vec3 v) {
        final double azRad = toRadians(azimuth);
        final double dipRad = toRadians(dip);
        final Vec3 expected = v.rotZ(-azRad).rotY(dipRad).rotZ(azRad);
        final Vec3 actual = v.correctForm(azRad, dipRad);
        if (!expected.equals(actual, delta)) {
            assertTrue(expected.equals(actual, delta));
        }
    }
    
    private boolean areCoplanar(List<Vec3> vectors, double limit) {
        final Vec3 normal = vectors.get(0).cross(vectors.get(1));
        return vectors.stream().mapToDouble(v -> Math.abs(v.dot(normal))).
                allMatch(x -> x < limit);
    }
    
    private boolean areCoplanar(Vec3 v0, Vec3 v1, Vec3 v2) {
        return areRoughlyCoplanar(v0, v1, v2, delta);
    }

    private boolean areRoughlyCoplanar(Vec3 v0, Vec3 v1, Vec3 v2, double limit) {
        final Vec3 cross = v0.cross(v1);
        final double dot = cross.dot(v2);
        return (Math.abs(dot) < limit);
    }

}
