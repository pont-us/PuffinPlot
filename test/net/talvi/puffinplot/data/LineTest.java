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
public class LineTest {
    
    @Test
    public void testGetEmptySlot() {
        final Line line = new Line(17);

        final int emptySlotIndex = 7;
        Sample emptySlotSample = null;
        for (int sampleIndex = 0; sampleIndex<10; sampleIndex++) {
            final Sample sample = new Sample("Test sample", null);
            if (sampleIndex == emptySlotIndex) {
                emptySlotSample = sample;
            }
            for (int i = 0; i < 10; i++) {
                final Datum d = new Datum(10 - i, 20 - i, 30 - i);
                sample.addDatum(d);
                line.add(d);
            }
        }
        
        assertNull(line.getEmptySlot());
        emptySlotSample.setEmptySlot(true);
        assertEquals(emptySlotSample, line.getEmptySlot().getSample());

    }
}
