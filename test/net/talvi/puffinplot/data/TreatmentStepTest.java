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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.talvi.puffinplot.TestUtils;
import net.talvi.puffinplot.TestUtils.ListHandler;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

public class TreatmentStepTest {
    
    private TreatmentStep defaultTreatmentStep;
    private static final double delta = 1e-10;
    
    @Before
    public void setUp() {
        defaultTreatmentStep = new TreatmentStep();
    }
    
    @Test
    public void testThreeArgumentConstructor() {
        final TreatmentStep d = new TreatmentStep(1, 2, 3);
        assertEquals(new Vec3(1, 2, 3), d.getMoment());
    }
    
    @Test
    public void testOneArgumentConstructor() {
        final Vec3 v = new Vec3(3, 2, 1);
        assertEquals(v, new TreatmentStep(v).getMoment());
    }
    
    @Test
    public void testZeroArgumentConstructor() {
        assertEquals(Vec3.ORIGIN, defaultTreatmentStep.getMoment());
    }
    
    @Test
    public void testSetValueWithBadNumberFormat() {
        final ListHandler handler = ListHandler.createAndAdd();
        new TreatmentStep().setValue(TreatmentStepField.AREA, "not a number", 1);
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetValueWithNonSettableField() {
        new TreatmentStep().setValue(TreatmentStepField.VIRT_MSJUMP, "1.0", 1);
    }
    
    @Test
    public void testGetVirtualFields() {
        final Vec3 vec = Vec3.fromPolarDegrees(12, 34, 56);
        final TreatmentStep treatmentStep = new TreatmentStep(vec);
        assertEquals(vec.getDecDeg(),
                Double.valueOf(treatmentStep.getValue(TreatmentStepField.VIRT_DECLINATION)),
                1e-10);
        assertEquals(vec.getIncDeg(),
                Double.valueOf(treatmentStep.getValue(TreatmentStepField.VIRT_INCLINATION)),
                1e-10);
        assertEquals(vec.mag(),
                Double.valueOf(treatmentStep.getValue(TreatmentStepField.VIRT_MAGNETIZATION)),
                1e-10);
    }
    
    @Test
    public void testSetAndGetValuesDouble() {
        final List<TreatmentStepField> fields = Arrays.asList(
                TreatmentStepField.DEPTH, TreatmentStepField.X_MOMENT, TreatmentStepField.Y_MOMENT,
                TreatmentStepField.Z_MOMENT, TreatmentStepField.MAG_SUS, TreatmentStepField.VOLUME,
                TreatmentStepField.AREA, TreatmentStepField.SAMPLE_AZ, TreatmentStepField.SAMPLE_DIP,
                TreatmentStepField.FORM_AZ, TreatmentStepField.FORM_DIP, TreatmentStepField.MAG_DEV,
                TreatmentStepField.AF_X, TreatmentStepField.AF_Y, TreatmentStepField.AF_Z,
                TreatmentStepField.TEMPERATURE, TreatmentStepField.IRM_FIELD,
                TreatmentStepField.ARM_FIELD, TreatmentStepField.VIRT_SAMPLE_HADE,
                TreatmentStepField.VIRT_FORM_STRIKE
        );
        
        final double value = 30;
        final String valueString = Double.toString(value);
        final TreatmentStep treatmentStep = new TreatmentStep();
        for (TreatmentStepField field: fields) {
            treatmentStep.setValue(field, valueString, 1);
            assertEquals(value, Double.parseDouble(treatmentStep.getValue(field)),
                    1e-10);
        }
    }
    
    @Test
    public void testHasMagMoment() {
        final TreatmentStep treatmentStep = new TreatmentStep(null);
        assertFalse(treatmentStep.hasMagMoment());
        treatmentStep.setMoment(Vec3.EAST);
        assertTrue(treatmentStep.hasMagMoment());
    }
    
    @Test
    public void testToggleSel() {
        assertFalse(defaultTreatmentStep.isSelected());
        defaultTreatmentStep.toggleSel();
        assertTrue(defaultTreatmentStep.isSelected());
        defaultTreatmentStep.toggleSel();
        assertFalse(defaultTreatmentStep.isSelected());
    }
    
    @Test
    public void testInvertMoment() {
        final TreatmentStep treatmentStep = new TreatmentStep(Vec3.NORTH);
        treatmentStep.invertMoment();
        assertEquals(Vec3.NORTH.invert(), treatmentStep.getMoment());
    }
    
    @Test
    public void testRot180() {
        final TreatmentStep treatmentStep = new TreatmentStep(Vec3.NORTH);
        treatmentStep.rot180(MeasurementAxis.X); // should do nothing
        assertEquals(Vec3.NORTH, treatmentStep.getMoment());
        treatmentStep.rot180(MeasurementAxis.Z);
        assertEquals(Vec3.NORTH.invert(), treatmentStep.getMoment());
        treatmentStep.setMoment(Vec3.DOWN);
        treatmentStep.rot180(MeasurementAxis.X);
        assertEquals(Vec3.DOWN.invert(), treatmentStep.getMoment());
    }
    
    @Test
    public void testSetAndGetSuite() {
        assertEquals(null, defaultTreatmentStep.getSuite());
        final Suite suite = new Suite("test");
        defaultTreatmentStep.setSuite(suite);
        assertEquals(suite, defaultTreatmentStep.getSuite());
    }
    
    @Test
    public void testGetFormattedTreatmentLevelThermal() {
        final TreatmentStep thermal = new TreatmentStep();
        thermal.setTreatType(TreatType.THERMAL);
        thermal.setTemp(70);
        assertEquals("70", thermal.getFormattedTreatmentLevel());
    }

    @Test
    public void testGetFormattedTreatmentLevelAf() {
        final TreatmentStep af = new TreatmentStep();
        af.setTreatType(TreatType.DEGAUSS_Z);
        af.setAfZ(0.6);
        assertEquals("600", af.getFormattedTreatmentLevel());
    }

    @Test
    public void testMaxTreatmentLevel() {
        final List<TreatmentStep> data = new ArrayList<>();
        for (double tl: new double[] {20, 80, 50}) {
            final TreatmentStep treatmentStep = new TreatmentStep();
            treatmentStep.setTreatType(TreatType.THERMAL);
            treatmentStep.setTemp(tl);
            data.add(treatmentStep);
        }
        assertEquals(80, TreatmentStep.maxTreatmentLevel(data), delta);
    }

    @Test
    public void testMaxIntensity() {
        final List<TreatmentStep> data = new ArrayList<>();
        for (double intensity: new double[] {3, 1, 4, 1, 5, 9, 2, 6}) {
            final TreatmentStep treatmentStep = new TreatmentStep(intensity, 0, 0);
            data.add(treatmentStep);
        }
        assertEquals(9, TreatmentStep.maxIntensity(data), delta);
    }

    @Test
    public void testMaxMagSus() {
        final List<TreatmentStep> data = new ArrayList<>();
        for (double magSus:
                new double[] {3, 1, 4, 1, 5, Double.NaN, 9, 2, 6, 5}) {
            final TreatmentStep treatmentStep = new TreatmentStep();
            treatmentStep.setMagSus(magSus);
            data.add(treatmentStep);
        }
        assertEquals(9, TreatmentStep.maxMagSus(data), delta);
    }
    
    @Test
    public void testIsMagSusOnly() {
        final TreatmentStep withMoment = new TreatmentStep(1, 2, 3);
        assertFalse(withMoment.isMagSusOnly());
        withMoment.setMagSus(7);
        assertFalse(withMoment.isMagSusOnly());
        final TreatmentStep noMoment = new TreatmentStep(null);
        assertFalse(noMoment.isMagSusOnly());
        noMoment.setMagSus(7);
        assertTrue(noMoment.isMagSusOnly());
    }
    
    @Test
    public void testSetAndGetArmField() {
        final TreatmentStep d = new TreatmentStep();
        final double armField = 77;
        d.setArmField(armField);
        assertEquals(armField, d.getArmField(), delta);
    }
    
    @Test
    public void testSetAndGetArmAxis() {
        final TreatmentStep d = new TreatmentStep();
        for (ArmAxis axis: ArmAxis.values()) {
            d.setArmAxis(axis);
            assertEquals(axis, d.getArmAxis());
        }
    }
    
    @Test
    public void testGetTreatmentStepArm() {
        testGetTreatmentStep(TreatType.ARM, TreatmentStep::setAfZ);
    }
    
    @Test
    public void testGetTreatmentStepDegaussXyz() {
        testGetTreatmentStep(TreatType.DEGAUSS_XYZ, TreatmentStep::setAfZ);
    }
    
    @Test
    public void testGetTreatmentStepDegaussZ() {
        testGetTreatmentStep(TreatType.DEGAUSS_Z, TreatmentStep::setAfZ);
    }
    
    @Test
    public void testGetTreatmentStepIRM() {
        testGetTreatmentStep(TreatType.IRM, TreatmentStep::setIrmField);
    }
    
    @Test
    public void testGetTreatmentStepThermal() {
        testGetTreatmentStep(TreatType.THERMAL, TreatmentStep::setTemp);
    }
    
    private static void testGetTreatmentStep(TreatType type,
            ObjDoubleConsumer<TreatmentStep> setValue) {
        final TreatmentStep d = new TreatmentStep();
        for (double value: new double[] {0, 0.01, 17, 529}) {
            d.setTreatType(type);
            setValue.accept(d, value);
            assertEquals(value, d.getTreatmentStep(), delta);
        }
    }
    
    @Test
    public void testSetAndGetSampAz() {
        testDoubleSetterAndGetter(TreatmentStep::setSampAz, TreatmentStep::getSampAz);
    }
    
    @Test
    public void testSetAndGetSampDip() {
        testDoubleSetterAndGetter(TreatmentStep::setSampDip, TreatmentStep::getSampDip);
    }
    
    @Test
    public void testSetAndGetSampHade() {
        testDoubleSetterAndGetter(TreatmentStep::setSampHade, TreatmentStep::getSampHade);
    }
    
    @Test
    public void testSetAndGetAfX() {
        testDoubleSetterAndGetter(TreatmentStep::setAfX, TreatmentStep::getAfX);
    }
    
    @Test
    public void testSetAndGetAfY() {
        testDoubleSetterAndGetter(TreatmentStep::setAfY, TreatmentStep::getAfY);
    }
    
    @Test
    public void testSetAndGetAfZ() {
        testDoubleSetterAndGetter(TreatmentStep::setAfZ, TreatmentStep::getAfZ);
    }
    
    @Test
    public void testSetAndGetArea() {
        testDoubleSetterAndGetter(TreatmentStep::setArea, TreatmentStep::getArea);
    }
    
    @Test
    public void testSetAndGetMagSus() {
        testDoubleSetterAndGetter(TreatmentStep::setMagSus, TreatmentStep::getMagSus);
    }
    
    @Test
    public void testSetAndGetVolume() {
        testDoubleSetterAndGetter(TreatmentStep::setVolume, TreatmentStep::getVolume);
    }

    @Test
    public void testSetAndGetFormAz() {
        testDoubleSetterAndGetter(TreatmentStep::setFormAz, TreatmentStep::getFormAz);
    }
    
    @Test
    public void testSetAndGetFormDip() {
        testDoubleSetterAndGetter(TreatmentStep::setFormDip, TreatmentStep::getFormDip);
    }
    
    @Test
    public void testSetAndGetFormStrike() {
        testDoubleSetterAndGetter(TreatmentStep::setFormStrike, TreatmentStep::getFormStrike);
    }
    
    @Test
    public void testSetAndGetXDrift() {
        testDoubleSetterAndGetter(TreatmentStep::setXDrift, TreatmentStep::getXDrift);
    }
    
    @Test
    public void testSetAndGetYDrift() {
        testDoubleSetterAndGetter(TreatmentStep::setYDrift, TreatmentStep::getYDrift);
    }
    
    @Test
    public void testSetAndGetZDrift() {
        testDoubleSetterAndGetter(TreatmentStep::setZDrift, TreatmentStep::getZDrift);
    }
    
    private void testDoubleSetterAndGetter(ObjDoubleConsumer<TreatmentStep> setter,
            ToDoubleFunction<TreatmentStep> getter) {
        final Suite suite = new Suite("test");
        final TreatmentStep treatmentStep = new TreatmentStep();
        treatmentStep.setSuite(suite);
        for (double value: new double[] {0, 0.01, 10.01, 17, 129, 280}) {
            suite.setSaved(true);
            setter.accept(treatmentStep, value);
            assertFalse(suite.isSaved());
            assertEquals(value, getter.applyAsDouble(treatmentStep), delta);
        }
    }
    
    @Test
    public void testSetAndGetRunNumber() {
        testIntSetterAndGetter(TreatmentStep::setRunNumber, TreatmentStep::getRunNumber);
    }
    
    @Test
    public void testSetAndGetSlotNumber() {
        testIntSetterAndGetter(TreatmentStep::setSlotNumber, TreatmentStep::getSlotNumber);
    }
    
    private void testIntSetterAndGetter(ObjIntConsumer<TreatmentStep> setter,
            ToIntFunction<TreatmentStep> getter) {
        final Suite suite = new Suite("test");
        final TreatmentStep treatmentStep = new TreatmentStep();
        treatmentStep.setSuite(suite);
        for (int value: new int[] {-1, 0, 5, 10, 12, 9997}) {
            suite.setSaved(true);
            setter.accept(treatmentStep, value);
            assertFalse(suite.isSaved());
            assertEquals(value, getter.applyAsInt(treatmentStep), delta);
        }
    }
    
    @Test
    public void testIgnoreOnLoading() {
        for (MeasType mt: MeasType.values()) {
            final TreatmentStep d = new TreatmentStep();
            d.setMeasType(mt);
            assertEquals(mt == MeasType.NONE, d.ignoreOnLoading());
        }
    }

    @Test
    public void testSetAndGetLine() {
        final TreatmentStep d = new TreatmentStep();
        final Line line = new Line(17);
        d.setLine(line);
        assertSame(line, d.getLine());
    }

    @Test
    public void testSetAndGetTimestamp() {
        final TreatmentStep d = new TreatmentStep();
        final String timestamp = "An arbitrary string";
        d.setTimestamp(timestamp);
        assertEquals(timestamp, d.getTimestamp());
    }
    
    @Test(expected = NullPointerException.class)
    public void testSetMomentToMeanNull() {
        new TreatmentStep().setMomentToMean(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetMomentToMeanEmpty() {
        new TreatmentStep().setMomentToMean(Collections.emptyList());
    }
    
    @Test
    public void testSetMomentToMean() {
        final TreatmentStep target = new TreatmentStep();
        final Random rnd = new Random(83);
        for (int nVectors = 1; nVectors < 10; nVectors++) {
            final List<Vec3> vectors = IntStream.range(0, nVectors).
                    mapToObj(i -> TestUtils.randomVector(rnd, 1)).
                    collect(Collectors.toList());
            final List<TreatmentStep> data = vectors.stream().map(v -> new TreatmentStep(v)).
                    collect(Collectors.toList());
            target.setMomentToMean(data);
            assertTrue(Vec3.mean(vectors).equals(target.getMoment(), delta));
        }
    }
        
    @Test
    public void testGetTreatmentLevelIrm() {
        final TreatmentStep d = new TreatmentStep();
        d.setTreatType(TreatType.IRM);
        d.setIrmField(0.7);
        assertEquals(0.7, d.getTreatmentLevel(), delta);
    }
    
    @Test
    public void testGetValueMsJump() {
        final TreatmentStep d = new TreatmentStep();
        d.setSample(new Sample("test", null) {
            @Override
            public double getMagSusJump() {
                return 17;
            }
        });
        assertEquals("17.0", d.getValue(TreatmentStepField.VIRT_MSJUMP));
    }
    
    @Test
    public void testToSummaryStringThermalAndDegauss() {
        final Sample sample = new Sample("test", null);
        final TreatmentStep d0 = makeSimpleDatum(TreatType.THERMAL,
                75, 24.1, -23.2, 7.123e-3);
        final TreatmentStep d1 = makeSimpleDatum(TreatType.DEGAUSS_XYZ,
                0.35, 0.0, 3.5, 1.45e-2);
        sample.addDatum(d0);
        sample.addDatum(d1);
        d0.setMagSus(1.234);
        assertEquals("Step 1/2 | Heating, 75.0°C | "
                + "Dec: 24.1° | Inc: −23.2° | Mag: 7.12e−03 A/m | MS: 1.23",
                d0.toSummaryString());
        assertEquals("Step 2/2 | 3-axis degauss, 350 mT | "
                + "Dec: 0.0° | Inc: 3.5° | Mag: 1.45e−02 A/m",
                d1.toSummaryString());
    }
    
    @Test
    public void testToSummaryStringNoneAndIrm() {
        final Sample sample = new Sample("test", null);
        final TreatmentStep d0 = makeSimpleDatum(TreatType.NONE,
                75, 24.1, -23.2, 7.123e-3);
        final TreatmentStep d1 = makeSimpleDatum(TreatType.IRM,
                0.35, 0.0, 3.5, 1.45e-2);
        sample.addDatum(d0);
        sample.addDatum(d1);
        d0.setMagSus(1.234);
        assertEquals("Step 1/2 | No treatment | "
                + "Dec: 24.1° | Inc: −23.2° | Mag: 7.12e−03 A/m | MS: 1.23",
                d0.toSummaryString());
        assertEquals("Step 2/2 | IRM, 350 mT | "
                + "Dec: 0.0° | Inc: 3.5° | Mag: 1.45e−02 A/m",
                d1.toSummaryString());
    }
    
    private static TreatmentStep makeSimpleDatum(TreatType treatType,
                                                 double level, double dec, double inc, double moment) {
        final TreatmentStep d = new TreatmentStep();
        d.setTreatType(treatType);
        switch (treatType) {
            case NONE:
            case UNKNOWN:
                break;
            case THERMAL:
                d.setTemp(level);
                break;
            case IRM:
                d.setIrmField(level);
                break;
            case DEGAUSS_XYZ:
                d.setAfX(level);
                d.setAfY(level);
                // fall-through intentional
            case DEGAUSS_Z:
                d.setAfZ(level);
                break;
            default:
                throw new Error("Unhandled treatment type "+treatType);
                
        }
        d.setMoment(Vec3.fromPolarDegrees(moment, inc, dec));
        return d;
    }
}
