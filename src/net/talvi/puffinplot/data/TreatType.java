package net.talvi.puffinplot.data;

import java.util.HashMap;
import java.util.Map;

public enum TreatType {
    NONE("none", "No demagnetization"),
    DEGAUSS_XYZ("degauss x, y, & z", "3-axis AF strength (G)"),
    DEGAUSS_Z("degauss z", "Z-axis AF strength (G)"),
    ARM("degauss z - arm axial", "ARM field strength"),
    IRM("irm", "IRM field strength"),
    THERMAL("thermal demag", "Temperature (°C)"),
    UNKNOWN("", "Unknown treatment");
	
    private final String axisLabel;
    private final String name;
    private final static Map<String, TreatType> nameMap =
            new HashMap<String, TreatType>();

    static {
         // See Effective Java, 2nd ed., item 30, p. 154 for this technique
         for (TreatType t: values()) nameMap.put(t.toString().toLowerCase(), t);
    }
    
    private TreatType() {
        name = null;
        axisLabel = null;
    }
    
    private TreatType(String name, String axisLabel) {
        this.name = name;
        this.axisLabel = axisLabel;
    }

    private static String normalizeString(String s) {
        s = s.toLowerCase();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length()-1);
        return s;
    }
    
    static TreatType fromString(String s) {
        TreatType t = nameMap.get(normalizeString(s));
        return t != null ? t : UNKNOWN;
    }

    @Override
    public String toString() {
        return name;
    }
    
    public String getAxisLabel() {
        return axisLabel;
    }
}
