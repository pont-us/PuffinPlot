package net.talvi.puffinplot.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.prefs.Preferences;
import static java.lang.Double.parseDouble;

public class SensorLengths {

    private final String[] lengths;
    private final static HashMap<String, SensorLengths> PRESETS =
            new LinkedHashMap<String, SensorLengths>();

    static {
        PRESETS.put("Equal", new SensorLengths("1", "1", "1"));
        PRESETS.put("OPRF (old)", new SensorLengths("-4.628", "4.404", "-6.280"));
        PRESETS.put("OPRF (new)", new SensorLengths("4.628", "-4.404", "-6.280"));
    }

    private SensorLengths(String[] lengths) {
        this.lengths = lengths;
    }

    private SensorLengths(String x, String y, String z) {
        this.lengths = new String[] {x, y, z};
    }

    public String[] toStrings() {
        return lengths;
    }

    public Vec3 toVector() {
        return new Vec3(parseDouble(lengths[0]),
                parseDouble(lengths[1]), parseDouble(lengths[2]));
    }

    public void save(Preferences prefs) {
        prefs.put("sensorLengthX", lengths[0]);
        prefs.put("sensorLengthY", lengths[1]);
        prefs.put("sensorLengthZ", lengths[2]);
    }

    public static SensorLengths fromPrefs(Preferences prefs) {
        final String[] l = new String[] {
            prefs.get("sensorLengthX", "1"),
            prefs.get("sensorLengthY", "1"),
            prefs.get("sensorLengthZ", "1"),
        };
        return new SensorLengths(l);
    }
}
