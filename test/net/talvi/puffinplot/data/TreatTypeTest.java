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
        TreatType[] expResult = null;
        TreatType[] result = TreatType.values();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    public void testValueOf() {
        System.out.println("valueOf");
        String name = "";
        TreatType expResult = null;
        TreatType result = TreatType.valueOf(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    public void testFromString() {
        System.out.println("fromString");
        String s = "";
        TreatType expResult = null;
        TreatType result = TreatType.fromString(s);
        assertEquals(TreatType.DEGAUSS, TreatType.fromString("DEGAUSS"));
        assertEquals(TreatType.THERMAL, TreatType.fromString("THERMAL"));
        assertEquals(TreatType.IRM, TreatType.fromString("IRM"));
        assertEquals(TreatType.NONE, TreatType.fromString("NONE"));
        assertEquals(TreatType.UNKNOWN, TreatType.fromString("{}{}{}"));
    }

    @Test
    public void testGetAxisLabel() {
        System.out.println("getAxisLabel");
        TreatType instance = null;
        String expResult = "";
        String result = instance.getAxisLabel();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}