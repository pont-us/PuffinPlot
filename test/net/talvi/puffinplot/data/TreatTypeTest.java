/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
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
    
    @Test
    public void testIncludesAf() {
        for (TreatType treatType: TreatType.values()) {
            if (treatType == TreatType.DEGAUSS_XYZ ||
                treatType == TreatType.DEGAUSS_Z ||
                    treatType == TreatType.ARM) {
                assertTrue(treatType.involvesAf());
            } else {
                assertFalse(treatType.involvesAf());
            }
        }
    }
    
    @Test
    public void testGetNiceName() {
        /* Nice names aren't precisely defined and we can't do an automated
         * test for niceness, so we just check for duplicates and empty
         * names.
         */
        
        final Collection<String> niceNames =
                Arrays.stream(TreatType.values()).map(tt -> tt.getNiceName()).
                        collect(Collectors.toList());
        final Set<String> duplicates = niceNames.stream().
                filter(name -> Collections.frequency(niceNames, name) > 1).
                collect(Collectors.toSet());
        assertTrue("Duplicate names:" + duplicates.toString(),
                duplicates.isEmpty());
        assertFalse("Empty name", niceNames.contains(""));
    }
    
    @Test
    public void testGetUnit() {
        for (TreatType treatType: TreatType.values()) {
            if (treatType.involvesAf() || treatType==TreatType.IRM) {
                assertEquals("T", treatType.getUnit());
            }
            if (treatType == TreatType.THERMAL) {
                assertEquals("°C", treatType.getUnit());
            }
        }
    }

}