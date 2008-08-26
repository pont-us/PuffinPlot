package net.talvi.puffinplot.data;

import java.util.HashMap;
import java.util.Map;

public enum TwoGeeField {
    
    SAMPLEID(String.class, "Sample ID"),
    MEASTYPE(MeasType.class, "Meas. type"),
    TREATMENT(TreatType.class, "Treatment Type"),
    AFX(Double.class, "AF X"),
    AFY(Double.class, "AF Y"),
    AFZ(Double.class, "AF Z"),
    TEMP(Double.class, "Temp C"),
    DECUC(Double.class, "Declination: Unrotated"),
    INCUC(Double.class, "Inclination: Unrotated"),
    DECSC(Double.class, "Declination: Sample Rotated"),
    INCSC(Double.class, "Inclination: Sample Rotated"),
    DECFC(Double.class, "Declination: Formation Rotated"),
    INCFC(Double.class, "Inclination: Formation Rotated"),
    INTENSITY(Double.class, "Intensity"),
    MSCORR(Double.class, "MS corr"),
    SAMPLEAZ(Double.class, "Sample Azimiuth"), // [sic]
    SAMPLEDIP(Double.class, "Sample Dip"),
    FORMAZ(Double.class, "Formation Dip Azimuth"),
    FORMDIP(Double.class, "Formation Dip"),
    MAGDEV(Double.class, "Mag Dev"),
    XCORR(Double.class, "X corr"),
    YCORR(Double.class, "Y corr"),
    ZCORR(Double.class, "Z corr"),
    XDRIFT(Double.class, "X drift"),
    YDRIFT(Double.class, "Y drift"),
    ZDRIFT(Double.class, "Z drift"),
    XMEAN(Double.class, "X mean"),
    YMEAN(Double.class, "Y mean"),
    ZMEAN(Double.class, "Z mean"),
    DEPTH(Double.class, "Depth"),
    IRMGAUSS(Double.class, "IRM Gauss"),
    ARMGAUSS(Double.class, "ARM Gauss"),
    ARMAXIS(ArmAxis.class, "ARM axis"),
    VOLUME(Double.class, "Volume"),
    XBKG1(Double.class, "X bkg #1"),
    XBKG2(Double.class, "X bkg #2"),
    YBKG1(Double.class, "Y bkg #1"),
    YBKG2(Double.class, "Y bkg #2"),
    ZBKG1(Double.class, "Z bkg #1"),
    ZBKG2(Double.class, "Z bkg #2"),
    RUNNUMBER(Integer.class, "Run #"),
    TIMESTAMP(String.class, "Sample Timestamp"),
    AREA(Double.class, "Area"),
    UNKNOWN(Object.class, null);
    
    private final String heading;
    private final static Map<String, TwoGeeField> map;
    private Class type;
    
    static {
        // This block is run after the enums have been initialized.
        // See http://deepjava.wordpress.com/2006/12/08/bootstrapping-static-fields-within-enums/
         map = new HashMap<String, TwoGeeField>();
         for (TwoGeeField f: values()) map.put(f.getHeading(), f);
    }

    private TwoGeeField(Class type, String heading) {
        this.type = type;
        this.heading = heading;
    }

    public static TwoGeeField getByHeader(String h) {
        TwoGeeField f =  map.get(h);
        return f!=null ? f : UNKNOWN;
    }

    public String getHeading() {
        return heading;
    }
    
    public Class getType() {
        return type;
    }
}
