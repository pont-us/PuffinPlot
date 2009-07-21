/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.talvi.puffinplot.data;

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
public class TreatTypeTest {

    public TreatTypeTest() {
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
    public void testValues() {
        System.out.println("values");
        TreatType[] expResult = {TreatType.NONE, TreatType.DEGAUSS_XYZ,
        TreatType.DEGAUSS_Z,
        TreatType.ARM, TreatType.IRM, TreatType.THERMAL, TreatType.UNKNOWN};
        TreatType[] result = TreatType.values();
        assertEquals(expResult, result);
    }

    @Test
    public void testFromString() {
        System.out.println("fromString");
        assertEquals(TreatType.DEGAUSS_XYZ, TreatType.fromString("Degauss X, Y, & Z"));
        assertEquals(TreatType.DEGAUSS_Z, TreatType.fromString("Degauss Z"));
        assertEquals(TreatType.THERMAL, TreatType.fromString("Thermal Demag"));
        assertEquals(TreatType.ARM, TreatType.fromString("Degauss Z - ARM axial"));
        assertEquals(TreatType.IRM, TreatType.fromString("IRM"));
        assertEquals(TreatType.NONE, TreatType.fromString("NONE"));
        assertEquals(TreatType.UNKNOWN, TreatType.fromString("{}{}{}"));
    }

    @Test
    public void testGetAxisLabel() {
        System.out.println("getAxisLabel");

        for (TreatType t: TreatType.values()) {
            String label = t.getAxisLabel();
            switch (t) {
            case NONE: assertEquals("No demagnetization", label);
                break;
            case DEGAUSS_XYZ: assertEquals("3-axis AF strength (G)", label);
                break;
            case DEGAUSS_Z: assertEquals("Z-axis AF strength (G)", label);
                break;
            case IRM: assertEquals("IRM field strength", label);
                break;
            case THERMAL: assertEquals("Temperature (°C)", label);
                break;
            case UNKNOWN: assertEquals("Unknown treatment", label);
                break;
            case ARM: assertEquals("ARM field strength", label);
                break;
            default: fail("No test for axis label "+label);
                break;
            }
        }
    }

}