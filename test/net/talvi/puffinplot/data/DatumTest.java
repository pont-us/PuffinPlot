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
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.talvi.puffinplot.TestUtils.ListHandler;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author pont
 */
public class DatumTest {
    
    private Datum defaultDatum;
    
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
        assertTrue(handler.oneWarningLogged());
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

}
