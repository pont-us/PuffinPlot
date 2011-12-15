package net.talvi.puffinplot.data;

/**
 * ArmAxis represents the axis along which an ARM 
 * (anhysteretic remanent magnetization) field has been applied.
 * 
 * @author pont
 */
public enum ArmAxis {
    /** an ARM axis lying along the axis of the magnetometer */
    AXIAL,
    /** no ARM was applied */
    NONE,
    /** the ARM axis is unknown, or cannot be represented by this enum */
    UNKNOWN;
    
    /**
     * <p>Creates an {@link ArmAxis} from the supplied string. The values
     * produced are as follows:</p>
     * 
     * <table>
     * <tr><th>Input</th><th>Result</th></tr>
     * <tr><td>{@code AXIAL}</td><td>{@code AXIAL}</td></tr>
     * <tr><td>{@code NONE}</td><td>{@code NONE}</td></tr>
     * <tr><td>{@code NA}</td><td>{@code NONE}</td></tr>
     * <tr><td>[any other string]</td><td>{@code UNKNOWN}</td></tr>
     * </table>
     * 
     * @param name a string specifying the ARM axis
     * @return the ARM axis specified by the supplied string
     */
    public static ArmAxis fromString(String name) {
        if ("AXIAL".equals(name)) return AXIAL;
        else if ("NONE".equals(name) || "NA".equals(name)) return NONE;
        else return UNKNOWN;
    }
}
