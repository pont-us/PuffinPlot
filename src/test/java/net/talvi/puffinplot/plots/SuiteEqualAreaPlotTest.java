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
package net.talvi.puffinplot.plots;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class SuiteEqualAreaPlotTest {
    
    private final SettablePlotParams params = new SettablePlotParams();
    private final SuiteEqualAreaPlot defaultPlot =
            new SuiteEqualAreaPlot(params);
    
    @Test
    public void testGetName() {
        assertEquals("equarea_suite", defaultPlot.getName());
    }

    @Test
    public void testGetNiceName() {
        assertEquals("Equal-area (suite)", defaultPlot.getNiceName());
    }

    @Test
    public void testGetShortName() {
        assertEquals("Suite", defaultPlot.getShortName());
    }

    @Test
    public void testDrawNoData() throws IOException {
        final Plot plot = makePlot();
        BufferedImage actual = TestUtils.makeImage(makePlot());
        assertTrue(
                TestUtils.isImageCorrect("SuiteEqualAreaPlot-empty", actual));
    }

    @Test
    public void testDrawNoSuite() throws IOException {
        final Sample sample = new Sample("test", null);
        params.setSample(sample);
        final BufferedImage actual = TestUtils.makeImage(makePlot());
        assertTrue(
                TestUtils.isImageCorrect("SuiteEqualAreaPlot-empty", actual));        
    }
    
    @Test
    public void testDrawDiscreteNoSites() throws IOException {
        final String filename = "SuiteEqualAreaPlotDiscreteNoSites";
        final Suite suite = TestUtils.createDiscreteSuite();
        TestUtils.doPcaOnAllSamples(suite, true);
        suite.calculateSuiteMeans(suite.getSamples(), Collections.emptyList());
        params.setSample(suite.getSampleByIndex(0));
        params.getSettingsMapBoolean().
                put("plots.labelPointsInSuitePlots", false);
        params.getSettingsMapBoolean().
                put("plots.highlightCurrentSample", true);
        final BufferedImage actual = TestUtils.makeImage(makePlot());
        assertTrue(TestUtils.isImageCorrect(filename, actual));        
    }
    
    @Test
    public void testDrawContinuousWithSites() throws IOException {
        final String filename = "SuiteEqualAreaPlotContinuousSites";
        final Suite suite = TestUtils.createContinuousSuite();
        suite.setSiteNamesByDepth(suite.getSamples(), 5);
        TestUtils.doPcaOnAllSamples(suite, true);
        suite.getSites().get(0).calculateFisherStats(Correction.NONE);
        suite.getSites().get(1).clearFisherStats();
        suite.calculateSuiteMeans(suite.getSamples(), Collections.emptyList());
        params.getSettingsMapBoolean().
                put("plots.showSiteA95sOnSuitePlot", true);
        params.setSample(suite.getSampleByIndex(0));
        final BufferedImage actual = TestUtils.makeImage(makePlot());
        assertTrue(TestUtils.isImageCorrect(filename, actual));        
    }
    
    @Test
    public void testDrawDiscreteArc() throws IOException {
        final String filename = "SuiteEqualAreaPlotDiscreteArc";
        final Suite suite = TestUtils.createDiscreteSuiteArc();
        TestUtils.doPcaOnAllSamples(suite, true);
        suite.calculateSuiteMeans(suite.getSamples(), Collections.emptyList());
        params.setSample(suite.getSampleByIndex(0));
        params.getSettingsMapBoolean().
                put("plots.labelPointsInSuitePlots", true);
        BufferedImage actual = TestUtils.makeImage(makePlot());
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    private SuiteEqualAreaPlot makePlot() {
        params.getSettingsMap().put("plotSizes",
                "equarea_suite false 10 10 240 240 ");
        return new SuiteEqualAreaPlot(params);
    }
    
}
