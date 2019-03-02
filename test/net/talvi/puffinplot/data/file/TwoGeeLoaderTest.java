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
import net.talvi.puffinplot.data.ArmAxis;
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

    private static final double[][] expected_sg12_7 = {
        // dec    int    intensity   Run #  TT AF X  AF Y  AF Z
        {247.76, 66.28, 1.1881E-05, 17208, 0, 0, 0, 0},
        {281.47, 71.52, 9.0456E-06, 17209, 1, 50, 50, 50},
        {276.52, 72.24, 6.2841E-06, 17210, 1, 100, 100, 100},
        {271.92, 65.34, 4.3688E-06, 17213, 1, 150, 150, 150},
        {265.57, 62.32, 4.1486E-06, 17214, 1, 200, 200, 200},
        {269.25, 72.97, 2.9370E-06, 17215, 1, 250, 250, 250},
        {213.53, 69.43, 2.6551E-06, 17216, 1, 300, 300, 300},
        {207.01, 49.40, 1.6676E-06, 17217, 1, 400, 400, 400},
        {17.13, 72.37, 7.5306E-07, 17218, 1, 500, 500, 500},
        {128.47, -36.61, 5.9503E-07, 17219, 1, 600, 600, 600},
        {166.37, -25.55, 3.0570E-07, 17220, 1, 800, 800, 800},
        {12.15, -27.18, 8.9747E-07, 17221, 1, 1000, 1000, 1000}
    };
    
    @Test
    public void testWithSg12_7() throws IOException {
        final Path filePath = copyFile("SG12-7.DAT");
        for (boolean polar: new boolean[] {false, true}) {
            final TwoGeeLoader loader = new TwoGeeLoader(filePath.toFile(),
                    TwoGeeLoader.Protocol.NORMAL, new Vec3(1, 1, 1), polar);
            for (int i = 0; i < loader.getTreatmentSteps().size(); i++) {
                final double[] expected = expected_sg12_7[i];
                final TreatmentStep step = loader.getTreatmentSteps().get(i);
                checkVariableFieldsSg12_7(expected, step);
                checkConstantFieldsSg12_7(step);
            }
        }
    }
    
    private static void checkConstantFieldsSg12_7(TreatmentStep step) {
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

    private static void checkVariableFieldsSg12_7(double[] expected,
            TreatmentStep step) {
        assertEquals(expected[0], step.getMoment().getDecDeg(), 0.005);
        assertEquals(expected[1], step.getMoment().getIncDeg(), 0.005);
        assertEquals(expected[2] * 1000, step.getMoment().mag(), 5e-5);

        /*
         * Due to a bug in the 2G software, (position - 1) is added to the run
         * number stored in the 2G file. The "expected" value in the array is
         * taken directly for the 2G file, so we check the loaded run number not
         * against the original 2G value but against a corrected value. For this
         * sample, the position is 2 so the expected true run number is one less
         * than the "run number" in the 2G file.
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
    
    private static final double[][] expected_fq0101_1 = {
        {20, 2.4097E-05, 9.4826E-06, 5.8278E-06, 6.0466},
        {40, 0.00011177, 1.1972E-05, -4.4373E-05, 7.0455},
        {64, 6.9455E-05, 4.4971E-06, -4.0854E-06, 7.0000},
        {102, -1.9346E-06, 2.3703E-06, -9.1195E-06, 7.0000},
        {123, -1.8121E-05, -8.0666E-06, 8.4264E-06, 7.0454}
    };
    
    @Test
    public void testThermal1posMagSus() throws IOException {
        final Path filePath = copyFile("FQ0101.1.DAT");
        final TwoGeeLoader loader = new TwoGeeLoader(filePath.toFile(),
                TwoGeeLoader.Protocol.NORMAL, new Vec3(4.628, -4.404, -6.280),
                false);
        for (int i = 0; i < loader.getTreatmentSteps().size(); i++) {
            final double[] expected = expected_fq0101_1[i];
            final TreatmentStep step = loader.getTreatmentSteps().get(i);
            checkVariableThermalFields(expected, step);
            checkConstantThermalFields(step, 10.0, 132.0, 19.0);
        }        
    }

    private static final double[][] expected_ccb0101_1 = {
        {25, 7.695436893203883E-05, -3.95378640776699E-06, -5.047961165048542E-06, 2},
        {50, 7.747320388349515E-05, -6.462135922330099E-07, -3.133E-06, 0.053433},
        {75, 7.668330097087379E-05, 2.8543689320388734E-08, -3.559436893203883E-06, 0},
        {100, 6.634805825242718E-05, -5.3432038834951455E-06, -5.413592233009709E-06, 0.054901},
        {125, 5.792815533980582E-05, -4.632135922330097E-06, -3.426116504854368E-06, 1},
        {150, 5.347029126213592E-05, -4.030776699029127E-06, -4.825320388349514E-06, 0.94635},
        {175, 5.378844660194174E-05, -3.031359223300971E-06, -4.595865048543689E-06, 1},
        {200, 4.8939611650485435E-05, -6.436407766990291E-06, -4.3798446601941745E-06, 1},
        {225, 4.438951456310679E-05, -7.4632038834951456E-06, -2.2870873786407764E-06, 1},
        {250, 4.4476407766990284E-05, -6.603980582524272E-06, -1.3360194174757276E-06, 1.0591},
        {275, 4.680009708737864E-05, -6.856019417475727E-06, 6.961165048543688E-07, 0.10781},
        {300, -9.3126213592233E-07, -9.665631067961164E-06, 7.477378640776699E-06, 3},
        {325, -1.1872038834951455E-05, -6.360485436893204E-06, -1.1988349514563107E-06, 2.0572},
        {350, -1.2436310679611648E-05, -1.2646407766990291E-05, 4.349417475728155E-06, 1.11},
        {375, -8.048640776699028E-06, -9.367961165048543E-06, -2.533398058252427E-06, 3.055},
        {400, 1.0062038834951456E-05, 1.2381553398058254E-06, -4.6175728155339795E-06, 0}
    };
    
    /**
     * Test with a file using the TRAY_NORMAL protocol
     * 
     * @throws IOException 
     */
    @Test
    public void testTrayNormal() throws IOException {
        final Path filePath = copyFile("CCB0101.1.DAT");
        final TwoGeeLoader loader = new TwoGeeLoader(filePath.toFile(),
                TwoGeeLoader.Protocol.TRAY_NORMAL,
                new Vec3(4.628, -4.404, -6.280), false);
        for (int i = 0; i < loader.getTreatmentSteps().size(); i++) {
            final double[] expected = expected_ccb0101_1[i];
            final TreatmentStep step = loader.getTreatmentSteps().get(i);
            checkVariableThermalFields(expected, step);
            checkConstantThermalFields(step, 10.3, 255, 3);
        }        
    }

    private void checkVariableThermalFields(double[] expected,
            TreatmentStep step) {
        assertEquals(expected[0], step.getTemperature(), DELTA);
        assertEquals(expected[1], step.getMoment().x, DELTA);
        assertEquals(expected[2], step.getMoment().y, DELTA);
        assertEquals(expected[3], step.getMoment().z, DELTA);
        assertEquals(expected[4], step.getMagSus(), DELTA);
    }

    private void checkConstantThermalFields(TreatmentStep step,
            double volume, double sampAz, double sampDip) {
        assertEquals(MeasurementType.DISCRETE, step.getMeasurementType());
        assertEquals(volume, step.getVolume(), DELTA);
        assertEquals(4, step.getArea(), DELTA);
        assertEquals(sampAz, step.getSampAz(), DELTA);
        assertEquals(sampDip, step.getSampDip(), DELTA);
        assertEquals(0, step.getFormAz(), DELTA);
        assertEquals(0, step.getFormDip(), DELTA);
        assertEquals(0, step.getMagDev(), DELTA);
        assertEquals(TreatmentType.THERMAL, step.getTreatmentType());
        assertEquals(0, step.getAfX(), DELTA);
        assertEquals(0, step.getAfY(), DELTA);
        assertEquals(0, step.getAfZ(), DELTA);
        assertTrue(Double.isNaN(step.getIrmField()));
        assertTrue(Double.isNaN(step.getArmField()));
        assertEquals(ArmAxis.NONE, step.getArmAxis());
    }

    private Path copyFile(String filename) throws IOException {
        final Path filePath
                = temporaryFolder.getRoot().toPath().resolve(filename);
        Files.copy(
                TestFileLocator.class.getResourceAsStream("twogee/" + filename),
                filePath);
        return filePath;
    }
    
}