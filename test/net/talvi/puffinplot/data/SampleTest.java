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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import Jama.Matrix;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.TestUtils.ListHandler;
import org.junit.Test;

import static java.lang.Math.toDegrees;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SampleTest {

    private static final double delta = 1e-10;
    private final Sample simpleSample;

    public SampleTest() {
        simpleSample = makeSimpleSample();
    }
    
    private static Sample makeSimpleSample() {
        final Sample s = new Sample("Test sample", null);
        for (int i=0; i<10; i++) {
            final TreatmentStep d = new TreatmentStep(10-i, 20-i, 30-i);
            d.setTreatmentType(TreatmentType.DEGAUSS_XYZ);
            d.setAfX(i);
            d.setAfY(i);
            d.setAfZ(i);
            s.addTreatmentStep(d);
        }
        return s;
    }
    
    @Test
    public void testSelectByTreatmentLevelRange() {
        
        final double[] mins = {2.5, 5.5, 10.5, Double.NEGATIVE_INFINITY};
        final double[] maxs = {7.5, 6.5, 0, Double.POSITIVE_INFINITY};
        
        for (int i = 0; i < mins.length; i++) {
            simpleSample.selectByTreatmentLevelRange(mins[i], maxs[i]);

            for (TreatmentStep d: simpleSample.getTreatmentSteps()) {
                final boolean shouldBeSelected
                        = d.getTreatmentLevel() >= mins[i]
                        && d.getTreatmentLevel() <= maxs[i];
                assertEquals(d.isSelected(), shouldBeSelected);
            }
        }
        
        simpleSample.selectByTreatmentLevelRange(Double.NaN, Double.NaN);
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
            final TreatmentStep d = new TreatmentStep();
            d.setSampAz(toDegrees(sampAz));
            d.setSampDip(toDegrees(sampDip));
            d.setFormAz(toDegrees(formAz));
            d.setFormDip(toDegrees(formDip));
            sample.addTreatmentStep(d);
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
            sample.addTreatmentStep(new TreatmentStep(vector));
        }
        return sample;
    }
    
    private boolean doSamplesHaveSameMoments(Sample s0, Sample s1) {
        if (s0.getTreatmentSteps().size() != s1.getTreatmentSteps().size()) {
            return false;
        }
        for (int i = 0; i < s0.getTreatmentSteps().size(); i++) {
            final Vec3 v0 = s0.getTreatmentStepByIndex(i).getMoment();
            final Vec3 v1 = s1.getTreatmentStepByIndex(i).getMoment();
            if (!v0.equals(v1, 1e-10)) {
                return false;
            }
        }
        return true;
    }
    
    @Test
    public void testSetValue() {
        final double value = 3.14;
        final String valueString = String.format(Locale.ENGLISH, "%g", value);
        final Sample sample = new Sample("sample1", null);

        sample.setValue(TreatmentParameter.SAMPLE_AZ, valueString);
        assertEquals(value, sample.getSampAz(), delta);
        sample.setValue(TreatmentParameter.SAMPLE_DIP, valueString);
        assertEquals(value, sample.getSampDip(), delta);
        sample.setValue(TreatmentParameter.VIRT_SAMPLE_HADE, valueString);
        assertEquals(value, sample.getSampHade(), delta);
        sample.setValue(TreatmentParameter.FORM_AZ, valueString);
        assertEquals(value, sample.getFormAz(), delta);
        sample.setValue(TreatmentParameter.FORM_DIP, valueString);
        assertEquals(value, sample.getFormDip(), delta);
        sample.setValue(TreatmentParameter.VIRT_FORM_STRIKE, valueString);
        assertEquals(value, sample.getFormStrike(), delta);
        sample.setValue(TreatmentParameter.MAG_DEV, valueString);
        assertEquals(value, sample.getMagDev(), delta);
    }
    
    @Test
    public void testSetValueUnhandledField() {
        final ListHandler handler = ListHandler.createAndAdd();
        final Sample sample = new Sample("sample1", null);
        sample.setValue(TreatmentParameter.MAG_SUS, "0");
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));
    }
    
    @Test
    public void testSetValueWithData() {
        final TreatmentParameter df = TreatmentParameter.SAMPLE_AZ;
        final double value = 2.71;
        final Sample sample = new Sample("sample1", null);
        for (int i=0; i<3; i++) {
            final TreatmentStep d = new TreatmentStep();
            sample.addTreatmentStep(d);
        }
        sample.setValue(df, String.format(Locale.ENGLISH, "%g", value));
        assertTrue(sample.getTreatmentSteps().stream().allMatch(d ->
                Math.abs(value - Double.parseDouble(d.getValue(df))) < delta
        ));
    }
    
    @Test
    public void testGetDiscreteId() {
        final Sample sample = new Sample("sample1", null);
        final TreatmentStep treatmentStep = new TreatmentStep(Vec3.ORIGIN);
        treatmentStep.setMeasurementType(MeasurementType.CONTINUOUS);
        final String discreteId = "discrete-id-1";
        treatmentStep.setDiscreteId(discreteId);
        treatmentStep.setSample(sample);
        sample.addTreatmentStep(treatmentStep);
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
        final TreatmentStep treatmentStep = new TreatmentStep(Vec3.ORIGIN);
        treatmentStep.setMeasurementType(MeasurementType.CONTINUOUS);
        treatmentStep.setSample(sample);
        sample.addTreatmentStep(treatmentStep);

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
        assertTrue(simpleSample.isSelectionContiguous());        
        for (int i=3; i<8; i++) {
            simpleSample.getTreatmentStepByIndex(i).setSelected(true);
        }
        assertTrue(simpleSample.isSelectionContiguous());
        simpleSample.getTreatmentStepByIndex(5).setSelected(false);
        assertFalse(simpleSample.isSelectionContiguous());
        simpleSample.selectAll();
        assertTrue(simpleSample.isSelectionContiguous());
    }
    
    @Test
    public void testGetSelectedTreatmentSteps() {
        for (int i=3; i<8; i++) {
            simpleSample.getTreatmentStepByIndex(i).setSelected(true);
        }
        assertEquals(simpleSample.getTreatmentSteps().subList(3, 8),
                simpleSample.getSelectedTreatmentSteps());
    }
    
    @Test
    public void testGetSelectionBitSet() {
        final List<Integer> bits = Arrays.asList(1, 3, 6);
        final BitSet expectedBitSet = new BitSet();
        for (Integer i: bits) {
            simpleSample.getTreatmentStepByIndex(i).setSelected(true);
            expectedBitSet.set(i);
        }
        assertEquals(expectedBitSet, simpleSample.getSelectionBitSet());
    }

    @Test
    public void testSetSelectionBitSet() {
        final BitSet bitSet = new BitSet();
        final List<Integer> bitList = Arrays.asList(1, 3, 6);
        bitList.forEach(i -> bitSet.set(i));
        simpleSample.setSelectionBitSet(bitSet);
        for (int i=0; i<simpleSample.getNumberOfSteps(); i++) {
            assertEquals(bitList.contains(i), simpleSample.getTreatmentStepByIndex(i).isSelected());
        }
    }

    @Test
    public void testHideAndDeselectSelectedPoints() {
        final Set<TreatmentStep> dataToHide = new HashSet<>();
        for (int i = 3; i < 7; i++) {
            dataToHide.add(simpleSample.getTreatmentStepByIndex(i));
        }
        dataToHide.forEach(d -> d.setSelected(true));
        assertTrue(simpleSample.getTreatmentSteps().stream().allMatch(d -> !d.isHidden()));
        simpleSample.hideAndDeselectSelectedPoints();
        assertTrue(simpleSample.getTreatmentSteps().stream().allMatch(d -> !d.isSelected()));
        assertTrue(simpleSample.getTreatmentSteps().stream().allMatch(
                d -> dataToHide.contains(d) == d.isHidden()));
    }
    
    @Test
    public void testUnhideAllPoints() {
        for (int i = 3; i < 7; i++) {
            simpleSample.getTreatmentStepByIndex(i).setHidden(true);
        }
        simpleSample.unhideAllPoints();
        assertTrue(simpleSample.getTreatmentSteps().stream().allMatch(d -> !d.isHidden()));
    }

    @Test
    public void testInvertMoments() {
        final List<Vec3> expected = simpleSample.getTreatmentSteps().stream().
                map(d -> d.getMoment().invert()).
                collect(Collectors.toList());
        simpleSample.invertMoments();
        for (int i=0; i<simpleSample.getNumberOfSteps(); i++) {
            assertTrue(expected.get(i).equals(
                    simpleSample.getTreatmentStepByIndex(i).getMoment(),
                    delta));
        }
    }
    
    @Test
    public void testSelectVisible() {
        for (int i = 3; i < 7; i++) {
            simpleSample.getTreatmentStepByIndex(i).setHidden(true);
        }
        simpleSample.selectVisible();
        assertTrue(simpleSample.getTreatmentSteps().stream().
                allMatch(d -> d.isHidden() != d.isSelected()));        
    }
    
    @Test
    public void testClearGreatCircle() {
        simpleSample.getTreatmentSteps().forEach(d -> d.setOnCircle(true));
        simpleSample.fitGreatCircle(Correction.NONE);
        assertNotNull(simpleSample.getGreatCircle());
        simpleSample.clearGreatCircle();
        assertNull(simpleSample.getGreatCircle());
    }
    
    @Test
    public void testClearCalculations() {
        simpleSample.getTreatmentSteps().forEach(d -> {
            d.setOnCircle(true);
            d.setInPca(true);
            d.setSelected(true);
        });
        simpleSample.doPca(Correction.NONE);
        simpleSample.fitGreatCircle(Correction.NONE);
        simpleSample.calculateFisher(Correction.NONE);
        simpleSample.calculateMdf();
        assertNotNull(simpleSample.getGreatCircle());
        assertNotNull(simpleSample.getPcaValues());
        assertNotNull(simpleSample.getFisherValues());
        assertNotNull(simpleSample.getMdf());
        simpleSample.clearCalculations();
        assertNull(simpleSample.getGreatCircle());
        assertNull(simpleSample.getPcaValues());
        assertNull(simpleSample.getFisherValues());
        assertNull(simpleSample.getMdf());        
    }
    
    @Test
    public void testFlip() {
        for (MeasurementAxis axis: MeasurementAxis.values()) {
            final Sample sample = makeSimpleSample();
            final List<Vec3> expected = sample.getTreatmentSteps().stream().
                    map(d -> d.getMoment().rot180(axis)).
                    collect(Collectors.toList());
            sample.flip(axis);
            for (int i = 0; i < sample.getNumberOfSteps(); i++) {
                assertTrue(expected.get(i).equals(
                        sample.getTreatmentStepByIndex(i).getMoment(),
                        delta));
            }
        }
    }
    
    @Test
    public void testGetSuite() {
        assertNull(simpleSample.getSuite());
        final Suite suite = new Suite("test");
        final Sample withSuite = new Sample("with suite", suite);
        assertNotNull(withSuite.getSuite());
    }
    
    @Test
    public void testTruncateTreatmentSteps() {
        final List<TreatmentStep> data = new ArrayList<>(simpleSample.getTreatmentSteps());
        simpleSample.truncateTreatmentSteps(7);
        assertEquals(data.subList(0, 7), simpleSample.getTreatmentSteps());
    }

    @Test
    public void testCalculateAndGetMagSusJump() {
        final Sample s = makeThermalMagSusSample(
                30, 10, 50, 24.999, 70, 24.999*2.5001, 90, 100);
        s.calculateMagSusJump();
        assertEquals(70, s.getMagSusJump(), delta);
    }
    
    @Test
    public void testCalculateAndGetMagSusJumpWithoutJump() {
        final Sample s = makeThermalMagSusSample(
                40, 40, 50, 50, 60, 60, 70, 70, 80, 80
        );
        s.calculateMagSusJump();
        assertEquals(0, s.getMagSusJump(), delta);
    }
    
    @Test
    public void testCalculateAndGetMagSusJumpWithoutMagSusData() {
        final Sample s = makeThermalMagSusSample(
                40, Double.NaN, 50, Double.NaN, 60, Double.NaN
        );
        s.calculateMagSusJump();
        assertEquals(0, s.getMagSusJump(), delta);
    }
    
    private static Sample makeThermalMagSusSample(double... tempsAndms) {
        final Sample s = new Sample("test", null);
        for (int i=0; i<tempsAndms.length; i+=2) {
            final TreatmentStep d = new TreatmentStep();
            d.setTreatmentType(TreatmentType.THERMAL);
            d.setTemperature(tempsAndms[i]);
            d.setMagSus(tempsAndms[i+1]);
            s.addTreatmentStep(d);
        }
        return s;
    }
    
    @Test
    public void testIsPcaAnchoredNoData() {
        assertFalse(new Sample("test", null).isPcaAnchored());
    }
    
    @Test
    public void testIsPcaAnchored() {
        final TreatmentStep d = new TreatmentStep();
        final Sample s = new Sample("test", null);
        s.addTreatmentStep(d);
        d.setPcaAnchored(false);
        assertFalse(s.isPcaAnchored());
        d.setPcaAnchored(true);
        assertTrue(s.isPcaAnchored());
    }
    
    @Test
    public void testGetNrmEmptySample() {
        final Sample s = new Sample("test", null);
        assertTrue(Double.isNaN(s.getNrm()));
    }
    
    @Test
    public void testGetNrm() {
        final Sample s = new Sample("test", null);
        final Vec3 nrm = new Vec3(3, 2, 1);
        s.addTreatmentStep(new TreatmentStep(nrm));
        s.addTreatmentStep(new TreatmentStep(nrm.times(0.8)));
        s.addTreatmentStep(new TreatmentStep(nrm.times(0.6)));
        assertEquals(nrm.mag(), s.getNrm(), delta);
    }
    
    @Test
    public void testHasMagSusData() {
        final Sample sample = new Sample("test", null);
        assertFalse(sample.hasMagSusData());
        sample.addTreatmentStep(new TreatmentStep());
        assertFalse(sample.hasMagSusData());
        final TreatmentStep treatmentStep = new TreatmentStep();
        treatmentStep.setMagSus(1);
        sample.addTreatmentStep(treatmentStep);
        assertTrue(sample.hasMagSusData());
    }
    
    @Test
    public void testGetMeasurementType() {
        final Sample sample = new Sample("test", null);
        assertEquals(MeasurementType.DISCRETE, sample.getMeasurementType());
        addStepWithMeasurementType(sample, MeasurementType.NONE);
        assertEquals(MeasurementType.DISCRETE, sample.getMeasurementType());
        addStepWithMeasurementType(sample, MeasurementType.UNKNOWN);
        assertEquals(MeasurementType.DISCRETE, sample.getMeasurementType());
        addStepWithMeasurementType(sample, MeasurementType.UNSET);
        assertEquals(MeasurementType.DISCRETE, sample.getMeasurementType());
        addStepWithMeasurementType(sample, MeasurementType.CONTINUOUS);
        assertEquals(MeasurementType.CONTINUOUS, sample.getMeasurementType());
    }
    
    private static void addStepWithMeasurementType(
            Sample sample, MeasurementType mt) {
        final TreatmentStep treatmentStep = new TreatmentStep(Vec3.NORTH);
        treatmentStep.setMeasurementType(mt);
        sample.addTreatmentStep(treatmentStep);
    }
    
    @Test
    public void testSetAndGetFormStrike() {
        for (int strike: new int[] {0, 1, 45, 180, 300, 359}) {
            simpleSample.setValue(TreatmentParameter.VIRT_FORM_STRIKE,
                    String.format(Locale.ENGLISH, "%d", strike));
            assertEquals(strike, simpleSample.getFormStrike(), delta);
        }
    }
    
    @Test
    public void testToStringsImportedDirection() {
        simpleSample.setImportedDirection(Vec3.fromPolarDegrees(1, 30, 40));
        assertEquals(Arrays.asList("IMPORTED_DIRECTION\t40.000\t30.000"),
                simpleSample.toStrings());
    }
    
    @Test
    public void testFromStringImportedDirection() {
        simpleSample.fromString("IMPORTED_DIRECTION\t40.000\t30.000");
        assertTrue(Vec3.fromPolarDegrees(1, 30, 40).
                equals(simpleSample.getDirection(), delta));
    }

    /**
     * Should return normally with no side effects.
     */
    @Test
    public void testFromStringWithEmptyString() {
        simpleSample.fromString("");
    }
    
    @Test
    public void testFromStringWithUnknownField() {
        ListHandler handler = ListHandler.createAndAdd();
        simpleSample.fromString("SOME_UNKNOWN_FIELD\t666");
        handler.wasOneMessageLogged(Level.WARNING);
    }
    
    @Test
    public void testGetTreatmentStepByTreatmentLevel() {
        assertEquals(5,
                simpleSample.getTreatmentStepByLevel(5).getTreatmentLevel(),
                delta);
    }
    
    @Test
    public void testGetTreatmentStepByTreatmentLevelWithNoData() {
        final Sample sample = new Sample("test", null);
        assertNull(sample.getTreatmentStepByLevel(0));
    }

    @Test
    public void testGetTreatmentStepByTreatmentTypeAndLevel() {
        final TreatmentStep treatmentStep =
                simpleSample.getTreatmentStepByTypeAndLevel(
                        Collections.singleton(TreatmentType.DEGAUSS_XYZ), 5);
        assertEquals(5, treatmentStep.getTreatmentLevel(), delta);
    }

    @Test
    public void testGetTreatmentStepByTreatmentLevelWithNoMatchingData() {
        assertNull(simpleSample.getTreatmentStepByLevel(17));
    }

    @Test
    public void testGetTreatmentStepByTreatmentTypeAndLevelWithNoData() {
        final Sample sample = new Sample("test", null);
        assertNull(sample.getTreatmentStepByTypeAndLevel(
                new HashSet(Arrays.asList(TreatmentType.values())), 0));
    }

    @Test
    public void testGetTreatmentStepByTreatmentTypeAndLevelWithNoMatchingData()
    {
        assertNull(simpleSample.getTreatmentStepByTypeAndLevel(
                Collections.singleton(TreatmentType.THERMAL), 5));
    }

    @Test
    public void testGetDirectionWithFisherValues() {
        simpleSample.selectAll();
        simpleSample.calculateFisher(Correction.NONE);
        final Vec3 expected =
                FisherValues.calculate(simpleSample.getSelectedTreatmentSteps().
                        stream().map(d -> d.getMoment()).
                        collect(Collectors.toList())).getMeanDirection();
        assertTrue(expected.equals(simpleSample.getDirection(), delta));
    }
    
    @Test
    public void testGetCirclePoints() {
        final List<Integer> circlePoints = Arrays.asList(2, 3, 6, 7);
        final List<Vec3> expected =
                circlePoints.stream().
                        map(i -> simpleSample.getTreatmentStepByIndex(i).
                        getMoment()).collect(Collectors.toList());
        circlePoints.forEach(i -> simpleSample.
                getTreatmentStepByIndex(i).setOnCircle(true));
        final List<Vec3> actual = simpleSample.getCirclePoints(Correction.NONE);
        assertEquals(expected.size(), actual.size());
        for (int i=0; i<expected.size(); i++) {
            assertTrue(expected.get(i).equals(actual.get(i), delta));
        }
    }
    
    @Test
    public void testGetFirstRunNumber() {
        IntStream.range(0, simpleSample.getNumberOfSteps()).
                forEach(i -> simpleSample.
                        getTreatmentStepByIndex(i).setRunNumber(i+5));
        assertEquals(5, simpleSample.getFirstRunNumber());
    }
    
    @Test
    public void testGetLastRunNumber() {
        IntStream.range(0, simpleSample.getNumberOfSteps()).
                forEach(i -> simpleSample.
                        getTreatmentStepByIndex(i).setRunNumber(i+5));
        assertEquals(simpleSample.getNumberOfSteps() + 4,
                simpleSample.getLastRunNumber());
    }
    
    @Test
    public void testGetTreatmentStepByRunNumber() {
        IntStream.range(0, simpleSample.getNumberOfSteps()).
                forEach(i -> simpleSample.
                        getTreatmentStepByIndex(i).setRunNumber(i+5));
        assertEquals(simpleSample.getTreatmentStepByIndex(5),
                simpleSample.getTreatmentStepByRunNumber(11));
    }
    
    @Test
    public void testGetSlotNumber() {
        simpleSample.getTreatmentStepByIndex(0).setSlotNumber(17);
        assertEquals(17, simpleSample.getSlotNumber());
    }
    
    @Test
    public void testGetVisibleTreatmentSteps() {
        simpleSample.getTreatmentStepByIndex(5).setHidden(true);
        final List<TreatmentStep> expected =
                new ArrayList<>(simpleSample.getTreatmentSteps());
        expected.remove(5);
        assertEquals(expected, simpleSample.getVisibleTreatmentSteps());
    }
    
    @Test
    public void testRemoveData() {
        final Set<TreatmentStep> toRemove =
                Arrays.stream(new int[] {2, 3, 5, 6}).
                mapToObj(i -> simpleSample.getTreatmentStepByIndex(i)).
                collect(Collectors.toSet());
        final List<TreatmentStep> shouldRemain =
                Arrays.stream(new int[] {0, 1, 4, 7, 8, 9}).
                        mapToObj(i -> simpleSample.getTreatmentStepByIndex(i)).
                        collect(Collectors.toList());
        simpleSample.removeData(toRemove);
        assertEquals(shouldRemain, simpleSample.getTreatmentSteps());
    }

    @Test
    public void testMergeDuplicateTreatmentSteps() {
        final Sample sample = new Sample("sample0", null);
        final List<Vec3> vectors = new ArrayList<>();
        final Random rnd = new Random(77);
        for (int i = 0; i < 3; i++) {
            final Vec3 vector = TestUtils.randomVector(rnd, 1);
            final TreatmentStep step = new TreatmentStep(vector);
            step.setTreatmentType(TreatmentType.THERMAL);
            step.setTemperature(50);
            sample.addTreatmentStep(step);
            vectors.add(vector);
        }
        final Vec3 expectedMean = Vec3.mean(vectors);
        sample.mergeDuplicateTreatmentSteps();
        assertEquals(1, sample.getTreatmentSteps().size());
        assertTrue(expectedMean.equals(sample.getTreatmentStepByIndex(0).getMoment()));
    }
    
    @Test
    public void testMergeSamplesWithInverseData() {
        final Sample sample0 = makeSimpleSample();
        final Sample sample1 = makeSimpleSample();
        sample1.getTreatmentSteps().forEach(d -> d.invertMoment());
        Sample.mergeSamples(Arrays.asList(sample0, sample1));
        assertTrue(sample0.getTreatmentSteps().stream()
                       .allMatch(step -> step.getIntensity() < delta));
    }
    
    @Test
    public void testMergeSamplesWithOverlappingData() {
        final Sample s0 = new Sample("Sample 0", null);
        for (int i = 0; i < 3; i++) {
            final TreatmentStep step =
                    new TreatmentStep(10 - i, 20 - i, 30 - i);
            step.setTreatmentType(TreatmentType.THERMAL);
            step.setTemperature(i * 100);
            s0.addTreatmentStep(step);
        }
        final Sample s1 = new Sample("Sample 1", null);
        for (int i = 0; i < 3; i++) {
            final TreatmentStep step =
                    new TreatmentStep(10 - i, 20 - i, 30 - i);
            step.setTreatmentType(TreatmentType.THERMAL);
            step.setTemperature(i*100 + 50);
            s1.addTreatmentStep(step);
        }
        Sample.mergeSamples(Arrays.asList(s0, s1));
        assertArrayEquals(
                new double[] {0, 50, 100, 150, 200, 250},
                s0.getTreatmentSteps().stream()
                        .mapToDouble(step -> step.getTemperature()).toArray(),
                delta);
    }
    
    @Test
    public void testMergeSamplesWithEmptyList() {
        Sample.mergeSamples(Collections.emptyList());
    }
    
    @Test
    public void testMergeSamplesWithOneSample() {
        final Sample expectedSample = makeSimpleSample();
        Sample.mergeSamples(Collections.singletonList(simpleSample));
        for (int i=0; i<expectedSample.getNumberOfSteps(); i++) {
            assertTrue(expectedSample.getTreatmentStepByIndex(i).getMoment().
                    equals(simpleSample.getTreatmentStepByIndex(i).getMoment(), delta));
        }
    }
    
}
