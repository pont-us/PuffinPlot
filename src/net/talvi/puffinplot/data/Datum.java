package net.talvi.puffinplot.data;

import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Datum {

    private static final double sensorLengthX = 4.628,
     sensorLengthY = 4.404,
     sensorLengthZ = 6.280,
     defaultCoreArea = 4.0; // can be overridden by Area field in file

    private String sampleId;
    private MeasType measType;
    private TreatType treatType;
    private double afx, afy, afz;
    private double temp;
    private double decUc, incUc, decSc, incSc, decFc, incFc;
    private double intensity;
    private double magSus = Double.NaN; // default to "not mag sus" if no such field
    private double sampAz, sampDip, formAz, formDip;
    private double magDev;
    private double xCorr, yCorr, zCorr;
    private double xDrift, yDrift, zDrift;    
    private double xMean, yMean, zMean;
    private double depth;
    private double irmGauss, armGauss;
    private double xbkg1, xbkg2, ybkg1, ybkg2, zbkg1, zbkg2;
    private ArmAxis armAxis;
    private final Point uc, sc, fc;
    private double volume;
    private double area = defaultCoreArea;
    private int runNumber = -1;
    private String timeStamp = null;
    
    public boolean selected = false;
    
    private final static Pattern delimPattern = Pattern.compile("\\t");

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
            String next = s.next();
            return Integer.parseInt(next);
        }

        public String next() {
            return s.next();
        }
    }
    
    public Datum(String zPlotLine, MeasType measType, TreatType treatType) {
        Scanner s = new Scanner(zPlotLine);
        s.useLocale(Locale.ENGLISH); // don't want to be using commas as decimal separators...
        s.useDelimiter(delimPattern); // might have spaces within fields
        depth = s.nextDouble();
        sampleId = s.next();
        afx = afy = afz = s.nextDouble();
        decUc = s.nextDouble();
        incUc = s.nextDouble();
        intensity = s.nextDouble();
        magSus = Double.NaN;
        uc = Point.fromPolarDegrees(intensity, incUc, decUc);
        fc = sc = uc;
        this.measType = measType; 
        this.treatType = treatType;
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
                // TODO: make this configurable
                // by a user preferences setting.
                //
                final double flip = timeStamp==null ? -1 : 1;
                uc = new Point(flip * xCorr / (area * sensorLengthX),
                        yCorr / (area * sensorLengthY),
                        flip * zCorr / (area * sensorLengthZ));
                break;
            case DISCRETE:
                uc = new Point(xCorr / volume, yCorr / volume, zCorr / volume);
                break;
            default:
                throw new IllegalArgumentException
                        ("Unknown measurement type "+measType);
        }
        sc = uc.correctSample(Math.toRadians(sampAz), Math.toRadians(sampDip));
        fc = sc.correctForm(Math.toRadians(formAz), Math.toRadians(formDip));
    }
    
    public Point getPoint(Correction c) {
        switch (c) {
            case FORMATION: return fc;
            case SAMPLE: return sc;
            case NONE: return uc;
            default: throw new IllegalArgumentException("unknown correction");
        }
    }

    public void toggleSel() {
        selected = !selected;
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
        default: throw new IllegalArgumentException("unhandled treatment type");
        // NONE, DEGAUSS, IRM, THERMAL, UNKNOWN;
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
            case SAMPLEAZ: return sampAz;
            case SAMPLEDIP: return sampDip;
            case FORMAZ: return formAz;
            case FORMDIP: return formDip;
            case XMEAN: return xMean;
            case YMEAN: return yMean;
            case ZMEAN: return zMean;
            case MAGDEV: return magDev;
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
            default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }
}
