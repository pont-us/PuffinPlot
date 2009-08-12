package net.talvi.puffinplot.data;

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
    private double magSus=NaN; // default to "not mag sus" if no such field
    private double sampAz=NaN, sampDip=NaN, formAz=NaN, formDip=NaN;
    private double magDev=0;
    private String depth=null;
    private double irmGauss=NaN, armGauss=NaN;
    private ArmAxis armAxis = ArmAxis.UNKNOWN;
    private Vec3 moment = null;
    private int runNumber = -1;
    private double volume = DEFAULT_VOLUME;
    private double area = DEFAULT_AREA;
    private String timeStamp = "UNSET"; // NB this is a magic value; see below
    private Line line;
    private boolean selected = false;
    private boolean pcaAnchored = true;
    private boolean hidden = false;
    private Sample sample;
    private boolean doVolumeCorrection;

    public Datum(double x, double y, double z) {
        moment = new Vec3(x, y, z);
    }
    
    public Datum(Vec3 v) {
        moment = v; // v is immutable so it's OK not to copy it
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

    
    public Object getValue(DatumField field) {
        switch (field) {
        case AFX: return afx;
        case AFY: return afy;
        case AFZ: return afz;
        case TEMP: return temp;
        case MSCORR: return magSus;
        case SAMPLEAZ: return getSampAz();
        case SAMPLEDIP: return getSampDip();
        case FORMAZ: return getFormAz();
        case FORMDIP: return getFormDip();
        case MAGDEV: return getMagDev();
        case XCORR: return 0;
        case YCORR: return 0;
        case ZCORR: return 0;
        case DEPTH: return depth;
        case IRMGAUSS: return irmGauss;
        case ARMGAUSS: return armGauss;
        case VOLUME: return volume;
        case SAMPLEID: return sampleId;
        case MEASTYPE: return measType;
        case TREATMENT: return treatType;
        case ARMAXIS: return armAxis;
        case TIMESTAMP: return timeStamp;
        case RUNNUMBER: return runNumber;
        case AREA: return area;
        case PP_SELECTED: return selected;
        case PP_ANCHOR_PCA: return isPcaAnchored();
        case PP_HIDDEN: return isHidden();
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }

    public void setValue(DatumField field, Object o) {
        double dummy;
        switch (field) {
        case AFX: afx = (Double) o; break;
        case AFY: afy = (Double) o; break;
        case AFZ: afz = (Double) o; break;
        case TEMP: temp = (Double) o; break;
        case MSCORR: magSus = (Double) o; break;
        case SAMPLEAZ: setSampAz((Double) o); break;
        case SAMPLEDIP: setSampDip((Double) o); break;
        case FORMAZ: setFormAz((Double) o); break;
        case FORMDIP: setFormDip((Double) o); break;
        case MAGDEV: setMagDev((Double) o); break;
        case XCORR: dummy = (Double) o; break;
        case YCORR: dummy = (Double) o; break;
        case ZCORR: dummy = (Double) o; break;
        case DEPTH: depth = (String) o; break;
        case IRMGAUSS: irmGauss = (Double) o; break;
        case ARMGAUSS: armGauss = (Double) o; break;
        case VOLUME: volume = (Double) o; break;
        case SAMPLEID: sampleId = (String) o; break;
        case MEASTYPE: measType = (MeasType) o; break;
        case TREATMENT: treatType = (TreatType) o; break;
        case ARMAXIS: armAxis = (ArmAxis) o; break;
        case TIMESTAMP: timeStamp = (String) o; break;
        case RUNNUMBER: runNumber = (Integer) o; break;
        case AREA: area = (Double) o; break;
        case PP_SELECTED: selected = (Boolean) o; break;
        case PP_ANCHOR_PCA: setPcaAnchored((boolean) (Boolean) o); break;
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }
}
