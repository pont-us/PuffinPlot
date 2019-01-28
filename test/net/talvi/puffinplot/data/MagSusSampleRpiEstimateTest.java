/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
package net.talvi.puffinplot.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MagSusSampleRpiEstimateTest {

    private final Sample nrmSample;
    private final Sample magSusSample;
    private final double ratio = 2;
    private final MagSusSampleRpiEstimate estimate;
    private double delta = 1e-10;
    
    public MagSusSampleRpiEstimateTest() {
        nrmSample = new Sample("NRM sample", null);
        final TreatmentStep nrmTreatmentStep = new TreatmentStep();
        nrmTreatmentStep.setDepth("7");
        nrmSample.addTreatmentStep(nrmTreatmentStep);
        magSusSample = new Sample("MS sample", null);
        final TreatmentStep msTreatmentStep = new TreatmentStep();
        msTreatmentStep.setMagSus(3);
        magSusSample.addTreatmentStep(msTreatmentStep);
        
        estimate = new MagSusSampleRpiEstimate(nrmSample, magSusSample, ratio);
    }

    @Test
    public void testGetNrmSample() {
        assertEquals(nrmSample, estimate.getNrmSample());
    }

    @Test
    public void testGetNormalizer() {
        assertEquals(magSusSample, estimate.getNormalizer());
    }

    @Test
    public void testGetRatio() {
        assertEquals(ratio, estimate.getRatio(), delta);
    }

    @Test
    public void testToCommaSeparatedString() {
        assertEquals("7,2.00000,3.00000\n", estimate.toCommaSeparatedString());
    }

    @Test
    public void testGetCommaSeparatedHeader() {
        assertEquals("ratio,MS", estimate.getCommaSeparatedHeader());
    }
    
}
