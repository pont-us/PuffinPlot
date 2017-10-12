/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author pont
 */
public class Vec3Test {

    private Collection<Vec3> testVectors;
    
    @Before
    public void setUp() {
        // randomly generated values
        final double[][] values = {
            {2.06, 7.14, 9.49},
            {8.63, 8.92, 4.26},
            {3.55, 1.64, 4.90},
            {2.69, 2.70, 0.26},
            {3.96, 3.37, 3.32},
            {2.63, 4.28, 3.03},
            {1.32, 2.74, 8.08},
            {0.91, 1.13, 9.99},
            {1.63, 4.19, 2.77},
            {7.58, 9.84, 1.45},
        };
        
        testVectors = new ArrayList<>(10);
        
        for (double[] xyz: values) {
            testVectors.add(new Vec3(xyz[0], xyz[1], xyz[2]));
        }
    }
    
    /**
     *  Tests the {@link Vec3#addDecRad(double)} method.
     */
    @Test
    public void testAddDecRad() {
        System.out.println("addDecRad");
        final double mag = 1.9;
        final double tolerance = 0.0001;
        double[] angles = {-279, -201, -140, -50, -10, 0, 10, 40, 70, 100, 190, 359};
        for (double dec: angles) {
            for (double inc: angles) {
                Vec3 original = Vec3.fromPolarDegrees(mag, inc, dec);
                for (double offset: angles) {
                    Vec3 expected = Vec3.fromPolarDegrees(mag, inc, dec+offset);
                    Vec3 result = original.addDecRad(Math.toRadians(offset));
                    assertEquals(expected.x, result.x, tolerance);
                    assertEquals(expected.y, result.y, tolerance);
                    assertEquals(expected.z, result.z, tolerance);
                }
            }
        }
    }
    
    /**
     *  Tests the {@link Vec3#addIncRad(double)} method.
     */
    @Test
    public void testAddIncRad() {
        System.out.println("addIncRad");
        final double mag = 1.0;
        final double tolerance = 0.0001;
        double[] decs = {-279, -201, -140, -50, -10, 0, 10, 40, 70, 100, 190, 359};
        double[] incs = {-80, -40, -10, 5, 20, 50, 80};
        double[] offsets = {-80, -50, -10, 5, 10, 40, 60, 85};
        for (double dec: decs) {
            for (double inc: incs) {
                Vec3 original = Vec3.fromPolarDegrees(mag, inc, dec);
                for (double offset: offsets) {
                    Vec3 expected = Vec3.fromPolarDegrees(mag, inc+offset, dec);
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
        System.out.println("equatorPoint");
        
        // A couple of simple tests
        testOneEquatorPoint(0, 1, 1, 0, 1, -1, 0, 1, 0);
        testOneEquatorPoint(1, 0, 1, 1, 0, -1, 1, 0, 0);
        
        // Unequal length vectors
        testOneEquatorPoint(0, 2, 5, 0, 1, -1, 0, 1, 0);
        
        // Point on equator
        testOneEquatorPoint(1, 0, 0, 1, 0, 1, 1, 0, 0);
        testOneEquatorPoint(11, 12, 13, 0, 1, 0, 0, 1, 0);
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
    
    @Test
    public void testRot180() {
        System.out.println("rot180");
        final double delta = 1e-6;
        MeasurementAxis[] axes = {
            MeasurementAxis.X,
            MeasurementAxis.Y,
            MeasurementAxis.Z
        };
        
        for (Vec3 vec3: testVectors) {
            // Test that double rotation about same axis
            // produces original vector
            for (MeasurementAxis axis: axes) {
                assertTrue(vec3.equals(vec3.rot180(axis).rot180(axis), delta));
            }
            
            // Test that rotating about all three axes produces
            // original vector
            assertTrue(vec3.equals(vec3.rot180(axes[0]).rot180(axes[1]).
                    rot180(axes[2]), delta));
        
            // Test that rotating about two axes is equivalent to
            // a rotation about the third axis
            for (int i=0; i<3; i++) {
                final Vec3 twoRots = vec3.rot180(axes[i]).
                        rot180(axes[(i+1)%3]);
                final Vec3 oneRot = vec3.rot180(axes[(i+2)%3]);
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
}
