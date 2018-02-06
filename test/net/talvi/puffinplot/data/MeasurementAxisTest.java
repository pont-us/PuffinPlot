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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.talvi.puffinplot.plots.Direction;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class MeasurementAxisTest {
    
    @Test
    public void testValues() {
        /* There's little point in exhaustively checking the values, so
         * we just sanity check that there are at least three of them.
         */
        assertTrue(MeasurementAxis.values().length >= 3);
    }

    @Test
    public void testValueOf() {
        for (MeasurementAxis axis: MeasurementAxis.values()) {
            assertEquals(axis, MeasurementAxis.valueOf(axis.toString()));
        }
    }

    @Test
    public void testOpposite() {
        final MeasurementAxis[][] pairArrays = {
            {MeasurementAxis.X, MeasurementAxis.MINUSX},
            {MeasurementAxis.Y, MeasurementAxis.MINUSY},
            {MeasurementAxis.Z, MeasurementAxis.MINUSZ},
            {MeasurementAxis.H, null},
        };
        
        final Set<Set<MeasurementAxis>> pairSets = new HashSet<>();
        for (MeasurementAxis[] pair: pairArrays) {
            pairSets.add(Arrays.stream(pair).collect(Collectors.toSet()));
        }
        
        for (MeasurementAxis axis: MeasurementAxis.values()) {
            final Set<MeasurementAxis> pair = Arrays.stream(
                    new MeasurementAxis[] {axis, axis.opposite()}).
                    collect(Collectors.toSet());
            assert(pairSets.contains(pair));
        }
    }

    @Test
    public void testGetDirection() {
        for (MeasurementAxis axis: MeasurementAxis.values()) {
            final Direction direction = axis.getDirection();
            switch (axis) {
                case X:
                    assertEquals(Direction.UP, direction);
                    break;
                case Y:
                    assertEquals(Direction.RIGHT, direction);
                    break;
                case MINUSX:
                    assertEquals(Direction.DOWN, direction);
                    break;
                case MINUSY:
                    assertEquals(Direction.LEFT, direction);
                    break;
                default:
                    assertEquals(null, direction);
            }
        }
    }
}
