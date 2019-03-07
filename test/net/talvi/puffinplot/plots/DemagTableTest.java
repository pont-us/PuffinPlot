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
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class DemagTableTest {
    
    private final SettablePlotParams params = new SettablePlotParams();
    private final DemagTable defaultPlot = new DemagTable(params);
    
    @Test
    public void testGetName() {
        assertEquals("datatable", defaultPlot.getName());
    }

    @Test
    public void testGetNiceName() {
        assertEquals("Data table", defaultPlot.getNiceName());
    }

    @Test
    public void testDrawNoSample() throws IOException {
        final String filename = "DemagTable-empty";
        final BufferedImage actual = TestUtils.makeImage(defaultPlot, 512, 512);
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    @Test
    public void testDrawNoSteps() throws IOException {
        final String filename = "DemagTable-empty";
        params.setSample(new Sample("test", null));
        final BufferedImage actual = TestUtils.makeImage(defaultPlot, 512, 512);
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    @Test
    public void testDrawAf() throws IOException {
        final String filename = "DemagTable-1";
        final Suite suite = TestUtils.createContinuousSuite();
        params.setSample(suite.getSampleByIndex(0));
        final BufferedImage actual = TestUtils.makeImage(makePlot(), 512, 512);
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    @Test
    public void testDrawThermalCropped() throws IOException {
        final String filename = "DemagTable-ThermalCropped";
        final Suite suite = TestUtils.createDiscreteSuiteArc();
        for (Sample sample: suite.getSamples()) {
            int temperature = 20;
            for (TreatmentStep step: sample.getTreatmentSteps()) {
                step.setTreatmentType(TreatmentType.THERMAL);
                step.setTemperature(temperature);
            }
        }
        params.getSettingsMap().put("plotSizes",
                "datatable true 10 10 400 100");
        params.setCorrection(Correction.NONE);
        params.setSample(suite.getSampleByIndex(0));
        final BufferedImage actual =
                TestUtils.makeImage(new DemagTable(params), 512, 128);
        // TestUtils.saveImage(actual, filename);
        assertTrue(TestUtils.isImageCorrect(filename, actual));        
    }
        
    @Test
    public void testDrawUnknownTreatment() throws IOException {
        final String filename = "DemagTable-UnknownTreatment";
        final Suite suite = TestUtils.createContinuousSuite();
        suite.getSamples().stream()
                .flatMap(sample -> sample.getTreatmentSteps().stream())
                .forEach(step -> step.setTreatmentType(TreatmentType.UNKNOWN));
        params.setSample(suite.getSampleByIndex(0));
        final BufferedImage actual = TestUtils.makeImage(makePlot(), 512, 512);
        TestUtils.saveImage(actual, filename);
        // assertTrue(TestUtils.isImageCorrect(filename, actual));
    }
    
    private DemagTable makePlot() {
        params.getSettingsMap().put("plotSizes",
                "datatable true 10 10 400 400");
        params.setCorrection(Correction.NONE);
        return new DemagTable(params);
    }
    

    
}
