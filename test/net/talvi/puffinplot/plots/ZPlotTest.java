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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;
import net.talvi.puffinplot.TestUtils;
import static net.talvi.puffinplot.TestUtils.createContinuousSuite;
import static net.talvi.puffinplot.TestUtils.makeImage;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class ZPlotTest {
    
    private final SettablePlotParams params = new SettablePlotParams();
    private final ZPlot defaultPlot = new ZPlot(params);
    
    @Test
    public void testGetName() {
        assertEquals("zplot", defaultPlot.getName());
    }

    @Test
    public void testGetNiceName() {
        assertEquals("Zplot", defaultPlot.getNiceName());
    }
    
    @Test
    public void testLegendGetNiceName() {
        assertEquals("Zplot key", defaultPlot.getLegend().getNiceName());
    }

    @Test
    public void testAreTreatmentStepsLabelled() {
        for (boolean setting: new boolean[] {false, true}) {
            params.getSettingsMapBoolean().put("plots.labelTreatmentSteps",
                    setting);
            assertEquals(setting, defaultPlot.areTreatmentStepsLabelled());
        }
    }

    @Test
    public void testDrawNoSample() throws IOException {
        final String filename = "Zplot-empty";
        final BufferedImage actual = makeImage(makePlot());
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }

    @Test
    public void testDrawNoSteps() throws IOException {
        final String filename = "Zplot-empty";
        params.setSample(new Sample("test", null));
        final BufferedImage actual = makeImage(makePlot());
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    @Test
    public void testDrawWithData1() throws IOException {
        final String filename = "Zplot-1";
        final Suite suite = createContinuousSuite();
        params.setSample(suite.getSampleByIndex(0));
        params.setCorrection(Correction.NONE);
        params.setVprojXaxis(MeasurementAxis.Y);
        params.setHprojXaxis(MeasurementAxis.X);
        params.setHprojYaxis(MeasurementAxis.MINUSY);
        final BufferedImage actual = makeZPlotImage(makePlot());
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }

    @Test
    public void testDrawWithData2() throws IOException {
        final String filename = "Zplot-2";
        final Suite suite = TestUtils.createDiscreteSuiteArc();
        params.getSettingsMapBoolean().put("plots.labelTreatmentSteps", true);
        params.setSample(suite.getSampleByIndex(0));
        params.setCorrection(Correction.NONE);
        params.setVprojXaxis(MeasurementAxis.X);
        params.setHprojXaxis(MeasurementAxis.Y);
        params.setHprojYaxis(MeasurementAxis.X);
        final BufferedImage actual = makeZPlotImage(makePlot());
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }

    @Test
    public void testDrawWithData3() throws IOException {
        final String filename = "Zplot-3";
        final Suite suite = TestUtils.createDiscreteSuiteArc();
        TestUtils.jitter(suite, new Random(17));
        suite.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                forEach(step -> {
                    step.setPcaAnchored(true);
                    step.setInPca(true);
                });
        suite.doSampleCalculations(Correction.NONE);
        params.setSample(suite.getSampleByIndex(0));
        params.setCorrection(Correction.NONE);
        params.setVprojXaxis(MeasurementAxis.X);
        params.setHprojXaxis(MeasurementAxis.Y);
        params.setHprojYaxis(MeasurementAxis.X);
        final BufferedImage actual = makeZPlotImage(makePlot());
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }

    @Test
    public void testDrawWithData4() throws IOException {
        final String filename = "Zplot-4";
        final Suite suite = TestUtils.createDiscreteSuite();
        TestUtils.jitter(suite, new Random(17));
        suite.getSamples().stream().
                flatMap(s -> s.getTreatmentSteps().stream()).
                forEach(step -> {
                    step.setPcaAnchored(false);
                    step.setInPca(true);
                });
        suite.doSampleCalculations(Correction.NONE);
        params.setSample(suite.getSampleByIndex(0));
        params.getSettingsMap().put("plots.zplotPcaDisplay", "Short");
        params.setCorrection(Correction.NONE);
        params.setVprojXaxis(MeasurementAxis.X);
        params.setHprojXaxis(MeasurementAxis.Y);
        params.setHprojYaxis(MeasurementAxis.X);
        final BufferedImage actual = makeZPlotImage(makePlot());
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }

    @Test
    public void testGetLegend() {
        assertNotNull(defaultPlot.getLegend());
    }
    
    public static BufferedImage makeZPlotImage(ZPlot plot) {
        final BufferedImage actual =
                new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D graphics = actual.createGraphics();
        plot.draw(graphics);
        plot.getLegend().draw(graphics);
        return actual;
    }
    
    private ZPlot makePlot() {
        params.getSettingsMap().put("plotSizes",
                "zplot false 10 60 240 200 zplotlegend true 10 10 240 40");
        return new ZPlot(params);
    }
    
}
