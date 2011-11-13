package net.talvi.puffinplot.data;

import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;
import static java.lang.Double.parseDouble;

public class SensorLengths {

    private String preset;
    private final List<String> lengths;
    private final static HashMap<String, List<String>> PRESETS =
            new LinkedHashMap<String, List<String>>();

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
    }

    public List<String> getLengths() {
        return (preset == null) ? lengths : PRESETS.get(preset);
    }

    public Vec3 toVector() {
        List<String> l = getLengths();
        return new Vec3(parseDouble(l.get(0)), parseDouble(l.get(1)),
                parseDouble(l.get(2)));
    }

    public void save(Preferences prefs) {
        prefs.put("sensorLengths", toString());
    }

    public static SensorLengths fromPrefs(Preferences prefs) {
        return fromString(prefs.get("sensorLengths", "PRESET\t1:1:1"));
    }

    @Override
    public String toString() {
        return preset != null
                ? "PRESET\t" + preset
                : String.format(Locale.ENGLISH, "CUSTOM\t%s\t%s\t%s", lengths.get(0),
                lengths.get(1), lengths.get(2));
    }

    public static SensorLengths fromString(String s) {
        Scanner sc = new Scanner(s);
        sc.useLocale(Locale.ENGLISH);
        sc.useDelimiter("\t");
        String type = sc.next();
        if ("PRESET".equals(type)) {
            String preset = sc.next();
            return SensorLengths.fromPresetName(preset);
        } else if ("CUSTOM".equals(type)) {
            return SensorLengths.fromStrings(sc.next(), sc.next(), sc.next());
        } else {
            throw new IllegalArgumentException("Unknown SensorLengths type "+type);
        }
    }

    public static SensorLengths fromStrings(String x, String y, String z) {
        return new SensorLengths(x, y, z);
    }

    public static String[] getPresetNames() {
        return PRESETS.keySet().toArray(new String[] {});
    }

    public static SensorLengths fromPresetName(String name) {
        return new SensorLengths(name);
    }

    public String getPreset() {
        return preset;
    }
}
