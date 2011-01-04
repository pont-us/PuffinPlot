package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum DatumField {

    /* When adding fields here, make sure also to add them to Datum.getValue()
     * and Datum.setValue(v).
     */

    // Identifiers
    DISCRETE_ID("Sample ID"),
    DEPTH("Depth"),
    RUN_NUMBER("Run #"),
    TIMESTAMP("Sample Timestamp"),
    SLOT_NUMBER("Tray slot number"),

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

    // Treatments
    TREATMENT("Treatment type"),
    AF_X("AF X field"),
    AF_Y("AF Y field"),
    AF_Z("AF Z field"),
    TEMPERATURE("Temperature"),
    IRM_FIELD("IRM Gauss"),
    ARM_FIELD("ARM Gauss"),
    ARM_AXIS("ARM axis"),

    // Processing and display parameters
    PP_SELECTED("PUFFIN selected"),
    PP_ANCHOR_PCA("PUFFIN anchor PCA"),
    PP_HIDDEN("PUFFIN hidden"),
    PP_ONCIRCLE("PUFFIN on circle"),
    PP_INPCA("PUFFIN in PCA"),

    // Virtual parameters (calculated from real ones)
    VIRT_MAGNETIZATION("Magnetization", "Magnetization", true);
    
    private final String heading;
    private final String niceName;
    private final boolean virtual;
    private final static Map<String, DatumField> nameMap
            = new HashMap<String, DatumField>();
    private static final List<String> realFieldHeaders;
    private static final List<DatumField> realFields;
    
    static {
        final List<DatumField> realFieldsTmp =
                new ArrayList<DatumField>(values().length - 1);
        final List<String> realFieldHeadersTmp =
                new ArrayList<String>(values().length-1);
        for (DatumField f : values())
        {
            nameMap.put(f.getHeading(), f);
            DatumField[] dfs = DatumField.values();
            if (!f.isVirtual()) {
                realFieldsTmp.add(f);
                realFieldHeadersTmp.add(f.toString());
            }
        }
        realFields = Collections.unmodifiableList(realFieldsTmp);
        realFieldHeaders = Collections.unmodifiableList(realFieldHeadersTmp);
    }

    private DatumField(String heading, String niceName, boolean virtual) {
        this.heading = heading;
        this.niceName = niceName;
        this.virtual = virtual;
    }

    private DatumField(String heading, String niceName) {
        this(heading, niceName, false);
    }

    private DatumField(String heading) {
        this(heading, heading);
    }

    public static DatumField getByHeader(String h) {
        return nameMap.get(h);
    }

    public String getHeading() {
        return heading;
    }
    
    public String getNiceName() {
        return niceName;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public static List<DatumField> getRealFields() {
        return realFields;
    }

    public static List<String> getRealFieldHeaders() {
        return realFieldHeaders;
    }
}
