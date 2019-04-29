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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.FileType;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class IapdLoaderTest {
    
    private static final String FILE_TEXT =
            "   TG1H1  299.0 71.0   0.0   0.0   11.20  1    1\r\n"
            +" 0     17145.4700  153.1 50.8  0.4   205.4  34.2  NOT MS\r\n"
            +" 5     17145.8600  146.0 52.5  0.4   199.7  35.0  NOT MS\r\n"
            +" 10    17000.0700  150.8 52.5  0.4   203.2  35.6  NOT MS\r\n"
            +" 15    16522.5600  150.0 52.7  0.3   202.6  35.7  NOT MS\r\n"
            +" 20    14724.4700  152.5 52.3  0.2   204.5  35.6  NOT MS\r\n"
            +" 30    14646.3500  148.7 52.8  0.2   201.6  35.6  NOT MS\r\n"
            +" 40    13529.9500  155.4 51.5  0.2   206.9  35.2  NOT MS\r\n"
            +" 50    12225.2500  153.4 52.7  0.1   205.1  36.1  NOT MS\r\n"
            +" 60    10728.6800  151.6 52.2  0.2   203.9  35.4  NOT MS\r\n"
            +" 70    9052.7750   151.7 52.2  0.2   204.0  35.4  NOT MS\r\n"
            +" 80    7389.7380   152.2 51.6  0.0   204.5  34.9  NOT MS\r\n"
            +" 90    5844.2110   152.4 51.1  0.0   204.8  34.4  NOT MS\r\n"
            +" 100   5481.4170   151.3 51.7  0.1   203.8  34.9  NOT MS\r\n"
            +" 150   3007.8650   153.7 52.4  0.1   205.4  35.9  NOT MS\r\n"
            +" 200   1121.1160   158.2 59.5  0.2   206.2  43.4  NOT MS\r\n"
            +" 500   725.0541    153.7 52.7  0.2   205.3  36.2  NOT MS\r\n";
    
    private static final String MINIMAL_FILE =
    "TG1H1  299.0 71.0   0.0   0.0   11.20  1    1\r\n" +
                " 0     17145.4700  153.1 50.8  9.9   205.4  34.2  NOT MS\r\n";
    
    private static final double[][] FILE_DATA = {
        {0,     17145.4700,  153.1,50.8,  0.4,   205.4,  34.2},
        {5,     17145.8600,  146.0,52.5,  0.4,   199.7,  35.0},
        {10,    17000.0700,  150.8,52.5,  0.4,   203.2,  35.6},
        {15,    16522.5600,  150.0,52.7,  0.3,   202.6,  35.7},
        {20,    14724.4700,  152.5,52.3,  0.2,   204.5,  35.6},
        {30,    14646.3500,  148.7,52.8,  0.2,   201.6,  35.6},
        {40,    13529.9500,  155.4,51.5,  0.2,   206.9,  35.2},
        {50,    12225.2500,  153.4,52.7,  0.1,   205.1,  36.1},
        {60,    10728.6800,  151.6,52.2,  0.2,   203.9,  35.4},
        {70,    9052.7750,   151.7,52.2,  0.2,   204.0,  35.4},
        {80,    7389.7380,   152.2,51.6,  0.0,   204.5,  34.9},
        {90,    5844.2110,   152.4,51.1,  0.0,   204.8,  34.4},
        {100,   5481.4170,   151.3,51.7,  0.1,   203.8,  34.9},
        {150,   3007.8650,   153.7,52.4,  0.1,   205.4,  35.9},
        {200,   1121.1160,   158.2,59.5,  0.2,   206.2,  43.4},
        {500,   725.0541,    153.7,52.7,  0.2,   205.3,  36.2}};
    
    private Map<Object, Object> options = new HashMap<>();
    private Suite suite = new Suite(getClass().getSimpleName());;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testWithMissingFile() throws IOException {
        final IapdLoader loader = new IapdLoader();
        final LoadedData loadedData = loader.readFile(
                temporaryFolder.getRoot().toPath().resolve("nonexistent")
                        .toFile(),
                options);
        // Check for "error reading file" warning.
        assertEquals(1, loadedData.getMessages().size());
        assertTrue(loadedData.getTreatmentSteps().isEmpty());
    }
    
    @Test
    public void testWithEmptyFile() throws IOException {
        final File file = makeFile("EMPTY.DAT", null);
        readFile(file);
        // Check for empty file warning.
        assertEquals(1, suite.getLoadWarnings().size());
        // Check for empty suite.
        assertTrue(suite.getSamples().isEmpty());
    }
    
    @Test
    public void testWithHighA95() throws IOException {
        readFile(makeFile("HIGH_A95.DAT", MINIMAL_FILE));
        assertEquals(1, suite.getLoadWarnings().size());
        assertEquals(1, suite.getSampleByIndex(0).getTreatmentSteps().size());
    }
    
    @Test
    public void testWithMissingSampleName() throws IOException {
        readFile(makeFile("NO_SAMPLE_NAME.DAT",
                " " + FILE_TEXT.substring(FILE_TEXT.indexOf("\n"))));
        // Should have "No sample name" and "Malformed header" warnings
        assertEquals(2, suite.getLoadWarnings().size());
        // The sample should load, albeit with default orientation
        assertEquals(1, suite.getSamples().size());
        assertEquals(16, suite.getSampleByIndex(0).getTreatmentSteps().size());
    }
    
    @Test
    public void testWithMalformedData() throws IOException {
        readFile(makeFile("BAD_DATA.DAT", FILE_TEXT + "malformed line\r\n"));
        assertEquals(1, suite.getLoadWarnings().size());
        /*
         * The malformed line should also be loaded as a treatment step, with
         * default values for the missing data.
         */
        assertEquals(17, suite.getSampleByIndex(0).getTreatmentSteps().size());
    }
    
    @Test
    public void testTreatmentTypes() throws IOException {
        for (TreatmentType tType: 
            // new TreatmentType[] {TreatmentType.DEGAUSS_Z}
            TreatmentType.values()
                ) {
            if (tType == TreatmentType.NONE || tType == TreatmentType.UNKNOWN) {
                continue;
            }
            options.put(TreatmentType.class, tType);
            suite = new Suite(getClass().getSimpleName());;
            readFile(makeFile("TREATMENT_TYPE.DAT", MINIMAL_FILE));
            assertEquals(tType, suite.getSampleByIndex(0).
                    getTreatmentStepByIndex(0).getTreatmentType());
        }
    }

    @Test
    public void testWithValidData() throws IOException {
        options.put(TreatmentType.class, TreatmentType.DEGAUSS_XYZ);
        options.put(MeasurementType.class, MeasurementType.DISCRETE);

        readFile(makeFile("IAPD-loader-test.DAT", FILE_TEXT));
        final Sample sample = suite.getSampleByName("TG1H1");
        final Correction sampCorr =
                new Correction(false, false, Correction.Rotation.SAMPLE, false);
        final Correction noCorr =
                new Correction(false, false, Correction.Rotation.NONE, false);
        assertNotNull(sample);
        for (double[] fields: FILE_DATA) {
            final double demagLevel = fields[0] / 1000;
            final double intensity = fields[1] / 1000;
            
            /*
             * The IAPD file format contains precalculated sample-corrected
             * directions, so we can read these directly and check them against
             * the values that PuffinPlot produces from applying the
             * orientations in the first line to the uncorrected sample
             * directions in fields 5 and 6.
             */
            final double decSamp = fields[2];
            final double incSamp = fields[3];
            
            final double decRaw = fields[5];
            final double incRaw = fields[6];
            
            /*
             * The demagnetization level is checked implicitly: if it's
             * wrong, the correct step won't be fetched and the other fields
             * won't match.
             */
            final TreatmentStep step =
                    sample.getTreatmentStepByLevel(demagLevel);
            assertEquals(intensity, step.getIntensity(), 1e-10);
            assertEquals(decSamp, step.getMoment(sampCorr).getDecDeg(), 0.05);
            assertEquals(incSamp, step.getMoment(sampCorr).getIncDeg(), 0.05);
            assertEquals(decRaw, step.getMoment(noCorr).getDecDeg(), 0.05);
            assertEquals(incRaw, step.getMoment(noCorr).getIncDeg(), 0.05);
        }
        assertEquals(0, suite.getLoadWarnings().size());
    }

    private File makeFile(String name, String contents) throws IOException {
        final File file =
                temporaryFolder.getRoot().toPath().resolve(name).toFile();
        file.createNewFile();
        
        if (contents != null) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(contents);
            }
        }
        
        return file;
    }
    
    private void readFile(File file) throws IOException {
        suite.readFiles(Arrays.asList(new File[] { file }),
                FileType.IAPD, options);
    }

}
