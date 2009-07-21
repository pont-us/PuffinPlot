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
    private final static Map<String, TreatType> nameMap;

    static {
        // This block is run after the enums have been initialized.
        // See http://deepjava.wordpress.com/2006/12/08/bootstrapping-static-fields-within-enums/
         nameMap = new HashMap<String, TreatType>();
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
    
    static TreatType fromString(String s) {
        TreatType t = nameMap.get(s.toLowerCase());
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
