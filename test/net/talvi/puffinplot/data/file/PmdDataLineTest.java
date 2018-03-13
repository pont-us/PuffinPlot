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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;

public class PmdDataLineTest {
    
    @Test
    public void PmdDataLineTest() {
        final String line = "M040  4.76E-06 -6.15E-06 -3.90E-06  7.91E-01 241.1 -39.4 241.1 -39.4  0.0 1";
        
        final PmdDataLine pdl = PmdDataLine.read(line);
        assertEquals(TreatType.DEGAUSS_XYZ, pdl.treatmentType);
        assertTrue(new Vec3(4.76E-06, -6.15E-06, -3.90E-06).equals(pdl.magnetization));
    }
    
}
