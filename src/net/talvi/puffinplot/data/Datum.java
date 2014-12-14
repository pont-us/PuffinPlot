/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import static java.lang.Double.NaN;
import static java.lang.Double.parseDouble;
import static java.lang.Math.toRadians;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * <p>Datum is the fundamental data class of PuffinPlot. It represents the
 * state of a sample at a particular point during the stepwise demagnetization
 * process. The essential item of data is the demagnetization vector
 * representing a particular magnetometer measurement. A large number of
 * other fields give additional data about demagnetization procedures,
 * other measurements, and sample characteristics. Some of these fields
 * (such as the sample orientation) are in fact sample-level data rather
 * than demagnetization-step-level data, and may be moved to {@link Sample}
 * in a later version of PuffinPlot.</p>
 * 
 * <p>Datum is a mutable container class which is intended to be instantiated
 * with no or very little data. Most of the fields can be set after
 * instantiation using setter methods.</p>
 * 
 * <p>In terms of PuffinPlot's user interface, a Datum often
 * defines the position and appearance of a point on one or more
 * of the plots.</p>
 * 
 * @see DatumField
 * 
 * @author pont
 */
public class Datum {
    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private static final double
            DEFAULT_AREA = 4.0, // can be overridden by Area field in file
            DEFAULT_VOLUME = 10.8; // can be overridden by Volume field in file
    private String discreteId = "UNSET";
    private MeasType measType = MeasType.UNSET;
    private TreatType treatType = TreatType.UNKNOWN;
    private double afx = NaN, afy = NaN, afz = NaN;
    private double temp = NaN;
    private double magSus = NaN;
    private double sampAz = NaN, sampDip = NaN, formAz = NaN, formDip = NaN;
    private double magDev = 0;
    private String depth = null;
    private double irmField = NaN, armField = NaN;
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
    private LinkedHashMap<String, String> h;

    /**
     * Creates a datum with a specified magnetization vector.
     * @param x x component of the magnetization vector
     * @param y y component of the magnetization vector
     * @param z z component of the magnetization vector
     */
    public Datum(double x, double y, double z) {
        moment = new Vec3(x, y, z);
    }
    
    /**
     * Creates a datum with a supplied magnetization vector.
     * @param vector the magnetization vector
     */
    public Datum(Vec3 vector) {
        moment = vector; // v is immutable so it's OK not to copy it
    }

    /**
     * Creates a datum with no data. The moment is set to zero.
     */
    public Datum() {
        moment = Vec3.ORIGIN;
    }

    /** Reports whether this datum is selected.
     * @return {@code true} if this datum is selected */
    public boolean isSelected()        { return selected; }
    /** Sets the selection state of this datum.
     * @param v {@code true} to select this datum, {@code false} to deselect */
    public void setSelected(boolean v) { touch(); selected = v; }
    /** Returns the sample's dip azimuth.
     * @return the sample's dip azimuth in degrees */
    public double getSampAz()          { return sampAz; }
    /** Sets the sample's dip azimuth.
     * @param v the sample dip azimuth to set, in degrees */
    public void setSampAz(double v)    { touch(); sampAz = v; }
    /** Returns the sample's dip angle.
     * @return the sample's dip angle, in degrees */
    public double getSampDip()         { return sampDip; }
    /** Sets the sample's dip angle.
     * @param v the sample dip angle to set, in degrees */
    public void setSampDip(double v)   { touch(); sampDip = v; }
    /** Returns the formation dip azimuth.
     * @return the formation dip azimuth in degrees */
    public double getFormAz()          { return formAz; }
    /** Sets the formation dip azimuth.
     * @param v the formation dip azimuth to set, in degrees */
    public void setFormAz(double v)    { touch(); formAz = v; }
    /** Returns the formation dip angle.
     * @return the formation dip angle, in degrees */
    public double getFormDip()         { return formDip; }
    /** Sets the formation dip angle.
     * @param v the formation dip angle to set, in degrees */
    public void setFormDip(double v)   { touch(); formDip = v; }
    /** Returns the local geomagnetic field declination for the sampling site.
     * @return the local geomagnetic field declination, in degrees */
    public double getMagDev()          { return magDev; }
    /** Sets the local geomagnetic field declination for the sampling site.
     * @param v the local geomagnetic field declination to set, in degrees */
    public void setMagDev(double v)    { touch(); magDev = v; }
    /** Reports whether PCA fits for this point should be anchored.
     * @return {@code true} if PCA fits to this point should be anchored */
    public boolean isPcaAnchored()     { return pcaAnchored; }
    /** Sets whether PCA fits for this point should be anchored.
     * @param v {@code true} to set PCA fits for this point to be anchored */
    public void setPcaAnchored(boolean v) { touch(); pcaAnchored = v; }
    /** Returns the measurement's data-file line. Not currently used.
     * @return the data-file line for this measurement; not currently used */
    public Line getLine()              { return line; }
    /** Sets the measurement's data-file line. Not currently used.
     * @param v the data-file line to set for this measurement; not currently used */
    public void setLine(Line v)        { touch(); line = v; }
    /** Returns the sample of which this measurement was made.
     * @return the sample on which this measurement was made */
    public Sample getSample()          { return sample; }
    /** Sets the sampe on which this measurement was made.
     * @param v the sample on which this measurement was made */
    public void setSample(Sample v)    { touch(); sample = v; }
    /** Reports whether this datum should be hidden on plots.
     * @return {@code true} if this datum should not be displayed on plots */
    public boolean isHidden()          { return hidden; }
    /** Sets whether this datum should be hidden on plots.
     * @param v {@code true} if this datum should not be displayed on plots */
    public void setHidden(boolean v)   { touch(); hidden = v; }
    /** For continuous measurements, returns the depth of this measurement within the core.
     * @return for continuous measurements, the depth of this measurement within the core */
    public String getDepth()           { return depth; }
    /** For continuous measurements, sets the depth of this measurement within the core.
     * @param v for continuous measurements, the depth of this measurement within the core */
    public void setDepth(String v)     { touch(); depth = v; }
    /** Returns the magnetic susceptibility of the sample at this stage of treatment
     * @return the magnetic susceptibility of the sample at this stage of treatment */
    public double getMagSus()          { return magSus; }
    /** Sets the magnetic susceptibility of the sample at this stage of treatment.
     * @param v the magnetic susceptibility of the sample at this stage of treatment */
    public void setMagSus(double v)    { touch(); magSus = v; }
    /** Returns the type of this measurement (discrete or continuous).
     * @return the type of this measurement (discrete or continuous) */
    public MeasType getMeasType()      { return measType; }
    /** Sets the type of this measurement (discrete or continuous).
     * @param v the type of this measurement (discrete or continuous) */
    public void setMeasType(MeasType v){ touch(); measType = v; }
    /** For discrete samples, returns the sample identifier (name).
     * @return for discrete samples, the identifier (name) of the sample */
    public String getDiscreteId()      { return discreteId; }
    /** For discrete samples, sets the sample identifier (name).
     * @param v for discrete samples, the identifier (name) of the sample */
    public void setDiscreteId(String v){ touch(); discreteId = v; }
    /** Returns the treatment applied before this measurement (AF, thermal, etc.).
     * @return the treatment applied before this measurement (AF, thermal, etc.) */
    public TreatType getTreatType()    { return treatType; }
    /** Sets the treatment applied before this measurement (AF, thermal, etc.).
     * @param v the treatment applied before this measurement (AF, thermal, etc.) */
    public void setTreatType(TreatType v) { touch(); treatType = v; }
    /** For AF or ARM treatment, returns the AF field strength in the x axis.
     * @return for AF or ARM treatment, the AF field strength in the x axis */
    public double getAfX()             { return afx; }
    /** For AF or ARM treatment, sets the AF field strength in the x axis.
     * @param v for AF or ARM treatment, the AF field strength in the x axis*/
    public void setAfX(double v)       { touch(); afx = v; }
    /** For AF or ARM treatment, returns the AF field strength in the y axis.
     * @return for AF or ARM treatment, the AF field strength in the y axis */
    public double getAfY()             { return afy; }
    /** For AF or ARM treatment, sets the AF field strength in the y axis.
     * @param v for AF or ARM treatment, the AF field strength in the y axis*/
    public void setAfY(double v)       { touch(); afy = v; }
    /** For AF or ARM treatment, returns the AF field strength in the z axis.
     * @return for AF or ARM treatment, the AF field strength in the z axis */
    public double getAfZ()             { return afz; }
    /** For AF or ARM treatment, sets the AF field strength in the z axis.
     * @param v for AF or ARM treatment, the AF field strength in the z axis*/
    public void setAfZ(double v)       { touch(); afz = v; }
    /** For IRM treatment, returns the IRM field strength.
     * @return for IRM treatment, the IRM field strength */
    public double getIrmField()        { return irmField; }
    /** For IRM treatment, sets the IRM field strength.
     * @param v for IRM treatment, the IRM field strength */
    public void setIrmField(double v)  { touch(); irmField = v; }
    /** For ARM treatment, returns the ARM bias field strength.
     * @return for ARM treatment, the ARM bias field strength */
    public double getArmField()        { return armField; }
    /** For ARM treatment, sets the ARM bias field strength.
     * @param v for ARM treatment, the ARM bias field strength */
    public void setArmField(double v)  { touch(); armField = v; }
    /** For ARM treatment, returns the axis of the ARM field.
     * @return for ARM treatment, the axis of the ARM field */
    public ArmAxis getArmAxis()        { return armAxis; }
    /** For ARM treatment, sets the axis of the ARM field.
     * @param v for ARM treatment, the axis of the ARM field */
    public void setArmAxis(ArmAxis v)  { touch(); armAxis = v; }
    /** For thermal treatment, returns the temperature in degrees Celsius.
     * @return for thermal treatment, the temperature in degrees Celsius */
    public double getTemp()            { return temp; }
    /** For thermal treatment, sets the temperature in degrees Celsius.
     * @param v for thermal treatment, the temperature in degrees Celsius*/
    public void setTemp(double v)      { touch(); temp = v; }
    /** For continuous measurements, returns the cross-sectional area of the core.
     * @return the cross-sectional area of the core */
    public double getArea()            { return area; }
    /** For continuous measurements, sets the cross-sectional area of the core.
     * @param v the cross-sectional area of the core */
    public void setArea(double v)      { touch(); area = v; }
    /** For discrete measurements, returns the volume of the sample.
     * @return for discrete measurements, the volume of the sample */
    public double getVolume()          { return volume; }
    /** For discrete measurements, sets the volume of the sample.
     * @param v for discrete measurements, the volume of the sample */
    public void setVolume(double v)    { touch(); volume = v; }
    /** Returns the number of the machine run during which this measurement was made.
     * @return the number of the machine run during which this measurement was made */
    public int getRunNumber()          { return runNumber; }
    /** Sets the number of the machine run during which this measurement was made.
     * @param v the number of the machine run during which this measurement was made */
    public void setRunNumber(int v)    { touch(); runNumber = v; }
    /** Returns the timestamp of this measurement.
     * @return the timestamp of this measurement */
    public String getTimestamp()       { return timestamp; }
    /** Sets the timestamp of this measurement.
     * @param v the timestamp of this measurement */
    public void setTimestamp(String v) { touch(); timestamp = v; }
    /** Returns the x drift correction value.
     * @return the x drift correction value */    
    public double getXDrift()          { return xDrift; }
    /** Sets the x drift correction value.
     * @param v the x drift correction value */    
    public void setXDrift(double v)    { touch(); xDrift = v; }
    /** Returns the y drift correction value.
     * @return the y drift correction value */    
    public double getYDrift()          { return yDrift; }
    /** Sets the y drift correction value.
     * @param v the y drift correction value */    
    public void setYDrift(double v)    { touch(); yDrift = v; }
    /** Returns the z drift correction value.
     * @return the z drift correction value */    
    public double getZDrift()          { return zDrift; }
    /** Sets the z drift correction value.
     * @param v the z drift correction value */    
    public void setZDrift(double v)    { touch(); zDrift = v; }
    /** Returns the number of the measurement tray slot in which the sample was measured.
     * @return the number of the measurement tray slot in which the sample was measured */    
    public int getSlotNumber()         { return slotNumber; }
    /** Sets the number of the measurement tray slot in which the sample was measured.
     * @param v the number of the measurement tray slot in which the sample was measured */
    public void setSlotNumber(int v)   { touch(); slotNumber = v; }
    /** Returns the data suite containing this measurement.
     * @return the data suite containing this measurement */
    public Suite getSuite()            { return suite; }
    /** Sets the data suite containing this measurement.
     * @param v the data suite containing this measurement */
    public void setSuite(Suite v)      { touch(); suite = v; }
    /** Reports whether this measurement is used for a great-circle fit.
     * @return {@code true} if this measurement is used for a great-circle fit */
    public boolean isOnCircle()        { return onCircle; }
    /** Sets whether this measurement is to be used for a great-circle fit.
     * @param v {@code true} to use this measurement for a great-circle fit */
    public void setOnCircle(boolean v) { touch(); onCircle = v; }
    /** Reports whether this measurement is used for a PCA fit.
     * @return {@code true} if this measurement is used for a PCA fit */
    public boolean isInPca()           { return inPca; }
    /** Sets whether this measurement is to be used for a PCA fit.
     * @param v {@code true} to use this measurement for a PCA fit */
    public void setInPca(boolean v)    { touch(); inPca = v; }
    
    /** Returns the sample hade for this datum.
     * @return the sample's hade, in degrees */
    public double getSampHade() {
        return 90 - sampDip;
    }
    
    /** Sets the sample hade for this datum.
     * Since the hade is the complement of the dip,
     * this will of course change the sample's dip.
     * @param hadeDeg the hade to set, in degrees
     */
    public void setSampHade(double hadeDeg) {
        touch();
        sampDip = 90 - hadeDeg;
    }
    
    /** Returns the formation strike for this datum. 
     * @return the formation strike, in degrees
     */
    public double getFormStrike() {
        double strike = formAz - 90;
        if (strike < 0) strike += 360;
        return strike;
    }
    
    /** Sets the formation strike for this datum.
     * This will of course also set the formation dip azimuth.
     * @param strikeDeg the formation strike, in degrees
     */
    public void setFormStrike(double strikeDeg) {
        touch();
        double az = strikeDeg + 90;
        if (az > 360) az -= 360;
        formAz = az;
    }
    
    /** Returns sample identifier or measurement depth.
     * If the measurement is discrete, returns the sample identifier;
     * if the measurement is continuous, returns a string representation
     * of the measurement's depth within the core.
     * @return sample identifier or depth within core, as appropriate
     */
    public String getIdOrDepth() {
        return measType == MeasType.CONTINUOUS ? depth : discreteId;
    }
    
    /** Reports whether this datum has any magnetic susceptibility data. 
     * @return {@code true} if there is magnetic susceptibility data
     * in this datum
     */
    public boolean hasMagSus() {
        return !Double.isNaN(magSus);
    }

    /** Reports whether this datum has magnetic susceptibility but not
     * magnetic moment data.
     * 
     * @return {@code true} if this datum contains magnetic susceptibility
     * data and does not contain magnetic moment data
     */
    public boolean isMagSusOnly() {
        // Q. But what if we have a measurement of exactly zero?
        // A. 1. You never have a measurement of exactly zero.
        // A. 2. It still wouldn't have reference equality with ORIGIN,
        //       since it would have been separately instantiated.
        return (moment == null || moment == Vec3.ORIGIN) && hasMagSus();
    }

    private boolean hasSampleOrientation() {
        return (!Double.isNaN(sampAz)) && (!Double.isNaN(sampDip));
    }

    private boolean hasFormationOrientation() {
        return (!Double.isNaN(formAz)) && (!Double.isNaN(formDip));
    }

    /**
     * <p>Strictly speaking, the name is not quite accurate:
     * we do not deal with magnetic
     * moment (which would be in Am<sup>2</sup>) but in magnetic dipole moment
     * per unit volume (in A/m). But
     * {@code getMagneticDipoleMomentPerUnitVolumeInAm} would be an inconveniently
     * long method name.</p>
     * 
     * @return the magnetic dipole moment
     * per unit volume in A/m */
    public Vec3 getMoment() { return moment; }
    
    /**
     * Sets the sample's magnetic dipole moment per unit volume
     * in A/m.
     * @param v the magnetic dipole moment per unit volume in A/m
     */
    public void setMoment(Vec3 v)      { touch(); moment = v; }

    /**
     * Returns the measured magnetic dipole moment per unit volume, as
     * modified by the supplied correction. The correction may specify that
     * the moment should be rotated to correct for sample and/or formation
     * orientation. It also allows the specification of tray and empty-slot
     * corrections, but these are not presently implemented here. (Tray
     * corrections are applied when loading a file, and empty-slot 
     * corrections are unimplemented.)
     * 
     * @param correction the corrections to apply to the magnetic moment measurement
     * @return the corrected magnetic dipole moment per unit volume in A/m
     */
    public Vec3 getMoment(Correction correction) {
        Vec3 result = moment;
        if (correction.includesEmpty()) {
            result = result.minus(getLine().getEmptySlot().getMoment());
        }
        if (correction.includesTray()) {
            Datum tray = getSuite().getTrayCorrection(this);
            if (tray != null) result = result.minus(tray.getMoment());
        }
        result = correctVector(result, correction);
        return result;
    }

    private Vec3 correctVector(Vec3 v, Correction c) {
        Vec3 result = v;
        if (c.includesSample() && hasSampleOrientation()) {
            result = result.correctSample(toRadians(sampAz + magDev),
                                          toRadians(sampDip));
            if (c.includesFormation() && hasFormationOrientation()) {
                double formAzTmp = formAz;
                if (c.isMagDevAppliedToFormation()) formAzTmp += magDev;
                result = result.correctForm(toRadians(formAzTmp),
                                            toRadians(formDip));
            }
        }
        return result;
    }
    
    /**
     *  Rotates magnetic moment data 180 degrees about the specified axis.
     *  @param axis the axis about which to perform the rotation
     */
    public void rot180(MeasurementAxis axis) {
        touch();
        moment = moment.rot180(axis);
    }

    
    /**
     * Inverts magnetic moment data.
     */
    void invertMoment() {
        touch();
        moment = moment.invert();
    }
    
    /**
     * Toggles the datum's selection state. {@code datum.toggleSel()}
     * is functionally equivalent to {@code datum.setSelected(!datum.isSelected()}.
     */
    public void toggleSel() {
        touch();
        setSelected(!isSelected());
    }

    /**
     * <p>Returns a numerical representation of the intensity of the treatment
     * which was applied immediately before this measurement. The interpretation
     * of the number depends on the treatment type. For thermal treatment, it
     * is a temperature in degrees Celsius. For magnetic treatments
     * (AF demagnetization, ARM, IRM) it returns the magnetic field. As 
     * is conventional, the magnetic field is returned as the equivalent 
     * magnetic induction in units of Tesla.</p>
     * 
     * <p>For ARM treatment, this method returns the strength of the
     * alternating field rather than the DC bias field.</p>
     * 
     * @return the treatment level
     */
    public double getTreatmentLevel() {
        switch (treatType) {
        case NONE: return 0;
        case DEGAUSS_XYZ:
            // This is a bit ill-defined: in general, of course, we can't
            // collapse a three-dimensional treatment into a single value.
            // We assume that the same treatment has been applied on each
            // axis, and that zero values are due to lazy construction of
            // the input file. (The exception is if all the values are 
            // zero, in which case we assume that it's an actual zero-level
            // treatment.) The logic below should handle NaN values cleanly,
            // but negative values will come out as zero. So far I've never
            // seen a file with negative AF treatment values.
            if (afx>0) return afx;
            if (afy>0) return afy;
            if (afz>0) return afz;
            return 0;
        case DEGAUSS_Z: return afz;
        case THERMAL: return temp;
        case ARM: return afz; //usually we vary this & keep bias field constant
        case IRM: return getIrmField();
        case UNKNOWN: return 0;
        default: throw new IllegalArgumentException("unhandled treatment type");
        }
    }
    
    /**
     * Returns the treatment level formatted as a string.
     * Temperatures are given in degrees Celsius, AF intensities
     * in millitesla.
     * 
     * @return a string representing the treatment level
     */
    public String getFormattedTreatmentLevel() {
        if (getTreatType().getUnit().equals("T")) {
            // magnetic treatment -- turn T into mT
            return String.format(Locale.ENGLISH, "%.0f",
                    getTreatmentLevel() * 1000);
        } else {
            // no conversion required for thermal treatment
            return String.format(Locale.ENGLISH, "%.0f", getTreatmentLevel());
        }
    }

    /**
     * Returns the maximum treatment level within the supplied group of
     * datum objects.
     * 
     * @param data a list of datum objects
     * @return the highest treatment level for any of the supplied datum objects
     */
    public static double maxTreatmentLevel(List<Datum> data) {
        double max = 0;
        for (Datum d: data) {
            double level = d.getTreatmentLevel();
            if (level > max) max = level;
        }
        return max;
    }

    /**
     * Returns the maximum magnitude of magnetic dipole moment per unit volume
     * within the supplied group of
     * datum objects.
     * 
     * @param data a list of datum objects
     * @return the highest magnitude of magnetic dipole moment per unit volume
     * for any of the supplied datum objects
     */
    public static double maxIntensity(List<Datum> data) {
        double max = 0;
        for (Datum d: data) {
            double i = d.getIntensity();
            if (i > max) max = i;
        }
        return max;
    }

    /**
     * Returns the maximum magnetic susceptibility within the supplied group of
     * datum objects.
     * 
     * @param data a list of datum objects
     * @return the highest magnetic susceptibility for any of the supplied datum objects
     */
    public static double maxMagSus(List<Datum> data) {
        double max = 0;
        for (Datum d: data) {
            double level = d.getMagSus();
            if (!Double.isNaN(level) && level > max) max = level;
        }
        return max;
    }

    /**
     * Returns the magnitude of the magnetic dipole moment per unit volume in A/m.
     * @return magnitude of magnetic dipole moment per unit volume in A/m
     */
    public double getIntensity() {
        return getMoment(Correction.NONE).mag();
    }

    /**
     * Returns {@code true} if this datum should be ignored (thrown away)
     * when loading a data file. Currently, this method returns true if
     * the measurement type is {@code NONE} &endash; that is, there is
     * no data within the object.
     * 
     * @return {@code true} if this datum should be ignored when loading a file
     */
    public boolean ignoreOnLoading() {
        return getMeasType() == MeasType.NONE;
    }

    /**
     * Reports whether the datum contains a magnetic moment measurement.
     * @return {@code true} if the datum contains a magnetic moment measurement
     */
    public boolean hasMagMoment() {
        return moment != null;
    }

    private String fmt(Double d) {
        return Double.toString(d);
    }

    /**
     * Returns a String representation of a value from a specified data field.
     * 
     * @param field the field to read
     * @return a string representation of the value contained in the field
     */
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
        case VIRT_MAGNETIZATION: return fmt(getIntensity());
        case VIRT_DECLINATION: return fmt(moment.getDecDeg());
        case VIRT_INCLINATION: return fmt(moment.getIncDeg());
        case VIRT_SAMPLE_HADE: return fmt(getSampHade());
        case VIRT_FORM_STRIKE: return fmt(getFormStrike());
        case VIRT_MSJUMP: return fmt(getSample().getMagSusJump());
            // TODO strike and hade
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }

    /**
     * Sets the value of a specified data field using a string.
     * 
     * @param field the field to set the value of
     * @param value a string representation of the value to set the field to
     * @param factor conversion factor for double values
     * 
     * For double values, the field is set to the parsed value multiplied
     * by the conversion factor.
     * 
     * @throws NumberFormatException if the format of the string is 
     * not compatible with the format of the field to be set
     */
    public void setValue(DatumField field, String value, double factor) {
        touch();
        try {
            doSetValue(field, value, factor);
        } catch (NumberFormatException e1) {
            logger.warning(String.format(Locale.ENGLISH,
                    "Invalid value %s for field %s; using default %s",
                    value, field.toString(), field.getDefaultValue()));
            try {
                /* NB default value is already in correct units, so we
                   set the conversion factor to 1. */
                doSetValue(field, field.getDefaultValue(), 1.);
            } catch (NumberFormatException e2) {
                final String msg = String.format(Locale.ENGLISH,
                        "Invalid value "+
                        "when setting field '%s' to value '%s':\n%s",
                        field.toString(), value, e2.getMessage());
                logger.warning(msg);
                // Failing silently is often a bad thing, but in this case
                // it might actually be possible to continue usefully without
                // this field. If it makes the program keel over later, the
                // log message should make it easier to trace.
            }
        }
    }
    
    private void doSetValue(DatumField field, String s, double factor) {
        double doubleVal = 0;
        final Class type = field.getType();
        boolean boolVal = false;
        int intVal = 0;
        if (type == double.class) {
            doubleVal = parseDouble(s) * factor;
        } else if (type == boolean.class) {
            boolVal = Boolean.parseBoolean(s);
        } else if (type == int.class) {
            intVal = Integer.parseInt(s);
        }
        switch (field) {
        case AF_X: afx = doubleVal; break;
        case AF_Y: afy = doubleVal; break;
        case AF_Z: afz = doubleVal; break;
        case TEMPERATURE: temp = doubleVal; break;
        case MAG_SUS: magSus = doubleVal; break;
        case SAMPLE_AZ: setSampAz(doubleVal); break;
        case SAMPLE_DIP: setSampDip(doubleVal); break;
        case FORM_AZ: setFormAz(doubleVal); break;
        case FORM_DIP: setFormDip(doubleVal); break;
        case MAG_DEV: setMagDev(doubleVal); break;
        case X_MOMENT: moment = moment.setX(doubleVal); break;
        case Y_MOMENT: moment = moment.setY(doubleVal); break;
        case Z_MOMENT: moment = moment.setZ(doubleVal); break;
        case DEPTH: depth = s; break;
        case IRM_FIELD: setIrmField(doubleVal); break;
        case ARM_FIELD: armField = doubleVal; break;
        case VOLUME: volume = doubleVal; break;
        case DISCRETE_ID: discreteId = s; break;
        case MEAS_TYPE: measType = MeasType.valueOf(s); break;
        case TREATMENT: treatType = TreatType.valueOf(s); break;
        case ARM_AXIS: armAxis = ArmAxis.fromString(s); break;
        case TIMESTAMP: timestamp = s; break;
        case SLOT_NUMBER: slotNumber = intVal; break;
        case RUN_NUMBER: runNumber = intVal; break;
        case AREA: area = doubleVal; break;
        case PP_SELECTED: selected = boolVal; break;
        case PP_ANCHOR_PCA: setPcaAnchored(boolVal); break;
        case PP_HIDDEN: setHidden(boolVal); break;
        case PP_ONCIRCLE: setOnCircle(boolVal); break;
        case PP_INPCA: setInPca(boolVal); break;
        case VIRT_SAMPLE_HADE: setSampHade(doubleVal); break;
        case VIRT_FORM_STRIKE: setFormStrike(doubleVal); break;
        default: throw new IllegalArgumentException("Unknown field "+field);
        }
    }

    double getTreatmentStep() {
        switch (getTreatType()) {
            case ARM:
            case DEGAUSS_XYZ:
            case DEGAUSS_Z:
                return afz;
            case IRM:
                return irmField;
            case THERMAL:
                return temp;
            default:
                return 0;
        }
    }

    /**
     * This class allows datum objects to be created from string representations
     * of a specified format. The headers (corresponding to field names)
     * are specified in the constructor, and data lines with a format
     * corresponding to the supplied headers can then be turned into 
     * datum objects.
     */
    public static class Reader {
        private final List<DatumField> fields;

        /**
         * Create a new reader using the supplied header strings.
         * Each header string should correspond to the string representation
         * of a {@link DatumField} field.
         * @param headers list of headers defining the data format
         */
        public Reader(List<String> headers) {
            fields = new ArrayList<>(headers.size());
            for (String s: headers) fields.add(DatumField.valueOf(s));
        }

        /**
         * Creates a a datum object using the supplied strings to 
         * populate the data fields. The values in the supplied list of
         * strings must occur in the same order as the corresponding
         * {@link DatumField}s supplied to the reader's constructor.
         * 
         * @param strings string representations of data values
         * @return a datum object containing the supplied values
         */
        public Datum fromStrings(List<String> strings) {
            final Datum d = new Datum(Vec3.ORIGIN);
            for (int i=0; i<strings.size(); i++) {
                d.setValue(fields.get(i), strings.get(i), 1.);
            }
            return d;
        }
    }

    /**
     * Produces a list of strings representing the data values within this
     * datum object. The order of the strings corresponds to the order of
     * the fields in {@link DatumField#realFields}.
     * 
     * @return  a list of strings representing the data values within this
     * datum
     */
    public List<String> toStrings() {
        List<String> result =
                new ArrayList<>(DatumField.getRealFields().size());
        for (DatumField df : DatumField.getRealFields()) {
            result.add(getValue(df));
        }
        return result;
    }

    /**
     * Produces a string containing string representations of the contents
     * of selected data fields. The values are separated by a specified 
     * delimiter.
     * 
     * @param fields the fields for which values should be produced
     * @param delimiter the string which should separate the values
     * @return a string representation of the requested values
     */
    public String exportFieldValues(Collection<DatumField> fields, String delimiter) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (DatumField field: fields) {
            if (!first) sb.append(delimiter);
            sb.append(getValue(field));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Sets this datum's suite as "modified". Intended to be called
     * from any method that modifies the datum, to keep track of whether
     * the suite has been saved since the last modification. 
     */
    public void touch() {
        if (suite != null) suite.setSaved(false);
    }
}
