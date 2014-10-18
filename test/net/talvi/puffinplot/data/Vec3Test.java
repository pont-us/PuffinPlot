/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.talvi.puffinplot.data;

import java.util.Locale;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class Vec3Test {

    public Vec3Test() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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


}