/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */

package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
