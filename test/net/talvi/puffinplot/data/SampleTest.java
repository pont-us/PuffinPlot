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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.TestUtils.ListHandler;

public class SampleTest {

    private static final double delta = 1e-10;
    private final Sample simpleSample;

    public SampleTest() {
        simpleSample = makeSimpleSample();
    }
    
    private static Sample makeSimpleSample() {
        final Sample s = new Sample("Test sample", null);
        for (int i=0; i<10; i++) {
            final Datum d = new Datum(10-i, 20-i, 30-i);
            d.setTreatType(TreatType.DEGAUSS_XYZ);
            d.setAfX(i);
            d.setAfY(i);
            d.setAfZ(i);
            s.addDatum(d);
        }
        return s;
    }
    
    @Test
    public void testSelectByTreatmentLevelRange() {
        
        final double[] mins = {2.5, 5.5, 10.5, Double.NEGATIVE_INFINITY};
        final double[] maxs = {7.5, 6.5, 0, Double.POSITIVE_INFINITY};
        
        for (int i = 0; i < mins.length; i++) {
            simpleSample.selectByTreatmentLevelRange(mins[i], maxs[i]);

            for (Datum d: simpleSample.getData()) {
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
        final String valueString = String.format(Locale.ENGLISH, "%g", value);
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
    public void testSetValueUnhandledField() {
        final ListHandler handler = ListHandler.createAndAdd();
        final Sample sample = new Sample("sample1", null);
        sample.setValue(DatumField.MAG_SUS, "0");
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));
    }
    
    @Test
    public void testSetValueWithData() {
        final DatumField df = DatumField.SAMPLE_AZ;
        final double value = 2.71;
        final Sample sample = new Sample("sample1", null);
        for (int i=0; i<3; i++) {
            final Datum d = new Datum();
            sample.addDatum(d);
        }
        sample.setValue(df, String.format(Locale.ENGLISH, "%g", value));
        assertTrue(sample.getData().stream().allMatch(d ->
                Math.abs(value - Double.parseDouble(d.getValue(df))) < delta
        ));
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
        assertTrue(simpleSample.isSelectionContiguous());        
        for (int i=3; i<8; i++) {
            simpleSample.getDatum(i).setSelected(true);
        }
        assertTrue(simpleSample.isSelectionContiguous());
        simpleSample.getDatum(5).setSelected(false);
        assertFalse(simpleSample.isSelectionContiguous());
        simpleSample.selectAll();
        assertTrue(simpleSample.isSelectionContiguous());
    }
    
    @Test
    public void testGetSelectedData() {
        for (int i=3; i<8; i++) {
            simpleSample.getDatum(i).setSelected(true);
        }
        assertEquals(simpleSample.getData().subList(3, 8),
                simpleSample.getSelectedData());
    }
    
    @Test
    public void testGetSelectionBitSet() {
        final List<Integer> bits = Arrays.asList(1, 3, 6);
        final BitSet expectedBitSet = new BitSet();
        for (Integer i: bits) {
            simpleSample.getDatum(i).setSelected(true);
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
        for (int i=0; i<simpleSample.getNumData(); i++) {
            assertEquals(bitList.contains(i), simpleSample.getDatum(i).isSelected());
        }
    }

    @Test
    public void testHideAndDeselectSelectedPoints() {
        final Set<Datum> dataToHide = new HashSet<>();
        for (int i = 3; i < 7; i++) {
            dataToHide.add(simpleSample.getDatum(i));
        }
        dataToHide.forEach(d -> d.setSelected(true));
        assertTrue(simpleSample.getData().stream().allMatch(d -> !d.isHidden()));
        simpleSample.hideAndDeselectSelectedPoints();
        assertTrue(simpleSample.getData().stream().allMatch(d -> !d.isSelected()));
        assertTrue(simpleSample.getData().stream().allMatch(
                d -> dataToHide.contains(d) == d.isHidden()));
    }
    
    @Test
    public void testUnhideAllPoints() {
        for (int i = 3; i < 7; i++) {
            simpleSample.getDatum(i).setHidden(true);
        }
        simpleSample.unhideAllPoints();
        assertTrue(simpleSample.getData().stream().allMatch(d -> !d.isHidden()));
    }

    @Test
    public void testInvertMoments() {
        final List<Vec3> expected = simpleSample.getData().stream().
                map(d -> d.getMoment().invert()).
                collect(Collectors.toList());
        simpleSample.invertMoments();
        for (int i=0; i<simpleSample.getNumData(); i++) {
            assertTrue(expected.get(i).equals(
                    simpleSample.getDatum(i).getMoment(),
                    delta));
        }
    }
    
    @Test
    public void testSelectVisible() {
        for (int i = 3; i < 7; i++) {
            simpleSample.getDatum(i).setHidden(true);
        }
        simpleSample.selectVisible();
        assertTrue(simpleSample.getData().stream().
                allMatch(d -> d.isHidden() != d.isSelected()));        
    }
    
    @Test
    public void testClearGreatCircle() {
        simpleSample.getData().forEach(d -> d.setOnCircle(true));
        simpleSample.fitGreatCircle(Correction.NONE);
        assertNotNull(simpleSample.getGreatCircle());
        simpleSample.clearGreatCircle();
        assertNull(simpleSample.getGreatCircle());
    }
    
    @Test
    public void testClearCalculations() {
        simpleSample.getData().forEach(d -> {
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
            final List<Vec3> expected = sample.getData().stream().
                    map(d -> d.getMoment().rot180(axis)).
                    collect(Collectors.toList());
            sample.flip(axis);
            for (int i = 0; i < sample.getNumData(); i++) {
                assertTrue(expected.get(i).equals(
                        sample.getDatum(i).getMoment(),
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
    public void testTruncateData() {
        final List<Datum> data = new ArrayList<>(simpleSample.getData());
        simpleSample.truncateData(7);
        assertEquals(data.subList(0, 7), simpleSample.getData());
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
            final Datum d = new Datum();
            d.setTreatType(TreatType.THERMAL);
            d.setTemp(tempsAndms[i]);
            d.setMagSus(tempsAndms[i+1]);
            s.addDatum(d);
        }
        return s;
    }
    
    @Test
    public void testIsPcaAnchoredNoData() {
        assertFalse(new Sample("test", null).isPcaAnchored());
    }
    
    @Test
    public void testIsPcaAnchored() {
        final Datum d = new Datum();
        final Sample s = new Sample("test", null);
        s.addDatum(d);
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
        s.addDatum(new Datum(nrm));
        s.addDatum(new Datum(nrm.times(0.8)));
        s.addDatum(new Datum(nrm.times(0.6)));
        assertEquals(nrm.mag(), s.getNrm(), delta);
    }
    
    @Test
    public void testHasMsData() {
        final Sample sample = new Sample("test", null);
        assertFalse(sample.hasMsData());
        sample.addDatum(new Datum());
        assertFalse(sample.hasMsData());
        final Datum datum = new Datum();
        datum.setMagSus(1);
        sample.addDatum(datum);
        assertTrue(sample.hasMsData());
    }
    
    @Test
    public void testGetMeasType() {
        final Sample sample = new Sample("test", null);
        assertEquals(MeasType.DISCRETE, sample.getMeasType());
        addDatumWithMeasurementType(sample, MeasType.NONE);
        assertEquals(MeasType.DISCRETE, sample.getMeasType());
        addDatumWithMeasurementType(sample, MeasType.UNKNOWN);
        assertEquals(MeasType.DISCRETE, sample.getMeasType());
        addDatumWithMeasurementType(sample, MeasType.UNSET);
        assertEquals(MeasType.DISCRETE, sample.getMeasType());
        addDatumWithMeasurementType(sample, MeasType.CONTINUOUS);
        assertEquals(MeasType.CONTINUOUS, sample.getMeasType());
    }
    
    private static void addDatumWithMeasurementType(Sample sample, MeasType mt) {
        final Datum datum = new Datum(Vec3.NORTH);
        datum.setMeasType(mt);
        sample.addDatum(datum);
    }
    
    @Test
    public void testSetAndGetFormStrike() {
        for (int strike: new int[] {0, 1, 45, 180, 300, 359}) {
            simpleSample.setValue(DatumField.VIRT_FORM_STRIKE,
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
    public void testGetDatumByTreatmentLevel() {
        assertEquals(5,
                simpleSample.getDatumByTreatmentLevel(5).getTreatmentLevel(),
                delta);
    }
    
    @Test
    public void testGetDatumByTreatmentLevelWithNoData() {
        final Sample sample = new Sample("test", null);
        assertNull(sample.getDatumByTreatmentLevel(0));
    }

    @Test
    public void testGetDatumByTreatmentTypeAndLevel() {
        final Datum datum = simpleSample.getDatumByTreatmentTypeAndLevel(
                Collections.singleton(TreatType.DEGAUSS_XYZ), 5);
        assertEquals(5, datum.getTreatmentLevel(), delta);
    }

    @Test
    public void testGetDatumByTreatmentLevelWithNoMatchingData() {
        assertNull(simpleSample.getDatumByTreatmentLevel(17));
    }

    @Test
    public void testGetDatumByTreatmentTypeAndLevelWithNoData() {
        final Sample sample = new Sample("test", null);
        assertNull(sample.getDatumByTreatmentTypeAndLevel(
                new HashSet(Arrays.asList(TreatType.values())), 0));
    }

    @Test
    public void testGetDatumByTreatmentTypeAndLevelWithNoMatchingData() {
        assertNull(simpleSample.getDatumByTreatmentTypeAndLevel(
                Collections.singleton(TreatType.THERMAL), 5));
    }

    @Test
    public void testGetDirectionWithFisherValues() {
        simpleSample.selectAll();
        simpleSample.calculateFisher(Correction.NONE);
        final Vec3 expected =
                FisherValues.calculate(simpleSample.getSelectedData().
                        stream().map(d -> d.getMoment()).
                        collect(Collectors.toList())).getMeanDirection();
        assertTrue(expected.equals(simpleSample.getDirection(), delta));
    }
    
    @Test
    public void testGetCirclePoints() {
        final List<Integer> circlePoints = Arrays.asList(2, 3, 6, 7);
        final List<Vec3> expected =
                circlePoints.stream().map(i -> simpleSample.getDatum(i).
                        getMoment()).collect(Collectors.toList());
        circlePoints.forEach(i -> simpleSample.getDatum(i).setOnCircle(true));
        final List<Vec3> actual = simpleSample.getCirclePoints(Correction.NONE);
        assertEquals(expected.size(), actual.size());
        for (int i=0; i<expected.size(); i++) {
            assertTrue(expected.get(i).equals(actual.get(i), delta));
        }
    }
    
    @Test
    public void testGetFirstRunNumber() {
        IntStream.range(0, simpleSample.getNumData()).
                forEach(i -> simpleSample.getDatum(i).setRunNumber(i+5));
        assertEquals(5, simpleSample.getFirstRunNumber());
    }
    
    @Test
    public void testGetLastRunNumber() {
        IntStream.range(0, simpleSample.getNumData()).
                forEach(i -> simpleSample.getDatum(i).setRunNumber(i+5));
        assertEquals(simpleSample.getNumData() + 4,
                simpleSample.getLastRunNumber());
    }
    
    @Test
    public void testDatumByRunNumber() {
        IntStream.range(0, simpleSample.getNumData()).
                forEach(i -> simpleSample.getDatum(i).setRunNumber(i+5));
        assertEquals(simpleSample.getDatum(5),
                simpleSample.getDatumByRunNumber(11));
    }
    
    @Test
    public void testGetSlotNumber() {
        simpleSample.getDatum(0).setSlotNumber(17);
        assertEquals(17, simpleSample.getSlotNumber());
    }
    
    @Test
    public void testGetVisibleData() {
        simpleSample.getDatum(5).setHidden(true);
        final List<Datum> expected =
                new ArrayList<>(simpleSample.getData());
        expected.remove(5);
        assertEquals(expected, simpleSample.getVisibleData());
    }
    
    @Test
    public void testRemoveData() {
        final Set<Datum> toRemove = Arrays.stream(new int[] {2, 3, 5, 6}).
                mapToObj(i -> simpleSample.getDatum(i)).
                collect(Collectors.toSet());
        final List<Datum> shouldRemain =
                Arrays.stream(new int[] {0, 1, 4, 7, 8, 9}).
                        mapToObj(i -> simpleSample.getDatum(i)).
                        collect(Collectors.toList());
        simpleSample.removeData(toRemove);
        assertEquals(shouldRemain, simpleSample.getData());
    }

    @Test
    public void testMergeDuplicateTreatmentSteps() {
        final Sample sample = new Sample("sample0", null);
        final List<Vec3> vectors = new ArrayList<>();
        final Random rnd = new Random(77);
        for (int step=0; step<3; step++) {
            final Vec3 vector = TestUtils.randomVector(rnd, 1);
            final Datum d = new Datum(vector);
            d.setTreatType(TreatType.THERMAL);
            d.setTemp(50);
            sample.addDatum(d);
            vectors.add(vector);
        }
        final Vec3 expectedMean = Vec3.mean(vectors);
        sample.mergeDuplicateTreatmentSteps();
        assertEquals(1, sample.getData().size());
        assertTrue(expectedMean.equals(sample.getDatum(0).getMoment()));
    }
    
    @Test
    public void testMergeSamplesWithInverseData() {
        final Sample sample0 = makeSimpleSample();
        final Sample sample1 = makeSimpleSample();
        sample1.getData().forEach(d -> d.invertMoment());
        Sample.mergeSamples(Arrays.asList(sample0, sample1));
        sample0.getData().stream().allMatch(d -> d.getIntensity() < delta);
    }
    
    @Test
    public void testMergeSamplesWithOverlappingData() {
        final Sample s0 = new Sample("Sample 0", null);
        for (int i=0; i<3; i++) {
            final Datum d = new Datum(10-i, 20-i, 30-i);
            d.setTreatType(TreatType.THERMAL);
            d.setTemp(i*100);
            s0.addDatum(d);
        }
        final Sample s1 = new Sample("Sample 1", null);
        for (int i=0; i<3; i++) {
            final Datum d = new Datum(10-i, 20-i, 30-i);
            d.setTreatType(TreatType.THERMAL);
            d.setTemp(i*100 + 50);
            s1.addDatum(d);
        }
        Sample.mergeSamples(Arrays.asList(s0, s1));
        assertArrayEquals(
                new double[] {0, 50, 100, 150, 200, 250},
                s0.getData().stream().mapToDouble(d -> d.getTemp()).toArray(),
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
        for (int i=0; i<expectedSample.getNumData(); i++) {
            assertTrue(expectedSample.getDatum(i).getMoment().
                    equals(simpleSample.getDatum(i).getMoment(), delta));
        }
    }
    
}
