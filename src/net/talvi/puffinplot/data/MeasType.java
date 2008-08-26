package net.talvi.puffinplot.data;

import java.util.regex.Pattern;

public enum MeasType {
    DISCRETE("discrete"),
    CONTINUOUS("continuous"),
    UNSET(),
    UNKNOWN();
    
    private Pattern namePattern;
    
    private MeasType() {
        namePattern = null;
    }
    
    private MeasType(String name) {
        namePattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
    }
    
    private boolean matches(String name) {
        return namePattern!=null && namePattern.matcher(name).find();
    }
    
    public static MeasType fromString(String s) {
        for (MeasType mt: MeasType.values())
            if (mt.matches(s)) return mt;
        return UNKNOWN;
    }
}
