package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;

/**
 * Tests the {@link ArmAxis} class.
 * 
 * @author pont
 */
//
@RunWith(Enclosed.class)
public class ArmAxisTest {

    @RunWith(value=Parameterized.class)
    public static class ParameterizedTests {
        
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
        public ParameterizedTests(String name, ArmAxis value) {
            this.name = name;
            this.value = value;
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
        
        /**
         * Tests the {@link ArmAxis#fromString(java.lang.String)} method.
         */
        @Test
        public void testGetByString() {
            if (ArmAxis.fromString(name) != value) {
                fail(name + " not initialized.");
            }
        }
    }
    
    /* Inner class for non-parameterized tests to stop them getting run
     * multiple times -- though they'll still get run twice: see
     * https://stackoverflow.com/a/28203229/6947739
     * 
     */
    public static class NonParameterizedTests {
        @Test
        public void testValues() {
            final Collection<ArmAxis> values = Arrays.asList(ArmAxis.values());
            assertTrue(values.contains(ArmAxis.AXIAL));
            assertTrue(values.contains(ArmAxis.UNKNOWN));
            assertTrue(values.contains(ArmAxis.NONE));
            assertEquals(3, values.size());
        }
        
            
        @Test
        public void testValueOf() {
            final ArmAxis[] axes = ArmAxis.values();
            for (ArmAxis axis: axes) {
                assertEquals(axis, ArmAxis.valueOf(axis.toString()));
            }
        }
    
        @Rule
        public final ExpectedException exception = ExpectedException.none();
    
        @Test
        public void testFromNullString() {
            exception.expect(NullPointerException.class);
            ArmAxis.fromString(null);
        }
    }
	
}
