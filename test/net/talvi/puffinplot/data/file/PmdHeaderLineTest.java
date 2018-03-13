/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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

public class PmdHeaderLineTest {
    
    @Test
    public void testRead() {
        assertEquals(new PmdHeaderLine("ss0208c", 49.4, 52.5, 0.0, 0.0, 11e-6),
                PmdHeaderLine.read("ss0208c   a= 49.4   b= 52.5   s=  0.0   "
                        + "d=  0.0   v=11.0E-6m3  06/18/2003 12:00"));
        assertEquals(new PmdHeaderLine("26a", 355.0, 77.0, 0.0, 0.0, 11e-6),
                PmdHeaderLine.read("     26a  a=355.0   b= 77.0   s=  0.0   "
                        + "d=  0.0   v=11.0E-6m3   05-27-1994 09:08"));
        assertEquals(new PmdHeaderLine("CO-17E", 296.0, 57.0, 90.0, 0.0, 5.0e-6),
                PmdHeaderLine.read("CO-17E    α=296.0   β= 57.0   s= 90.0   "
                        + "d=  0.0   v= 5.0E-6m3  12-12-2017 14:01"));
        assertEquals(new PmdHeaderLine("42-297", 312.0, 51.0, 48.0, 81.0, 11.0e-6),
                PmdHeaderLine.read("42-297    a=312.0   b= 51.0   s= 48.0   "
                        + "d= 81.0   v=11.0E-6m3  04-16-1992 14:45"));
        assertEquals(new PmdHeaderLine("ss0107a", 9.4, 33.5, 0.0, 0.0, 11.0e-6),
                PmdHeaderLine.read("ss0107a   a=  9.4   b= 33.5   s=  0.0   "
                        + "d=  0.0   v=11.0E-6m3  06/17/2003 12:00"));
    }
    
}
