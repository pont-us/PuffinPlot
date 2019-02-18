/* This file is part of PuffinPlot, a program for palaeomagnetic
 * treatmentSteps plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class ZplotLoaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static final double delta = 1e-10;
    
    private final double[][] expectedAf = {    
        {1.51,   0, 199.8373622,  5.313179896, 7.97E-08},
        {1.51, 100, 220.2829700,  13.44816356, 3.43E-08},
        {1.51, 150, 219.8045534,  16.19181213, 3.25E-08},
        {1.51, 250, 217.9923572,  10.88707946, 3.13E-08},
        {1.51, 300, 221.7152113,  11.29587495, 3.22E-08},
        {1.51, 350, 228.5559766,  14.99193715, 2.53E-08},
        {1.51, 450, 229.8346015,  27.91186646, 2.69E-08},
        {1.51, 500, 227.7824361,  56.96929353, 4.05E-08},
        {1.52,   0, 201.4223463,  2.852784108, 6.98E-08},
        {1.52, 100, 226.3487989,  9.556564530, 2.48E-08},
        {1.52, 150, 221.4408018,  13.79930381, 2.53E-08},
        {1.52, 250, 220.6505329,  5.773577779, 2.55E-08},
        {1.52, 300, 224.0610622,  6.451081410, 2.91E-08},
        {1.52, 350, 235.3750945,  9.106624253, 1.60E-08},
        {1.52, 450, 229.8284897,  32.76863625, 2.24E-08},
        {1.52, 500, 226.7045051,  61.01874590, 3.63E-08},
        {1.53,   0, 201.9480927, -4.039223460, 4.28E-08},
        {1.53, 100, 307.3653136, -15.80320933, 1.04E-08},
        {1.53, 150, 263.6575240, -1.113388374, 9.43E-09},
        {1.53, 250, 249.3747844, -12.34185816, 1.25E-08},
        {1.53, 300, 240.4086634, -4.782347680, 1.87E-08},
        {1.53, 350, 320.9263622, -35.33284654, 6.40E-09},
        {1.53, 450, 235.2907620,  46.88039254, 1.23E-08},
        {1.53, 500, 227.2171144,  73.80122660, 2.78E-08}
    };
    
    @Test
    public void testLoadContinuousAf() throws IOException {
        final File testFile = copyTestData("zplot-af-1.txt");
        final ZplotLoader loader = new ZplotLoader(testFile);
        final List<TreatmentStep> steps = loader.getTreatmentSteps();
        assertEquals(expectedAf.length, steps.size());
        for (int i=0; i<expectedAf.length; i++) {
            final double[] values = expectedAf[i];
            final TreatmentStep step = steps.get(i);
            assertEquals(TreatmentType.DEGAUSS_XYZ, step.getTreatmentType());
            assertEquals(MeasurementType.CONTINUOUS, step.getMeasurementType());
            assertEquals(values[0], Double.parseDouble(step.getDepth()), delta);
            assertEquals(values[1]/10000, step.getAfX(), delta);
            assertEquals(values[2], step.getMoment().getDecDeg(), delta);
            assertEquals(values[3], step.getMoment().getIncDeg(), delta);
            assertEquals(values[4]*1000, step.getMoment().mag(), delta);
        }
    }
    
    private final String[] expectedSampleNames = {
        "E5249.1", "E5249.1", "E5249.1", "E5249.1", "E5249.1", "E5249.1",
        "E5249.1", "E5249.1", "E5249.1", "E5249.1", "E5249.2", "E5249.2",
        "E5249.2", "E5249.2", "E5249.2", "E5249.2", "E5249.2", "E5249.2",
        "E5249.2", "E5249.2", "E5250.1", "E5250.1", "E5250.1", "E5250.1",
        "E5250.1", "E5250.1", "E5250.1", "E5250.1", "E5250.1", "E5250.1"
    };
    
    private final double[][] expectedDiscreteThermal = {
        { 20, 318.29, -77.08, 5.22E-07},
        { 47, 356.88, -57.69, 5.28E-07},
        {102,   1.01, -56.56, 4.82E-07},
        {149, 335.94, -75.80, 3.88E-07},
        {149,  40.32, -79.41, 3.24E-07},
        {251, 352.75, -57.67, 2.92E-07},
        {301,   0.23, -57.60, 2.49E-07},
        {353, 357.33, -61.52, 1.67E-07},
        {402,  30.79, -54.96, 7.37E-08},
        {450,  18.48, -39.01, 8.01E-08},
        { 20, 349.66, -76.48, 4.57E-07},
        { 47,  10.98, -53.04, 4.82E-07},
        {102,  15.96, -52.96, 4.42E-07},
        {149,  20.44, -73.46, 3.34E-07},
        {149,  69.28, -68.05, 2.99E-07},
        {251,  19.60, -59.01, 2.58E-07},
        {301,  18.38, -58.16, 2.26E-07},
        {353,  25.76, -57.04, 1.43E-07},
        {402,  64.82, -58.85, 3.56E-08},
        {450, 318.30, -71.46, 4.60E-08},
        { 20, 311.67, -69.59, 4.18E-07},
        { 47, 353.67, -51.36, 4.30E-07},
        {102, 353.43, -53.25, 3.73E-07},
        {149, 322.37, -68.52, 2.83E-07},
        {149,  23.28, -76.66, 2.25E-07},
        {251, 342.79, -56.89, 2.16E-07},
        {301, 345.50, -55.99, 1.86E-07},
        {353, 348.90, -59.59, 1.34E-07},
        {402, 337.73, -50.89, 6.68E-08},
        {450,   6.25, -21.35, 8.23E-08}
    };
    
    @Test
    public void testLoadDiscreteThermal() throws IOException {
        final File testFile = copyTestData("zplot-thermal-1.txt");
        final ZplotLoader loader = new ZplotLoader(testFile);
        final List<TreatmentStep> steps = loader.getTreatmentSteps();
        assertEquals(expectedDiscreteThermal.length, steps.size());
        for (int i=0; i<expectedDiscreteThermal.length; i++) {
            final double[] values = expectedDiscreteThermal[i];
            final TreatmentStep step = steps.get(i);
            assertEquals(TreatmentType.THERMAL, step.getTreatmentType());
            assertEquals(MeasurementType.DISCRETE, step.getMeasurementType());
            assertEquals(expectedSampleNames[i], step.getIdOrDepth());
            assertEquals(values[0], step.getTemperature(), delta);
            assertEquals(values[1], step.getMoment().getDecDeg(), delta);
            assertEquals(values[2], step.getMoment().getIncDeg(), delta);
            assertEquals(values[3]*1000, step.getMoment().mag(), delta);
        }
    }
    
    @Test
    public void testEmptyFile() throws IOException {
        final File file = TestUtils.writeStringToTemporaryFile("empty.txt", "",
                temporaryFolder);
        final ZplotLoader loader = new ZplotLoader(file);
        assertTrue(loader.treatmentSteps.isEmpty());
        assertEquals(1, loader.messages.size());
    }

    @Test
    public void testShortInvalidFile() throws IOException {
        final File file = TestUtils.writeStringToTemporaryFile(
                "invalid.txt",
                "This is not a valid Zplot file.",
                temporaryFolder);
        final ZplotLoader loader = new ZplotLoader(file);
        assertTrue(loader.treatmentSteps.isEmpty());
        assertEquals(1, loader.messages.size());
    }
    
    @Test
    public void testLongInvalidFile() throws IOException {
        final File file = TestUtils.writeStringToTemporaryFile(
                "invalid.txt",
                "This\nis\nnot\na\nvalid\nZplot\nfile.",
                temporaryFolder);
        final ZplotLoader loader = new ZplotLoader(file);
        assertTrue(loader.treatmentSteps.isEmpty());
        assertEquals(1, loader.messages.size());
    }
    
    @Test
    public void testFileWithNoHeaderLine() throws IOException {
        final File file = TestUtils.writeStringToTemporaryFile(
                "invalid.txt",
                "File Name: jam\n" +
                        "The first line was valid, but this is" +
                        "nevertheless not a valid Zplot file.",
                temporaryFolder);
        final ZplotLoader loader = new ZplotLoader(file);
        assertTrue(loader.treatmentSteps.isEmpty());
        assertEquals(1, loader.messages.size());
    }
    
    @Test
    public void testNonexistentFile() {
        final File file = temporaryFolder.getRoot().toPath().
                resolve("nonexistent.txt").toFile();
        final ZplotLoader loader = new ZplotLoader(file);
        assertTrue(loader.treatmentSteps.isEmpty());
        assertEquals(1, loader.messages.size());        
    }
    
    private final File copyTestData(String filename) throws IOException {
                final InputStream testInputStream =
                TestFileLocator.class.getResourceAsStream(filename);
        final Path inputPath = temporaryFolder.getRoot().toPath().
                resolve(filename);
        Files.copy(testInputStream, inputPath);
        return inputPath.toFile();
    }
    
}
