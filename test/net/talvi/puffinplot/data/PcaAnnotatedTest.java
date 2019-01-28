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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class PcaAnnotatedTest {
    

    /**
     * This method doesn't test the calculation exhaustively, because that's
     * already done in PcaValuesTest. It performs a simple test for the
     * functionality not contained in PcaValues: extracting the relevant
     * data from a Sample object, and writing the results of the PCA
     * calculation to a list of Strings. For completeness we also test
     * the PcaValues object returned by getPcaValues(), although any problems
     * there should also show up in toStrings().
     */
    @Test
    public void testCalculateAndGetPcaValuesAndToStrings() {
        for (boolean insertUnusedValues: new boolean[] {false, true}) {
            final Sample sample = constructSample(insertUnusedValues);    
            final PcaValues pcaValues = PcaAnnotated.
                    calculate(sample, Correction.NONE).getPcaValues();
            assertEquals(235.9, pcaValues.getDirection().getDecDeg(), 0.1);
            assertEquals(-18.0, pcaValues.getDirection().getIncDeg(), 0.1);
            assertEquals(36.2, pcaValues.getMad3(), 0.1);
            
            final String actual = String.join("|",
                    PcaAnnotated.calculate(sample,Correction.NONE).toStrings());
            assertEquals("235.9101|-17.9915|32.0069|36.2362|N|"
                    + "(19.60 -2.00 25.00)e0 + (-0.53 -0.79 -0.31)t|5|0.0|40.0|"
                    + (insertUnusedValues ? "N" : "Y"),
                    actual);            
        }
        
    }
    
    private Sample constructSample(boolean insertUnusedValues) {
        final Random rnd = new Random(23);
        final List<Vec3> vectors = new ArrayList<>(7);
        for (int i=0; i<5; i++) {
            vectors.add(new Vec3(rnd.nextInt(100)-50,
                    rnd.nextInt(100)-50, rnd.nextInt(100)-50));
        }
        final Sample sample = new Sample("test", null);
        for (int i=0; i<vectors.size(); i++) {
            final Vec3 v = vectors.get(i);
            sample.addDatum(constructDatum(v, i*10, true));
            if (insertUnusedValues && i==2) {
                sample.addDatum(constructDatum(v, i*10, false));
                sample.addDatum(constructDatum(v, i*10, false));
            }
        }
        return sample;
    }

    private TreatmentStep constructDatum(final Vec3 v, double afz, boolean inPca) {
        final TreatmentStep treatmentStep = new TreatmentStep(v);
        treatmentStep.setInPca(inPca);
        treatmentStep.setPcaAnchored(false);
        treatmentStep.setTreatType(TreatType.DEGAUSS_Z);
        treatmentStep.setAfZ(afz);
        return treatmentStep;
    }
    
    @Test
    public void testCalculateWithEmptySample() {
        assertNull(PcaAnnotated.calculate(new Sample("test", null),
                Correction.NONE));
    }

    @Test
    public void testGetHeaders() {
        final String result = String.join("|", PcaAnnotated.getHeaders());
        assertEquals("PCA dec. (deg)|PCA inc. (deg)|PCA MAD1|PCA MAD3|"
                + "PCA anchored|PCA equation|PCA npoints|"
                + "PCA start (degC or mT)|PCA end (degC or mT)|PCA contiguous",
                result);
    }

    @Test
    public void testGetEmptyFields() {
        assertEquals(PcaAnnotated.getHeaders().size(),
                PcaAnnotated.getEmptyFields().size());
        assertTrue(PcaAnnotated.getEmptyFields().stream().
                allMatch(field -> "".equals(field)));
    }
    
}
