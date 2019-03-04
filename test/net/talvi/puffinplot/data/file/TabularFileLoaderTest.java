/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
import java.util.Map;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.FieldUnit;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.MomentUnit;
import net.talvi.puffinplot.data.TreatmentParameter;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.Vec3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class TabularFileLoaderTest {
    
    private static final double DELTA = 1e-20;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testWithNonExistentFile() throws IOException {
        final File file = temporaryFolder.getRoot().toPath().
                resolve("non-existent").toFile();
        final Map<Integer, TreatmentParameter> columnMap
                = FileFormatTest.makeColMap(
                        0, TreatmentParameter.X_MOMENT,
                        1, TreatmentParameter.Y_MOMENT,
                        2, TreatmentParameter.Z_MOMENT
                );
        final FileFormat format = new FileFormat(columnMap, 0,
                MeasurementType.DISCRETE,
                TreatmentType.DEGAUSS_XYZ, " ", false, null, MomentUnit.AM,
                FieldUnit.MILLITESLA);
        final TabularFileLoader loader = new TabularFileLoader(file, format);
        assertEquals(1, loader.getMessages().size());
        assertEquals(0, loader.getTreatmentSteps().size());
    }
    
    @Test
    public void testWithSimpleFile() throws IOException {
        final Map<Integer, TreatmentParameter> columnMap =
                FileFormatTest.makeColMap(0, TreatmentParameter.AF_Z,
                    1, TreatmentParameter.X_MOMENT,
                    2, TreatmentParameter.Y_MOMENT,
                    3, TreatmentParameter.Z_MOMENT,
                    4, TreatmentParameter.DISCRETE_ID
                );
        final FileFormat format = new FileFormat(columnMap, 0,
                MeasurementType.DISCRETE,
                TreatmentType.DEGAUSS_XYZ, " ", false, null, MomentUnit.AM,
                FieldUnit.MILLITESLA);
        final File file = TestUtils.writeStringToTemporaryFile("test1",
                "5 1 2 -3 \"sample1\"\n",
                temporaryFolder);
        final TabularFileLoader loader = new TabularFileLoader(file, format);
        assertEquals(1, loader.getTreatmentSteps().size());
        final TreatmentStep step = loader.getTreatmentSteps().get(0);
        assertEquals(0.005, step.getAfZ(), DELTA);
        assertEquals(1, step.getMoment().x, DELTA);
        assertEquals(2, step.getMoment().y, DELTA);
        assertEquals(-3, step.getMoment().z, DELTA);
        // NB: we don't (currently) strip quotation marks
        assertEquals("\"sample1\"", step.getIdOrDepth());
    }
    
    
    @Test
    public void testInsufficientVectorComponents() throws IOException {
        final Map<Integer, TreatmentParameter> columnMap =
            FileFormatTest.makeColMap(
                0, TreatmentParameter.X_MOMENT
            );
        final FileFormat format = new FileFormat(columnMap, 0,
                MeasurementType.DISCRETE,
                TreatmentType.DEGAUSS_XYZ, " ", false, null, MomentUnit.AM,
                FieldUnit.MILLITESLA);
        final File file = TestUtils.writeStringToTemporaryFile("test1",
                "17\n",
                temporaryFolder);
        final TabularFileLoader loader = new TabularFileLoader(file, format);
        assertEquals(1, loader.getMessages().size());
        assertEquals(1, loader.getTreatmentSteps().size());
        final TreatmentStep step = loader.getTreatmentSteps().get(0);
        assertTrue(new Vec3(17, 0, 0).equals(step.getMoment()));        
    }

    @Test
    public void testDirectionOnly() throws IOException {
        final Map<Integer, TreatmentParameter> columnMap
                = FileFormatTest.makeColMap(
                        0, TreatmentParameter.VIRT_DECLINATION,
                        1, TreatmentParameter.VIRT_INCLINATION
                );
        final FileFormat format = new FileFormat(columnMap, 0,
                MeasurementType.DISCRETE,
                TreatmentType.DEGAUSS_XYZ, " ", false, null, MomentUnit.AM,
                FieldUnit.MILLITESLA);
        final File file = TestUtils.writeStringToTemporaryFile("test1",
                "23 42\n",
                temporaryFolder);
        final TabularFileLoader loader = new TabularFileLoader(file, format);
        assertEquals(1, loader.getMessages().size());
        assertEquals(1, loader.getTreatmentSteps().size());
        final TreatmentStep step = loader.getTreatmentSteps().get(0);
        assertTrue(Vec3.fromPolarDegrees(1, 42, 23).equals(step.getMoment()));
    }

}
