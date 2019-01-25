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

public class DatumTest {
    
    private Datum defaultDatum;
    private static final double delta = 1e-10;
    
    @Before
    public void setUp() {
        defaultDatum = new Datum();
    }
    
    @Test
    public void testThreeArgumentConstructor() {
        final Datum d = new Datum(1, 2, 3);
        assertEquals(new Vec3(1, 2, 3), d.getMoment());
    }
    
    @Test
    public void testOneArgumentConstructor() {
        final Vec3 v = new Vec3(3, 2, 1);
        assertEquals(v, new Datum(v).getMoment());
    }
    
    @Test
    public void testZeroArgumentConstructor() {
        assertEquals(Vec3.ORIGIN, defaultDatum.getMoment());
    }
    
    @Test
    public void testSetValueWithBadNumberFormat() {
        final ListHandler handler = ListHandler.createAndAdd();
        new Datum().setValue(DatumField.AREA, "not a number", 1);
        assertTrue(handler.wasOneMessageLogged(Level.WARNING));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetValueWithNonSettableField() {
        new Datum().setValue(DatumField.VIRT_MSJUMP, "1.0", 1);
    }
    
    @Test
    public void testGetVirtualFields() {
        final Vec3 vec = Vec3.fromPolarDegrees(12, 34, 56);
        final Datum datum = new Datum(vec);
        assertEquals(vec.getDecDeg(),
                Double.valueOf(datum.getValue(DatumField.VIRT_DECLINATION)),
                1e-10);
        assertEquals(vec.getIncDeg(),
                Double.valueOf(datum.getValue(DatumField.VIRT_INCLINATION)),
                1e-10);
        assertEquals(vec.mag(),
                Double.valueOf(datum.getValue(DatumField.VIRT_MAGNETIZATION)),
                1e-10);
    }
    
    @Test
    public void testSetAndGetValuesDouble() {
        final List<DatumField> fields = Arrays.asList(
                DatumField.DEPTH, DatumField.X_MOMENT, DatumField.Y_MOMENT,
                DatumField.Z_MOMENT, DatumField.MAG_SUS, DatumField.VOLUME,
                DatumField.AREA, DatumField.SAMPLE_AZ, DatumField.SAMPLE_DIP,
                DatumField.FORM_AZ, DatumField.FORM_DIP, DatumField.MAG_DEV,
                DatumField.AF_X, DatumField.AF_Y, DatumField.AF_Z,
                DatumField.TEMPERATURE, DatumField.IRM_FIELD,
                DatumField.ARM_FIELD, DatumField.VIRT_SAMPLE_HADE,
                DatumField.VIRT_FORM_STRIKE
        );
        
        final double value = 30;
        final String valueString = Double.toString(value);
        final Datum datum = new Datum();
        for (DatumField field: fields) {
            datum.setValue(field, valueString, 1);
            assertEquals(value, Double.parseDouble(datum.getValue(field)),
                    1e-10);
        }
    }
    
    @Test
    public void testHasMagMoment() {
        final Datum datum = new Datum(null);
        assertFalse(datum.hasMagMoment());
        datum.setMoment(Vec3.EAST);
        assertTrue(datum.hasMagMoment());
    }
    
    @Test
    public void testToggleSel() {
        assertFalse(defaultDatum.isSelected());
        defaultDatum.toggleSel();
        assertTrue(defaultDatum.isSelected());
        defaultDatum.toggleSel();
        assertFalse(defaultDatum.isSelected());        
    }
    
    @Test
    public void testInvertMoment() {
        final Datum datum = new Datum(Vec3.NORTH);
        datum.invertMoment();
        assertEquals(Vec3.NORTH.invert(), datum.getMoment());
    }
    
    @Test
    public void testRot180() {
        final Datum datum = new Datum(Vec3.NORTH);
        datum.rot180(MeasurementAxis.X); // should do nothing
        assertEquals(Vec3.NORTH, datum.getMoment());
        datum.rot180(MeasurementAxis.Z);
        assertEquals(Vec3.NORTH.invert(), datum.getMoment());
        datum.setMoment(Vec3.DOWN);
        datum.rot180(MeasurementAxis.X);
        assertEquals(Vec3.DOWN.invert(), datum.getMoment());
    }
    
    @Test
    public void testSetAndGetSuite() {
        assertEquals(null, defaultDatum.getSuite());
        final Suite suite = new Suite("test");
        defaultDatum.setSuite(suite);
        assertEquals(suite, defaultDatum.getSuite());
    }
    
    @Test
    public void testGetFormattedTreatmentLevelThermal() {
        final Datum thermal = new Datum();
        thermal.setTreatType(TreatType.THERMAL);
        thermal.setTemp(70);
        assertEquals("70", thermal.getFormattedTreatmentLevel());
    }

    @Test
    public void testGetFormattedTreatmentLevelAf() {
        final Datum af = new Datum();
        af.setTreatType(TreatType.DEGAUSS_Z);
        af.setAfZ(0.6);
        assertEquals("600", af.getFormattedTreatmentLevel());
    }

    @Test
    public void testMaxTreatmentLevel() {
        final List<Datum> data = new ArrayList<>();
        for (double tl: new double[] {20, 80, 50}) {
            final Datum datum = new Datum();
            datum.setTreatType(TreatType.THERMAL);
            datum.setTemp(tl);
            data.add(datum);
        }
        assertEquals(80, Datum.maxTreatmentLevel(data), delta);
    }

    @Test
    public void testMaxIntensity() {
        final List<Datum> data = new ArrayList<>();
        for (double intensity: new double[] {3, 1, 4, 1, 5, 9, 2, 6}) {
            final Datum datum = new Datum(intensity, 0, 0);
            data.add(datum);
        }
        assertEquals(9, Datum.maxIntensity(data), delta);
    }

    @Test
    public void testMaxMagSus() {
        final List<Datum> data = new ArrayList<>();
        for (double magSus:
                new double[] {3, 1, 4, 1, 5, Double.NaN, 9, 2, 6, 5}) {
            final Datum datum = new Datum();
            datum.setMagSus(magSus);
            data.add(datum);
        }
        assertEquals(9, Datum.maxMagSus(data), delta);
    }
    
    @Test
    public void testIsMagSusOnly() {
        final Datum withMoment = new Datum(1, 2, 3);
        assertFalse(withMoment.isMagSusOnly());
        withMoment.setMagSus(7);
        assertFalse(withMoment.isMagSusOnly());
        final Datum noMoment = new Datum(null);
        assertFalse(noMoment.isMagSusOnly());
        noMoment.setMagSus(7);
        assertTrue(noMoment.isMagSusOnly());
    }
    
    @Test
    public void testSetAndGetArmField() {
        final Datum d = new Datum();
        final double armField = 77;
        d.setArmField(armField);
        assertEquals(armField, d.getArmField(), delta);
    }
    
    @Test
    public void testSetAndGetArmAxis() {
        final Datum d = new Datum();
        for (ArmAxis axis: ArmAxis.values()) {
            d.setArmAxis(axis);
            assertEquals(axis, d.getArmAxis());
        }
    }
    
    @Test
    public void testGetTreatmentStepArm() {
        testGetTreatmentStep(TreatType.ARM, Datum::setAfZ);
    }
    
    @Test
    public void testGetTreatmentStepDegaussXyz() {
        testGetTreatmentStep(TreatType.DEGAUSS_XYZ, Datum::setAfZ);
    }
    
    @Test
    public void testGetTreatmentStepDegaussZ() {
        testGetTreatmentStep(TreatType.DEGAUSS_Z, Datum::setAfZ);
    }
    
    @Test
    public void testGetTreatmentStepIRM() {
        testGetTreatmentStep(TreatType.IRM, Datum::setIrmField);
    }
    
    @Test
    public void testGetTreatmentStepThermal() {
        testGetTreatmentStep(TreatType.THERMAL, Datum::setTemp);
    }
    
    private static void testGetTreatmentStep(TreatType type,
            ObjDoubleConsumer<Datum> setValue) {
        final Datum d = new Datum();
        for (double value: new double[] {0, 0.01, 17, 529}) {
            d.setTreatType(type);
            setValue.accept(d, value);
            assertEquals(value, d.getTreatmentStep(), delta);
        }
    }
    
    @Test
    public void testSetAndGetSampAz() {
        testDoubleSetterAndGetter(Datum::setSampAz, Datum::getSampAz);
    }
    
    @Test
    public void testSetAndGetSampDip() {
        testDoubleSetterAndGetter(Datum::setSampDip, Datum::getSampDip);
    }
    
    @Test
    public void testSetAndGetSampHade() {
        testDoubleSetterAndGetter(Datum::setSampHade, Datum::getSampHade);
    }
    
    @Test
    public void testSetAndGetAfX() {
        testDoubleSetterAndGetter(Datum::setAfX, Datum::getAfX);
    }
    
    @Test
    public void testSetAndGetAfY() {
        testDoubleSetterAndGetter(Datum::setAfY, Datum::getAfY);
    }
    
    @Test
    public void testSetAndGetAfZ() {
        testDoubleSetterAndGetter(Datum::setAfZ, Datum::getAfZ);
    }
    
    @Test
    public void testSetAndGetArea() {
        testDoubleSetterAndGetter(Datum::setArea, Datum::getArea);
    }
    
    @Test
    public void testSetAndGetMagSus() {
        testDoubleSetterAndGetter(Datum::setMagSus, Datum::getMagSus);
    }
    
    @Test
    public void testSetAndGetVolume() {
        testDoubleSetterAndGetter(Datum::setVolume, Datum::getVolume);
    }

    @Test
    public void testSetAndGetFormAz() {
        testDoubleSetterAndGetter(Datum::setFormAz, Datum::getFormAz);
    }
    
    @Test
    public void testSetAndGetFormDip() {
        testDoubleSetterAndGetter(Datum::setFormDip, Datum::getFormDip);
    }
    
    @Test
    public void testSetAndGetFormStrike() {
        testDoubleSetterAndGetter(Datum::setFormStrike, Datum::getFormStrike);
    }
    
    @Test
    public void testSetAndGetXDrift() {
        testDoubleSetterAndGetter(Datum::setXDrift, Datum::getXDrift);
    }
    
    @Test
    public void testSetAndGetYDrift() {
        testDoubleSetterAndGetter(Datum::setYDrift, Datum::getYDrift);
    }
    
    @Test
    public void testSetAndGetZDrift() {
        testDoubleSetterAndGetter(Datum::setZDrift, Datum::getZDrift);
    }
    
    private void testDoubleSetterAndGetter(ObjDoubleConsumer<Datum> setter,
            ToDoubleFunction<Datum> getter) {
        final Suite suite = new Suite("test");
        final Datum datum = new Datum();
        datum.setSuite(suite);
        for (double value: new double[] {0, 0.01, 10.01, 17, 129, 280}) {
            suite.setSaved(true);
            setter.accept(datum, value);
            assertFalse(suite.isSaved());
            assertEquals(value, getter.applyAsDouble(datum), delta);
        }
    }
    
    @Test
    public void testSetAndGetRunNumber() {
        testIntSetterAndGetter(Datum::setRunNumber, Datum::getRunNumber);
    }
    
    @Test
    public void testSetAndGetSlotNumber() {
        testIntSetterAndGetter(Datum::setSlotNumber, Datum::getSlotNumber);
    }
    
    private void testIntSetterAndGetter(ObjIntConsumer<Datum> setter,
            ToIntFunction<Datum> getter) {
        final Suite suite = new Suite("test");
        final Datum datum = new Datum();
        datum.setSuite(suite);
        for (int value: new int[] {-1, 0, 5, 10, 12, 9997}) {
            suite.setSaved(true);
            setter.accept(datum, value);
            assertFalse(suite.isSaved());
            assertEquals(value, getter.applyAsInt(datum), delta);
        }
    }
    
    @Test
    public void testIgnoreOnLoading() {
        for (MeasType mt: MeasType.values()) {
            final Datum d = new Datum();
            d.setMeasType(mt);
            assertEquals(mt == MeasType.NONE, d.ignoreOnLoading());
        }
    }

    @Test
    public void testSetAndGetLine() {
        final Datum d = new Datum();
        final Line line = new Line(17);
        d.setLine(line);
        assertSame(line, d.getLine());
    }

    @Test
    public void testSetAndGetTimestamp() {
        final Datum d = new Datum();
        final String timestamp = "An arbitrary string";
        d.setTimestamp(timestamp);
        assertEquals(timestamp, d.getTimestamp());
    }
    
    @Test(expected = NullPointerException.class)
    public void testSetMomentToMeanNull() {
        new Datum().setMomentToMean(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetMomentToMeanEmpty() {
        new Datum().setMomentToMean(Collections.emptyList());
    }
    
    @Test
    public void testSetMomentToMean() {
        final Datum target = new Datum();
        final Random rnd = new Random(83);
        for (int nVectors = 1; nVectors < 10; nVectors++) {
            final List<Vec3> vectors = IntStream.range(0, nVectors).
                    mapToObj(i -> TestUtils.randomVector(rnd, 1)).
                    collect(Collectors.toList());
            final List<Datum> data = vectors.stream().map(v -> new Datum(v)).
                    collect(Collectors.toList());
            target.setMomentToMean(data);
            assertTrue(Vec3.mean(vectors).equals(target.getMoment(), delta));
        }
    }
        
    @Test
    public void testGetTreatmentLevelIrm() {
        final Datum d = new Datum();
        d.setTreatType(TreatType.IRM);
        d.setIrmField(0.7);
        assertEquals(0.7, d.getTreatmentLevel(), delta);
    }
    
    @Test
    public void testGetValueMsJump() {
        final Datum d = new Datum();
        d.setSample(new Sample("test", null) {
            @Override
            public double getMagSusJump() {
                return 17;
            }
        });
        assertEquals("17.0", d.getValue(DatumField.VIRT_MSJUMP));
    }
    
    @Test
    public void testToSummaryStringThermalAndDegauss() {
        final Sample sample = new Sample("test", null);
        final Datum d0 = makeSimpleDatum(TreatType.THERMAL,
                75, 24.1, -23.2, 7.123e-3);
        final Datum d1 = makeSimpleDatum(TreatType.DEGAUSS_XYZ,
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
        final Datum d0 = makeSimpleDatum(TreatType.NONE,
                75, 24.1, -23.2, 7.123e-3);
        final Datum d1 = makeSimpleDatum(TreatType.IRM,
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
    
    private static Datum makeSimpleDatum(TreatType treatType,
            double level, double dec, double inc, double moment) {
        final Datum d = new Datum();
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
