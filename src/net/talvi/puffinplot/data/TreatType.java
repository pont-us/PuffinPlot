package net.talvi.puffinplot.data;

public enum TreatType {
    NONE("No treatment", "", ""),
    DEGAUSS_XYZ("3-axis degauss", "3-axis AF strength", "T"),
    DEGAUSS_Z("z-axis degauss", "Z-axis AF strength", "T"),
    ARM("z-axis ARM", "AF strength", "T"), //AF usually more interesting than bias
    IRM("IRM", "IRM field", "T"),
    THERMAL("Heating", "Temperature", "°C"),
    UNKNOWN("Unknown", "Unknown treatment", "?");

    // Human-friendly name for treatment
    private final String name;
    // The actual quantifiable `thing' applied (temperature or field)
    private final String treatment;
    // Unit name for the applied `thing'
    private String unit;

    private TreatType(String name, String treatment, String unit) {
        this.name = name;
        this.treatment = treatment;
        this.unit = unit;
    }

    public String getNiceName() {
        return name;
    }
    
    public String getAxisLabel() {
        return treatment;
    }

    public String getUnit() {
        return unit;
    }
}
