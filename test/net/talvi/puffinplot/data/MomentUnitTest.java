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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class MomentUnitTest {

    @Test
    public void testValues() {
        final Set<MomentUnit> values =
                new HashSet<>(Arrays.asList(MomentUnit.values()));
        assertTrue(values.contains(MomentUnit.AM));
        assertTrue(values.contains(MomentUnit.MILLIAM));
        assertEquals(2, values.size());
    }

    @Test
    public void testValueOf() {
        assertEquals(MomentUnit.AM, MomentUnit.valueOf("AM"));
        assertEquals(MomentUnit.MILLIAM, MomentUnit.valueOf("MILLIAM"));
    }

    @Test
    public void testGetFactorForAm() {
        // 1 can be represented exactly in IEEE-754, so we don't need a tolerance.
        assertEquals(1, MomentUnit.AM.getFactorForAm(), 0);
        assertEquals(0.001, MomentUnit.MILLIAM.getFactorForAm(), 1e-8);
    }
    
}
