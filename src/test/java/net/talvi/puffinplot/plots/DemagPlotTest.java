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
package net.talvi.puffinplot.plots;

import java.awt.image.BufferedImage;
import java.io.IOException;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.Suite;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class DemagPlotTest {

    private final SettablePlotParams params = new SettablePlotParams();
    private final DemagPlot defaultPlot = new DemagPlot(params);
    
    @Test
    public void testGetName() {
        assertEquals("demag", defaultPlot.getName());
    }

    @Test
    public void testGetNiceName() {
        assertEquals("Demag.", defaultPlot.getNiceName());
    }

    @Test
    public void testDrawNoSample() throws IOException {
        final String filename = "DemagPlot-empty";
        final BufferedImage actual = TestUtils.makeImage(defaultPlot);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    @Test
    public void testDraw1() throws IOException {
        final String filename = "DemagPlot-1";
        final Suite suite = TestUtils.createContinuousSuite();
        params.setSample(suite.getSampleByIndex(0));
        final BufferedImage actual = TestUtils.makeImage(makePlot());
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    private DemagPlot makePlot() {
        params.getSettingsMap().put("plotSizes",
                "demag true 10 10 210 230");
        return new DemagPlot(params);
    }
    
}
