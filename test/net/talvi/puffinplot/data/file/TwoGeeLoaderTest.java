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
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author pont
 */
public class TwoGeeLoaderTest {

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

}