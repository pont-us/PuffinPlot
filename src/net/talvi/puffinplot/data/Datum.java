package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.List;
import static java.lang.Double.NaN;
import static java.lang.Math.toRadians;

public class Datum {

    private static final double
            DEFAULT_AREA = 4.0, // can be overridden by Area field in file
            DEFAULT_VOLUME = 10.8; // can be overridden by Volume field in file

    private String sampleId = "UNSET";
    private MeasType measType = MeasType.UNSET;
    private TreatType treatType = TreatType.UNKNOWN;
    private double afx=NaN, afy=NaN, afz=NaN;
    private double temp=NaN;
    private double magSus=NaN;
    private double sampAz=NaN, sampDip=NaN, formAz=NaN, formDip=NaN;
    private double magDev=0;
    private String depth=null;
    private double irmGauss=NaN, armGauss=NaN;
    private ArmAxis armAxis = ArmAxis.UNKNOWN;
    private Vec3 moment = null;
    private int runNumber = -1;
    private double volume = DEFAULT_VOLUME;
    private double area = DEFAULT_AREA;
    private String timeStamp = "UNSET";
    private Line line;
    private boolean selected = false;
    private boolean pcaAnchored = true;
    private boolean hidden = false;
    private Sample sample;
    private static final List<String> fieldNames;
    private static final List<DatumField> fields;

    static {
        DatumField[] dfs = DatumField.values();
        fields = new ArrayList<DatumField>(dfs.length-1);
        fieldNames = new ArrayList<String>(dfs.length-1);
        for (DatumField df : dfs) {
            if (df != DatumField.UNKNOWN) {
                fields.add(df);
                fieldNames.add(df.toString());
            }
        }
    }

    public Datum(double x, double y, double z) {
        moment = new Vec3(x, y, z);
    }
    
    public Datum(Vec3 v) {
        moment = v; // v is immutable so it's OK not to copy it
    }

    public static List<String> getFieldNames() {
        return fieldNames;
    }

    public Datum() {}

    public boolean isSelected()        { return selected; }
    public void setSelected(boolean v) { selected = v; }
    public double getSampAz()          { return sampAz; }
    public void setSampAz(double v)    { sampAz = v; }
    public double getSampDip()         { return sampDip; }
    public void setSampDip(double v)   { sampDip = v; }
    public double getFormAz()          { return formAz; }
    public void setFormAz(double v)    { formAz = v; }
    public double getFormDip()         { return formDip; }
    public void setFormDip(double v)   { formDip = v; }
    public double getMagDev()          { return magDev; }
    public void setMagDev(double v)    { magDev = v; }
    public boolean isPcaAnchored()     { return pcaAnchored; }
    public void setPcaAnchored(boolean v) { pcaAnchored = v; }
    public Line getLine()              { return line; }
    public Sample getSample()          { return sample; }
    public boolean isHidden()          { return hidden; }
    public void setHidden(boolean v)   { hidden = v; }
    public void setSample(Sample v)    { sample = v; }
    public String getDepth()           { return depth; }
    public void setDepth(String v)     { depth = v; }
    public double getMagSus()          { return magSus; }
    public void setMagSus(double v)    { magSus = v; }
    public MeasType getMeasType()      { return measType; }
    public void setMeasType(MeasType v) { measType = v; }
    public String getSampleId()        { return sampleId; }
    public void setSampleId(String v)  { sampleId = v; }
    public TreatType getTreatType()    { return treatType; }
    public void setTreatType(TreatType v) { treatType = v; }
    public double getAfX()             { return afx; }
    public void setAfX(double v)       { afx = v; }
    public double getAfY()             { return afy; }
    public void setAfY(double v)       { afy = v; }
    public double getAfZ()             { return afz; }
    public void setAfZ(double v)       { afz = v; }
    public double getTemp()            { return temp; }
    public void setTemp(double v)      { temp = v; }
    public void setMoment(Vec3 v)      { moment = v; }
    public double getArea()            { return area; }
    public void setArea(double v)      { area = v; }
    public double getVolume()          { return volume; }
    public void setVolume(double v)    { volume = v; }

    public String getSampleIdOrDepth() {
        return measType == MeasType.CONTINUOUS ? depth : sampleId;
    }
    public boolean hasMagSus()          { return !Double.isNaN(magSus); }

    public boolean isMagSusOnly() {
        return moment == null && hasMagSus();
    }

    private Vec3 getFc(boolean emptyCorr) {
        return Double.isNaN(formAz) || Double.isNaN(formDip)
                ? getSc(emptyCorr)
                : getSc(emptyCorr).correctForm(toRadians(formAz - magDev), toRadians(formDip));
    }

    private Vec3 getSc(boolean emptyCorr) {
        return Double.isNaN(sampAz) || Double.isNaN(sampDip)
                ? getUc(emptyCorr)
                : getUc(emptyCorr).correctSample(toRadians(sampAz - magDev), toRadians(sampDip));
    }

    private Vec3 getUc(boolean emptyCorr) {
        return (!emptyCorr) || getLine().getEmptySlot() == null
                ? moment
                : moment.minus(getLine().getEmptySlot().getUc(false));
    }
    
    public Vec3 getPoint(Correction c, boolean emptyCorrection) {
        switch (c) {
            case FORMATION: return getFc(emptyCorrection);
            case SAMPLE: return getSc(emptyCorrection);
            case NONE: return getUc(emptyCorrection);
            default: throw new IllegalArgumentException("unknown correction");
        }
    }
    
    /*
     *  Rotate orientations 180 degrees about X axis.
     */
    public void rotX180() {
        moment = moment.rotX180();
    }

    public void toggleSel() {
        setSelected(!isSelected());
    }

    public double getDemagLevel() {
        switch (treatType) {
        case NONE: return 0;
        case DEGAUSS_XYZ: return afx>0?afx : afy>0?afy : afz;
        case DEGAUSS_Z: return afz;
        case THERMAL: return temp;
        case ARM: return armGauss;
        case IRM: return irmGauss;
        case UNKNOWN: return 0;
        default: throw new IllegalArgumentException("unhandled treatment type");
        }
    }

    public static double maximumDemag(List<Datum> ds) {
        double max = 0;
        for (Datum d: ds) {
            double level = d.getDemagLevel();
            if (level > max) max = level;
        }
        return max;
    }

    public static double maximumIntensity(List<Datum> ds, boolean emptyCorr) {
        double max = 0;
        for (Datum d: ds) {
            double i = d.getIntensity(emptyCorr);
            if (i > max) max = i;
        }
        return max;
    }

    public static double maximumMagSus(List<Datum> ds) {
        double max = 0;
        for (Datum d: ds) {
            double level = d.getMagSus();
            if (!Double.isNaN(level) && level > max) max = level;
        }
        return max;
    }

    public double getIntensity(boolean emptyCorrection) {
        return getUc(emptyCorrection).mag();
    }

    public boolean ignoreOnLoading() {
        return getTreatType() == TreatType.ARM ||
                getMeasType() == MeasType.NONE;
    }

    private String fmt(Double d) {
        return Double.toString(d);
    }

    public String getValue(DatumField field) {
        switch (field) {
        case AFX: return fmt(afx);
        case AFY: return fmt(afy);
        case AFZ: return fmt(afz);
        case TEMP: return fmt(temp);
        case MAG_SUS: return fmt(magSus);
        case SAMPLE_AZ: return fmt(getSampAz());
        case SAMPLE_DIP: return fmt(getSampDip());
        case FORM_AZ: return fmt(getFormAz());
        case FORM_DIP: return fmt(getFormDip());
        case MAG_DEV: return fmt(getMagDev());
        case X_MOMENT: return fmt(moment.x);
        case Y_MOMENT: return fmt(moment.y);
        case Z_MOMENT: return fmt(moment.z);
        case DEPTH: return depth;
        case IRMGAUSS: return fmt(irmGauss);
        case ARMGAUSS: return fmt(armGauss);
        case VOLUME: return fmt(volume);
        case SAMPLE_ID: return sampleId;
        case MEAS_TYPE: return measType.toString();
        case TREATMENT: return treatType.toString();
        case ARMAXIS: return armAxis.toString();
        case TIMESTAMP: return timeStamp;
        case RUN_NUMBER: return Integer.toString(runNumber);
        case AREA: return fmt(area);
        case PP_SELECTED: return Boolean.toString(selected);
        case PP_ANCHOR_PCA: return Boolean.toString(isPcaAnchored());
        case PP_HIDDEN: return Boolean.toString(isHidden());
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }

    public void setValue(DatumField field, String s) {
        switch (field) {
        case AFX: afx = Double.parseDouble(s); break;
        case AFY: afy = Double.parseDouble(s); break;
        case AFZ: afz = Double.parseDouble(s); break;
        case TEMP: temp = Double.parseDouble(s); break;
        case MAG_SUS: magSus = Double.parseDouble(s); break;
        case SAMPLE_AZ: setSampAz(Double.parseDouble(s)); break;
        case SAMPLE_DIP: setSampDip(Double.parseDouble(s)); break;
        case FORM_AZ: setFormAz(Double.parseDouble(s)); break;
        case FORM_DIP: setFormDip(Double.parseDouble(s)); break;
        case MAG_DEV: setMagDev(Double.parseDouble(s)); break;
        case X_MOMENT: moment = moment.setX(Double.parseDouble(s)); break;
        case Y_MOMENT: moment = moment.setY(Double.parseDouble(s)); break;
        case Z_MOMENT: moment = moment.setZ(Double.parseDouble(s)); break;
        case DEPTH: depth = (String) s; break;
        case IRMGAUSS: irmGauss = Double.parseDouble(s); break;
        case ARMGAUSS: armGauss = Double.parseDouble(s); break;
        case VOLUME: volume = Double.parseDouble(s); break;
        case SAMPLE_ID: sampleId = s; break;
        case MEAS_TYPE: measType = MeasType.fromString(s); break;
        case TREATMENT: treatType = TreatType.fromString(s); break;
        case ARMAXIS: armAxis = ArmAxis.fromString(s); break;
        case TIMESTAMP: timeStamp = s; break;
        case RUN_NUMBER: runNumber = Integer.parseInt(s); break;
        case AREA: area = Double.parseDouble(s); break;
        case PP_SELECTED: selected = Boolean.parseBoolean(s); break;
        case PP_ANCHOR_PCA: setPcaAnchored(Boolean.parseBoolean(s)); break;
        case PP_HIDDEN: setHidden(Boolean.parseBoolean(s)); break;
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }


    public static class Reader {

        private List<DatumField> fields;

        public Reader(List<String> headers) {
            fields = new ArrayList(headers.size());
            for (String s: headers) fields.add(DatumField.valueOf(s));
        }

        public Datum fromStrings(List<String> strings) {
            Datum d = new Datum(Vec3.ORIGIN);
            for (int i=0; i<strings.size(); i++) {
                d.setValue(fields.get(i), strings.get(i));
            }
            return d;
        }
    }

    public List<String> toStrings() {
        List<String> result = new ArrayList<String>(fields.size());
        for (DatumField df : fields) {
            result.add(getValue(df));
        }
        return result;
    }
}
