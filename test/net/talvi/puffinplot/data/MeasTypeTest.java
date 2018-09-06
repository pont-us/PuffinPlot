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
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class MeasTypeTest {
    
    @Test
    public void testValueOf() {
        for (MeasType mt: MeasType.values()) {
            assertEquals(mt, MeasType.valueOf(mt.toString()));
        }
    }

    @Test
    public void testFromString() {
        assertEquals(MeasType.CONTINUOUS,
                MeasType.fromString("*** CoNtInUoUs   ***"));
        assertEquals(MeasType.DISCRETE,
                MeasType.fromString("Anything works, as long"
                        + "as the word \"DIScrete\"  is in there"
                        + "somewhere."));
        assertEquals(MeasType.NONE,
                MeasType.fromString("NA"));
        assertEquals(MeasType.UNKNOWN,
                MeasType.fromString("any old gibberish"));
    }

    @Test
    public void testGetColumnHeader() {
        assertEquals("Sample", MeasType.DISCRETE.getColumnHeader());
        assertEquals("Depth", MeasType.CONTINUOUS.getColumnHeader());
    }

    @Test
    public void testIsActualMeasurement() {
        for (MeasType mt: MeasType.values()) {
            assertEquals(mt == MeasType.CONTINUOUS || mt == MeasType.DISCRETE,
                    mt.isActualMeasurement());
        }
    }

    @Test
    public void testIsDiscrete() {
        for (MeasType mt: MeasType.values()) {
            assertEquals(mt == MeasType.DISCRETE, mt.isDiscrete());
        }
    }

    @Test
    public void testIsContinuous() {
        for (MeasType mt: MeasType.values()) {
            assertEquals(mt == MeasType.CONTINUOUS, mt.isContinuous());
        }
    }

    @Test
    public void testGetNiceName() {
        assertEquals("Continuous", MeasType.CONTINUOUS.getNiceName());
    }
    
}
