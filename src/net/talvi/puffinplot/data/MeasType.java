package net.talvi.puffinplot.data;

import java.util.regex.Pattern;

public enum MeasType {
    DISCRETE("discrete", "sample"),
    CONTINUOUS("continuous", "depth"),
    NONE("^na$", "no measurement type"),
    UNSET(),
    UNKNOWN();
    
    private final String columnHeader;
    private final Pattern namePattern;
    
    private MeasType() {
        namePattern = null;
        columnHeader = null;
    }
    
    private MeasType(String name, String columnHeader) {
        namePattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
        this.columnHeader = columnHeader;
    }
    
    private boolean matches(String name) {
        return namePattern!=null && namePattern.matcher(name).find();
    }
    
    public static MeasType fromString(String s) {
        for (MeasType mt: MeasType.values())
            if (mt.matches(s)) return mt;
        return UNKNOWN;
    }

    public String getColumnHeader() {
        return columnHeader;
    }

    public boolean isActualMeasurement() {
        return (this != NONE && this != UNSET && this != UNKNOWN);
    }

}
