/* This file is part of PuffinPlot, a program for palaeomagnetic
 * treatmentSteps plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import Jama.Matrix;

import static java.lang.Double.parseDouble;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;
import java.util.function.Consumer;
import java.util.function.ObjDoubleConsumer;

/**
 * This class represents a sample on which measurements have been made. It may
 * correspond either to a discrete, physical specimen, or to a particular point
 * on a continuous long core or u-channel sample.
 */
public class Sample {

    private List<TreatmentStep> treatmentSteps;
    private Site site;
    private String nameOrDepth;
    private double depth;
    private boolean isEmptySlot = false;
    private GreatCircle greatCircle;
    private PcaAnnotated pca;
    private MedianDestructiveField mdf;
    private boolean hasMsData = false;
    private Tensor ams;
    private double magSusJump = 0; // temperature at which mag. sus. jumps
    private CustomFields<Boolean> customFlags;
    private CustomFields<String> customNotes;
    private final Suite suite;
    private double sampAz = Double.NaN, sampDip = Double.NaN;
    private double formAz = Double.NaN, formDip = Double.NaN;
    private double magDev = Double.NaN;
    private FisherValues fisherValues;
    private Vec3 importedDirection = null;
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");

    /**
     * Creates a new sample. For discrete samples, the supplied name can take
     * any form; for long core samples, it should be a string representation of
     * a number giving the depth.
     * 
     * @param name sample identifier or numerical depth
     * @param suite the treatmentSteps suite of which this sample is a part
     */
    public Sample(String name, Suite suite) {
        this.nameOrDepth = name;
        setDepth(name);
        this.suite = suite;
        this.treatmentSteps = new ArrayList<>();
        this.customFlags = new CustomFields<>();
        this.customNotes = new CustomFields<>();
    }

    /**
     * Set the depth of this sample. If the supplied string is not
     * interpretable as a number, it will be ignored.
     * 
     * @param newDepth a string representing a number to use as the
     * depth of this sample
     */
    public final void setDepth(String newDepth) {
        double depthTmp = Double.NaN;
        try {
            depthTmp = Double.parseDouble(newDepth);
        } catch (NumberFormatException ex) {
            /*
             * Nothing to do here: if it's not valid it's probably just a
             * discrete sample.
             */
        }
        this.depth = depthTmp;
    }
    
    /**
     * Clears PCA calculations for this sample.
     */
    public void clearPca() {
        touch();
        pca = null;
        forEachTreatmentStep(step -> step.setInPca(false));
    }
    
    /**
     * Clears great-circle fit for this sample.
     */
    public void clearGreatCircle() {
        touch();
        greatCircle = null;
        forEachTreatmentStep(step -> step.setOnCircle(false));
    }
    
    /**
     * Clears all calculations for this sample (PCA, MDF, and great-circle fit)
     * and deselects all treatment steps.
     */
    public void clearCalculations() {
        touch();
        clearPca();
        clearGreatCircle();
        fisherValues = null;
        mdf = null;
        selectNone();
    }
    
    /**
     * Returns the suite containing this sample.
     *
     * @return the suite containing this sample
     */
    public Suite getSuite() {
        return suite;
    }
    
    /**
     * Calculates the median destructive field using the visible treatmentSteps
     * points of this sample and stores the results within this sample.
     * 
     * @see #getMdf()
     */
    public void calculateMdf() {
        touch();
        mdf = MedianDestructiveField.calculate(getVisibleTreatmentSteps());
    }

    /**
     * Returns the results of the MDF calculation if it has been performed,
     * or {@code null} if not.
     * 
     * @return the results of the MDF calculation if it has been performed,
     * or {@code null} if not
     * 
     * @see #calculateMdf()
     */
    public MedianDestructiveField getMdf() {
        return mdf;
    }
    
    /**
     * Returns the depth of a this sample within a continuous long core,
     * where applicable.
     * For a discrete sample, an arbitrary value may be returned.
     * 
     * @return the depth of a measurement on a continuous long core
     */
    public double getDepth() {
        return depth;
    }

    /**
     * Returns the intensity of the sample's natural remanent magnetization 
     * (NRM). The value returned is a magnetic dipole moment per unit
     * volume in units of A/m.
     * 
     * @return the intensity of the sample's natural remanent magnetization
     */
    public double getNrm() {
        if (treatmentSteps.isEmpty()) {
            return Double.NaN;
        } else {
            return treatmentSteps.get(0).getIntensity();
        }
    }

    /**
     * Finds the first jump in magnetic susceptibility in the sample's
     * demagnetization data. The temperature at which this jump occurs is stored
     * within this sample and can be retrieved by the {@code getMagSusJump()}
     * method. A jump is defined as an increase of at least 2.5 times in a
     * single treatment step. If there is no magnetic susceptibility data, or if
     * no jump occurs, a value of 0 will be stored.
     * 
     * @see #getMagSusJump()
     * 
     */
    public void calculateMagSusJump() {
        touch();
        final double limit = 2.5;
        double msj = 0;
        double prevMagSus = 1e200;
        for (TreatmentStep step: treatmentSteps) {
            double magSus = step.getMagSus();
            if (!Double.isNaN(magSus)) {
                if (magSus > prevMagSus * limit) {
                    msj = step.getTemperature();
                    break;
                }
                prevMagSus = magSus;
            }
        }
        magSusJump = msj;
    }

    /**
     * Returns the results of the magnetic susceptibility jump calculation,
     * if it has been performed. If not, an arbitrary value may be returned.
     * 
     * @return the results of the magnetic susceptibility jump calculation
     * @see #calculateMagSusJump()
     */
    public double getMagSusJump() {
        return magSusJump;
    }

    /**
     * Rotates all magnetic moment data (as stored in treatment steps) 180
     * degrees about the specified axis.
     *
     * @param axis the axis about which to rotate the data
     */
    public void flip(MeasurementAxis axis) {
        touch();
        forEachTreatmentStep(step -> step.rot180(axis));
    }
    
    /**
     * Inverts all magnetic moment data.
     */
    public void invertMoments() {
        touch();
        forEachTreatmentStep(TreatmentStep::invertMoment);
    }
    
    /**
     * Sets all the selected treatment steps within this sample to be hidden, so
     * they will not be shown on plots. The points are deselected as well as
     * being hidden (so after a call to the method no treatment steps will
     * be selected).
     *
     * @see TreatmentStep#isHidden()
     */
    public void hideAndDeselectSelectedPoints() {
        touch();
        for (TreatmentStep step: getTreatmentSteps()) {
            if (step.isSelected()) {
                step.setSelected(false);
                step.setHidden(true);
            }
        }
    }
    
    /**
     * Selects all the treatment steps within this sample.
     *
     * @see TreatmentStep#setSelected(boolean)
     */
    public void selectAll() {
        touch();
        forEachTreatmentStep(step -> step.setSelected(true));
    }

    /**
     * Selects all the visible (non-hidden) treatment steps within this sample.
     *
     * @see TreatmentStep#setSelected(boolean)
     * @see TreatmentStep#isHidden()
     */
    public void selectVisible() {
        touch();
        for (TreatmentStep step: getTreatmentSteps()) {
            if (!step.isHidden()) {
                step.setSelected(true);
            }
        }
    }

    /**
     * De-selects all the treatment steps within this sample.
     */
    public void selectNone() {
        touch();
        forEachTreatmentStep(step -> step.setSelected(false));
    }
    
    /**
     * Selects all treatment steps within a certain treatment level range.
     *
     * ‘Treatment level’ refers to AF field strength, temperature, etc. Note
     * that this is distinct from the treatment <i>step</i>, which is just a
     * non-negative integer used to index successive treatment rounds.
     *
     * Negative and positive infinities can be passed as arguments, with
     * the expected behaviour. Passing NaN as an argument will not throw
     * an exception or produce additional side effects, but the resulting
     * datum selection behaviour is undefined (i.e. any points might be
     * selected).
     * 
     * @param min minimum treatment intensity
     * @param max maximum treatment intensity
     * 
     * @see TreatmentStep#getTreatmentLevel()
     */
    public void selectByTreatmentLevelRange(double min, double max) {
        for (TreatmentStep step: getTreatmentSteps()) {
            final double level = step.getTreatmentLevel();
            step.setSelected(min <= level && level <= max);
        }
    }
    
    /**
     * Reports whether this sample contains any treatmentSteps.
     *
     * @return {@code true} if this sample contains any treatmentSteps
     */
    public boolean hasTreatmentSteps() {
        return !treatmentSteps.isEmpty();
    }
    
    /**
     * Returns all the treatment steps within this sample.
     *
     * @return all the treatment steps within this sample
     */
    public List<TreatmentStep> getTreatmentSteps() {
        return Collections.unmodifiableList(treatmentSteps);
    }
    
    /**
     * Returns all the visible (non-hidden) treatment steps within this sample.
     *
     * @return all the visible (non-hidden) treatment steps within this sample
     */
    public List<TreatmentStep> getVisibleTreatmentSteps() {
        return getTreatmentSteps().stream().filter(step -> !step.isHidden()).
                collect(Collectors.toList());
    }

    /**
     * Returns all the selected treatment steps within this sample.
     *
     * @return all the selected treatment steps within this sample
     */
    public List<TreatmentStep> getSelectedTreatmentSteps() {
        return getTreatmentSteps().stream().filter(step -> step.isSelected()).
                collect(Collectors.toList());
    }
    
    /**
     * Returns {@code true} if the selected treatment steps are contiguous. This
     * is the case if there are no unselected steps between the first selected
     * point and the last selected step.
     *
     * @return {@code true} if the selected points are contiguous
     */
    public boolean isSelectionContiguous() {
        int runEndsSeen = 0;
        boolean thisIsSelected = false, lastWasSelected = false;
        for (TreatmentStep step: getTreatmentSteps()) {
            thisIsSelected = step.isSelected();
            if (lastWasSelected && !thisIsSelected) runEndsSeen++;
            lastWasSelected = thisIsSelected;
        }
        if (thisIsSelected) runEndsSeen++;
        return (runEndsSeen <= 1);
    }

    /**
     * Returns the number of treatment steps within this sample.
     *
     * @return the number of treatment steps within this sample
     */
    public int getNumberOfSteps() {
        return getTreatmentSteps().size();
    }

    /**
     * Returns a specified treatment step from this sample.
     *
     * @param i the index of the requested treatment step
     * @return the treatment step with the selected index, if it exists
     * @throws IndexOutOfBoundsException if no treatment step with the
     * selected index exists
     */
    public TreatmentStep getTreatmentStepByIndex(int i) {
        return getTreatmentSteps().get(i);
    }
    
    /**
     * Returns the first TreatmentStep in this Sample with the given treatment
     * level.
     *
     * If the Sample contains more than one TreatmentStep at the given level,
     * only the first is returned. If there is no matching TreatmentStep,
     * {@code null} is returned. Any treatment levels differing by less than
     * 1e-6 are considered equal.
     *
     * @param level a treatment level
     * @return the first datum in the sample with the given treatment level
     */
    public TreatmentStep getTreatmentStepByLevel(double level) {
        final double threshold = 1e-6;
        for (TreatmentStep step: treatmentSteps) {
            if (abs(step.getTreatmentLevel() - level) < threshold) {
                return step;
            }
        }
        return null;
    }
    
    /**
     * Returns a TreatmentStep with a specified treatment type and level. The
     * treatment type is checked against a supplied set. A step is considered
     * to match if its treatment type is in the set and its treatment level is
     * approximately equal to the specified level. A step's treatment level is
     * approximately equal to the specified level if the two differ by less than
     * 1e-6.
     *
     * If the Sample contains more than one TreatmentStep with the given level
     * and type, only the first is returned. If there is no matching
     * TreatmentStep, {@code null} is returned.
     *
     * This method is mainly intended for use with ARM demagnetization data,
     * which can contain a mixture of "degauss" and "ARM" treatment types.
     *
     * @param types a set of treatment types
     * @param level a treatment level
     * @return the first datum in the sample with the given treatment level
     */
    public TreatmentStep getTreatmentStepByTypeAndLevel(
            Set<TreatmentType> types, double level) {
        final double threshold = 1e-6;
        for (TreatmentStep step: treatmentSteps) {
            if (abs(step.getTreatmentLevel() - level) < threshold
                    && types.contains(step.getTreatmentType())) {
                return step;
            }
        }
        return null;
    }
    
    /**
     * Returns an array of the treatment levels in this sample.
     * 
     * @return an array of the treatment levels in this sample
     */
    public double[] getTreatmentLevels() {
        return treatmentSteps.stream().mapToDouble(d -> d.getTreatmentLevel()).
                sorted().distinct().toArray();
    }
    
    /**
     * Adds a treatment step to this sample.
     * 
     * If this sample contains no treatment steps, its sample and formation
     * orientations and magnetic deviation will be set from the values in
     * the added treatment step.
     * 
     * @param treatmentStep a treatment step to add to this sample
     */
    public void addTreatmentStep(TreatmentStep treatmentStep) {
        touch();
        if (treatmentSteps.isEmpty()) {
            setSampAz(treatmentStep.getSampAz());
            setSampDip(treatmentStep.getSampDip());
            setFormAz(treatmentStep.getFormAz());
            setFormDip(treatmentStep.getFormDip());
            setMagDev(treatmentStep.getMagDev());
        }
        treatmentSteps.add(treatmentStep);
        if (treatmentStep.hasMagSus()) {
            hasMsData = true;
        }
        treatmentStep.setSample(this);
    }

    /**
     * Sets the orientation corrections for this sample's magnetic moment data.
     *
     * @param sampleAz the sample dip azimuth
     * @param sampleDip the sample dip angle
     * @param formAz the formation dip azimuth
     * @param formDip the formation dip angle
     * @param magDev the geomagnetic field declination at the sampling site
     */
    public void setCorrections(double sampleAz, double sampleDip,
            double formAz, double formDip, double magDev) {
        touch();
        this.setSampAz(sampleAz);
        this.setSampDip(sampleDip);
        this.setFormAz(formAz);
        this.setFormDip(formDip);
        this.setMagDev(magDev);
    }

    /**
     * Reports whether this sample has any magnetic susceptibility data.
     *
     * @return {@code true} if this sample has any magnetic susceptibility data
     */
    public boolean hasMagSusData() {
        return hasMsData;
    }

    /**
     * Flags all selected treatment steps for inclusion in principal component
     * analysis.
     */
    public void useSelectionForPca() {
        touch();
        forEachTreatmentStep(step -> step.setInPca(step.isSelected()));
    }
    
    /**
     * Reports whether principal component analysis should be anchored for this
     * sample.
     *
     * @return {@code true} if principal component analysis should be anchored
     * for this sample
     */
    public boolean isPcaAnchored() {
        return getTreatmentSteps().isEmpty() ? false :
                getTreatmentSteps().get(0).isPcaAnchored();
    }
    
    /**
     * Sets whether principal component analysis should be anchored for this
     * sample
     *
     * @param pcaAnchored {@code true} to anchor principal component analysis
     * for this sample
     */
    public void setPcaAnchored(boolean pcaAnchored) {
        touch();
        forEachTreatmentStep(step -> step.setPcaAnchored(pcaAnchored));
    }
    
    /**
     * Performs principal component analysis on a subset of the magnetic moment
     * data of this sample. The treatment steps to use are determined by the
     * result of {@link TreatmentStep#isInPca()}. The results are stored within
     * the sample and may be retrieved with {@link #getPcaAnnotated()}.
     *
     * @param correction the correction to apply to the magnetic moment
     * treatmentSteps
     */
    public void doPca(Correction correction) {
        if (!hasTreatmentSteps()) {
            return;
        }
        /*
         * Use the first treatment step's anchoring status for all of them
         * (a partially anchored PCA makes no sense). This is just
         * belt-and-braces really, since they should be uniform across the
         * sample. Eventually pcaAnchored will be moved entirely from
         * TreatmentStep to its rightful home in Sample and this kind of
         * ugliness will become unnecessary.
         */
        touch();
        boolean firstStepAnchored = getTreatmentSteps().get(0).isPcaAnchored();
        setPcaAnchored(firstStepAnchored);
        pca = PcaAnnotated.calculate(this, correction);
    }
    
    /**
     * Returns the annotated results of the last PCA calculation.
     *
     * @return the annotated results of the last PCA calculation
     */
    public PcaAnnotated getPcaAnnotated() {
        return pca;
    }
    
    /**
     * Returns the results of the last PCA calculation. Returns null if there is
     * no stored PCA calculation.
     *
     * @return the results of the last PCA calculation
     */
    public PcaValues getPcaValues() {
        return pca == null ? null : pca.getPcaValues();
    }
    
    /**
     * Returns the results of the last Fisher calculation. Returns null if there
     * are no stored Fisher statistics.
     *
     * @return the results of the last Fisher calculation
     */
    public FisherValues getFisherValues() {
        return fisherValues;
    }
    
    /**
     * Returns the imported sample direction, if any.
     * 
     * The imported direction may be set with
     * {@link #setImportedDirection(net.talvi.puffinplot.data.Vec3)}
     * or {@link #fromString(java.lang.String)}.
     * If no imported direction has been set, null is returned.
     * 
     * @return this sample's imported direction, if any
     */
    public Vec3 getImportedDirection() {
        return importedDirection;
    }
    
    /**
     * Sets an imported direction for this sample.
     * 
     * The direction can be retrieved with
     * {@link #setImportedDirection(net.talvi.puffinplot.data.Vec3)} and is
     * exported by {@link #toStrings()}.
     * 
     * @param importedDirection the imported direction to set
     */
    public void setImportedDirection(Vec3 importedDirection) {
        this.importedDirection = importedDirection;
    }

    /**
     * Returns the sample direction.
     * 
     * If there is a PCA direction, it will be returned; the method falls
     * back to the Fisher direction and the imported direction in that 
     * order. If none of those are set, {@code null} is returned.
     * 
     * @return the sample direction, if any
     */
    public Vec3 getDirection() {
        if (getPcaAnnotated() != null) {
            return getPcaValues().getDirection();
        } else if (getFisherValues() != null) {
            return getFisherValues().getMeanDirection();
        } else if (getImportedDirection() != null) {
            return getImportedDirection();
        } else {
            return null;
        }
    }

    /**
     * Flags the selected treatment steps for use in the next great-circle fit.
     */
    public void useSelectionForCircleFit() {
        touch();
        forEachTreatmentStep(step -> step.setOnCircle(step.isSelected()));
    }

    /**
     * Returns the current great-circle fit for this sample, if any.
     *
     * @return the current great-circle fit for this sample, if any
     */
    public GreatCircle getGreatCircle() {
        return greatCircle;
    }
    
    /**
     * Returns the magnetic moment vectors used for the current great-circle
     * fit.
     *
     * @param correction the correction to apply to the magnetic moment vectors
     * @return the magnetic moment vectors used for the current great-circle fit
     */
    public List<Vec3> getCirclePoints(Correction correction) {
        final List<Vec3> result = new ArrayList<>(getTreatmentSteps().size());
        for (TreatmentStep step: getTreatmentSteps()) {
            if (step.isOnCircle()) result.add(step.getMoment(correction));
        }
        return result;
    }

    /**
     * Fits a great circle to a subset of the magnetic moment vectors in this
     * sample. A treatment step is used for the fit if
     * {@link TreatmentStep#isOnCircle()} is true for it.
     *
     * @param correction the correction to apply to the magnetic moment
     * treatmentSteps
     */
    public void fitGreatCircle(Correction correction) {
        touch();
        final List<Vec3> points = getCirclePoints(correction);
        if (points.size() < 2) return;
        greatCircle = GreatCircle.fromBestFit(points);
    }
    
    /**
     * Returns the treatment level for the first point used in the great-circle
     * fit. Temperatures are returned in degrees Celsius. Magnetic field
     * strengths are returned as equivalent magnetic induction in millitesla.
     * (Note that this differs from PuffinPlot's internal representation,
     * which is in tesla.)
     * 
     * @return the treatment level for the first point used in the great-circle
     * fit
     */
    public double getFirstGcStepLevel() {
        for (TreatmentStep step: treatmentSteps) {
            if (step.isOnCircle()) {
                return step.getTreatmentLevel() *
                        (step.getTreatmentType().isMagneticField() ? 1000. : 1);
            }
        }
        return -1;
    }

    /**
     * Returns the treatment level for the last point used in the great-circle
     * fit. Temperatures are returned in degrees Celsius. Magnetic field
     * strengths are returned as equivalent magnetic induction in millitesla.
     * (Note that this differs from PuffinPlot's internal representation,
     * which is in tesla.)
     *
     * @return the treatment level for the last point used in the great-circle
     * fit
     */
    public double getLastGcStepLevel() {
        double result = -1;
        for (TreatmentStep step: treatmentSteps) {
            if (step.isOnCircle()) {
                result = step.getTreatmentLevel() *
                        (step.getTreatmentType().isMagneticField() ? 1000. : 1);
            }
        }
        return result;
    }
    
    /**
     * Returns the measurement type of this sample (discrete or continuous).
     *
     * @return the measurement type of this sample (discrete or continuous)
     */
    public MeasurementType getMeasurementType() {
        for (TreatmentStep step: getTreatmentSteps()) {
            if (step.getMeasurementType().isActualMeasurement()) {
                return step.getMeasurementType();
            }
        }
        return MeasurementType.DISCRETE;
    }

    /**
     * Returns the sample identifier or depth. For a discrete sample this will
     * return the name (identifier) of the sample. For a continuous sample it
     * will return a string representation of the depth.
     *
     * @return the sample identifier or depth
     */
    public String getNameOrDepth() {
        return nameOrDepth;
    }
    
    /**
     * Set the identifier of a sample. If the sample is discrete, its name is
     * set to the supplied identifier. If the sample is continuous, the sample's
     * depth is set to the supplied identifier, which is assumed to be a valid
     * string representation of a floating-point number.
       * 
     * @param newNameOrDepth
     */
    public void setNameOrDepth(String newNameOrDepth) {
        nameOrDepth = newNameOrDepth;
        if (getMeasurementType() == MeasurementType.CONTINUOUS) {
            setDepth(nameOrDepth);
            forEachTreatmentStep(step -> step.setDepth(nameOrDepth));
        }
        if (getMeasurementType() == MeasurementType.DISCRETE) {
            forEachTreatmentStep(step -> step.setDiscreteId(nameOrDepth));
        }
    }

    /**
     * Returns the tray slot number for discrete samples. Not currently used.
     *
     * @return the tray slot number for discrete samples
     */
    public int getSlotNumber() {
        return getTreatmentSteps().get(0).getSlotNumber();
    }

    /**
     * Returns the run number for the first treatment step in this sample.
     *
     * @return the run number for the first treatment step in this sample
     */
    public int getFirstRunNumber() {
        return getTreatmentSteps().get(0).getRunNumber();
    }

    /**
     * Returns the run number for the last treatment step in this sample.
     *
     * @return the run number for the last treatment step in this sample
     */
    public int getLastRunNumber() {
        return getTreatmentSteps().get(getTreatmentSteps().size()-1).
                getRunNumber();
    }

    /**
     * Returns the datum with the highest run number which is less than the
     * supplied run number. Intended to be used in applying tray corrections.
     * Not currently used.
     *
     * @param maxRunNumber the run number against which to compare run numbers
     * associated with this sample
     * @return the TreatmentStep in this sample which has the highest run number
     * smaller than the supplied run number, or null if this sample contains no
     * such TreatmentStep.
     */
    public TreatmentStep getTreatmentStepByRunNumber(int maxRunNumber) {
        TreatmentStep result = null;
        for (TreatmentStep step: getTreatmentSteps()) {
            if (step.getRunNumber() < maxRunNumber) {
                result = step;
            }
        }
        return result;
    }

    /**
     * Reports whether this sample is an empty slot on the measurement tray.
     *
     * @return {@code true} if this sample is an empty slot on the measurement
     * tray
     */
    public boolean isEmptySlot() {
        return isEmptySlot;
    }

    /**
     * Sets whether this sample is an empty slot on the measurement tray.
     *
     * @param isEmptySlot {@code true} to declare this sample as an empty slot
     * on the measurement tray
     */
    public void setEmptySlot(boolean isEmptySlot) {
        touch();
        this.isEmptySlot = isEmptySlot;
    }

    /**
     * Unhides all treatment steps within this sample.
     */
    public void unhideAllPoints() {
        touch();
        forEachTreatmentStep(step -> step.setHidden(false));
    }

    /**
     * Returns the selected treatment step indices as a bit set. The
     * treatment steps within the sample are ordered, and each bit in the bit
     * set is set to the selection state of the corresponding treatmentSteps
     * point. This is useful for copying and pasting selection patterns,
     * allowing corresponding points to be selected in multiple samples.
       * 
     * @return the selected treatment step indices as a bit set
     * @see #setSelectionBitSet(java.util.BitSet)
     */
    public BitSet getSelectionBitSet() {
        final BitSet result = new BitSet(treatmentSteps.size());
        for (int i = 0; i< treatmentSteps.size(); i++) {
            final TreatmentStep treatmentStep = treatmentSteps.get(i);
            result.set(i, treatmentStep.isSelected());
        }
        return result;
    }
    
    /**
     * Sets the selection state of the sample's treatment steps from a supplied
     * bit set. For each index in the bit set, the treatment step with the same
     * index within the sample is selected if the bit has a 1 value.
     *
     * @param selection a template for the selection state of the treatment
     * steps
     * @see #getSelectionBitSet()
     */
    public void setSelectionBitSet(BitSet selection) {
        touch();
        for (int i = 0; i<Math.min(selection.size(), treatmentSteps.size());
                i++) {
            final TreatmentStep treatmentStep = treatmentSteps.get(i);
            treatmentStep.setSelected(selection.get(i));
        }
    }

    /**
     * Returns the site for this sample.
     *
     * @return the site for this sample
     */
    public Site getSite() {
        return site;
    }

    /**
     * Sets the site for this sample.
     *
     * @param site the site for this sample
     */
    public void setSite(Site site) {
        touch();
        this.site = site;
    }

    /**
     * Sets the AMS data for this sample using the supplied tensor.
     * 
     * @param k11 tensor value (1,1)
     * @param k22 tensor value (2,2)
     * @param k33 tensor value (3,3)
     * @param k12 tensor value (1,2)
     * @param k23 tensor value (2,3)
     * @param k13 tensor value (1,3)
     */
    public void setAmsFromTensor(double k11, double k22, double k33,
            double k12, double k23, double k13) {
        touch();
        final Matrix scm = new Matrix(Vec3.getSampleCorrectionMatrix(
                toRadians(getSampAz() + getMagDev()),
                toRadians(getSampDip())));
        final Matrix fcm = new Matrix(Vec3.getFormationCorrectionMatrix(
                toRadians(getFormAz() + getMagDev()),
                toRadians(getFormDip())));
        ams = new Tensor(k11, k22, k33, k12, k23, k13, scm, fcm);
    }

    /**
     * Sets the AMS data for the sample using the supplied principal directions.
     * All angles are in degrees.
     *
     * @param i1 inclination of principal axis 1
     * @param d1 declination of principal axis 1
     * @param i2 inclination of principal axis 2
     * @param d2 declination of principal axis 2
     * @param i3 inclination of principal axis 3
     * @param d3 declination of principal axis 3
     */
    public void setAmsDirections(double i1, double d1, double i2, double d2,
            double i3, double d3) {
        touch();
        final Vec3 k1 = correctFully(Vec3.fromPolarDegrees(1., i1, d1));
        final Vec3 k2 = correctFully(Vec3.fromPolarDegrees(1., i2, d2));
        final Vec3 k3 = correctFully(Vec3.fromPolarDegrees(1., i3, d3));
        ams = Tensor.fromDirections(k1, k2, k3);
    }
    
    private Vec3 correctFully(Vec3 v) {
       final Vec3 sc  = v.correctSample(toRadians(getSampAz() + getMagDev()),
               toRadians(getSampDip()));
       final Vec3 fc = sc.correctForm(toRadians(getFormAz() + getMagDev()),
               toRadians(getFormDip()));
       return fc;
    }

    /**
     * Returns this sample's AMS tensor.
     *
     * @return this sample's AMS tensor, or {@code null} if none has been set
     */
    public Tensor getAms() {
        return ams;
    }
    
    /**
     * Returns a specified subset of demagnetization treatmentSteps as strings.
     * This method takes a list of {@link TreatmentParameter}s and returns a
     * list of strings. Each string in the list represents one
     * {@link TreatmentStep} in this sample, and consists of a concatenation of
     * string representations of the requested fields (in the corresponding
     * order), delimited by tab characters.
     * 
     * @param  fields the fields to export
     * @return a string representation of the requested fields for
     *         each {@link TreatmentStep} in this sample
     */
    public List<String> exportFields(List<TreatmentParameter> fields) {
        return getTreatmentSteps().stream().
                map(d -> d.exportFieldValues(fields, "\t")).
                collect(Collectors.toList());
    }

    /**
     * Returns a list of Strings representing data pertaining to this sample.
     * (Note that this only includes sample-level data, not TreatmentStep-level
     * data such as magnetic moment measurements.)
     * 
     * @return a list of Strings representing data pertaining to this sample
     */
    public List<String> toStrings() {
        List<String> result = new ArrayList<>();
        if (customFlags.size()>0) {
            result.add("CUSTOM_FLAGS\t" + customFlags.exportAsString());
        }
        if (customNotes.size()>0) {
            result.add("CUSTOM_NOTES\t" + customNotes.exportAsString());
        }
        if (site != null) {
            result.add("SITE\t" + site.getName());
        }
        if (importedDirection != null) {
            result.add(String.format(Locale.ENGLISH,
                    "IMPORTED_DIRECTION\t%.3f\t%.3f",
                    importedDirection.getDecDeg(),
                    importedDirection.getIncDeg()));
        }
        return result;
    }
    
    /**
     * Sets some of sample's fields based on a supplied string. Any string
     * produced by {@link #toStrings()} is a valid input for this method.
     *
     * @param string a string specifying some of the sample's fields
     */
    public void fromString(String string) {
        final String[] parts =
                string.split("\t", -1); // don't discard trailing empty strings
        switch (parts[0]) {
            case "CUSTOM_FLAGS":
                final List<Boolean> flags = new ArrayList<>(parts.length-1);
                for (int i=1; i<parts.length; i++) {
                    flags.add(Boolean.parseBoolean(parts[i]));
                }   customFlags = new CustomFields<>(flags);
                break;
            case "CUSTOM_NOTES":
                final List<String> notes = new ArrayList<>(parts.length-1);
                for (int i=1; i<parts.length; i++) {
                    notes.add(parts[i]);
                }   customNotes = new CustomFields<>(notes);
                break;
            case "SITE":
                final Site mySite = suite.getOrCreateSite(parts[1]);
                mySite.addSample(this);
                break;
            case "IMPORTED_DIRECTION":
                final double dec = Double.parseDouble(parts[1]);
                final double inc = Double.parseDouble(parts[2]);
                importedDirection = Vec3.fromPolarDegrees(1., inc, dec);
                break;
            default:
                LOGGER.log(Level.WARNING, "Sample field {0} not recognized.",
                        parts[0]);
                break;
        }
    }

    /**
     * Returns this sample's custom flags.
     *
     * @return this sample's custom flags
     */
    public CustomFields<Boolean> getCustomFlags() {
        return customFlags;
    }
    
    /**
     * Returns this sample's custom notes.
     *
     * @return this sample's custom notes
     */
    public CustomFields<String> getCustomNotes() {
        return customNotes;
    }

    /**
     * Returns this sample's dip azimuth in degrees.
     *
     * @return this sample's dip azimuth in degrees
     */
    public double getSampAz() {
        return sampAz;
    }

    /**
     * Sets this sample's dip azimuth in degrees.
     *
     * @param sampAz this sample's dip azimuth in degrees
     */
    private void setSampAz(double sampAz) {
        touch();
        this.sampAz = sampAz;
        setForAllSteps(TreatmentStep::setSampAz, this.sampAz);
    }

    /**
     * Returns this sample's dip angle in degrees.
     *
     * @return this sample's dip angle in degrees
     */
    public double getSampDip() {
        return sampDip;
    }

    private void setSampDip(double sampDip) {
        touch();
        this.sampDip = sampDip;
        setForAllSteps(TreatmentStep::setSampDip, this.sampDip);
    }
    
    /**
     * Returns this sample's hade in degrees.
     *
     * @return this sample's hade in degrees
     */
    public double getSampHade() {
        return 90 - sampDip;
    }

    private void setSampHade(double sampHade) {
        touch();
        this.sampDip = 90 - sampHade;
        setForAllSteps(TreatmentStep::setSampDip, this.sampDip);
    }

    /**
     * Returns this sample's formation dip azimuth in degrees.
     *
     * @return sampAz this sample's formation dip azimuth in degrees
     */
    public double getFormAz() {
        return formAz;
    }

    private void setFormAz(double formAz) {
        touch();
        this.formAz = formAz;
        setForAllSteps(TreatmentStep::setFormAz, this.formAz);
    }
    
    /**
     * Returns this sample's formation strike in degrees.
     *
     * @return sampAz this sample's formation strike in degrees
     */
    public double getFormStrike() {
        double strike = formAz - 90;
        if (strike < 0) {
            strike += 360;
        }
        return strike;
    }

    private void setFormStrike(double formStrike) {
        touch();
        double az = formStrike + 90;
        if (az > 360) {
            az -= 360;
        }
        this.formAz = az;
        setForAllSteps(TreatmentStep::setFormAz, this.formAz);
    }

    /**
     * Returns this sample's formation dip angle in degrees.
     *
     * @return sampAz this sample's formation dip angle in degrees
     */
    public double getFormDip() {
        return formDip;
    }

    private void setFormDip(double formDip) {
        touch();
        this.formDip = formDip;
        setForAllSteps(TreatmentStep::setFormDip, this.formDip);
    }

    /**
     * Returns the geomagnetic field declination at the sampling site.
     *
     * @return the geomagnetic field declination at the sampling site
     */
    public double getMagDev() {
        return magDev;
    }

    private void setMagDev(double magDev) {
        touch();
        this.magDev = magDev;
        setForAllSteps(TreatmentStep::setMagDev, this.magDev);
    }
    
    private void setForAllSteps(ObjDoubleConsumer<TreatmentStep> setter,
            double value) {
        getTreatmentSteps().stream()
                .forEach(step -> setter.accept(step, value));
    }

    /**
     * Sets the value of a specified field for each treatment step in the
     * sample.
     *
     * @param field the field to set
     * @param value the value to which to set the specified field
     */
    public void setValue(TreatmentParameter field, String value) {
        touch();
        switch (field) {
            case SAMPLE_AZ:
                setSampAz(parseDouble(value));
                break;
            case SAMPLE_DIP:
                setSampDip(parseDouble(value));
                break;
            case VIRT_SAMPLE_HADE:
                setSampHade(parseDouble(value));
                break;
            case FORM_AZ:
                setFormAz(parseDouble(value));
                break;
            case FORM_DIP:
                setFormDip(parseDouble(value));
                break;
            case VIRT_FORM_STRIKE:
                setFormStrike(parseDouble(value));
                break;
            case MAG_DEV:
                setMagDev(parseDouble(value));
                break;
            default:
                LOGGER.log(Level.WARNING, "Unhandled TreatmentStep field: {0}",
                        field.name());
                break;
        }
        for (TreatmentStep step: getTreatmentSteps()) {
            step.setValue(field, value, 1);
        }
    }
    
    /**
     * Sets this sample's suite as "modified". Intended to be called
     * from any method that modifies the sample, to keep track of whether
     * the suite has been saved since the last modification. 
     */
    public void touch() {
        if (suite != null) {
            suite.setSaved(false);
        }
    }

    /**
     * Calculates a Fisherian mean for the selected demagnetization steps.
     * 
     * @param correction the correction to apply to the moment measurements
     */
    public void calculateFisher(Correction correction) {
        final List<TreatmentStep> selection = getSelectedTreatmentSteps();
        final List<Vec3> directions = new ArrayList<>(selection.size());
        for (TreatmentStep step: selection) {
            directions.add(step.getMoment(correction));
        }
        fisherValues = FisherValues.calculate(directions);
    }
    
    /**
     * Truncates the list of treatment steps to a specified number of
     * entries.
     * 
     * @param items the number of treatment steps to retain; the first
     * {@code items} steps will be kept, and the rest deleted.
     */
    public void truncateTreatmentSteps(int items) {
        treatmentSteps = treatmentSteps.subList(0, items);
    }
    
    /**
     * Rotate all the magnetic moment vectors of this sample about the
     * z axis.
     *
     * @param angleDegrees the angle (in degrees) by which to rotate the
     * magnetic moments
     */
    public void rotateAroundZAxis(double angleDegrees) {
        forEachTreatmentStep(step -> step.setMoment(
                step.getMoment().rotZ(Math.toRadians(angleDegrees))));
    }
    
    /**
     * Returns the discrete ID of this sample, as determined by the discrete ID
     * of the first TreatmentStep within the sample. For samples with continuous
     * measurement type, this is the identifier of the entire measured core
     * section. If the sample contains no TreatmentStep objects, this method
     * will return {@code null}.
     * 
     * @return discrete sample ID, core section ID, or {@code null}
     */
    public String getDiscreteId() {
        if (hasTreatmentSteps()) {
            return getTreatmentStepByIndex(0).getDiscreteId();
        } else {
            return null;
        }
    }

    /**
     * Sets the discrete ID of this sample by setting the discrete IDs of its
     * {@code TreatmentStep} objects, if the sample has any
     * {@code TreatmentStep} objects. If the sample has no {@code TreatmentStep}
     * objects, {@code IllegalStateException} will be thrown. (This limitation
     * is a historical artefact which will be removed when PuffinPlot's
     * data model is revised to store the discrete ID within the
     * {@code Sample} object itself.)
     *
     * @param discreteId the discrete ID to set
     */
    public void setDiscreteId(String discreteId) {
        if (hasTreatmentSteps()) {
            forEachTreatmentStep(step -> step.setDiscreteId(discreteId));
        } else {
            throw new IllegalStateException(
                    "This sample has no treatment steps.");
        }
    }
    
    /**
     * Remove specified treatmentSteps from this sample. Any
     * {@code TreatmentStep} in the collection passed to this method will be
     * removed. Any {@code TreatmentStep} in the collection which is not in this
     * Sample will be ignored.
     * 
     * @param toRemove the treatmentSteps to remove
     */
    public void removeData(Collection<TreatmentStep> toRemove) {
        toRemove.forEach(d -> treatmentSteps.remove(d));
    }

    /**
     * Merges duplicate measurements within this Sample. Measurements are
     * duplicates if they have the same treatment type and treatment level. They
     * are merged by retaining only the first measurement in each set of
     * duplicates and setting its moment to the mean of the moments of the
     * entire set of duplicates.
     * 
     * @return the treatmentSteps which were removed
     * 
     */
    public Set<TreatmentStep> mergeDuplicateTreatmentSteps() {
        final Map<TreatmentTypeAndLevel, List<TreatmentStep>> treatmentMap =
                new HashMap<>();
        for (TreatmentStep step: this.getTreatmentSteps()) {
            final TreatmentTypeAndLevel key = new TreatmentTypeAndLevel(step);
            if (!treatmentMap.containsKey(key)) {
                treatmentMap.put(key, new ArrayList<>());
            }
            treatmentMap.get(key).add(step);
        }
        /*
         * First check if any merging needs to be done at all, to avoid
         * needlessly creating a new object.
         */
        boolean anyMergables = false;
        for (List<TreatmentStep> steps: treatmentMap.values()) {
            if (steps.size() > 1) {
                anyMergables = true;
                break;
            }
        }
        if (!anyMergables) {
            return Collections.emptySet();
        }
        final Set<TreatmentStep> toRemove = new HashSet<>(treatmentMap.size());
        for (TreatmentStep step: this.getTreatmentSteps()) {
            final List<TreatmentStep> duplicates =
                    treatmentMap.get(new TreatmentTypeAndLevel(step));
            if (duplicates.get(0) == step) {
                step.setMomentToMean(duplicates);
            } else {
                toRemove.add(step);
            }
        }
        this.removeData(toRemove);
        return toRemove;
    }

    /**
     * Merges the demagnetization data of the supplied samples into the first
     * sample in the list. Note that the TreatmentStep objects in the subsequent
     * samples are not cloned: their references are copied directly into the
     * first sample's treatmentSteps list. The subsequent samples should
     * therefore be discarded after calling this function.
     * 
     * @param samples 
     */
    public static void mergeSamples(List<Sample> samples) {
        if (samples.size() < 2) {
            return;
        }
        final Sample firstSample = samples.get(0);
        for (Sample sample: samples.subList(1, samples.size())) {
            sample.getTreatmentSteps().
                    forEach(step -> firstSample.addTreatmentStep(step));
        }
        firstSample.mergeDuplicateTreatmentSteps();
        firstSample.treatmentSteps.sort(new TreatmentLevelComparator());
    }
    
    private static class TreatmentTypeAndLevel {
        private final TreatmentType treatmentType;
        private final Double treatmentLevel;
        
        public TreatmentTypeAndLevel(TreatmentStep treatmentStep) {
            this.treatmentType = treatmentStep.getTreatmentType();
            this.treatmentLevel = treatmentStep.getTreatmentLevel();
        }
        
        @Override
        public int hashCode() {
            return treatmentType.hashCode() ^ treatmentLevel.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TreatmentTypeAndLevel other = (TreatmentTypeAndLevel) obj;
            if (this.treatmentType != other.treatmentType) {
                return false;
            }
            if (!Objects.equals(this.treatmentLevel, other.treatmentLevel)) {
                return false;
            }
            return true;
        }
    }
    
    private void forEachTreatmentStep(Consumer<TreatmentStep> f) {
        getTreatmentSteps().forEach(f);
    }
}
