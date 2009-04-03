package net.talvi.puffinplot.data;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;
import static java.lang.Double.NaN;
import static java.lang.Math.toRadians;

public class Datum {

    // Note that sensors may have negative effective lengths, depending
    // on how they're mounted. These are the absolute values, and some
    // may be negated when calculating magnetization vectors for long cores.
    // (See the constructor for details.)
    private static final double sensorLengthX = 4.628,
     sensorLengthY = 4.404,
     sensorLengthZ = 6.280,
     defaultCoreArea = 4.0, // can be overridden by Area field in file
     defaultVolume = 10.8; // can be overridden by Volume field in file

    private String sampleId = "UNSET";
    private MeasType measType = MeasType.UNSET;
    private TreatType treatType = TreatType.UNKNOWN;
    private double afx=NaN, afy=NaN, afz=NaN;
    private double temp=NaN;
    private double decUc, incUc=NaN, decSc=NaN, incSc=NaN, decFc=NaN, incFc=NaN;
    private double intensity=NaN;
    private double magSus=NaN; // default to "not mag sus" if no such field
    private double sampAz=NaN, sampDip=NaN, formAz=NaN, formDip=NaN;
    private double magDev=0;
    private double xCorr=NaN, yCorr=NaN, zCorr=NaN;
    private double xDrift=NaN, yDrift=NaN, zDrift=NaN;    
    private double xMean=NaN, yMean=NaN, zMean=NaN;
    private double depth=NaN;
    private double irmGauss=NaN, armGauss=NaN;
    private double xbkg1=NaN, xbkg2=NaN, ybkg1=NaN, ybkg2=NaN, zbkg1=NaN, zbkg2=NaN;
    private ArmAxis armAxis = ArmAxis.UNKNOWN;
    private Vec3 uc, sc, fc;
    private double volume = defaultVolume;
    private double area = defaultCoreArea;
    private int runNumber = -1;
    private String timeStamp = "UNSET"; // NB this is a magic value; see below
    
    private boolean selected = false;
    private boolean pcaAnchored = false;
    
    private final static Pattern delimPattern = Pattern.compile("\\t");
    private final static Pattern numberPattern = Pattern.compile("\\d+(\\.\\d+)?");

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public double getSampAz() {
        return sampAz;
    }

    public void setSampAz(double sampAz) {
        this.sampAz = sampAz;
        applyCorrections();
    }

    public double getSampDip() {
        return sampDip;
    }

    public void setSampDip(double sampDip) {
        this.sampDip = sampDip;
        applyCorrections();
    }

    public double getFormAz() {
        return formAz;
    }

    public void setFormAz(double formAz) {
        this.formAz = formAz;
        applyCorrections();
    }

    public double getFormDip() {
        return formDip;
    }

    public void setFormDip(double formDip) {
        this.formDip = formDip;
        applyCorrections();
    }

    public double getMagDev() {
        return magDev;
    }

    public void setMagDev(double magDev) {
        this.magDev = magDev;
        applyCorrections();
    }

    public boolean isPcaAnchored() {
        return pcaAnchored;
    }

    public void setPcaAnchored(boolean pcaAnchored) {
        this.pcaAnchored = pcaAnchored;
    }

    private static class NaScanner {
        
/*  The nice way to do this would be to define a DecimalFormat where "NA" is the
 *  NaN symbol. But we can't specify a NumberFormat for a scanner, only a locale
 *  -- in which case we'd have to define a custom locale with the correct
 *  DecimalFormat, register it via the Java Extension Mechanism, and then
 *  select it with Scanner.useLocale(). NumberFormatProviders aren't available
 *  in Java 5 so this would be impossible on OS X anyway, and either way
 *  it would be a lot of effort.
 */
        
        private Scanner s;

        public NaScanner(String line) {
            s = new Scanner(line);
            s.useLocale(Locale.ENGLISH); // don't want to be using commas as decimal separators...
            s.useDelimiter(delimPattern); // might have spaces within fields
        }

        public double nextD() {
            String next = s.next();
            return  ("NA".equals(next))
                    ? Double.NaN
                    : Double.parseDouble(next);
        }
        
        public int nextInt() {
            return s.nextInt();
        }
        
        public boolean nextBoolean() {
            return s.nextBoolean();
        }

        public String next() {
            return s.next();
        }
    }
    
    public Datum(String zPlotLine) {
        Scanner s = new Scanner(zPlotLine);
        s.useLocale(Locale.ENGLISH); // don't want to be using commas as decimal separators...
        s.useDelimiter(delimPattern); // might have spaces within fields
        
        String depthOrSample = s.next();
        measType = (numberPattern.matcher(depthOrSample).matches())
                ? MeasType.CONTINUOUS
                : MeasType.DISCRETE;
        switch (measType) {
        case CONTINUOUS: depth = Double.parseDouble(depthOrSample);
            break;
        case DISCRETE: sampleId = depthOrSample;
            break;
        default: throw new Error("Can't happen.");
        }
        String project = s.next();
        double afOrThermalDemag = s.nextDouble();
        decUc = s.nextDouble();
        incUc = s.nextDouble();
        intensity = s.nextDouble();
        String operation = s.next();
        magSus = Double.NaN;
        uc = Vec3.fromPolarDegrees(intensity, incUc, decUc);
        fc = sc = uc;
        treatType = TreatType.DEGAUSS;
        if (project.toLowerCase().contains("therm") ||
                operation.toLowerCase().contains("therm"))
            treatType = TreatType.THERMAL;
        switch (treatType) {
        case DEGAUSS: afx = afy = afz = afOrThermalDemag;
        break;
        case THERMAL: temp = afOrThermalDemag;
        break;
        default: throw new Error("Can't happen.");
        }
    }
    
    public Datum(String line, List<TwoGeeField> fields) {
        NaScanner s = new NaScanner(line);
        for (TwoGeeField f: fields) {
            try {
                switch (f) {
            case SAMPLEID: sampleId = s.next(); break;
            case MEASTYPE: measType = MeasType.fromString(s.next()); break;
            case TREATMENT: treatType = TreatType.fromString(s.next()); break;
            case AFX: afx = s.nextD(); break;
            case AFY: afy = s.nextD(); break;
            case AFZ: afz = s.nextD(); break;
            case TEMP: temp = s.nextD(); break;
            case DECUC: decUc = s.nextD(); break;
            case INCUC: incUc = s.nextD(); break;
            case DECSC: decSc = s.nextD(); break;
            case INCSC: incSc = s.nextD(); break;
            case DECFC: decFc = s.nextD(); break;
            case INCFC: incFc = s.nextD(); break;
            case INTENSITY: intensity = s.nextD(); break;
            case MSCORR: magSus = s.nextD(); break;
            case SAMPLEAZ: sampAz = s.nextD(); break;
            case SAMPLEDIP: sampDip = s.nextD(); break;
            case FORMAZ: formAz = s.nextD(); break;
            case FORMDIP: formDip = s.nextD(); break;
            case XMEAN: xMean = s.nextD(); break;
            case YMEAN: yMean = s.nextD(); break;
            case ZMEAN: zMean = s.nextD(); break;
            case MAGDEV: magDev = s.nextD(); break;
            case XCORR: xCorr = s.nextD(); break;
            case YCORR: yCorr = s.nextD(); break;
            case ZCORR: zCorr = s.nextD(); break;
            case XDRIFT: xDrift = s.nextD(); break;
            case YDRIFT: yDrift = s.nextD(); break;
            case ZDRIFT: zDrift = s.nextD(); break;
            case DEPTH: depth = s.nextD(); break;
            case IRMGAUSS: irmGauss = s.nextD(); break;
            case ARMGAUSS: armGauss = s.nextD(); break;
            case ARMAXIS: armAxis = ArmAxis.getByString(s.next()); break;
            case VOLUME: volume = s.nextD(); break;
            case XBKG1: xbkg1 = s.nextD(); break;
            case XBKG2: xbkg2 = s.nextD(); break;
            case YBKG1: ybkg1 = s.nextD(); break;
            case YBKG2: ybkg2 = s.nextD(); break;
            case ZBKG1: zbkg1 = s.nextD(); break;
            case ZBKG2: zbkg2 = s.nextD(); break;
            case RUNNUMBER: runNumber = s.nextInt(); break;
            case TIMESTAMP: timeStamp = s.next(); break;
            case AREA: area = s.nextD(); break;
            case PP_SELECTED: selected = s.nextBoolean(); break;
            case PP_ANCHOR_PCA: pcaAnchored = s.nextBoolean(); break;
            case UNKNOWN: s.next(); break;
            default: s.next(); break;
                }
            } catch (InputMismatchException e) {
                throw new IllegalArgumentException("Expected a number for " +
                        f.getHeading());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Expected a number for " +
                        f.getHeading());
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException("Couldn't read " +
                        f.getHeading());
            }
        }
        
        if (Double.isNaN(magDev)) magDev = 0; // sensible default

        if (measType==null) {
          throw new IllegalArgumentException("No measurement type specified");
        }
        
        switch (measType) {
            case CONTINUOUS:
                // Try to guess whether this file is from the era when
                // the X and Z SQUIDs were installed back-to-front.
                // The timestamp was added to the file format at around
                // the time Gary fixed the SQUIDs, so this should be a
                // fairly good indicator. 
                // (Used to be run number but G. thought we might have
                // had that in the format previously.)
                
                // ADDENDUM 2009-02-12: turns out that the effective
                // sensor lengths for the Y and Z SQUIDs have been 
                // negated in the new set-up. To summarize:
                //   old new
                // x  -   +
                // y  +   -
                // z  -   -
                
                // TODO: make this configurable
                // by a user preferences setting.
                //
                boolean old = timeStamp.equals("UNSET");
                double xVol = area * sensorLengthX;
                double yVol = area * sensorLengthY;
                double zVol = area * sensorLengthZ;
                uc = new Vec3( (old ? -1 : 1) * xCorr / xVol,
                        (old ? 1 : -1) * yCorr / yVol,
                        -zCorr / zVol);
                break;
            case DISCRETE:
                uc = new Vec3(xCorr / volume, yCorr / volume, zCorr / volume);
                break;
            default:
                throw new IllegalArgumentException
                        ("Unknown measurement type "+measType);
        }
        applyCorrections();
    }
    
    private void applyCorrections() {
        sc = Double.isNaN(sampAz) || Double.isNaN(sampDip)
                ? uc : uc.correctSample(toRadians(sampAz - magDev), toRadians(sampDip));
        fc = Double.isNaN(formAz) || Double.isNaN(formDip)
                ? sc : sc.correctForm(toRadians(formAz - magDev), toRadians(formDip));
    }
    
    public Vec3 getPoint(Correction c) {
        switch (c) {
            case FORMATION: return fc;
            case SAMPLE: return sc;
            case NONE: return uc;
            default: throw new IllegalArgumentException("unknown correction");
        }
    }
    
    /*
     *  Rotate orientations 180 degrees about X axis.
     */
    public void rotX180() {
        uc = uc.rotX180();
        applyCorrections();
    }

    public void toggleSel() {
        setSelected(!isSelected());
    }
    
    public boolean isMagSus() {
        return !Double.isNaN(magSus);
    }

    public MeasType getMeasType() {
        return measType;
    }

    public String getSampleId() {
        return sampleId;
    }
    
    public TreatType getTreatType() {
        return treatType;
    }
    
    public double getDemagLevel() {
        switch (treatType) {
        case NONE: return 0;
        case DEGAUSS: return afx>0?afx : afy>0?afy : afz;
        case THERMAL: return temp;
        case ARM: return afz;
        default: throw new IllegalArgumentException("unhandled treatment type");
        }
    }

    public double getIntensity() {
        return intensity;
    }

    public double getDepth() {
        return depth;
    }
    
    public Object getValue(TwoGeeField field) {
        switch (field) {
        case AFX: return afx;
        case AFY: return afy;
        case AFZ: return afz;
        case TEMP: return temp;
        case DECUC: return decUc;
        case INCUC: return incUc;
        case DECSC: return decSc;
        case INCSC: return incSc;
        case DECFC: return decFc;
        case INCFC: return incFc;
        case INTENSITY: return intensity;
        case MSCORR: return magSus;
        case SAMPLEAZ: return getSampAz();
        case SAMPLEDIP: return getSampDip();
        case FORMAZ: return getFormAz();
        case FORMDIP: return getFormDip();
        case XMEAN: return xMean;
        case YMEAN: return yMean;
        case ZMEAN: return zMean;
        case MAGDEV: return getMagDev();
        case XCORR: return xCorr;
        case YCORR: return yCorr;
        case ZCORR: return zCorr;
        case XDRIFT: return xDrift;
        case YDRIFT: return yDrift;
        case ZDRIFT: return zDrift;
        case DEPTH: return depth;
        case IRMGAUSS: return irmGauss;
        case ARMGAUSS: return armGauss;
        case VOLUME: return volume;
        case XBKG1: return xbkg1;
        case XBKG2: return xbkg2;
        case YBKG1: return ybkg1;
        case YBKG2: return ybkg2;
        case ZBKG1: return zbkg1;
        case ZBKG2: return zbkg2;
        case SAMPLEID: return sampleId;
        case MEASTYPE: return measType;
        case TREATMENT: return treatType;
        case ARMAXIS: return armAxis;
        case TIMESTAMP: return timeStamp;
        case RUNNUMBER: return runNumber;
        case AREA: return area;
        case PP_SELECTED: return selected;
        case PP_ANCHOR_PCA: return isPcaAnchored();
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }
    
    public void setValue(TwoGeeField field, Object o) {
        switch (field) {
        case AFX: afx = (Double) o; break;
        case AFY: afy = (Double) o; break;
        case AFZ: afz = (Double) o; break;
        case TEMP: temp = (Double) o; break;
        case DECUC: decUc = (Double) o; break;
        case INCUC: incUc = (Double) o; break;
        case DECSC: decSc = (Double) o; break;
        case INCSC: incSc = (Double) o; break;
        case DECFC: decFc = (Double) o; break;
        case INCFC: incFc = (Double) o; break;
        case INTENSITY: intensity = (Double) o; break;
        case MSCORR: magSus = (Double) o; break;
        case SAMPLEAZ: setSampAz((Double) o); break;
        case SAMPLEDIP: setSampDip((Double) o); break;
        case FORMAZ: setFormAz((Double) o); break;
        case FORMDIP: setFormDip((Double) o); break;
        case XMEAN: xMean = (Double) o; break;
        case YMEAN: yMean = (Double) o; break;
        case ZMEAN: zMean = (Double) o; break;
        case MAGDEV: setMagDev((Double) o); break;
        case XCORR: xCorr = (Double) o; break;
        case YCORR: yCorr = (Double) o; break;
        case ZCORR: zCorr = (Double) o; break;
        case XDRIFT: xDrift = (Double) o; break;
        case YDRIFT: yDrift = (Double) o; break;
        case ZDRIFT: zDrift = (Double) o; break;
        case DEPTH: depth = (Double) o; break;
        case IRMGAUSS: irmGauss = (Double) o; break;
        case ARMGAUSS: armGauss = (Double) o; break;
        case VOLUME: volume = (Double) o; break;
        case XBKG1: xbkg1 = (Double) o; break;
        case XBKG2: xbkg2 = (Double) o; break;
        case YBKG1: ybkg1 = (Double) o; break;
        case YBKG2: ybkg2 = (Double) o; break;
        case ZBKG1: zbkg1 = (Double) o; break;
        case ZBKG2: zbkg2 = (Double) o; break;
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
