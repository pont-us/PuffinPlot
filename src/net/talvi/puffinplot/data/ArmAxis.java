package net.talvi.puffinplot.data;

public enum ArmAxis {
    AXIAL,
    NONE,
    UNKNOWN;
    
    public static ArmAxis getByString(String s) {
        if ("AXIAL".equals(s)) return AXIAL;
        else if ("NONE".equals(s)) return NONE;
        else return UNKNOWN;
    }
}
