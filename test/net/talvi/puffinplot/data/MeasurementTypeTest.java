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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author pont
 */
public class MeasurementTypeTest {
    
    @Test
    public void testValueOf() {
        for (MeasurementType mt: MeasurementType.values()) {
            assertEquals(mt, MeasurementType.valueOf(mt.toString()));
        }
    }

    @Test
    public void testFromString() {
        assertEquals(MeasurementType.CONTINUOUS,
                MeasurementType.fromString("*** CoNtInUoUs   ***"));
        assertEquals(MeasurementType.DISCRETE,
                MeasurementType.fromString("Anything works, as long"
                        + "as the word \"DIScrete\"  is in there"
                        + "somewhere."));
        assertEquals(MeasurementType.NONE,
                MeasurementType.fromString("NA"));
        assertEquals(MeasurementType.UNKNOWN,
                MeasurementType.fromString("any old gibberish"));
    }

    @Test
    public void testGetColumnHeader() {
        assertEquals("Sample", MeasurementType.DISCRETE.getColumnHeader());
        assertEquals("Depth", MeasurementType.CONTINUOUS.getColumnHeader());
    }

    @Test
    public void testIsActualMeasurement() {
        for (MeasurementType mt: MeasurementType.values()) {
            assertEquals(mt == MeasurementType.CONTINUOUS || mt == MeasurementType.DISCRETE,
                    mt.isActualMeasurement());
        }
    }

    @Test
    public void testIsDiscrete() {
        for (MeasurementType mt: MeasurementType.values()) {
            assertEquals(mt == MeasurementType.DISCRETE, mt.isDiscrete());
        }
    }

    @Test
    public void testIsContinuous() {
        for (MeasurementType mt: MeasurementType.values()) {
            assertEquals(mt == MeasurementType.CONTINUOUS, mt.isContinuous());
        }
    }

    @Test
    public void testGetNiceName() {
        assertEquals("Continuous", MeasurementType.CONTINUOUS.getNiceName());
    }
    
}
