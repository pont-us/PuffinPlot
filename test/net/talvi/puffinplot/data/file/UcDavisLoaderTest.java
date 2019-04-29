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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import net.talvi.puffinplot.TestUtils;

import net.talvi.puffinplot.data.FileType;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class UcDavisLoaderTest {

    private File file;
    private Suite suite;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static final String FILE_TEXT =
            "Position:	X(0)	Y(0)	Z(0)	D(0)	I(0)	J(0)	X(20)	Y(20)	Z(20)	D(20)	I(20)	J(20)	X(25)	Y(25)	Z(25)	D(25)	I(25)	J(25)	X(30)	Y(30)	Z(30)	D(30)	I(30)	J(30)	X(40)	Y(40)	Z(40)	D(40)	I(40)	J(40)	X(50)	Y(50)	Z(50)	D(50)	I(50)	J(50)	X(60)	Y(60)	Z(60)	D(60)	I(60)	J(60)\r\n" +
            "0.00947	1.65E-05	1.40E-05	-1.02E-04	40.4	-71.8	4.16E-03	2.62E-05	1.08E-05	-9.63E-05	22.4	-65.5	4.10E-03	2.37E-05	9.87E-06	-8.83E-05	22.6	-65.8	3.75E-03	2.02E-05	9.09E-06	-7.71E-05	24.2	-66.0	3.27E-03	1.50E-05	6.79E-06	-5.77E-05	24.4	-66.2	2.44E-03	9.09E-06	4.85E-06	-3.90E-05	28.1	-67.7	1.63E-03	6.01E-06	3.20E-06	-2.67E-05	28.0	-68.4	1.11E-03\r\n" +
            "0.01942	3.00E-05	1.70E-05	-1.27E-04	29.5	-67.2	5.35E-03	4.23E-05	1.29E-05	-1.19E-04	16.9	-60.0	5.31E-03	3.83E-05	1.18E-05	-1.09E-04	17.1	-60.4	4.87E-03	3.26E-05	1.10E-05	-9.54E-05	18.6	-60.8	4.23E-03	2.43E-05	8.09E-06	-7.15E-05	18.4	-60.9	3.17E-03	1.50E-05	6.15E-06	-4.84E-05	22.3	-62.5	2.11E-03	9.66E-06	3.76E-06	-3.30E-05	21.3	-64.0	1.42E-03\r\n" +
            "0.02937	4.34E-05	1.86E-05	-1.47E-04	23.2	-63.4	6.34E-03	5.77E-05	1.41E-05	-1.36E-04	13.8	-56.0	6.37E-03	5.23E-05	1.29E-05	-1.25E-04	13.9	-56.3	5.83E-03	4.44E-05	1.21E-05	-1.09E-04	15.3	-56.9	5.06E-03	3.30E-05	8.75E-06	-8.19E-05	14.9	-57.2	3.77E-03	2.06E-05	7.07E-06	-5.56E-05	18.9	-58.7	2.52E-03	1.31E-05	4.00E-06	-3.80E-05	17.0	-60.8	1.69E-03\r\n" +
            "\r\n\r\n"
            ;

    private static final double[][] FILE_DATA =
    //   position trt x         y         z          d     i      j
    //   0        1   2         3         4          5     6      7
    {   {0.00947, 00, 1.65E-05, 1.40E-05, -1.02E-04, 40.4, -71.8, 4.16E-03},
        {0.01942, 00, 3.00E-05, 1.70E-05, -1.27E-04, 29.5, -67.2, 5.35E-03},
        {0.02937, 00, 4.34E-05, 1.86E-05, -1.47E-04, 23.2, -63.4, 6.34E-03},
        {0.00947, 20, 2.62E-05, 1.08E-05, -9.63E-05, 22.4, -65.5, 4.10E-03},
        {0.01942, 20, 4.23E-05, 1.29E-05, -1.19E-04, 16.9, -60.0, 5.31E-03},
        {0.02937, 20, 5.77E-05, 1.41E-05, -1.36E-04, 13.8, -56.0, 6.37E-03},
        {0.00947, 25, 2.37E-05, 9.87E-06, -8.83E-05, 22.6, -65.8, 3.75E-03},
        {0.01942, 25, 3.83E-05, 1.18E-05, -1.09E-04, 17.1, -60.4, 4.87E-03},
        {0.02937, 25, 5.23E-05, 1.29E-05, -1.25E-04, 13.9, -56.3, 5.83E-03},
        {0.00947, 30, 2.02E-05, 9.09E-06, -7.71E-05, 24.2, -66.0, 3.27E-03},
        {0.01942, 30, 3.26E-05, 1.10E-05, -9.54E-05, 18.6, -60.8, 4.23E-03},
        {0.02937, 30, 4.44E-05, 1.21E-05, -1.09E-04, 15.3, -56.9, 5.06E-03},
        {0.00947, 40, 1.50E-05, 6.79E-06, -5.77E-05, 24.4, -66.2, 2.44E-03},
        {0.01942, 40, 2.43E-05, 8.09E-06, -7.15E-05, 18.4, -60.9, 3.17E-03},
        {0.02937, 40, 3.30E-05, 8.75E-06, -8.19E-05, 14.9, -57.2, 3.77E-03},
        {0.00947, 50, 9.09E-06, 4.85E-06, -3.90E-05, 28.1, -67.7, 1.63E-03},
        {0.01942, 50, 1.50E-05, 6.15E-06, -4.84E-05, 22.3, -62.5, 2.11E-03},
        {0.02937, 50, 2.06E-05, 7.07E-06, -5.56E-05, 18.9, -58.7, 2.52E-03},
        {0.00947, 60, 6.01E-06, 3.20E-06, -2.67E-05, 28.0, -68.4, 1.11E-03},
        {0.01942, 60, 9.66E-06, 3.76E-06, -3.30E-05, 21.3, -64.0, 1.42E-03},
        {0.02937, 60, 1.31E-05, 4.00E-06, -3.80E-05, 17.0, -60.8, 1.69E-03}};

    @Test
    public void testWithNonexistentFile() throws IOException {
        final UcDavisLoader loader = new UcDavisLoader();
        final LoadedData loadedData = loader.readFile(
                temporaryFolder.getRoot().toPath().resolve("nonexistent").
                        toFile());
        assertEquals(1, loadedData.getMessages().size());
        assertTrue(loadedData.getTreatmentSteps().isEmpty());
    }
    
    @Test
    public void tetWithEmptyFile() throws IOException {
        final UcDavisLoader loader = new UcDavisLoader();
                final LoadedData loadedData = 
                        loader.readFile(TestUtils.writeStringToTemporaryFile(
                        "empty", null, temporaryFolder));
        assertEquals(1, loadedData.getMessages().size());
        assertTrue(loadedData.getTreatmentSteps().isEmpty());
    }
    
    @Test
    public void testWithValidData() throws IOException {
        file = TestUtils.writeStringToTemporaryFile("uc-davis-test.dat",
                FILE_TEXT, temporaryFolder);
        suite = new Suite(getClass().getSimpleName());
        
        suite.readFiles(Arrays.asList(new File[] { file } ),
                FileType.UCDAVIS, new HashMap<>());
        
        for (int i = 0; i < FILE_DATA.length; i++) {
            final double[] dataline = FILE_DATA[i];
            final String depthString = String.format(Locale.ENGLISH,
                    "%.5f", dataline[0]);
            final double level = dataline[1] / 1000.;
            final TreatmentStep step =
                    suite.getSampleByName(depthString).
                            getTreatmentStepByLevel(level);
            final Vec3 v = step.getMoment();
            /*
             * We don't test the Cartesian data, since it doesn't match the
             * polar data: see comments in UcDavisLoader for details.
             */
            assertEquals(dataline[5], v.getDecDeg(), 1e-9);
            assertEquals(dataline[6], v.getIncDeg(), 1e-9);
            assertEquals(dataline[7], step.getIntensity(), 1e-9);
        }
    }

    @Test
    public void testColumnDefEquals() {
        final UcDavisLoader.ColumnDef x0a =
                UcDavisLoader.ColumnDef.fromHeader("X(0)");
        final UcDavisLoader.ColumnDef x0b =
                UcDavisLoader.ColumnDef.fromHeader("X(0)");
        final UcDavisLoader.ColumnDef y0 =
                UcDavisLoader.ColumnDef.fromHeader("Y(0)");
        final UcDavisLoader.ColumnDef x20 =
                UcDavisLoader.ColumnDef.fromHeader("X(20)");
        
        assertFalse(x0a.equals(null));
        assertFalse(x0a.equals(new Object()));
        assertFalse(x0a.equals(x20));
        assertFalse(x0a.equals(y0));
        assertTrue(x0a.equals(x0a));
        assertTrue(x0a.equals(x0b));
    }
}
