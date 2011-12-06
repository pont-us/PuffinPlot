/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot;

import java.util.BitSet;
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
public class UtilTest {
    
    public UtilTest() {
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

    private BitSet makeBitSet(String spec) {
        BitSet result = new BitSet();
        for (int i=0; i<spec.length(); i++) {
            result.set(i, spec.substring(i, i+1).equals("1"));
        }
        return result;
    }
    
    /**
     * Test of numberRangeStringToBitSet method, of class Util.
     */
    @Test
    public void testNumberRangeStringToBitSet() {
        System.out.println("numberRangeStringToBitSet");
        final String[] inputsAndResults = {
            "1", "1",
            "1,3", "101",
            "4-6", "000111",
            "4-6,8-10,10,11,15-16", "0001110111100011",
            "1-4,3-5,10,12-14,17", "11111000010111001"
        };
        final int limit = 128;
        for (int i=0; i<inputsAndResults.length; i+=2) {
            assertEquals(makeBitSet(inputsAndResults[i+1]),
                    Util.numberRangeStringToBitSet(inputsAndResults[i], limit));
        }
    }
}
