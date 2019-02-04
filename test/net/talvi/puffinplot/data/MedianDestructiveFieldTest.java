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
package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.talvi.puffinplot.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class MedianDestructiveFieldTest {
    
    
    
    @Test
    public void testInsufficientInput() {
        assertNull(MedianDestructiveField.calculate(Collections.emptyList()));
        assertNull(MedianDestructiveField.calculate(
                Collections.singletonList(makeTreatmentStep(0, 100))));
    }
 

    @Test
    public void testGetHeaders() {
        final String expected = "MDF half-intensity (A/m),"
                + "MDF demagnetization (degC or T),MDF midpoint reached";
        final String actual = String.join(",",
                MedianDestructiveField.getHeaders());
        assertEquals(expected, actual);
        assertTrue(TestUtils.isPrintableAscii(actual));
    }

    @Test
    public void testGetEmptyFields() {
        assertEquals(MedianDestructiveField.getHeaders().size(),
                MedianDestructiveField.getEmptyFields().size());
        assertTrue(MedianDestructiveField.getEmptyFields().stream().
                allMatch(field -> "".equals(field)));
    }

    @Test
    public void testToStrings() {
        assertEquals("50.0|50.0|Y",
                String.join("|", MedianDestructiveField.calculate(
                makeTreatmentStepList(new double[][] {{0, 100}, {100, 0}})).
                toStrings()));
        assertTrue(String.join("|", MedianDestructiveField.calculate(
                makeTreatmentStepList(new double[][] {{0, 100}, {100, 60}})).
                toStrings()).endsWith("|N"));
    }

    @Test
    public void testCalculate() {
        final double[][][][] inputsAndOutputs = new double[][][][]
        {
            { {{0, 100}, {10, 50}, {20, 20}}, {{10, 50, 1}} },
            { {{0, 140}, {59, 71}, {61, 69}}, {{60, 70, 1}} },
            { {{0, 100}, {10, 60}, {20, 51}}, {{-1, -1, 0}} }
        };
        
        for (double[][][] inputAndOutput: inputsAndOutputs) {
            assert(inputAndOutput.length == 2);
            assert(inputAndOutput[1].length == 1);
            assert(inputAndOutput[1][0].length == 3);
            final double[][] input = inputAndOutput[0];
            final double[] output = inputAndOutput[1][0];
            testCalculate(makeTreatmentStepList(input), output);
        }
        
    }

    private static void testCalculate(
            List<TreatmentStep> input, double[] output) {
        final MedianDestructiveField mdf =
                MedianDestructiveField.calculate(input);
        
        final boolean isHalfIntReached = (output[2] > 0.99);
        
        assertEquals(isHalfIntReached, mdf.isHalfIntReached());
        if (isHalfIntReached) {
            assertEquals(output[0], mdf.getDemagLevel(), 1e-6);
            assertEquals(output[1], mdf.getIntensity(), 1e-6);
        }
    }

    private static List<TreatmentStep> makeTreatmentStepList(
            double[][] levelsAndIntensities) {
        final List<TreatmentStep> steps =
                new ArrayList<>(levelsAndIntensities.length);
        for (double[] levelAndIntensity: levelsAndIntensities) {
            assert(levelAndIntensity.length == 2);
            steps.add(makeTreatmentStep(levelAndIntensity[0],
                    levelAndIntensity[1]));
        }
        return steps;
    }

    private static TreatmentStep makeTreatmentStep(
            double treatmentLevel, double intensity) {
        final TreatmentStep d = new TreatmentStep(intensity, 0, 0);
        d.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
        d.setAfX(treatmentLevel);
        d.setAfY(treatmentLevel);
        d.setAfZ(treatmentLevel);
        return d;
    }    
}
