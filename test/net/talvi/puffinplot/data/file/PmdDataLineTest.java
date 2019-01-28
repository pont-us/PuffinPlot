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

import net.talvi.puffinplot.data.TreatmentType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PmdDataLineTest {
    
    @Test
    public void testRead() {
        assertEquals(new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 40, new double[] {
            4.76E-06, -6.15E-06, -3.90E-06,
            7.91E-01, 241.1, -39.4, 241.1, -39.4,  0.0}, "1"),
                PmdDataLine.read("M040  4.76E-06 -6.15E-06 -3.90E-06  "
                        + "7.91E-01 241.1 -39.4 241.1 -39.4  0.0 1"));
        
        assertEquals(new PmdDataLine(TreatmentType.NONE, 0, new double[] {
            -2.02E-07, 1.86E-07, 4.50E-07,
            4.79E-02, 351.9, 56.6, 115.9, 35.5, 1.4}, ""),
                PmdDataLine.read("NRM  -2.02E-07  1.86E-07  4.50E-07  "
                        + "4.79E-02 351.9  56.6 115.9  35.5  1.4 "));
        
        assertEquals(new PmdDataLine(TreatmentType.THERMAL, 580, new double[] {
            -2.15E-08, -5.73E-09,  1.01E-08,
            2.22E-03, 177.5,  70.8, 150.1,  -5.9,  3.8}, "< 50%"),
                PmdDataLine.read("T580 -2.15E-08 -5.73E-09  1.01E-08  "
                        + "2.22E-03 177.5  70.8 150.1  -5.9  3.8 < 50%"));
        
        assertEquals(new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 004, new double[] {
            -6.20E-06, +3.48E-06, +1.17E-05,
            +1.24E+00,  53.0,  62.0,  53.0,  62.0,  0.4}, " cryoSlug"),
                PmdDataLine.read("H004 -6.20E-06 +3.48E-06 +1.17E-05 "
                        + "+1.24E+00  53.0  62.0  53.0  62.0  0.4  cryoSlug"));
        
        assertEquals(new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 5, new double[] {
            2.85E-08, -6.82E-08,  7.35E-08,
            2.08E-02, 254.5,   8.9, 254.5,   8.9,  0.0}, "JR5-2"),
                PmdDataLine.read("5MT   2.85E-08 -6.82E-08  7.35E-08  "
                        + "2.08E-02 254.5   8.9 254.5   8.9  0.0 JR5-2"));
        
        assertEquals(new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 90, new double[] {
            2.53E-09, -5.26E-09,  6.20E-09,
            1.70E-03, 257.4,   8.5, 257.4,   8.5,  0.0}, "JR5-2"),
                PmdDataLine.read("90MT  2.53E-09 -5.26E-09  6.20E-09  "
                        + "1.70E-03 257.4   8.5 257.4   8.5  0.0 JR5-2"));
        
        assertEquals(new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 100, new double[] {
            1.75E-09, -5.21E-09,  5.46E-09,
            1.55E-03, 252.7,  11.2, 252.7,  11.2,  0.0}, "JR5-2"),
                PmdDataLine.read("100M  1.75E-09 -5.21E-09  5.46E-09  "
                        + "1.55E-03 252.7  11.2 252.7  11.2  0.0 JR5-2"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidLineFormat() {
        PmdDataLine.read("This string doesn't match the required format.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNumberFormat() {
        PmdDataLine.read("100M  1.75E-09 -5.21E-09  5.Q6E-09  "
                        + "1.55E-03 252.7  11.2 252.7  11.2  0.0 JR5-2");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTreatmentString() {
        PmdDataLine.read("BEER  1.75E-09 -5.21E-09  5.26E-09  "
                        + "1.55E-03 252.7  11.2 252.7  11.2  0.0 JR5-2");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidTreatmentType() {
        PmdDataLine.read("Q100  1.75E-09 -5.21E-09  5.26E-09  "
                        + "1.55E-03 252.7  11.2 252.7  11.2  0.0 JR5-2");
    }
    
    @Test
    public void testEqualsAndHash() {
        final PmdDataLine pdl0 =
                new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 90, new double[] {
            2.53E-09, -5.26E-09,  6.20E-09,
            1.70E-03, 257.4,   8.5, 257.4,   8.5,  0.0}, "JR5-2");
        final PmdDataLine pdl1 =
                new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 90, new double[] {
            2.53E-09, -5.26E-09,  6.20E-09,
            1.70E-03, 257.4,   8.5, 257.4,   8.5,  0.0}, "JR5-2");
        final PmdDataLine pdl2 =
                new PmdDataLine(TreatmentType.DEGAUSS_XYZ, 40, new double[] {
            4.76E-06, -6.15E-06, -3.90E-06,
            7.91E-01, 241.1, -39.4, 241.1, -39.4,  0.0}, "1");
        assertEquals(pdl0, pdl0);
        assertEquals(pdl0, pdl1);
        assertEquals(pdl1, pdl0);
        assertNotEquals(pdl0, pdl2);
        assertNotEquals(pdl2, pdl0);
        assertEquals(pdl0.hashCode(), pdl1.hashCode());
        assertNotEquals(pdl0, null);
        assertNotEquals(pdl0, this);
    }
}
