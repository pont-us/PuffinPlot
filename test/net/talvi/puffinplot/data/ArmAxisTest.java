package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

/**
 * Tests the {@link ArmAxis} class.
 * 
 * @author pont
 */
@RunWith(value=Parameterized.class)
public class ArmAxisTest {

    private final String name;
    private final ArmAxis value;
	
    /**
     * Creates data to test the class.
     * 
     * @return an array of testing data
     */
    @Parameters
    public static Collection data() {
        return Arrays.asList( new Object[][] {
        		{"AXIAL", ArmAxis.AXIAL},
                        {"NA", ArmAxis.NONE},
        		{"NONE", ArmAxis.NONE}});
    }
	
    /**
     * Creates a new instance of ArmAxisTest.
     * 
     * @param name the name of the ARM axis
     * @param value the ArmAxis object corresponding to the name
     */
    public ArmAxisTest(String name, ArmAxis value) {
    	this.name = name;
    	this.value = value;
    }
    
    /**
     * Tests the {@link ArmAxis#fromString(java.lang.String)} method.
     */
    @Test
    public void testGetByString() {
        if (ArmAxis.fromString(name) != value) {
            fail(name + " not initialized.");
        }
    }
	
    /**
     * Tests the {@link ArmAxis#fromString(java.lang.String)} method with an
     * unknown axis name.
     */
    @Test
    public void unknownStringTest() {
        assertEquals(ArmAxis.fromString("NONEXISTENT"), ArmAxis.UNKNOWN);
        assertEquals(ArmAxis.fromString("utter gibberish"), ArmAxis.UNKNOWN);
    }
	
}
