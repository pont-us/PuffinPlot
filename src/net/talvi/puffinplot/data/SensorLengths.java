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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;
import static java.lang.Double.parseDouble;

/**
 * <p>Represents the effective SQUID sensor lengths of a magnetometer, 
 * as determined by the response curves of the SQUID sensors. These
 * sensor lengths are used when loading 2G long core magnetometer data,
 * since in this case the components of the magnetic moment measurements
 * are not pre-corrected for effective sensor length.</p>
 * 
 * <p>Hard-coded, pre-defined sets of sensor lengths can be
 * selected by name, but SensorLengths can also represent any
 * ‘custom’ set of sensor lengths.</p>
 * 
 * @author pont
 */
public class SensorLengths {

    private final String preset;
    private final List<String> lengths;
    private final static HashMap<String, List<String>> PRESETS =
            new LinkedHashMap<>();

    static {
        addPreset("1:1:1", "1", "1", "1");
        addPreset("OPRF (old)", "-4.628", "4.404", "-6.280");
        addPreset("OPRF (new)", "4.628", "-4.404", "-6.280");
    }

    private static void addPreset(String name, String x, String y, String z) {
        PRESETS.put(name, Arrays.asList(x, y, z));
    }

    private SensorLengths(String preset) {
        this.preset = preset;
        lengths = null;
    }

    private SensorLengths(String x, String y, String z) {
        this.lengths = Arrays.asList(x, y, z);
        preset = null;
    }

    /** Returns a list of string representations of the sensor lengths in
     * the order x, y, z.
     * @return a list of string representations of the sensor lengths */
    public List<String> getLengths() {
        return (preset == null) ? lengths : PRESETS.get(preset);
    }

    /** Returns the sensor lengths as a three-dimensional vector. 
     * @return the sensor lengths as a three-dimensional vector */
    public Vec3 toVector() {
        List<String> l = getLengths();
        return new Vec3(parseDouble(l.get(0)), parseDouble(l.get(1)),
                parseDouble(l.get(2)));
    }

    /** Writes a string representation of the sensor lengths to
     * a specified {@link Preferences} object. The value is 
     * stored under the key {@code sensorLengths}.
     * 
     * @param prefs the preferences object to which to store the sensor lengths
     */
    public void save(Preferences prefs) {
        prefs.put("sensorLengths", toString());
    }

    /** Creates a sensor lengths object from a string representation in
     * a {@link Preferences} object. The string is read from the key
     * {@code sensorLengths}. If there is no such key in the preferences
     * object, each sensor length defaults to 1.
     * 
     * @param prefs a preferences object from which to read the definition
     * @return the sensor lengths defined in the preferences object
     */
    public static SensorLengths fromPrefs(Preferences prefs) {
        return fromString(prefs.get("sensorLengths", "PRESET\t1:1:1"));
    }

    /** Returns a string representation of this object. The
     * string may be passed to {@link #fromString} to reconstruct
     * the original object.
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return preset != null
                ? "PRESET\t" + preset
                : String.format(Locale.ENGLISH, "CUSTOM\t%s\t%s\t%s", lengths.get(0),
                lengths.get(1), lengths.get(2));
    }

    /** Creates a new sensor lengths object from a string definition.
     * The definition must be in the format produced by {@link #toString()}.
     * 
     * @param string a string definition
     * @return the sensor lengths specified in the string
     * @throws IllegalArgumentException if the string is null or not recognized
     */
    public static SensorLengths fromString(String string) {
        Scanner sc = new Scanner(string);
        sc.useLocale(Locale.ENGLISH);
        sc.useDelimiter("\t");
        String type = sc.next();
        if (null != type) switch (type) {
            case "PRESET":
                final String preset = sc.next();
                return SensorLengths.fromPresetName(preset);
            case "CUSTOM":
                return SensorLengths.fromStrings(sc.next(), sc.next(), sc.next());
            default:
                throw new IllegalArgumentException("Unknown SensorLengths type "+type);
        }
        throw new IllegalArgumentException("null passed to SensorLengths.fromString");
    }

    /**
     * Creates a new sensor lengths object from three strings specifying
     * the individual sensor lengths. Each string must contain a decimal
     * representation of a number.
     * 
     * @param x x sensor length
     * @param y y sensor length
     * @param z z sensor length
     * @return a sensor lengths object representing the specified lengths
     */
    public static SensorLengths fromStrings(String x, String y, String z) {
        return new SensorLengths(x, y, z);
    }

    /** Returns the names of the hard-coded preset sensor lengths. 
     * @return the names of the hard-coded preset sensor lengths
     */
    public static String[] getPresetNames() {
        return PRESETS.keySet().toArray(new String[] {});
    }

    /** Creates a sensor lengths object with lengths determined by a named preset. 
     * @param name a named preset
     * @return a sensor lengths object with lengths set according to the preset
     */
    public static SensorLengths fromPresetName(String name) {
        return new SensorLengths(name);
    }

    /** Returns the name of the preset sensor lengths, if any.
     * If these sensor lengths were created from a named preset,
     * this method returns the name of the preset. Otherwise it
     * returns {@code null}.
     * @return the name of the preset sensor lengths, if any
     */
    public String getPreset() {
        return preset;
    }
}
