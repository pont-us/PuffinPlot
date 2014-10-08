package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

@RunWith(value=Parameterized.class)
public class ArmAxisTest {

    private final String name;
    private final ArmAxis value;
	
    @Parameters
    public static Collection data() {
        return Arrays.asList( new Object[][] {
        		{"AXIAL", ArmAxis.AXIAL},
                        {"NA", ArmAxis.NONE},
        		{"NONE", ArmAxis.NONE}});
    }
	
    public ArmAxisTest(String name, ArmAxis value) {
    	this.name = name;
    	this.value = value;
    }
    
    @Test
    public void testGetByString() throws Exception {
        if (ArmAxis.fromString(name) != value) {
            fail(name + " not initialized.");
        }
    }
	
    @Test
    public void unknownStringTest() throws Exception {
        assertEquals(ArmAxis.fromString("NONEXISTENT"), ArmAxis.UNKNOWN);
        assertEquals(ArmAxis.fromString("utter gibberish"), ArmAxis.UNKNOWN);
    }
	
}
