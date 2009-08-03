package net.talvi.puffinplot.data;

import java.util.List;
import java.util.regex.Pattern;
import static java.lang.Double.NaN;
import static java.lang.Math.toRadians;
import static java.lang.Math.sin;
import static java.lang.Math.cos;

public class Datum {

    private static final double
         defaultCoreArea = 4.0, // can be overridden by Area field in file
     defaultVolume = 10.8; // can be overridden by Volume field in file

   private String sampleId = "UNSET";
    private MeasType measType = MeasType.UNSET;
    private TreatType treatType = TreatType.UNKNOWN;
    private double afx=NaN, afy=NaN, afz=NaN;
    private double temp=NaN;
    private double magSus=NaN; // default to "not mag sus" if no such field
    private double sampAz=NaN, sampDip=NaN, formAz=NaN, formDip=NaN;
    private double magDev=0;
    private double depth=NaN;
    private double irmGauss=NaN, armGauss=NaN;
    private ArmAxis armAxis = ArmAxis.UNKNOWN;
    private Vec3 moment;
    private int runNumber = -1;
    private double volume = defaultVolume;
    private double area = defaultCoreArea;
    private String timeStamp = "UNSET"; // NB this is a magic value; see below

    private Line line;
    private boolean selected = false;
    private boolean pcaAnchored = true;
    private boolean hidden = false;
    
    private final static Pattern delimPattern = Pattern.compile("\\t");
    private final static Pattern numberPattern = Pattern.compile("\\d+(\\.\\d+)?");
    private Sample sample;
    private boolean doVolumeCorrection;

    public static class Builder {
        private static final double BLANK = Double.NaN;
        private double x = BLANK, y = BLANK, z = BLANK;
        private double temperature = BLANK;
        private double afx = BLANK,  afy = BLANK,  afz = BLANK;
        private double magSus = BLANK;
        private double sampAz = BLANK, sampDip = BLANK;
        private double formAz = BLANK, formDip = BLANK;
        private ArmAxis armAxis = ArmAxis.UNKNOWN;
        private boolean doVolumeCorrection = false;
        private double volume = BLANK, area = BLANK;

        private Builder(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static Builder polar(double dec, double inc, double intensity) {
            final double m = intensity;
            final double i = toRadians(inc);
            final double d = toRadians(dec);
            return new Builder(m * cos(i) * cos(d),
                    m * cos(i) * sin(d),
                    m * sin(i));
        }

        public static Builder cartesian(double x, double y, double z) {
            return new Builder(x, y, z);
        }

        public Builder temperature(double value) { temperature = value; return this; }
        public Builder afx(double value) { afx = value; return this; }
        public Builder afy(double value) { afy = value; return this; }
        public Builder afz(double value) { afz = value; return this; }
        public Builder magSus(double value) { magSus = value; return this; }
        public Builder sampAz(double value) { sampAz = value; return this; }
        public Builder sampDip(double value) { sampDip = value; return this; }
        public Builder formAz(double value) { formAz = value; return this; }
        public Builder formDip(double value) { formDip = value; return this; }
        //public Builder armAxis(double value) { armAxis = value; return this; }
        //public Builder doVolumeCorrection(double value) { doVolumeCorrection = value; return this; }
        public Builder volume(double value) { volume = value; return this; }
        public Builder area(double value) { area = value; return this; }
        //public Builder (double value) {  = value; return this; }
    }

    public Datum(Builder builder) {
        final Builder b = builder;
        moment = new Vec3(b.x, b.y, b.z);
        temp = b.temperature;
        afx = b.afx;
        afy = b.afy;
        afz = b.afz;
        magSus = b.magSus;
        sampAz = b.sampAz;
        sampDip = b.sampDip;
        formAz = b.formAz;
        formDip = b.formDip;
        armAxis = b.armAxis;
        doVolumeCorrection = b.doVolumeCorrection;
        volume = b.volume;
        area = b.area;
    }

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
    public double getDepth()           { return depth; }
    public double getMagSus()          { return magSus; }
    public void setMagSus(double v)    { magSus = v; }
    public MeasType getMeasType()      { return measType; }
    public String getSampleId()        { return sampleId; }
    public TreatType getTreatType()    { return treatType; }

    public boolean isMagSus()          { return !Double.isNaN(magSus); }

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
        return /* isMagSus() || */
                (getTreatType() == TreatType.ARM) ||
                getMeasType() == MeasType.NONE;
    }


//    public Datum(String dataLine, List<TwoGeeField> fields, Line line,
//            boolean oldSquids) {
//        this.line = line;
//        NaScanner s = new NaScanner(dataLine);
//        for (TwoGeeField f: fields) {
//            try {
//                switch (f) {
//            case SAMPLEID: sampleId = s.next(); break;
//            case MEASTYPE: measType = MeasType.fromString(s.next()); break;
//            case TREATMENT: treatType = TreatType.fromString(s.next()); break;
//            case AFX: afx = s.nextD(); break;
//            case AFY: afy = s.nextD(); break;
//            case AFZ: afz = s.nextD(); break;
//            case TEMP: temp = s.nextD(); break;
//            case DECUC: decUc = s.nextD(); break;
//            case INCUC: incUc = s.nextD(); break;
//            case DECSC: decSc = s.nextD(); break;
//            case INCSC: incSc = s.nextD(); break;
//            case DECFC: decFc = s.nextD(); break;
//            case INCFC: incFc = s.nextD(); break;
//            case INTENSITY: intensity = s.nextD(); break;
//            case MSCORR: magSus = s.nextD(); break;
//            case SAMPLEAZ: sampAz = s.nextD(); break;
//            case SAMPLEDIP: sampDip = s.nextD(); break;
//            case FORMAZ: formAz = s.nextD(); break;
//            case FORMDIP: formDip = s.nextD(); break;
//            case XMEAN: xMean = s.nextD(); break;
//            case YMEAN: yMean = s.nextD(); break;
//            case ZMEAN: zMean = s.nextD(); break;
//            case MAGDEV: magDev = s.nextD(); break;
//            case XCORR: xCorr = s.nextD(); break;
//            case YCORR: yCorr = s.nextD(); break;
//            case ZCORR: zCorr = s.nextD(); break;
//            case XDRIFT: xDrift = s.nextD(); break;
//            case YDRIFT: yDrift = s.nextD(); break;
//            case ZDRIFT: zDrift = s.nextD(); break;
//            case DEPTH: depth = s.nextD(); break;
//            case IRMGAUSS: irmGauss = s.nextD(); break;
//            case ARMGAUSS: armGauss = s.nextD(); break;
//            case ARMAXIS: armAxis = ArmAxis.getByString(s.next()); break;
//            case VOLUME: volume = s.nextD(); break;
//            case XBKG1: xbkg1 = s.nextD(); break;
//            case XBKG2: xbkg2 = s.nextD(); break;
//            case YBKG1: ybkg1 = s.nextD(); break;
//            case YBKG2: ybkg2 = s.nextD(); break;
//            case ZBKG1: zbkg1 = s.nextD(); break;
//            case ZBKG2: zbkg2 = s.nextD(); break;
//            case RUNNUMBER: runNumber = s.nextInt(); break;
//            case TIMESTAMP: timeStamp = s.next(); break;
//            case AREA: area = s.nextD(); break;
//            case PP_SELECTED: selected = s.nextBoolean(); break;
//            case PP_ANCHOR_PCA: pcaAnchored = s.nextBoolean(); break;
//            case PP_HIDDEN: hidden = s.nextBoolean(); break;
//            case UNKNOWN: s.next(); break;
//            default: s.next(); break;
//                }
//            } catch (InputMismatchException e) {
//                throw new IllegalArgumentException("Expected a number for " +
//                        f.getHeading());
//            } catch (NumberFormatException e) {
//                throw new IllegalArgumentException("Expected a number for " +
//                        f.getHeading());
//            } catch (NoSuchElementException e) {
//                throw new IllegalArgumentException("Couldn't read " +
//                        f.getHeading());
//            }
//        }
//
//        if (Double.isNaN(magDev)) magDev = 0; // sensible default
//
//        if (measType==null) {
//          throw new IllegalArgumentException("No measurement type specified");
//        }
//
//        switch (measType) {
//            case CONTINUOUS:
//
//                // We no longer try to guess whether the SQUIDs are in
//                // the old or new orientation; Faye said it's unreliable.
//
//                // ADDENDUM 2009-02-12: turns out that the effective
//                // sensor lengths for the Y and Z SQUIDs have been
//                // negated in the new set-up. To summarize:
//                //   old new
//                // x  -   +
//                // y  +   -
//                // z  -   -
//
//                double xVol = area * sensorLengthX;
//                double yVol = area * sensorLengthY;
//                double zVol = area * sensorLengthZ;
//                uc = new Vec3( (oldSquids ? -1 : 1) * xCorr / xVol,
//                        (oldSquids ? 1 : -1) * yCorr / yVol,
//                        -zCorr / zVol);
//                break;
//            case DISCRETE:
//                uc = new Vec3(xCorr / volume, yCorr / volume, zCorr / volume);
//                break;
//            case NONE:
//                // do nothing, since there is no data.
//                break;
//            default:
//                throw new IllegalArgumentException
//                        ("Unknown measurement type "+measType);
//        }
//        if (!ignoreOnLoading()) line.add(this);
//        // applyCorrections();
//    }

    
//    public Object getValue(TwoGeeField field) {
//        switch (field) {
//        case AFX: return afx;
//        case AFY: return afy;
//        case AFZ: return afz;
//        case TEMP: return temp;
//        case DECUC: return decUc;
//        case INCUC: return incUc;
//        case DECSC: return decSc;
//        case INCSC: return incSc;
//        case DECFC: return decFc;
//        case INCFC: return incFc;
//        case INTENSITY: return intensity;
//        case MSCORR: return magSus;
//        case SAMPLEAZ: return getSampAz();
//        case SAMPLEDIP: return getSampDip();
//        case FORMAZ: return getFormAz();
//        case FORMDIP: return getFormDip();
//        case XMEAN: return xMean;
//        case YMEAN: return yMean;
//        case ZMEAN: return zMean;
//        case MAGDEV: return getMagDev();
//        case XCORR: return xCorr;
//        case YCORR: return yCorr;
//        case ZCORR: return zCorr;
//        case XDRIFT: return xDrift;
//        case YDRIFT: return yDrift;
//        case ZDRIFT: return zDrift;
//        case DEPTH: return depth;
//        case IRMGAUSS: return irmGauss;
//        case ARMGAUSS: return armGauss;
//        case VOLUME: return volume;
//        case XBKG1: return xbkg1;
//        case XBKG2: return xbkg2;
//        case YBKG1: return ybkg1;
//        case YBKG2: return ybkg2;
//        case ZBKG1: return zbkg1;
//        case ZBKG2: return zbkg2;
//        case SAMPLEID: return sampleId;
//        case MEASTYPE: return measType;
//        case TREATMENT: return treatType;
//        case ARMAXIS: return armAxis;
//        case TIMESTAMP: return timeStamp;
//        case RUNNUMBER: return runNumber;
//        case AREA: return area;
//        case PP_SELECTED: return selected;
//        case PP_ANCHOR_PCA: return isPcaAnchored();
//        case PP_HIDDEN: return isHidden();
//        default: throw new IllegalArgumentException("Unknown field "+field);
//        }
//    }
//
//    public void setValue(TwoGeeField field, Object o) {
//        switch (field) {
//        case AFX: afx = (Double) o; break;
//        case AFY: afy = (Double) o; break;
//        case AFZ: afz = (Double) o; break;
//        case TEMP: temp = (Double) o; break;
//        case DECUC: decUc = (Double) o; break;
//        case INCUC: incUc = (Double) o; break;
//        case DECSC: decSc = (Double) o; break;
//        case INCSC: incSc = (Double) o; break;
//        case DECFC: decFc = (Double) o; break;
//        case INCFC: incFc = (Double) o; break;
//        case INTENSITY: intensity = (Double) o; break;
//        case MSCORR: magSus = (Double) o; break;
//        case SAMPLEAZ: setSampAz((Double) o); break;
//        case SAMPLEDIP: setSampDip((Double) o); break;
//        case FORMAZ: setFormAz((Double) o); break;
//        case FORMDIP: setFormDip((Double) o); break;
//        case XMEAN: xMean = (Double) o; break;
//        case YMEAN: yMean = (Double) o; break;
//        case ZMEAN: zMean = (Double) o; break;
//        case MAGDEV: setMagDev((Double) o); break;
//        case XCORR: xCorr = (Double) o; break;
//        case YCORR: yCorr = (Double) o; break;
//        case ZCORR: zCorr = (Double) o; break;
//        case XDRIFT: xDrift = (Double) o; break;
//        case YDRIFT: yDrift = (Double) o; break;
//        case ZDRIFT: zDrift = (Double) o; break;
//        case DEPTH: depth = (Double) o; break;
//        case IRMGAUSS: irmGauss = (Double) o; break;
//        case ARMGAUSS: armGauss = (Double) o; break;
//        case VOLUME: volume = (Double) o; break;
//        case XBKG1: xbkg1 = (Double) o; break;
//        case XBKG2: xbkg2 = (Double) o; break;
//        case YBKG1: ybkg1 = (Double) o; break;
//        case YBKG2: ybkg2 = (Double) o; break;
//        case ZBKG1: zbkg1 = (Double) o; break;
//        case ZBKG2: zbkg2 = (Double) o; break;
//        case SAMPLEID: sampleId = (String) o; break;
//        case MEASTYPE: measType = (MeasType) o; break;
//        case TREATMENT: treatType = (TreatType) o; break;
//        case ARMAXIS: armAxis = (ArmAxis) o; break;
//        case TIMESTAMP: timeStamp = (String) o; break;
//        case RUNNUMBER: runNumber = (Integer) o; break;
//        case AREA: area = (Double) o; break;
//        case PP_SELECTED: selected = (Boolean) o; break;
//        case PP_ANCHOR_PCA: setPcaAnchored((boolean) (Boolean) o); break;
//        default: throw new IllegalArgumentException("Unknown field "+field);
//        }
//    }
}
