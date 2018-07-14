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

import org.junit.Test;
import static org.junit.Assert.*;
import static net.talvi.puffinplot.data.file.OrientationParameters.AzimuthParameter;
import static net.talvi.puffinplot.data.file.OrientationParameters.DipParameter;


/**
 *
 * @author pont
 */
public class OrientationParametersTest {
    
    @Test
    public void testRead() {
        final OrientationParameters actual =
                OrientationParameters.read("3", "0", "3", "0");
        final OrientationParameters expected =
                new OrientationParameters(AzimuthParameter.A3, DipParameter.D0,
                        AzimuthParameter.A3, DipParameter.D0);
        assertEquals(expected, actual);
    } 

    @Test
    public void testEquals() {
        final OrientationParameters same0 =
                new OrientationParameters(AzimuthParameter.A3, DipParameter.D0,
                        AzimuthParameter.A3, DipParameter.D0);
        final OrientationParameters same1 =
                new OrientationParameters(AzimuthParameter.A3, DipParameter.D0,
                        AzimuthParameter.A3, DipParameter.D0);
        final OrientationParameters notSame =
                new OrientationParameters(AzimuthParameter.A9, DipParameter.D0,
                        AzimuthParameter.A3, DipParameter.D0);
        assertEquals(same0, same0);
        assertEquals(same1, same1);
        assertEquals(notSame, notSame);

        assertNotEquals(same0, notSame);
        assertNotEquals(same1, notSame);
        assertNotEquals(notSame, same0);
        assertNotEquals(notSame, same1);
        
        assertNotEquals("x", same0);
        assertNotEquals(same0, "x");
    }
    
}
