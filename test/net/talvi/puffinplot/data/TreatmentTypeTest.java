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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author pont
 */
public class TreatmentTypeTest {

    /**
     * Tests that the possible values of TreatmentType are as expected.
     */
    @Test
    public void testValues() {
        TreatmentType[] expResult = {TreatmentType.NONE, TreatmentType.DEGAUSS_XYZ,
            TreatmentType.DEGAUSS_Z, TreatmentType.ARM, TreatmentType.IRM,
            TreatmentType.THERMAL, TreatmentType.UNKNOWN};
        TreatmentType[] result = TreatmentType.values();
        Assert.assertArrayEquals(expResult, result);
    }

    /**
     * Tests the {@link TreatmentType#getAxisLabel()} method.
     */
    @Test
    public void testGetAxisLabel() {
        for (TreatmentType t: TreatmentType.values()) {
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
        for (TreatmentType treatmentType : TreatmentType.values()) {
            if (treatmentType == TreatmentType.DEGAUSS_XYZ ||
                treatmentType == TreatmentType.DEGAUSS_Z ||
                    treatmentType == TreatmentType.ARM) {
                assertTrue(treatmentType.involvesAf());
            } else {
                assertFalse(treatmentType.involvesAf());
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
                Arrays.stream(TreatmentType.values()).map(tt -> tt.getNiceName()).
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
        for (TreatmentType treatmentType : TreatmentType.values()) {
            if (treatmentType.involvesAf() || treatmentType == TreatmentType.IRM) {
                assertEquals("T", treatmentType.getUnit());
            }
            if (treatmentType == TreatmentType.THERMAL) {
                assertEquals("°C", treatmentType.getUnit());
            }
        }
    }

}