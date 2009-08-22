package net.talvi.puffinplot.data;

import java.util.HashMap;
import java.util.Map;

public enum DatumField {

    /* When adding fields here, make sure also to add them to Datum.getValue()
     * and Datum.setValue(v).
     */

    // Identifiers
    SAMPLE_ID("Sample ID"), // depth or name
    RUN_NUMBER("Run #"),
    TIMESTAMP("Sample Timestamp"),

    // Lab measurements
    MEAS_TYPE("Measurement type"),
    X_MOMENT("X moment"),
    Y_MOMENT("Y moment"),
    Z_MOMENT("Z moment"),
    MAG_SUS("Magnetic susceptibility"),
    VOLUME("Volume"),
    AREA("Area"),

    // Field measurements
    SAMPLE_AZ("Sample azimuth"),
    SAMPLE_DIP("Sample dip"),
    FORM_AZ("Formation dip azimuth"),
    FORM_DIP("Formation dip"),
    MAG_DEV("Magnetic deviation"),
    DEPTH("Depth"),

    // Treatments
    TREATMENT("Treatment type"),
    AFX("AF X field"),
    AFY("AF Y field"),
    AFZ("AF Z field"),
    TEMP("Temperature"),
    IRMGAUSS("IRM Gauss"),
    ARMGAUSS("ARM Gauss"),
    ARMAXIS("ARM axis"),

    // Processing and display parameters
    PP_SELECTED("PUFFIN selected"),
    PP_ANCHOR_PCA("PUFFIN anchor PCA"),
    PP_HIDDEN("PUFFIN hidden"),
    UNKNOWN(null);
    
    private final String heading;
    private final String niceName;
    private final static Map<String, DatumField> nameMap
            = new HashMap<String, DatumField>();
    
    static {
         for (DatumField f: values()) nameMap.put(f.getHeading(), f);
    }

    private DatumField(String heading, String niceName) {
        this.heading = heading;
        this.niceName = niceName;
    }

    private DatumField(String heading) {
        this(heading, heading);
    }

    public static DatumField getByHeader(String h) {
        DatumField f =  nameMap.get(h);
        return f!=null ? f : UNKNOWN;
    }

    public String getHeading() {
        return heading;
    }
    
    public String getNiceName() {
        return niceName;
    }
}
