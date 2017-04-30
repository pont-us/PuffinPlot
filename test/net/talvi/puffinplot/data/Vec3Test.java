/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.talvi.puffinplot.data;

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class Vec3Test {

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
}
