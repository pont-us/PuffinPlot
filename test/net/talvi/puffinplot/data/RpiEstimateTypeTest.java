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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author pont
 */
public class RpiEstimateTypeTest {
    
    @Test
    public void testValues() {
        final Set<RpiEstimateType> actual =
                Arrays.asList(RpiEstimateType.values()).stream().
                        collect(Collectors.toSet());
        final Set<RpiEstimateType> expected = Arrays.asList(new
                RpiEstimateType[] {
            RpiEstimateType.MAG_SUS,
            RpiEstimateType.ARM_DEMAG
        }).stream().collect(Collectors.toSet());
        assertEquals(expected, actual);
    }
    
    @Test
    public void testValueOf() {
        for (RpiEstimateType type: RpiEstimateType.values()) {
            assertEquals(type, RpiEstimateType.valueOf(type.toString()));
        }
    }
}
