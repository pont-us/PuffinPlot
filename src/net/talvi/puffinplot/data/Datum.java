package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.List;
import static java.lang.Double.NaN;
import static java.lang.Math.toRadians;

public class Datum {

    private static final double
            DEFAULT_AREA = 4.0, // can be overridden by Area field in file
            DEFAULT_VOLUME = 10.8; // can be overridden by Volume field in file

    private String discreteId = "UNSET";
    private MeasType measType = MeasType.UNSET;
    private TreatType treatType = TreatType.UNKNOWN;
    private double afx=NaN, afy=NaN, afz=NaN;
    private double temp=NaN;
    private double magSus=NaN;
    private double sampAz=NaN, sampDip=NaN, formAz=NaN, formDip=NaN;
    private double magDev=0;
    private String depth=null;
    private double irmField=NaN, armField=NaN;
    private ArmAxis armAxis = ArmAxis.UNKNOWN;
    private Vec3 moment = null;
    private int runNumber = -1;
    private double volume = DEFAULT_VOLUME;
    private double area = DEFAULT_AREA;
    private String timestamp = "UNSET";
    private double xDrift, yDrift, zDrift;
    private int slotNumber = -1;

    private Line line;
    private boolean selected = false;
    private boolean inPca = false;
    private boolean onCircle = false;
    private boolean pcaAnchored = true;
    private boolean hidden = false;

    private Sample sample;
    private Suite suite;
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
    public void setLine(Line v)        { line = v; }
    public Sample getSample()          { return sample; }
    public boolean isHidden()          { return hidden; }
    public void setHidden(boolean v)   { hidden = v; }
    public void setSample(Sample v)    { sample = v; }
    public String getDepth()           { return depth; }
    public void setDepth(String v)     { depth = v; }
    public double getMagSus()          { return magSus; }
    public void setMagSus(double v)    { magSus = v; }
    public MeasType getMeasType()      { return measType; }
    public void setMeasType(MeasType v){ measType = v; }
    public String getDiscreteId()      { return discreteId; }
    public void setDiscreteId(String v){ discreteId = v; }
    public TreatType getTreatType()    { return treatType; }
    public void setTreatType(TreatType v) { treatType = v; }
    public double getAfX()             { return afx; }
    public void setAfX(double v)       { afx = v; }
    public double getAfY()             { return afy; }
    public void setAfY(double v)       { afy = v; }
    public double getAfZ()             { return afz; }
    public void setAfZ(double v)       { afz = v; }
    public double getIrmField()        { return irmField; }
    public void setIrmField(double v)  { irmField = v; }
    public double getArmField()        { return armField; }
    public void setArmField(double v)  { armField = v; }
    public ArmAxis getArmAxis()        { return armAxis; }
    public void setArmAxis(ArmAxis v)  { armAxis = v; }
    public double getTemp()            { return temp; }
    public void setTemp(double v)      { temp = v; }
    public double getArea()            { return area; }
    public void setArea(double v)      { area = v; }
    public double getVolume()          { return volume; }
    public void setVolume(double v)    { volume = v; }
    public int getRunNumber()          { return runNumber; }
    public void setRunNumber(int v)    { runNumber = v; }
    public String getTimestamp()       { return timestamp; }
    public void setTimestamp(String v) { timestamp = v; }
    public double getXDrift()          { return xDrift; }
    public void setXDrift(double v)    { xDrift = v; }
    public double getYDrift()          { return yDrift; }
    public void setYDrift(double v)    { yDrift = v; }
    public double getZDrift()          { return zDrift; }
    public void setZDrift(double v)    { zDrift = v; }
    public int getSlotNumber()         { return slotNumber; }
    public void setSlotNumber(int v)   { slotNumber = v; }
    public Suite getSuite()            { return suite; }
    public void setSuite(Suite v)      { suite = v; }
    public boolean isOnCircle()        { return onCircle; }
    public void setOnCircle(boolean v) { onCircle = v; }
    public boolean isInPca()           { return inPca; }
    public void setInPca(boolean v)    { inPca = v; }

    public String getIdOrDepth() {
        return measType == MeasType.CONTINUOUS ? depth : discreteId;
    }
    public boolean hasMagSus()          { return !Double.isNaN(magSus); }

    public boolean isMagSusOnly() {
        return moment == null && hasMagSus();
    }

    private boolean sampleCorrectionExists() {
        return (!Double.isNaN(sampAz)) && (!Double.isNaN(sampDip));
    }

    private boolean formationCorrectionExists() {
        return (!Double.isNaN(formAz)) && (!Double.isNaN(formDip));
    }

    /**
    * Sets the sample's magnetic dipole moment per unit volume
    * in A/m.
    * @param v
    */
    public void setMoment(Vec3 v)      { moment = v; }

    /**
     * The name is slightly misleading: we do not deal with magnetic
     * moment (which would be in Am^2) but in magnetic dipole moment
     * per unit volume (in A/m). But
     * getMagneticDipoleMomentPerUnitVolumeInAm would be an inconveniently
     * long method name.
     *
     * @param c
     * @return
     */
    public Vec3 getMoment(Correction c) {
        Vec3 result = moment;
        if (c.includesEmpty()) {
            result = result.minus(getLine().getEmptySlot().moment);
        }
        if (c.includesTray()) {
            Datum tray = getSuite().getTrayCorrection(this);
            if (tray != null) result = result.minus(tray.moment);
        }
        if (c.includesSample() && sampleCorrectionExists()) {
            result = result.correctSample(toRadians(sampAz + magDev),
                                          toRadians(sampDip));
            if (c.includesFormation() && formationCorrectionExists()) {
                result = result.correctForm(toRadians(formAz + magDev),
                                            toRadians(formDip));
            }
        }
        return result;
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
        case ARM: return afz; //usually we vary this & keep bias field constant
        case IRM: return getIrmField();
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

    public static double maximumIntensity(List<Datum> ds, Correction corr) {
        double max = 0;
        for (Datum d: ds) {
            double i = d.getIntensity(corr);
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

    /**
     * Returns magnetic dipole moment per unit volume in A/m.
     * The correction should not, of course, matter.
     * TODO: verify this empirically and remove the parameter.
     * @param correction
     * @return
     */
    public double getIntensity(Correction correction) {
        return getMoment(correction).mag();
    }

    public boolean ignoreOnLoading() {
        return /* getTreatType() == TreatType.ARM || */
                getMeasType() == MeasType.NONE;
    }

    public boolean hasMagMoment() {
        return moment != null;
    }

    private String fmt(Double d) {
        return Double.toString(d);
    }

    public String getValue(DatumField field) {
        switch (field) {
        case AF_X: return fmt(afx);
        case AF_Y: return fmt(afy);
        case AF_Z: return fmt(afz);
        case TEMPERATURE: return fmt(temp);
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
        case IRM_FIELD: return fmt(getIrmField());
        case ARM_FIELD: return fmt(armField);
        case VOLUME: return fmt(volume);
        case DISCRETE_ID: return discreteId;
        case MEAS_TYPE: return measType.toString();
        case TREATMENT: return treatType.toString();
        case ARM_AXIS: return armAxis.toString();
        case TIMESTAMP: return timestamp;
        case RUN_NUMBER: return Integer.toString(runNumber);
        case SLOT_NUMBER: return Integer.toString(slotNumber);
        case AREA: return fmt(area);
        case PP_SELECTED: return Boolean.toString(selected);
        case PP_ANCHOR_PCA: return Boolean.toString(isPcaAnchored());
        case PP_HIDDEN: return Boolean.toString(isHidden());
        case PP_ONCIRCLE: return Boolean.toString(isOnCircle());
        case PP_INPCA: return Boolean.toString(isInPca());
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }

    public void setValue(DatumField field, String s) {
        switch (field) {
        case AF_X: afx = Double.parseDouble(s); break;
        case AF_Y: afy = Double.parseDouble(s); break;
        case AF_Z: afz = Double.parseDouble(s); break;
        case TEMPERATURE: temp = Double.parseDouble(s); break;
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
        case IRM_FIELD: setIrmField(Double.parseDouble(s)); break;
        case ARM_FIELD: armField = Double.parseDouble(s); break;
        case VOLUME: volume = Double.parseDouble(s); break;
        case DISCRETE_ID: discreteId = s; break;
        case MEAS_TYPE: measType = MeasType.valueOf(s); break;
        case TREATMENT: treatType = TreatType.valueOf(s); break;
        case ARM_AXIS: armAxis = ArmAxis.fromString(s); break;
        case TIMESTAMP: timestamp = s; break;
        case SLOT_NUMBER: slotNumber = Integer.parseInt(s); break;
        case RUN_NUMBER: runNumber = Integer.parseInt(s); break;
        case AREA: area = Double.parseDouble(s); break;
        case PP_SELECTED: selected = Boolean.parseBoolean(s); break;
        case PP_ANCHOR_PCA: setPcaAnchored(Boolean.parseBoolean(s)); break;
        case PP_HIDDEN: setHidden(Boolean.parseBoolean(s)); break;
        case PP_ONCIRCLE: setOnCircle(Boolean.parseBoolean(s)); break;
        case PP_INPCA: setInPca(Boolean.parseBoolean(s)); break;
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
