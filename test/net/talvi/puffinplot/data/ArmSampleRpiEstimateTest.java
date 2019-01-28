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
package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class ArmSampleRpiEstimateTest {

    private static final double delta = 1e-10;

    final List<Double> intensities = new ArrayList<>(Arrays.asList(
            2., 4., 6., -1., 8.));
    final Sample nrmSample;
    final Sample armSample;
    final double meanRatio = 5;
    final double slope = 3;
    final double r = 2;
    final double rSquared = 4;
    final ArmSampleRpiEstimate estimate;
    
    public ArmSampleRpiEstimateTest() {
        nrmSample = new Sample("NRM sample", null);
        final TreatmentStep nrmTreatmentStep = new TreatmentStep();
        nrmTreatmentStep.setDepth("7");
        nrmSample.addDatum(nrmTreatmentStep);
        armSample = new Sample("ARM sample", null);
        final TreatmentStep armTreatmentStep = new TreatmentStep(1, 2, 2);
        armSample.addDatum(armTreatmentStep);
        
        estimate = new ArmSampleRpiEstimate(
                intensities, nrmSample, armSample, meanRatio, slope, r,
                rSquared);
    }
    
    @Test
    public void testGetNrmSample() {
        assertEquals(nrmSample, estimate.getNrmSample());
    }

    @Test
    public void testGetNormalizer() {
        assertEquals(armSample, estimate.getNormalizer());
    }

    @Test
    public void testGetIntensities() {
        assertEquals(intensities, estimate.getIntensities());
    }

    @Test
    public void testGetMeanRatio() {
        assertEquals(meanRatio, estimate.getMeanRatio(), delta);
    }

    @Test
    public void testGetSlope() {
        assertEquals(slope, estimate.getSlope(), delta);
    }

    @Test
    public void testGetR() {
        assertEquals(r, estimate.getR(), delta);
    }

    @Test
    public void testGetrSquared() {
        assertEquals(rSquared, estimate.getrSquared(), delta);
    }

    @Test
    public void testToCommaSeparatedString() {
        assertEquals("7,2.00000,4.00000,6.00000,,8.00000,"
                + "5.00000,3.00000,2.00000,4.00000,3.00000\n",
                estimate.toCommaSeparatedString());
    }

    @Test
    public void testGetCommaSeparatedHeader() {
        assertEquals("mean ratio,slope,r,r-squared,ARM",
                estimate.getCommaSeparatedHeader());
    }
    
}
