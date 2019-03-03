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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import static net.talvi.puffinplot.TestUtils.writeStringToTemporaryFile;
import net.talvi.puffinplot.data.ArmAxis;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentParameter;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class PplLoaderTest {
    
    private static final double DELTA = 1e-10;
    private static final double[][] expectedTable = {
        {3.00, 22262, 0.0033623471882640588, -0.004918509615384615, -0.004648425787106446, 0.0, 0, 1, 0, 0, 0},
        {3.00, 22264, 0.0028580684596577017, -0.0047906850961538456, -0.004616566716641679, 0.005, 0, 1, 0, 0, 0},
        {3.00, 22265, 0.0023847188264058683, -0.00418251201923077, -0.004277736131934033, 0.01, 0, 1, 0, 0, 0},
        {3.00, 22266, 0.0019810513447432766, -0.0034710937500000002, -0.003652473763118441, 0.015, 0, 1, 0, 0, 0},
        {3.00, 22267, 0.0015812958435207827, -0.0028296274038461535, -0.00291544227886057, 0.02, 1, 1, 0, 0, 1},
        {3.00, 22268, 0.001168520782396088, -0.002302403846153846, -0.0022276986506746628, 0.025, 1, 1, 0, 0, 1},
        {3.00, 22269, 8.693765281173594E-4, -0.0018548076923076923, -0.0016383058470764618, 0.03, 1, 1, 0, 0, 1},
        {3.00, 22270, 5.2820293398533E-4, -0.0012987379807692307, -9.155172413793103E-4, 0.04, 1, 1, 0, 0, 1},
        {3.00, 22271, 2.674694376528117E-4, -8.774038461538462E-4, -4.226761619190405E-4, 0.05, 1, 1, 0, 0, 1},
        {3.00, 22272, 1.688753056234719E-4, -6.794471153846154E-4, -1.6564467766116942E-4, 0.06, 1, 1, 0, 0, 1},
        {3.00, 22273, 4.403606356968216E-6, -3.359134615384615E-4, 1.9004122938530736E-4, 0.08, 1, 1, 0, 0, 1},
        {3.00, 22274, 5.8761613691931544E-5, -2.185576923076923E-4, 3.1503748125937035E-4, 0.1, 0, 1, 0, 0, 0},
        {4.00, 22262, 0.004200244498777506, -0.00464248798076923, -0.005350449775112444, 0.0, 0, 1, 0, 0, 0},
        {4.00, 22264, 0.003507823960880196, -0.0045345552884615385, -0.005245877061469266, 0.005, 0, 1, 0, 0, 0},
        {4.00, 22265, 0.0028503056234718827, -0.003959975961538462, -0.004809970014992505, 0.01, 0, 1, 0, 0, 0}
    };
    
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testWithEmptyFile() throws IOException {
        final PplLoader loader = new PplLoader(
                writeStringToTemporaryFile("empty.ppl", null, temporaryFolder));
        checkEmptyWithMessage(loader);
    }
    
    @Test
    public void testWithInvalidFile() throws IOException {
        final PplLoader loader = new PplLoader(
                writeStringToTemporaryFile("invalid.ppl",
                        "Not a PuffinPlot file.", temporaryFolder));
        checkEmptyWithMessage(loader);
        
    }

    @Test
    public void testWithNoData() throws IOException {
        final PplLoader loader = new PplLoader(
                writeStringToTemporaryFile("invalid.ppl",
                        "PuffinPlot file. Version 3\n", temporaryFolder));
        checkEmptyWithMessage(loader);
    }

    @Test
    public void testWithFutureVersion() throws IOException {
        final PplLoader loader = new PplLoader(
                writeStringToTemporaryFile("invalid.ppl",
                        "PuffinPlot file. Version 9\n", temporaryFolder));
        checkEmptyWithMessage(loader);
    }

    private void checkEmptyWithMessage(PplLoader loader) {
        assertTrue(loader.treatmentSteps.isEmpty());
        assertTrue(loader.getExtraLines().isEmpty());
        assertEquals(1, loader.getMessages().size());
    }
    
    @Test
    public void testWithVersion3File() throws IOException {
        final String filename = "c5h-truncated.ppl";
        final Path filePath =
                temporaryFolder.getRoot().toPath().resolve(filename);
        Files.copy(TestFileLocator.class.getResourceAsStream(filename),
                filePath);
        final PplLoader loader = new PplLoader(filePath.toFile());
        loader.treatmentSteps.forEach(PplLoaderTest::checkConstantValues);
        assertEquals(96, loader.treatmentSteps.size());
        assertEquals(6, loader.getExtraLines().size());
        assertTrue(loader.messages.isEmpty());
        for (int i=0; i<expectedTable.length; i++) {
            final double[] exp = expectedTable[i];
            final TreatmentStep step = loader.treatmentSteps.get(i);
            checkVariableValues(exp, step);
        }
    }

    private static void checkVariableValues(double[] exp,
            TreatmentStep step) throws NumberFormatException {
        assertEquals(exp[0], Double.parseDouble(step.getDepth()), DELTA);
        assertEquals(exp[1], step.getRunNumber(), DELTA);
        assertEquals(exp[2], step.getMoment().x, DELTA);
        assertEquals(exp[3], step.getMoment().y, DELTA);
        assertEquals(exp[4], step.getMoment().z, DELTA);
        assertEquals(exp[5], step.getAfZ(), DELTA);
        assertEquals(exp[6], step.isSelected() ? 1 : 0, DELTA);
        assertEquals(exp[7], step.isPcaAnchored()? 1 : 0, DELTA);
        assertEquals(exp[8], step.isHidden()? 1 : 0, DELTA);
        assertEquals(exp[9], step.isOnCircle()? 1 : 0, DELTA);
        assertEquals(exp[10], step.isInPca()? 1 : 0, DELTA);
    }
    
    private static void checkConstantValues(TreatmentStep step) {
        assertEquals("C5H", step.getDiscreteId());
        assertEquals(MeasurementType.CONTINUOUS, step.getMeasurementType());
        assertEquals(10.8, step.getVolume(), DELTA);
        assertEquals(4, step.getArea(), DELTA);
        assertEquals(0, step.getSampAz(), DELTA);
        assertEquals(90, step.getSampDip(), DELTA);
        assertEquals(0, step.getFormAz(), DELTA);
        assertEquals(0, step.getFormDip(), DELTA);
        assertEquals(0, step.getMagDev(), DELTA);
        assertEquals(0, step.getFormDip(), DELTA);
        assertEquals(0, step.getFormDip(), DELTA);
        assertEquals(ArmAxis.NONE, step.getArmAxis());
        assertTrue(step.getAfZ() == 0 ||
                step.getTreatmentType() == TreatmentType.DEGAUSS_XYZ);
        
        Arrays.asList(TreatmentParameter.MAG_SUS,
                TreatmentParameter.AF_X,
                TreatmentParameter.AF_Y,
                TreatmentParameter.TEMPERATURE,
                TreatmentParameter.IRM_FIELD,
                TreatmentParameter.ARM_FIELD).
                forEach(f -> assertEquals("NaN", step.getValue(f)));
    }
    
    private static final double[][] expectedVersion2 = {
        {25, 1.2539417475728154E-05, 0.00010844922330097089, -0.0001057888349514563, 10},
        {50, 2.456368932038835E-05, 9.959029126213592E-05, -9.436456310679613E-05, 10.052},
        {75, 2.2204563106796116E-05, 0.00010166300970873787, -0.00011500757281553397, 9.1056},
        {100, 3.3783495145631077E-06, 0.00013295048543689322, -9.034660194174755E-05, 11},
        {125, 1.3048737864077671E-05, 1.7123300970873785E-05, -1.9799320388349512E-05, 9.0536},
        {150, 1.0973883495145629E-05, 1.0697184466019416E-05, -1.675038834951456E-05, 10},
        {175, 4.529310679611651E-05, 1.1473786407767007E-06, -1.8276893203883495E-05, 11.053},
        {200, 1.3350388349514562E-05, 1.408378640776699E-05, -1.5256407766990291E-05, 9.055},
        {25, 2.392955339805825E-05, 7.725155339805826E-06, -3.2551941747572815E-05, 12},
        {50, 2.781087378640776E-05, 4.955757281553397E-06, -3.5526699029126214E-05, 11.155},
        {75, 2.7847864077669902E-05, -1.8592233009708654E-07, -2.257757281553398E-05, 10.317},
        {100, 3.0117087378640778E-05, -2.8426213592233005E-06, -3.227735922330097E-05, 11},
        {125, 4.03804854368932E-05, 7.101403883495145E-06, -2.5261262135922328E-05, 10.149},
        {150, 4.169631067961165E-05, -8.934368932038834E-06, -2.0503524271844657E-05, 12},
        {175, 1.196174757281553E-05, 1.1867572815533979E-05, -2.0949805825242717E-05, 10.159},
        {200, 4.4693592233009704E-05, -1.0288349514563102E-06, -2.0131941747572812E-05, 10.152}
    };
    
    @Test
    public void testWithVersion2File() throws IOException {
        final String filename = "waipara-truncated.ppl";
        final Path filePath
                = temporaryFolder.getRoot().toPath().resolve(filename);
        Files.copy(TestFileLocator.class.getResourceAsStream(filename),
                filePath);
        final PplLoader loader = new PplLoader(filePath.toFile());
        for (int i=0; i<expectedVersion2.length; i++) {
            final double[] expected = expectedVersion2[i];
            final TreatmentStep step = loader.getTreatmentSteps().get(i);
            assertEquals(expected[0], step.getTemperature(), DELTA);
            assertEquals(expected[1], step.getMoment().x, DELTA);
            assertEquals(expected[2], step.getMoment().y, DELTA);
            assertEquals(expected[3], step.getMoment().z, DELTA);
            assertEquals(expected[4], step.getMagSus(), DELTA);
            
            /*
             * This is unfortunately a rather trivial test for the conversion of
             * AF intensities in a Version 2 file, because I don't have any
             * version 2 AF-treated files to test with. Creating one is also
             * hard because even the first released PuffinPlot version (0.95)
             * was using PPL format version 3, and building older PuffinPlot
             * versions from source to create Version 2 test files is tricky
             * (obsolete Java and library versions, etc.). But this also means
             * that the chances of PuffinPlot ever actually encountering a
             * Version 2 AF file are effectively zero.
             */
            assertEquals(0, step.getAfX(), DELTA);
            assertEquals(0, step.getAfY(), DELTA);
            assertEquals(0, step.getAfZ(), DELTA);
        }
    }

}
