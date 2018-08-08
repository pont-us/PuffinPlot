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

import net.talvi.puffinplot.data.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;
import static net.talvi.puffinplot.data.file.OrientationParameters.AzimuthParameter;
import static net.talvi.puffinplot.data.file.OrientationParameters.DipParameter;
import static java.lang.Math.toRadians;


public class OrientationParametersTest {
    
    @Test
    public void testConvert() {
        final double[][] expected = {
            {12,  0, 12,  0, 359.5,  -3.6, 350.8, -18.8},
            {12,  0, 12, 90, 359.5,  -3.6,   5.6,  36.2},
            {12,  0,  3,  0, 269.5,  -3.6, 275.6,  36.2},
            {12,  0,  3, 90, 269.5,  -3.6, 265.8,  13.4},
            {12,  0,  6,  0, 179.5,  -3.6, 175.8,  13.4},
            {12,  0,  6, 90, 179.5,  -3.6, 188.9, -42.9},
            {12,  0,  9,  0,  89.5,  -3.6,  98.9, -42.9},
            {12,  0,  9, 90,  89.5,  -3.6,  80.8, -18.8},
            {12, 90, 12,  0,  19.5, -17.7, 356.7, -42.4},
            {12, 90, 12, 90,  19.5, -17.7,  18.8,  15.2},
            {12, 90,  3,  0, 289.5, -17.7, 288.8,  15.2},
            {12, 90,  3, 90, 289.5, -17.7, 290.7,  13.7},
            {12, 90,  6,  0, 199.5, -17.7, 200.7,  13.7},
            {12, 90,  6, 90, 199.5, -17.7, 222.4, -44.3},
            {12, 90,  9,  0, 109.5, -17.7, 132.4, -44.3},
            {12, 90,  9, 90, 109.5, -17.7,  86.7, -42.4},
            { 3,  0, 12,  0, 275.9, -74.1, 255.2, -31.8},
            { 3,  0, 12, 90, 275.9, -74.1, 318.8, -36.7},
            { 3,  0,  3,  0, 185.9, -74.1, 228.8, -36.7},
            { 3,  0,  3, 90, 185.9, -74.1, 320.7, -58.9},
            { 3,  0,  6,  0,  95.9, -74.1, 230.7, -58.9},
            { 3,  0,  6, 90,  95.9, -74.1, 358.6, -51.9},
            { 3,  0,  9,  0,   5.9, -74.1, 268.6, -51.9},
            { 3,  0,  9, 90,   5.9, -74.1, 345.2, -31.8},
            { 3, 90, 12,  0, 110.2, -19.6, 134.9, -45.3},
            { 3, 90, 12, 90, 110.2, -19.6,  85.5, -44.2},
            { 3, 90,  3,  0,  20.2, -19.6, 355.5, -44.2},
            { 3, 90,  3, 90,  20.2, -19.6,  18.4,  13.2},
            { 3, 90,  6,  0, 290.2, -19.6, 288.4,  13.2},
            { 3, 90,  6, 90, 290.2, -19.6, 292.2,  12.4},
            { 3, 90,  9,  0, 200.2, -19.6, 202.2,  12.4},
            { 3, 90,  9, 90, 200.2, -19.6, 224.9, -45.3},
            { 6,  0, 12,  0, 214.7,   2.4, 204.4,  38.5},
            { 6,  0, 12, 90, 214.7,   2.4, 220.7, -19.3},
            { 6,  0,  3,  0, 124.7,   2.4, 130.7, -19.3},
            { 6,  0,  3, 90, 124.7,   2.4, 117.1, -34.3},
            { 6,  0,  6,  0,  34.7,   2.4,  27.1, -34.3},
            { 6,  0,  6, 90,  34.7,   2.4,  44.0,  23.0},
            { 6,  0,  9,  0, 304.7,   2.4, 314.0,  23.0},
            { 6,  0,  9, 90, 304.7,   2.4, 294.4,  38.5},
            { 6, 90, 12,  0, 200.8, -17.5, 201.6,  14.5},
            { 6, 90, 12, 90, 200.8, -17.5, 223.5, -43.3},
            { 6, 90,  3,  0, 110.8, -17.5, 133.5, -43.3},
            { 6, 90,  3, 90, 110.8, -17.5,  88.1, -43.0},
            { 6, 90,  6,  0,  20.8, -17.5, 358.1, -43.0},
            { 6, 90,  6, 90,  20.8, -17.5,  20.0,  14.7},
            { 6, 90,  9,  0, 290.8, -17.5, 290.0,  14.7},
            { 6, 90,  9, 90, 290.8, -17.5, 291.6,  14.5},
            { 9,  0, 12,  0, 295.9,  70.2,  38.4,  55.9},
            { 9,  0, 12, 90, 295.9,  70.2, 181.1,  59.0},
            { 9,  0,  3,  0, 205.9,  70.2,  91.1,  59.0},
            { 9,  0,  3, 90, 205.9,  70.2, 173.7,  31.7},
            { 9,  0,  6,  0, 115.9,  70.2,  83.7,  31.7},
            { 9,  0,  6, 90, 115.9,  70.2, 141.4,  29.8},
            { 9,  0,  9,  0,  25.9,  70.2,  51.4,  29.8},
            { 9,  0,  9, 90,  25.9,  70.2, 128.4,  55.9},
            { 9, 90, 12,  0, 290.1, -15.6, 290.4,  16.7},
            { 9, 90, 12, 90, 290.1, -15.6, 290.1,  15.8},
            { 9, 90,  3,  0, 200.1, -15.6, 200.1,  15.8},
            { 9, 90,  3, 90, 200.1, -15.6, 221.1, -42.3},
            { 9, 90,  6,  0, 110.1, -15.6, 131.1, -42.3},
            { 9, 90,  6, 90, 110.1, -15.6,  89.2, -41.2},
            { 9, 90,  9,  0,  20.1, -15.6, 359.2, -41.2},
            { 9, 90,  9, 90,  20.1, -15.6,  20.4,  16.7},
        };
        
        final Vec3 v = new Vec3(0.08e-2, -1.45e-2, -0.46e-2);
        
        for (double[] params: expected) {
            final OrientationParameters op = OrientationParameters.read(
                    (int) params[0], (int) params[1],
                    (int) params[2], (int) params[3]);
            final VectorAndOrientations standardized =
                    op.convertToPuffinPlotConvention(
                            new VectorAndOrientations(v, 107, 88, 66, 44));
            final Vec3 sampleCorrected = standardized.vector.correctSample(
                    toRadians(standardized.sampleAzimuth),
                    toRadians(standardized.sampleDip));
            final Vec3 formationCorrected = sampleCorrected.correctForm(
                    toRadians(standardized.formationAzimuth),
                    toRadians(standardized.formationDip));
            
            /*
             * We don't check the direction in the specimen co-ordinate
             * system, because Remasoft can't generate expected values
             * for this. Ideally we'd have a reference value for the
             * direction of the input vector after conversion from the
             * original orientation parameters to PuffinPlot's orientation
             * parameters. In practice this is hard to do since Remasoft
             * can't explicitly perform this conversion. However, checking
             * the sample and formation corrected directions implicitly checks
             * the original direction. (In principle there might be an
             * error with the sample correction which precisely cancels
             * out an error with the orientation parameter conversion, but
             * this seems vanishingly unlikely.)
             * 
             */
            
            assertEquals(params[4], sampleCorrected.getDecDeg(), 0.1);
            assertEquals(params[5], sampleCorrected.getIncDeg(), 0.1);
            assertEquals(params[6], formationCorrected.getDecDeg(), 0.1);
            assertEquals(params[7], formationCorrected.getIncDeg(), 0.1);
        }
    }
    
    @Test
    public void testRead() {
        final OrientationParameters actual =
                OrientationParameters.read("3", "0", "3", "0");
        final OrientationParameters expected =
                new OrientationParameters(AzimuthParameter.A3, DipParameter.D0,
                        AzimuthParameter.A3, DipParameter.D0);
        assertEquals(expected, actual);
    } 

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownAzimuthParameter() {
        OrientationParameters.AzimuthParameter.read(1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testUnknownDipParameter() {
        OrientationParameters.DipParameter.read(45);
    }
    
    @Test
    public void testEqualsAndHashCode() {
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
        
        assertEquals(same0.hashCode(), same1.hashCode());
    }
    
}
