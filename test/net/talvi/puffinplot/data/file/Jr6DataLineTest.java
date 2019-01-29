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
package net.talvi.puffinplot.data.file;

import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.Vec3;
import org.junit.Test;

import static net.talvi.puffinplot.data.file.OrientationParameters.AzimuthParameter;
import static net.talvi.puffinplot.data.file.OrientationParameters.DipParameter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Jr6DataLineTest {
    
    @Test
    public void testReadNrm() {
        
        final String string =
                "BC0101A1  NRM       0.08 -1.45 -0.46  -2"
                + " 107  88  11  12  13  14 12  0 12  0   1";
        
        final Jr6DataLine line = Jr6DataLine.read(string);
        assertEquals("BC0101A1", line.getName());
        assertEquals(TreatmentType.NONE, line.getTreatmentType());
        assertEquals(0, line.getTreatmentLevel());
        assertTrue(new Vec3(0.08e-2, -1.45e-2, -0.46e-2).equals(
                line.getMagnetization(), 1e-10));
        assertEquals(107, line.getAzimuth());
        assertEquals(88, line.getDip());
        assertEquals(11, line.getFoliationAzimuth());
        assertEquals(12, line.getFoliationDip());
        assertEquals(13, line.getLineationTrend());
        assertEquals(14, line.getLineationPlunge());
        assertEquals(new OrientationParameters(AzimuthParameter.A12,
                DipParameter.D0, AzimuthParameter.A12, DipParameter.D0),
                line.getOrientationParameters());
    }

    /**
     * Unfortunately I haven't been able to find any documentation for the
     * JR6 treatment codes. "M" for ARM is an educated guess. For the
     * present, this test defines what I believe to be correct behaviour
     * for the "M" code. If this turns out to be false, I can always change
     * the test and code later -- for now, the best I can do is to make
     * sure it's well-defined.
     */
    @Test
    public void testReadArm() {
        final String string =
                "BC0101A1  M50       0.08 -1.45 -0.46  -2"
                + " 107  88  11  12  13  14 12  0 12  0   1";
        final Jr6DataLine line = Jr6DataLine.read(string);
        assertEquals("BC0101A1", line.getName());
        assertEquals(TreatmentType.ARM, line.getTreatmentType());
        assertEquals(50, line.getTreatmentLevel());
        assertTrue(new Vec3(0.08e-2, -1.45e-2, -0.46e-2).equals(
                line.getMagnetization(), 1e-10));
        assertEquals(107, line.getAzimuth());
        assertEquals(88, line.getDip());
        assertEquals(11, line.getFoliationAzimuth());
        assertEquals(12, line.getFoliationDip());
        assertEquals(13, line.getLineationTrend());
        assertEquals(14, line.getLineationPlunge());
        assertEquals(new OrientationParameters(AzimuthParameter.A12,
                DipParameter.D0, AzimuthParameter.A12, DipParameter.D0),
                line.getOrientationParameters());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownTreatmentCode() {
        // Using "X" as a treatment code here.
        final String string =
                "BC0101A1  X50       0.08 -1.45 -0.46  -2"
                + " 107  88  11  12  13  14 12  0 12  0   1";
        Jr6DataLine.read(string);
    }

    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLineFormat() {
        Jr6DataLine.read("This string doesn't match the required format.");
    }
    

    
}
