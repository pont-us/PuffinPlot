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
public class LocationTest {
    
    @Test
    public void testToVec3() {
        final Vec3 v = new Vec3(1.2, 2.3, 3.4).normalize();
        Location instance = Location.fromVec3(v);
        Vec3 expResult = v;
        Vec3 result = instance.toVec3();
        assertTrue(expResult.minus(result).mag() < 0.001);
    }
}
