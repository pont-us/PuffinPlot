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

import java.io.InputStream;
import java.util.List;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Test;
import static org.junit.Assert.*;

public class Jr6LoaderTest {
    
    private final double[][] expected = {
        {107, 88,  0,   0, 1.523319E-02, 273.2, -17.6, 359.5,  -3.6},
        {107, 88,  1, 100, 1.482801E-02, 273.7, -17.7, 359.4,  -4.1},
        {107, 88,  1, 150, 1.454957E-02, 274.1, -18.0, 359.1,  -4.6},
        {107, 88,  1, 200, 1.443122E-02, 274.6, -17.8, 359.4,  -5.0},
        {107, 88,  1, 250, 1.406023E-02, 275.2, -18.2, 358.9,  -5.5},
        {107, 88,  1, 300, 1.323556E-02, 275.0, -17.1, 360.0,  -5.4},
        {107, 88,  1, 350, 1.238588E-02, 275.3, -16.9,   0.2,  -5.7},
        {107, 88,  1, 400, 1.148129E-02, 275.7, -15.7,   1.5,  -6.0},
        {107, 88,  1, 425, 1.080787E-02, 274.9, -15.0,   2.1,  -5.3},
        {107, 88,  1, 450, 8.332881E-03, 275.2, -17.6, 359.5,  -5.6},
        {107, 88,  1, 475, 7.281346E-03, 274.5, -17.6, 359.5,  -4.9},
        {107, 88,  1, 500, 1.404671E-03, 269.5,  40.4,  57.4,   1.7},
        {107, 88,  1, 520, 2.435446E-03,  58.2, -72.3, 271.7, -11.1},
        {107, 88,  1, 540, 1.107031E-02,  64.5,  -5.4, 202.0, -25.5},
        {107, 88,  1, 560, 5.173374E-03, 224.0, -55.6, 312.1,  22.1},
        {107, 88,  1, 580, 5.215563E-03,  42.7,  80.2, 113.6,  -5.2},
    };
    
    @Test
    public void testConstructor() {
        
        final String filename = "BC-Mersin (TDemag-580).jr6";
        
        final InputStream stream =
                TestFileLocator.class.getResourceAsStream(filename);
        
        final Jr6Loader jr6loader = new Jr6Loader(stream, filename);
        final List<Datum> loadedData = jr6loader.getData();
        assertFalse(loadedData.isEmpty());
        assertEquals(290, loadedData.size());
        assertTrue(loadedData.stream().
                allMatch(d -> d.getMeasType() == MeasType.DISCRETE));

        final Suite suite = new Suite("test");
        for (Datum d: loadedData) {
            suite.addDatum(d);
        }
        
        final Sample sample = suite.getSampleByName("BC0101A1");
        assertNotNull(sample);
        assertEquals(16, sample.getData().size());
        final Correction sampCorr = new Correction(false, false, Correction.Rotation.SAMPLE, false);
        for (int i=0; i<expected.length; i++) {
            final Datum actualDatum = sample.getData().get(i);
            final double[] expectedValues = expected[i];
            assertEquals(expectedValues[5], actualDatum.getMoment().getDecDeg(), 0.1);
            assertEquals(expectedValues[6], actualDatum.getMoment().getIncDeg(), 0.1);
            assertEquals(expectedValues[7], actualDatum.getMoment(sampCorr).getDecDeg(), 0.1);
            assertEquals(expectedValues[8], actualDatum.getMoment(sampCorr).getIncDeg(), 0.1);
        }
    }
    
}
