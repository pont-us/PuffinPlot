package net.talvi.puffinplot.data;

import java.util.HashMap;
import java.util.Map;

public enum TwoGeeField {

    /* When adding fields here, make sure also to add them to the Datum
     * constructor and to Datum.getValue().
     */

    SAMPLEID("Sample ID"),
    MEASTYPE("Meas. type"),
    TREATMENT("Treatment Type"),
    AFX("AF X"),
    AFY("AF Y"),
    AFZ("AF Z"),
    TEMP("Temp C"),
    DECUC("Declination: Unrotated"),
    INCUC("Inclination: Unrotated"),
    DECSC("Declination: Sample Rotated"),
    INCSC("Inclination: Sample Rotated"),
    DECFC("Declination: Formation Rotated"),
    INCFC("Inclination: Formation Rotated"),
    INTENSITY("Intensity"),
    MSCORR("MS corr"),
    SAMPLEAZ("Sample Azimiuth" /* sic */, "Sample azimuth"),
    SAMPLEDIP("Sample Dip", "Sample dip"),
    FORMAZ("Formation Dip Azimuth", "Formation dip azimuth"),
    FORMDIP("Formation Dip", "Formation dip"),
    MAGDEV("Mag Dev", "Magnetic deviation"),
    XCORR("X corr"),
    YCORR("Y corr"),
    ZCORR("Z corr"),
    XDRIFT("X drift"),
    YDRIFT("Y drift"),
    ZDRIFT("Z drift"),
    XMEAN("X mean"),
    YMEAN("Y mean"),
    ZMEAN("Z mean"),
    DEPTH("Depth"),
    IRMGAUSS("IRM Gauss"),
    ARMGAUSS("ARM Gauss"),
    ARMAXIS("ARM axis"),
    VOLUME("Volume"),
    XBKG1("X bkg #1"),
    XBKG2("X bkg #2"),
    YBKG1("Y bkg #1"),
    YBKG2("Y bkg #2"),
    ZBKG1("Z bkg #1"),
    ZBKG2("Z bkg #2"),
    RUNNUMBER("Run #"),
    TIMESTAMP("Sample Timestamp"),
    AREA("Area"),
    PP_SELECTED("PUFFIN selected"),
    PP_ANCHOR_PCA("PUFFIN anchor PCA"),
    PP_HIDDEN("PUFFIN hidden"),
    UNKNOWN(null);
    
    private final String heading;
    private final String niceName;
    private final static Map<String, TwoGeeField> map;
    
    static {
        // This block is run after the enums have been initialized.
        // See http://deepjava.wordpress.com/2006/12/08/bootstrapping-static-fields-within-enums/
         map = new HashMap<String, TwoGeeField>();
         for (TwoGeeField f: values()) map.put(f.getHeading(), f);
    }

    private TwoGeeField(String heading, String niceName) {
        this.heading = heading;
        this.niceName = niceName;
    }

    private TwoGeeField(String heading) {
        this(heading, heading);
    }

    public static TwoGeeField getByHeader(String h) {
        TwoGeeField f =  map.get(h);
        return f!=null ? f : UNKNOWN;
    }

    public String getHeading() {
        return heading;
    }
    
    public String getNiceName() {
        return niceName;
    }
}
