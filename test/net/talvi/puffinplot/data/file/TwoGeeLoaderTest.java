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
import java.nio.file.Files;
import java.nio.file.Path;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class TwoGeeLoaderTest {

    
    private static final double[][] expected1 = {
        // dec    int    intensity   Run #  TT AF X  AF Y  AF Z
        {247.76,  66.28, 1.1881E-05, 17208, 0,    0,    0,    0},
        {281.47,  71.52, 9.0456E-06, 17209, 1,   50,   50,   50},
        {276.52,  72.24, 6.2841E-06, 17210, 1,  100,  100,  100},
        {271.92,  65.34, 4.3688E-06, 17213, 1,  150,  150,  150},
        {265.57,  62.32, 4.1486E-06, 17214, 1,  200,  200,  200},
        {269.25,  72.97, 2.9370E-06, 17215, 1,  250,  250,  250},
        {213.53,  69.43, 2.6551E-06, 17216, 1,  300,  300,  300},
        {207.01,  49.40, 1.6676E-06, 17217, 1,  400,  400,  400},
        { 17.13,  72.37, 7.5306E-07, 17218, 1,  500,  500,  500},
        {128.47, -36.61, 5.9503E-07, 17219, 1,  600,  600,  600},
        {166.37, -25.55, 3.0570E-07, 17220, 1,  800,  800,  800},
        { 12.15, -27.18, 8.9747E-07, 17221, 1, 1000, 1000, 1000}
    };
    private static final double DELTA = 1e-10;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testWithEmptyFile() throws IOException {
        final File file =
                TestUtils.writeStringToTemporaryFile(
                        "EMPTY.DAT", null, temporaryFolder);
        final TwoGeeLoader loader =
                new TwoGeeLoader(file, TwoGeeLoader.Protocol.NORMAL,
                        new Vec3(1, 1, 1), true);
        assertTrue(loader.treatmentSteps.isEmpty());
        assertEquals(1, loader.getMessages().size());
    }
    
    @Test
    public void testWithMalformedFile() throws IOException {
        final File file = TestUtils.writeStringToTemporaryFile(
                        "MALFORMED.DAT",
                        "This is not a 2G file.\r\nLine 2\r\n",
                        temporaryFolder);
        final TwoGeeLoader loader =
                  new TwoGeeLoader(file, TwoGeeLoader.Protocol.NORMAL,
                        new Vec3(1, 1, 1), true);
        /*
         * At present, TwoGeeLoader makes a best effort even in this hopeless
         * case: the second line is read in as a treatment step, but since
         * none of the data is interpretable, it is initialized with default
         * values.
         */
        assertEquals(1, loader.getTreatmentSteps().size());
        assertTrue(loader.getMessages().isEmpty());
    }

    @Test
    public void testWithSg12_7() throws IOException {
        final String filename = "SG12-7.DAT";
        final Path filePath
                = temporaryFolder.getRoot().toPath().resolve(filename);
        Files.copy(
                TestFileLocator.class.getResourceAsStream("twogee/" + filename),
                filePath);
        for (boolean polar: new boolean[] {false, true}) {
            final TwoGeeLoader loader = new TwoGeeLoader(filePath.toFile(),
                    TwoGeeLoader.Protocol.NORMAL, new Vec3(1, 1, 1), polar);
            for (int i = 0; i < loader.getTreatmentSteps().size(); i++) {
                final double[] expected = expected1[i];
                final TreatmentStep step = loader.getTreatmentSteps().get(i);
                checkVariableFields(expected, step);
                checkConstantFields(step);
            }
        }
    }
    
    private static void checkConstantFields(TreatmentStep step) {
        assertEquals(8.02, step.getVolume(), DELTA);
        /*
         * TwoGeeLoader doesn't read the depth for discrete samples; the slot
         * number is added to the specified depth by the 2G software, so the
         * value in the output file is in any case unreliable.
         */
        assertNull(step.getDepth());
        assertEquals(0, step.getSampAz(), DELTA);
        assertEquals(90, step.getSampDip(), DELTA);
        assertEquals(0, step.getFormAz(), DELTA);
        assertEquals(0, step.getFormDip(), DELTA);
        assertEquals(0, step.getMagDev(), DELTA);
        assertTrue(Double.isNaN(step.getMagSus()));
        assertTrue(Double.isNaN(step.getArmField()));
        assertTrue(Double.isNaN(step.getIrmField()));
        assertEquals(MeasurementType.DISCRETE, step.getMeasurementType());
    }
    
    private static void checkVariableFields(double[] expected,
            TreatmentStep step) {
        assertEquals(expected[0], step.getMoment().getDecDeg(), 0.005);
        assertEquals(expected[1], step.getMoment().getIncDeg(), 0.005);
        assertEquals(expected[2] * 1000, step.getMoment().mag(), 5e-5);
        
        /*
         * Due to a bug in the 2G software, (position - 1) is added to the run
         * number stored in the 2G file. The "expected" value in the array is
         * taken directly for the 2G file, so we check the loaded run number not
         * against the original 2G value but against a corrected value. For this
         * sample, the position is 2 so the expected true run number is
         * one less than the "run number" in the 2G file.
         */
        assertEquals(expected[3] - 1, step.getRunNumber(), 0);
        
        assertEquals(new TreatmentType[]
            {TreatmentType.NONE, TreatmentType.DEGAUSS_XYZ}[(int) expected[4]],
                step.getTreatmentType());
        
        /*
         * Divide values in file by 10000 to convert from oersted to tesla.
         */
        assertEquals(expected[5] / 10000, step.getAfX(), DELTA);
        assertEquals(expected[5] / 10000, step.getAfY(), DELTA);
        assertEquals(expected[5] / 10000, step.getAfZ(), DELTA);
    }
    
}