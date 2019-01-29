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
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author pont
 */
public class LocationTest {
    
    @Test
    public void testToAndFromVec3() {
        final Vec3 v = new Vec3(1.2, 2.3, 3.4).normalize();
        Location instance = Location.fromVec3(v);
        Vec3 expResult = v;
        Vec3 result = instance.toVec3();
        assertTrue(expResult.minus(result).mag() < 0.001);
    }
    
    @Test
    public void testUnitConversions() {
        final double latDeg = 23;
        final double longDeg = 42;
        final double latRad = Math.toRadians(latDeg);
        final double longRad = Math.toRadians(longDeg);
        final Location locFromDeg = Location.fromDegrees(latDeg, longDeg);
        assertEquals(latDeg, locFromDeg.getLatDeg(), 1e-6);
        assertEquals(longDeg, locFromDeg.getLongDeg(), 1e-6);
        assertEquals(latDeg, locFromDeg.getLatDeg(), 1e-6);
        assertEquals(longDeg, locFromDeg.getLongDeg(), 1e-6);
        final Location locFromRad = Location.fromRadians(latRad, longRad);
        assertEquals(latDeg, locFromRad.getLatDeg(), 1e-6);
        assertEquals(longDeg, locFromRad.getLongDeg(), 1e-6);
        assertEquals(latRad, locFromRad.getLatRad(), 1e-6);        
        assertEquals(longRad, locFromRad.getLongRad(), 1e-6);
    }
    
    @Test
    public void testGetHeaders() {
        final List<String> expected = Arrays.asList(new String[] {
            "Lat (deg)", "Long (deg)"
        });
        assertEquals(expected, Location.getHeaders());
    }
    
    @Test
    public void testGetEmptyFields() {
        assertEquals(Location.getHeaders().size(),
                Location.getEmptyFields().size());
        assertTrue(Location.getEmptyFields().stream().
                allMatch(field -> "".equals(field)));
    }

    @Test
    public void testToStrings() {
        final double latDeg = -12.3;
        final double longDeg = 45.6;
        final Location location = Location.fromDegrees(latDeg, longDeg);
        final List<String> actual = location.toStrings();
        assertEquals(latDeg, Double.parseDouble(actual.get(0)), 1e-6);
        assertEquals(longDeg, Double.parseDouble(actual.get(1)), 1e-6);
    }
}
