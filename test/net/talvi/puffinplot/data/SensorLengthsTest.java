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
import java.util.prefs.Preferences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SensorLengthsTest {
    
    @Test
    public void testGetLengths() {
        final Vec3 original = new Vec3(0.1, -1, 5.5);
        final Double[] comps = makeSensorLengthsFromVector(original).
                getLengths().stream().map(s -> Double.parseDouble(s)).
                toArray(Double[]::new);
        assertTrue(original.equals(new Vec3(comps[0], comps[1], comps[2]),
                1e-10));
    }

    @Test
    public void testSaveAndFromPrefsPreset() {
        final Preferences prefs =
                Preferences.userNodeForPackage(SensorLengthsTest.class);
        SensorLengths.fromPresetName("1:1:1").save(prefs);
        assertEquals("1:1:1", SensorLengths.fromPrefs(prefs).getPreset());
    }

    @Test
    public void testSaveAndFromPrefsCustom() {
        final Preferences prefs =
                Preferences.userNodeForPackage(SensorLengthsTest.class);
        final Vec3 v = new Vec3(0.1, -1, 5.5);
        makeSensorLengthsFromVector(v).save(prefs);
        assertTrue(v.equals(SensorLengths.fromPrefs(prefs).toVector(), 1e-10));
    }

    @Test
    public void testToAndFromStringPreset() {
        final SensorLengths sl0 = SensorLengths.fromPresetName("1:1:1");
        final SensorLengths sl1 = SensorLengths.fromString(sl0.toString());
        assertTrue(new Vec3(1, 1, 1).equals(sl1.toVector(), 1e-10));
    }

    @Test(expected = NullPointerException.class)
    public void testFromStringWithNull() {
        SensorLengths.fromString(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testFromStringWithMalformedString() {
        SensorLengths.fromString("wibble");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testFromStringWithMissingField() {
        SensorLengths.fromString("PRESET");
    }
    
    @Test
    public void testToAndFromStringCustom() {
        final Vec3 v = new Vec3(3.1, -4.1, 5.9);
        final SensorLengths sl0 = makeSensorLengthsFromVector(v);
        final SensorLengths sl1 = SensorLengths.fromString(sl0.toString());
        assertTrue(v.equals(sl1.toVector(), 1e-10));
    }

    @Test
    public void testFromStringsAndToVector() {
        final Vec3 v = new Vec3(2.3, 4.2, -6.9);
        final SensorLengths sl = makeSensorLengthsFromVector(v);
        assertTrue(v.equals(sl.toVector(), 1e-10));
        
    }

    @Test
    public void testGetPresetNames() {
        final String[] presetNames = SensorLengths.getPresetNames();
        /*
         * It's pointless duplication to test the exact contents here, but we
         * should at least expect that the array contains the standard 1:1:1
         * preset.
         */
        assertTrue(Arrays.asList(presetNames).contains("1:1:1"));
    }

    @Test
    public void testFromPresetName() {
        final SensorLengths sl = SensorLengths.fromPresetName("1:1:1");
        assertTrue(new Vec3(1, 1, 1).equals(sl.toVector(), 1e-10));
    }

    @Test
    public void testGetPresetOnPresetLengths() {
        final SensorLengths sl = SensorLengths.fromPresetName("1:1:1");
        assertEquals("1:1:1", sl.getPreset());
    }

    @Test
    public void testGetPresetOnCustomLengths() {
        final SensorLengths sl = SensorLengths.fromStrings("3", "2", "1");
        assertNull(sl.getPreset());
    }
    
    private static SensorLengths makeSensorLengthsFromVector(Vec3 v) {
        return SensorLengths.fromStrings(Double.toString(v.x),
                Double.toString(v.y), Double.toString(v.z));
    }
}
