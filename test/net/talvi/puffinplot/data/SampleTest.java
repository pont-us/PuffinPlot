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

import Jama.Matrix;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.Math.toDegrees;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 *
 * @author pont
 */
public class SampleTest {

    final private static double delta = 1e-10;

    private static Sample makeSimpleSample() {
        final Sample defaultSample = new Sample("Test sample", null);
        for (int i=0; i<10; i++) {
            final Datum d = new Datum(10-i, 20-i, 30-i);
            d.setTreatType(TreatType.DEGAUSS_XYZ);
            d.setAfX(i);
            d.setAfY(i);
            d.setAfZ(i);
            defaultSample.addDatum(d);
        }
        return defaultSample;
    }
    
    @Test
    public void testSelectByTreatmentLevelRange() {
        final Sample sample = makeSimpleSample();
        
        final double[] mins = {2.5, 5.5, 10.5, Double.NEGATIVE_INFINITY};
        final double[] maxs = {7.5, 6.5, 0, Double.POSITIVE_INFINITY};
        
        for (int i = 0; i < mins.length; i++) {
            sample.selectByTreatmentLevelRange(mins[i], maxs[i]);

            for (Datum d : sample.getData()) {
                final boolean shouldBeSelected
                        = d.getTreatmentLevel() >= mins[i]
                        && d.getTreatmentLevel() <= maxs[i];
                assertEquals(d.isSelected(), shouldBeSelected);
            }
        }
        
        sample.selectByTreatmentLevelRange(Double.NaN, Double.NaN);
        // Result is undefined so we don't test it, but we're making sure
        // that calling with NaNs doesn't produce an exception.
    }

    @Test
    public void testSetAmsFromTensor() {
        final Random rnd = new Random(13);
        for (int test=0; test<10; test++) {
            final Sample sample = new Sample("test", null);
            final double sampAz = rnd.nextDouble()*2*Math.PI;
            final double sampDip = rnd.nextDouble()*2*Math.PI-Math.PI;
            final double formAz = rnd.nextDouble()*2*Math.PI;
            final double formDip = rnd.nextDouble()*2*Math.PI-Math.PI;
            final Datum d = new Datum();
            d.setSampAz(toDegrees(sampAz));
            d.setSampDip(toDegrees(sampDip));
            d.setFormAz(toDegrees(formAz));
            d.setFormDip(toDegrees(formDip));
            sample.addDatum(d);
            sample.setAmsFromTensor(1, 2, 3, 4, 5, 6);
            final Tensor actual = sample.getAms();
            final Tensor expected = new Tensor(1, 2, 3, 4, 5, 6,
                    new Matrix(Vec3.getSampleCorrectionMatrix(sampAz, sampDip)),
                    new Matrix(Vec3.getFormationCorrectionMatrix(
                            formAz, formDip)));
            for (int axisIndex=0; axisIndex<3; axisIndex++) {
                assertTrue(expected.getAxis(axisIndex).equals(
                        actual.getAxis(axisIndex), 1e-6));
            }
        }
    }
    
    @Test
    public void testRotateAroundZAxis() {
        final Vec3[] directions = {
            Vec3.ORIGIN, Vec3.DOWN, Vec3.NORTH, Vec3.NORTH.plus(Vec3.DOWN)
        };
        final Vec3[] directionsPlus90 = {
            Vec3.ORIGIN, Vec3.DOWN, Vec3.EAST, Vec3.EAST.plus(Vec3.DOWN)            
        };
        
        final Sample sample = makeSampleFromVectors(directions);
        final Sample sampleForwardRotated = makeSampleFromVectors(directions);
        sampleForwardRotated.rotateAroundZAxis(90);
        final Sample samplePlus90 = makeSampleFromVectors(directions);
        final Sample samplePlus90BackwardRotated = makeSampleFromVectors(directions);
        sampleForwardRotated.rotateAroundZAxis(-90);
        
        assertTrue(doSamplesHaveSameMoments(sampleForwardRotated, samplePlus90));
        assertTrue(doSamplesHaveSameMoments(sample, samplePlus90BackwardRotated));
    }
    
    private Sample makeSampleFromVectors(Vec3[] vectors) {
        final Sample sample = new Sample("test sample", null);
        for (Vec3 vector: vectors) {
            sample.addDatum(new Datum(vector));
        }
        return sample;
    }
    
    private boolean doSamplesHaveSameMoments(Sample s0, Sample s1) {
        if (s0.getData().size() != s1.getData().size()) {
            return false;
        }
        for (int i=0; i < s0.getData().size(); i++) {
            final Vec3 v0 = s0.getDatum(i).getMoment();
            final Vec3 v1 = s1.getDatum(i).getMoment();
            if (!v0.equals(v1, 1e-10)) {
                return false;
            }
        }
        return true;
    }
    
    @Test
    public void testSetValue() {
        final double value = 3.14;
        final String valueString = String.format("%g", value);
        final Sample sample = new Sample("sample1", null);

        sample.setValue(DatumField.SAMPLE_AZ, valueString);
        assertEquals(value, sample.getSampAz(), delta);
        sample.setValue(DatumField.SAMPLE_DIP, valueString);
        assertEquals(value, sample.getSampDip(), delta);
        sample.setValue(DatumField.VIRT_SAMPLE_HADE, valueString);
        assertEquals(value, sample.getSampHade(), delta);
        sample.setValue(DatumField.FORM_AZ, valueString);
        assertEquals(value, sample.getFormAz(), delta);
        sample.setValue(DatumField.FORM_DIP, valueString);
        assertEquals(value, sample.getFormDip(), delta);
        sample.setValue(DatumField.VIRT_FORM_STRIKE, valueString);
        assertEquals(value, sample.getFormStrike(), delta);
        sample.setValue(DatumField.MAG_DEV, valueString);
        assertEquals(value, sample.getMagDev(), delta);
    }
    
    @Test
    public void testGetDiscreteId() {
        final Sample sample = new Sample("sample1", null);
        final Datum datum = new Datum(Vec3.ORIGIN);
        datum.setMeasType(MeasType.CONTINUOUS);
        final String discreteId = "discrete-id-1";
        datum.setDiscreteId(discreteId);
        datum.setSample(sample);
        sample.addDatum(datum);
        assertEquals(discreteId, sample.getDiscreteId());
    }
    
    @Test
    public void testGetDiscreteIdWithNoData() {
        final Sample sample = new Sample("sample1", null);
        assertNull(sample.getDiscreteId());        
    }
    
    @Test
    public void testSetDiscreteId() {
        final Sample sample = new Sample("sample1", null);
        final Datum datum = new Datum(Vec3.ORIGIN);
        datum.setMeasType(MeasType.CONTINUOUS);
        datum.setSample(sample);
        sample.addDatum(datum);

        final String discreteId = "discrete-id-1";
        sample.setDiscreteId(discreteId);
        assertEquals(discreteId, sample.getDiscreteId());        
    }
    
    @Test(expected = IllegalStateException.class)
    public void testSetDiscreteIdWithNoData() {
        final Sample sample = new Sample("sample1", null);
        final String discreteId = "discrete-id-1";
        sample.setDiscreteId(discreteId);
    }

    @Test
    public void testIsSelectionContiguous() {
        final Sample sample = makeSimpleSample();
        assertTrue(sample.isSelectionContiguous());        
        for (int i=3; i<8; i++) {
            sample.getDatum(i).setSelected(true);
        }
        assertTrue(sample.isSelectionContiguous());
        sample.getDatum(5).setSelected(false);
        assertFalse(sample.isSelectionContiguous());
    }
    
    @Test
    public void testGetSelectedData() {
        final Sample sample = makeSimpleSample();
        for (int i=3; i<8; i++) {
            sample.getDatum(i).setSelected(true);
        }
        assertEquals(sample.getData().subList(3, 8),
                sample.getSelectedData());
    }
    
    @Test
    public void testGetSelectionBitSet() {
        final Sample sample = makeSimpleSample();
        final List<Integer> bits = Arrays.asList(1, 3, 6);
        final BitSet expectedBitSet = new BitSet();
        for (Integer i: bits) {
            sample.getDatum(i).setSelected(true);
            expectedBitSet.set(i);
        }
        assertEquals(expectedBitSet, sample.getSelectionBitSet());
    }

    @Test
    public void testSetSelectionBitSet() {
        final BitSet bitSet = new BitSet();
        final List<Integer> bitList = Arrays.asList(1, 3, 6);
        final Sample sample = makeSimpleSample();
        bitList.forEach(i -> bitSet.set(i));
        sample.setSelectionBitSet(bitSet);
        for (int i=0; i<sample.getNumData(); i++) {
            assertEquals(bitList.contains(i), sample.getDatum(i).isSelected());
        }
    }


}
