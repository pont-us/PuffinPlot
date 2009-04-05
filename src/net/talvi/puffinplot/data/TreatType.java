package net.talvi.puffinplot.data;

import java.util.regex.Pattern;

public enum TreatType {
    NONE("none"),
    DEGAUSS("degauss x, y, & z"),
    ARM("degauss z - arm axial"),
    IRM("irm"),
    THERMAL("thermal"),
    UNKNOWN();
	
    private Pattern namePattern;
    
    private TreatType() {
        namePattern = null;
    }
    
    private TreatType(String name) {
        namePattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
    }
    
    private boolean matches(String name) {
        return namePattern != null
                ? namePattern.matcher(name).find()
                : false;
    }
    
    static TreatType fromString(String s) {
        for (TreatType tt : TreatType.values())
            if (tt.matches(s)) return tt;
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return namePattern.pattern();
    }
    
    public String getAxisLabel() {
        switch (this) {
        case NONE: return "No demagnetization";
        case DEGAUSS: return "AF strength (mT)";
        case IRM: return "IRM";
        case ARM: return "ARM";
        case THERMAL: return "Temperature (°C)";
        default: return "unknown units";
        }
    }
}
