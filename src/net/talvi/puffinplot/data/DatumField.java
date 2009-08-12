package net.talvi.puffinplot.data;

import java.util.HashMap;
import java.util.Map;

public enum DatumField {

    /* When adding fields here, make sure also to add them to the Datum
     * constructor and to Datum.getValue().
     */

    // Identifiers
    SAMPLEID("Sample ID"),
    RUNNUMBER("Run #"),
    TIMESTAMP("Sample Timestamp"),

    // Lab measurements
    MEASTYPE("Measurement type"),
    XCORR("X moment"),
    YCORR("Y moment"),
    ZCORR("Z moment"),
    MSCORR("Magnetic susceptibility"),
    VOLUME("Volume"),
    AREA("Area"),

    // Field measurements
    SAMPLEAZ("Sample azimuth"),
    SAMPLEDIP("Sample dip"),
    FORMAZ("Formation dip azimuth"),
    FORMDIP("Formation dip"),
    MAGDEV("Magnetic deviation"),
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
    private final static Map<String, DatumField> map
            = new HashMap<String, DatumField>();
    
    static {
         for (DatumField f: values()) map.put(f.getHeading(), f);
    }

    private DatumField(String heading, String niceName) {
        this.heading = heading;
        this.niceName = niceName;
    }

    private DatumField(String heading) {
        this(heading, heading);
    }

    public static DatumField getByHeader(String h) {
        DatumField f =  map.get(h);
        return f!=null ? f : UNKNOWN;
    }

    public String getHeading() {
        return heading;
    }
    
    public String getNiceName() {
        return niceName;
    }
}
