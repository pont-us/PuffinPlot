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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class Jr6DataLineTest {
    
    @Test
    public void testRead() {
        
        final String string =
                "BC0101A1  NRM       0.08 -1.45 -0.46  -2"
                + " 107  88   0   0   0   0 12  0 12  0   1";
        
        final Jr6DataLine line = Jr6DataLine.read(string);
        assertEquals("BC0101A1", line.getName());
        assertEquals(TreatType.NONE, line.getTreatmentType());
        
    }
    
}
