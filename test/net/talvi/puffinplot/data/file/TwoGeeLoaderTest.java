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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
        for (boolean polar: new boolean[] {false, true}) {
            final TwoGeeLoader loader = new TwoGeeLoader(copyFile("SG12-7.DAT"),
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
        checkThermalFile("FQ0101.1.DAT", expected_fq0101_1,
                TwoGeeLoader.Protocol.NORMAL, 10.0, 132, 19);
    }

    private static final double[][] expected_ccb0101_1_tray_normal = {
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
        checkThermalFile("CCB0101.1.DAT", expected_ccb0101_1_tray_normal,
                TwoGeeLoader.Protocol.TRAY_NORMAL, 10.3, 255, 3);
    }
    
    private static final double[][] expected_fqk0618_1_yflip = {
        {25, -1.0671990291262135E-05, -2.582883495145631E-05, -7.997912621359223E-05, 8.3333},
        {50, -9.774466019417474E-06, -1.9637029126213596E-05, -7.876844660194174E-05, 8.7522},
        {75, -1.191019417475728E-05, -1.4483106796116504E-05, -7.481893203883495E-05, 8.8371},
        {100, -1.2684223300970872E-05, -1.3024805825242716E-05, -6.63883495145631E-05, 8.4195},
        {125, -1.3629126213592232E-05, -8.836456310679612E-06, -6.321699029126213E-05, 9.2538},
        {150, -1.235631067961165E-05, -8.504563106796115E-06, -6.371067961165047E-05, 9},
        {175, -1.033398058252427E-05, -9.605135922330098E-06, -6.256019417475728E-05, 9},
        {200, -1.3329126213592233E-05, -6.488446601941748E-06, -6.223980582524271E-05, 8.6667},
        {225, -1.2083155339805825E-05, -1.798689320388349E-06, -6.20257281553398E-05, 8.085},
        {250, -3.0187864077669897E-06, -5.892718446601931E-07, -5.5415048543689316E-05, 9},
        {275, 1.1278038834951456E-06, 1.1588805825242717E-05, -4.724126213592232E-05, 8.9136},
        {300, -3.2413106796116498E-06, 1.1044368932038834E-05, -4.8966019417475737E-05, 8.1666},
        {325, -5.707330097087379E-05, -8.26388349514563E-06, -3.4692233009708736E-05, 19.667}
    };
    
    @Test
    public void testTrayNormalYflip() throws IOException {
        checkThermalFile("FQK0618.1.DAT", expected_fqk0618_1_yflip,
                TwoGeeLoader.Protocol.TRAY_NORMAL_YFLIP, 10.3, 107, 45);
    }

    private static final double[][] expected_ccb0101_1_ignore = {
        {25, 7.695436893203883E-05, -3.95378640776699E-06, -5.047961165048542E-06, 2},
        {50, 7.75135922330097E-05, 9.611650485436694E-09, -2.4036893203883495E-06, 0.053433},
        {75, 7.695242718446601E-05, 1.978640776699034E-07, -2.9092233009708732E-06, 0},
        {100, 6.643883495145631E-05, -4.481067961165048E-06, -6.430097087378641E-06, 0.054901},
        {125, 5.766116504854368E-05, -3.6150485436893206E-06, -3.229902912621358E-06, 1},
        {150, 5.472912621359223E-05, -4.038543689320388E-06, -2.985339805825242E-06, 0.94635},
        {175, 5.4297087378640766E-05, -2.584368932038835E-06, -3.102718446601942E-06, 1},
        {200, 5.00378640776699E-05, -7.016504854368931E-06, -3.048155339805825E-06, 1},
        {225, 4.5785436893203875E-05, -8.170873786407766E-06, -1.5308737864077668E-06, 1},
        {250, 4.5426213592233E-05, -6.518058252427183E-06, -1.1818446601941743E-06, 1.0591},
        {275, 4.7704854368932036E-05, -8.33883495145631E-06, 8.932038834951458E-08, 0.10781},
        {300, 6.928155339805824E-07, -9.880582524271843E-06, 7.478252427184466E-06, 3},
        {325, -9.690291262135921E-06, -6.349223300970874E-06, -2.0359223300970875E-06, 2.0572},
        {350, -1.0040776699029123E-05, -1.2408737864077669E-05, 3.8872815533980576E-06, 1.11},
        {375, -6.845631067961164E-06, -8.346601941747572E-06, -3.8135922330097086E-06, 3.055},
        {400, 1.1293203883495144E-05, 1.6600970873786408E-06, -5.3800970873786395E-06, 0}
    };
    
    @Test
    public void testTrayNormalIgnore() throws IOException {
        checkThermalFile("CCB0101.1.DAT", expected_ccb0101_1_ignore,
                TwoGeeLoader.Protocol.TRAY_NORMAL_IGNORE, 10.3, 255, 3);        
    }
    
    private void checkThermalFile(String filename, double[][] expected,
            TwoGeeLoader.Protocol protocol,
            double volume, double sampAz, double sampDip) throws IOException {
        final TwoGeeLoader loader = new TwoGeeLoader(copyFile(filename),
                protocol,
                new Vec3(4.628, -4.404, -6.280), false);
        assertEquals(expected.length, loader.getTreatmentSteps().size());
        for (int i = 0; i < loader.getTreatmentSteps().size(); i++) {
            final TreatmentStep step = loader.getTreatmentSteps().get(i);
            checkVariableThermalFields(expected[i], step);
            checkConstantThermalFields(step, volume, sampAz, sampDip);
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

    private File copyFile(String filename) throws IOException {
        final Path filePath
                = temporaryFolder.getRoot().toPath().resolve(filename);
        Files.copy(
                TestFileLocator.class.getResourceAsStream("twogee/" + filename),
                filePath,
                StandardCopyOption.REPLACE_EXISTING);
        return filePath.toFile();
    }
    
}