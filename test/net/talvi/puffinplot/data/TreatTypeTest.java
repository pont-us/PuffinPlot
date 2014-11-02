/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.talvi.puffinplot.data;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class TreatTypeTest {

    /**
     * Tests that the possible values of TreatType are as expected.
     */
    @Test
    public void testValues() {
        System.out.println("values");
        TreatType[] expResult = {TreatType.NONE, TreatType.DEGAUSS_XYZ,
            TreatType.DEGAUSS_Z, TreatType.ARM, TreatType.IRM,
            TreatType.THERMAL, TreatType.UNKNOWN};
        TreatType[] result = TreatType.values();
        Assert.assertArrayEquals(expResult, result);
    }

    /**
     * Tests the {@link TreatType#getAxisLabel()} method.
     */
    @Test
    public void testGetAxisLabel() {
        System.out.println("getAxisLabel");

        for (TreatType t: TreatType.values()) {
            String label = t.getAxisLabel();
            switch (t) {
            case NONE: assertEquals("", label);
                break;
            case DEGAUSS_XYZ: assertEquals("3-axis AF strength", label);
                break;
            case DEGAUSS_Z: assertEquals("Z-axis AF strength", label);
                break;
            case IRM: assertEquals("IRM field", label);
                break;
            case THERMAL: assertEquals("Temperature", label);
                break;
            case UNKNOWN: assertEquals("Unknown treatment", label);
                break;
            case ARM: assertEquals("AF strength", label);
                break;
            default: fail("No test for axis label "+label);
                break;
            }
        }
    }

}