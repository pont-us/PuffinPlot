package net.talvi.puffinplot.data;

/**
 * A type of treatment applied to a sample. The most common are alternating-field
 * and heating.
 * @author pont
 */
public enum TreatType {
    /** no treatment applied */
    NONE("No treatment", "", ""),
    /** static alternating-field treatment along three orthogonal axes */
    DEGAUSS_XYZ("3-axis degauss", "3-axis AF strength", "T"),
    /** static alternating-field treatment along one axis*/
    DEGAUSS_Z("z-axis degauss", "Z-axis AF strength", "T"),
    /** anhysteretic remanent magnetization: alternating-field treatment with 
     a DC biasing field */
    ARM("z-axis ARM", "AF strength", "T"), //AF usually more interesting than bias
    /** isothermal remanent magnetization: a pulsed non-alternating field */
    IRM("IRM", "IRM field", "T"),
    /** heating */
    THERMAL("Heating", "Temperature", "°C"),
    /** unknown treatment type */
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

    /** Returns a user-friendly name for this treatment.
     * @return a user-friendly name for this treatment */
    public String getNiceName() {
        return name;
    }
    
    /** Returns the axis label to use when plotting a graph involving this treatment.
     * @return the axis label to use when plotting a graph involving this treatment */
    public String getAxisLabel() {
        return treatment;
    }

    /** The units in which this treatment is quantified. 
     * @return the units in which this treatment is quantified */
    public String getUnit() {
        return unit;
    }
    
    /** @return {@code true} if this treatment involves application of
     * an alternating magnetic field */
    public boolean involvesAf() {
        return this==DEGAUSS_XYZ || this==DEGAUSS_Z || this==ARM;
    }
}
