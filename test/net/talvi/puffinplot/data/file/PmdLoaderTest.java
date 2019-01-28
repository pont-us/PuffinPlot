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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test loading of text-based PMD (Enkin) format files.
 * 
 */
public class PmdLoaderTest {
    
    private final static TreatType[] TREATMENT_TYPES =
        {TreatType.NONE, TreatType.DEGAUSS_XYZ, TreatType.THERMAL};
    private final static String[][] FILENAMES_AND_DATA = {
        {"CO-17E.PMD", "CO-17E"},
        {"IPG_AF.PMD", "26a"},
        {"IPG_Thermal.PMD", "42-297"},
        {"ss0202a.pmd", "ss0202a"},
        {"BC0101A1.pmd", "BC0101A1"}
    };
    private final static double[][][] NUMERICAL_DATA = {
        {{296.0, 57.0, 90.0,  0.0, 5.0E-6}, // CO-17E.PMD
            {0, 0, 3.47E-08, -9.52E-08, 8.57E-08},
            {1, 5, 2.85E-08, -6.82E-08, 7.35E-08},
            {1, 10, 3.14E-08, -5.70E-08, 6.43E-08},
            {1, 15, 2.42E-08, -4.20E-08, 4.87E-08},
            {1, 20, 1.75E-08, -3.27E-08, 3.67E-08},
            {1, 30, 1.23E-08, -2.40E-08, 2.67E-08},
            {1, 40, 1.27E-08, -1.69E-08, 2.19E-08},
            {1, 50, 6.27E-09, -1.33E-08, 1.68E-08},
            {1, 60, 3.62E-09, -6.55E-09, 8.19E-09},
            {1, 70, 2.17E-09, -3.73E-09, 7.76E-09},
            {1, 80, 3.49E-09, -5.71E-09, 7.34E-09},
            {1, 90, 2.53E-09, -5.26E-09, 6.20E-09},
            {1, 100, 1.75E-09, -5.21E-09, 5.46E-09}},
        {{355.0, 77.0, 0.0, 0.0, 11e-6}, // IPG_AF.PMD
            {0, 0,   2.45E-05, -3.13E-05, -2.01E-05},
            {1, 4,  2.37E-05, -2.97E-05, -1.97E-05},
            {1, 8,  2.00E-05, -2.45E-05, -1.67E-05},
            {1, 12,  1.53E-05, -1.85E-05, -1.25E-05},
            {1, 16,  1.19E-05, -1.46E-05, -9.72E-06},
            {1, 20,  9.49E-06, -1.18E-05, -7.71E-06},
            {1, 25,  7.63E-06, -9.64E-06, -6.22E-06},
            {1, 30,  6.17E-06, -7.94E-06, -5.11E-06},
            {1, 40,  4.76E-06, -6.15E-06, -3.90E-06},
            {1, 50,  3.49E-06, -4.56E-06, -2.91E-06},
            {1, 60,  2.54E-06, -3.32E-06, -2.32E-06},
            {1, 80,  1.68E-06, -2.09E-06, -1.72E-06},
            {1, 100,  1.05E-06, -1.21E-06, -1.27E-06}},
        {{312.0, 51.0, 48.0, 81.0, 11.0E-6}, // IPG_Thermal.PMD
            {0, 0,  -2.02E-07,  1.86E-07,  4.50E-07},
            {2, 120, -1.59E-07,  1.35E-07,  3.49E-07},
            {2, 200, -8.67E-08,  8.24E-08,  2.17E-07},
            {2, 250, -7.35E-08,  5.68E-08,  1.72E-07},
            {2, 300, -5.77E-08,  4.39E-08,  1.30E-07},
            {2, 350, -4.91E-08,  4.58E-08,  1.08E-07},
            {2, 400, -4.27E-08,  4.06E-08,  9.19E-08},
            {2, 450, -3.88E-08,  2.07E-08,  6.99E-08},
            {2, 500, -3.92E-08,  1.24E-08,  6.33E-08},
            {2, 530, -4.02E-08, -1.49E-09,  4.27E-08},
            {2, 560, -2.85E-08,  4.20E-09,  4.36E-08},
            {2, 580, -2.15E-08, -5.73E-09,  1.01E-08},
            {2, 600,  2.08E-09, -1.72E-09,  8.10E-09}},
        {{97.0, 47.0, 0.0, 0.0, 11.0E-6}, // ss0202a.pmd
            {1, 0, -9.54E-06, -5.34E-06, +6.30E-06},
            {1, 6, -9.57E-06, -4.92E-06, +9.76E-06},
            {1, 12, -9.97E-06, -4.59E-06, +1.49E-05},
            {1, 20, -9.58E-06, -4.30E-06, +1.53E-05},
            {1, 30, -8.78E-06, -3.91E-06, +1.43E-05},
            {1, 45, -7.67E-06, -3.42E-06, +1.27E-05},
            {1, 80, -5.77E-06, -2.58E-06, +9.61E-06},
            {1, 190, -2.96E-06, -1.28E-06, +4.98E-06}},
        {{287.0, -88.0, 0.0, 0.0, 11.0E-6},
            {0,   0, 8.80E-09, -1.60E-07, -5.06E-08},
            {2, 100, 9.90E-09, -1.55E-07, -4.95E-08},
            {2, 150, 1.10E-08, -1.52E-07, -4.95E-08},
            {2, 200, 1.21E-08, -1.51E-07, -4.84E-08},
            {2, 250, 1.32E-08, -1.46E-07, -4.84E-08},
            {2, 300, 1.21E-08, -1.39E-07, -4.29E-08},
            {2, 350, 1.21E-08, -1.30E-07, -3.96E-08},
            {2, 400, 1.21E-08, -1.21E-07, -3.41E-08},
            {2, 425, 9.90E-09, -1.14E-07, -3.08E-08},
            {2, 450, 7.92E-09, -8.70E-08, -2.77E-08},
            {2, 475, 5.94E-09, -7.61E-08, -2.42E-08},
            {2, 500,-1.10E-10, -1.18E-08,  1.00E-08},
            {2, 520, 4.29E-09,  6.93E-09, -2.55E-08},
            {2, 540, 5.21E-08,  1.09E-07, -1.14E-08},
            {2, 560,-2.31E-08, -2.23E-08, -4.70E-08},
            {2, 580, 7.15E-09,  6.60E-09,  5.65E-08}},
    };
    
    @Test
    public void testLoad() {
        for (int i=0; i<FILENAMES_AND_DATA.length; i++) {
            testLoadOneFile(FILENAMES_AND_DATA[i], NUMERICAL_DATA[i]);
        }
    }

    private void testLoadOneFile(String[] filenameAndData,
            double[][] numericalData) {
        final InputStream stream =
                TestFileLocator.class.getResourceAsStream(filenameAndData[0]);
        final PmdLoader pmdLoader =
                new PmdLoader(stream, Collections.emptyMap(), filenameAndData[0]);
        final List<TreatmentStep> loadedData = pmdLoader.getData();
        if (!pmdLoader.messages.isEmpty()){
            System.out.println(pmdLoader.messages);
        }
        checkData(numericalData, filenameAndData, loadedData);
    }
    
    private void checkData(double[][] expected, String[] expectedStrings,
            List<TreatmentStep> actual) {
        assertEquals(expected.length-1, actual.size());
        for (int i=0; i<actual.size(); i++) {
            final double[] expVals = expected[i+1];
            final TreatmentStep d = actual.get(i);
            assertEquals(new Vec3(expVals[2], expVals[3], expVals[4]).
                    divideBy(expected[0][4]), d.getMoment());
            assertEquals(expected[0][0], d.getSampAz(), 1e-10);
            assertEquals(90 - expected[0][1], d.getSampDip(), 1e-10);
            assertEquals((expected[0][2] + 90) % 360, d.getFormAz(), 1e-10);
            assertEquals(expected[0][3], d.getFormDip(), 1e-10);
            assertEquals(TREATMENT_TYPES[(int) expVals[0]], d.getTreatType());
            assertEquals(MeasType.DISCRETE, d.getMeasType());
            assertEquals(expectedStrings[1], d.getIdOrDepth());
            switch (d.getTreatType()) {
                case DEGAUSS_XYZ:
                    assertEquals(expVals[1], d.getAfX()*1000, 1e-10);
                    break;
                case THERMAL:
                    assertEquals(expVals[1], d.getTemp(), 1e-10);
                    break;
            }
        }
    }
    
    @Test
    public void testNonExistentFile() {
        /*
         * The filename is a UUID in the root directory, which makes it
         * fairly certain that it doesn't exist.
         */
        final PmdLoader pmdLoader = PmdLoader.readFile(
                new File("/12233e64-2cf3-11e8-9643-2316aaca3cc2"),
                Collections.emptyMap());
        assertEquals(0, pmdLoader.getData().size());
        assertTrue(pmdLoader.getMessages().size() > 0);
    }
    
    @Test
    public void testClosedStream() {
        final InputStream stream = TestFileLocator.class.
                getResourceAsStream(FILENAMES_AND_DATA[0][0]);
        try {
            stream.close();
        } catch (IOException ex) {
            Logger.getLogger(PmdLoaderTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        final PmdLoader pmdLoader =
                new PmdLoader(stream, Collections.emptyMap(), FILENAMES_AND_DATA[0][0]);
        assertTrue(pmdLoader.getMessages().size() > 0);
    }
    
    @Test
    public void testUnknownHeaderFormat() {
        final InputStream inputStream = new ByteArrayInputStream(
                ("\nss0208c   a= 49.4   b= 52.5   s=  0.0   "
                        + "d=  0.0   v=11.0E-6m3  06/18/2003 12:00\n"
                        + "Malformed header line here\n").
                        getBytes(StandardCharsets.US_ASCII));
        final PmdLoader pmdLoader =
                new PmdLoader(inputStream, Collections.emptyMap(), "test");
        assertTrue(pmdLoader.getMessages().size() > 0);
        assertEquals(0, pmdLoader.getData().size());
    }
    
    @Test
    public void testInconsistentData() {
        final String fileContents = "11-02\n" +
                "     26a  a=355.0   b= 77.0   s=  0.0   d=  0.0   v=11.0E-6m3   05-27-1994 09:08\n" +
                "STEP  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   GDEC  GINC  SDEC  SINC a95 \n" +
                "NRM   2.45E-05 -3.13E-05 -2.01E-05  4.05E+00 240.8 -39.6 240.8 -59.6  0.0 1\n";
        final InputStream inputStream = new ByteArrayInputStream(
                fileContents.getBytes(StandardCharsets.US_ASCII));
        final PmdLoader pmdLoader =
                new PmdLoader(inputStream, Collections.emptyMap(), "test");
        assertTrue(pmdLoader.getMessages().size() > 0);
        assertTrue(pmdLoader.getMessages().get(0).toLowerCase().
                contains("inconsistent"));
        assertEquals(1, pmdLoader.getData().size());
    }
    
        
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testReadFile() {
        try {
            final File pmdFile = temporaryFolder.newFile("test.pmd");
            final FileWriter fileWriter = new FileWriter(pmdFile);
            fileWriter.write("JR6 file\n" +
                    "BC0101A1  a=287.0   b=-88.0   s=  0.0   d=  0.0   v=11.0E-6m3\n" +
                    "STEP  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   Dg    Ig    Ds    Is   a95 \n" +
                    "NRM   8.80E-09 -1.60E-07 -5.06E-08  1.52E-02 214.7   2.4   0.0   0.0  1.0 ");
            fileWriter.close();
            PmdLoader.readFile(pmdFile, Collections.emptyMap());
        } catch (IOException ex) {
            Logger.getLogger(Jr6LoaderTest.class.getName()).
                    log(Level.SEVERE, null, ex);
            fail("IO Exception");
        }
    }
    
}
