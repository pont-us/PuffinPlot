package net.talvi.puffinplot.data;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import static java.lang.Math.toRadians;
import static java.lang.Double.parseDouble;

/**
 * This class represents a sample on which measurements have been made.
 * It may correspond either to a discrete, physical specimen, or to
 * a particular point on a continuous long core or u-channel sample.
 * 
 * @author pont
 */
public class Sample {
    
    private final List<Datum> data;
    private Site site;
    private final String nameOrDepth;
    private final double depth;
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

    /**
     * Creates a new sample. For discrete samples, the supplied name can
     * take any form; for long core samples, it should be a string representation
     * of a number giving the depth.
     * 
     * @param name sample identifier or numerical depth
     * @param suite the data suite of which this sample is a part
     */
    public Sample(String name, Suite suite) {
        this.nameOrDepth = name;
        double depthTmp = Double.NaN;
        try {
            depthTmp = Double.parseDouble(name);
        } catch (NumberFormatException ex) {
            // Nothing to do here: if it's not valid it's probably just
            // a discrete sample.
        }
        this.depth = depthTmp;
        this.suite = suite;
        this.data = new ArrayList<Datum>();
        this.customFlags = new CustomFields<Boolean>();
        this.customNotes = new CustomFields<String>();
    }
    
    /** Clears all calculations for this sample (PCA, MDF, and great-circle
     * fit) and deselects all data points. */
    public void clearCalculations() {
        pca = null;
        mdf = null;
        greatCircle = null;
        selectNone();
        for (Datum d: getData()) d.setOnCircle(false);
    }
    
    /** Returns the suite containing this sample.
     * @return the suite containing this sample */
    public Suite getSuite() {
        return suite;
    }
    
    /**
     * Calculates the median destructive field using the visible data
     * points of this sample and stores the results within this sample.
     * 
     * @see #getMdf()
     */
    public void calculateMdf() {
        mdf = MedianDestructiveField.calculate(getVisibleData());
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
        if (data.isEmpty()) return Double.NaN;
        return data.get(0).getIntensity();
    }

    /**
     * Calculates the position of the first jump in magnetic susceptibility
     * in the sample's demagnetization data. The result is stored within 
     * this sample. A jump is defined as an increase of at least 2.5 times
     * in a single treatment step. If the is no magnetic susceptibility
     * data, or if no jump occurs, a value of 0 will be used.
     * 
     * @see #getMagSusJump()
     * 
     */
    public void calculateMagSusJump() {
        final double limit = 2.5;
        double msj = 0;
        double prevMagSus = 1e200;
        for (Datum d: data) {
            double magSus = d.getMagSus();
            if (!Double.isNaN(magSus)) {
                if (magSus > prevMagSus * limit) {
                    msj = d.getTemp();
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
     * Rotates all magnetic moment data 180 degrees about the sepcified axis.
     * @param axis the axis about which to rotate the data
     */
    public void flip(MeasurementAxis axis) {
        for (Datum d: getData()) d.rot180(axis);
    }

    /** Sets all the selected data points within this sample to be hidden,
     * so they will not be shown on plots.
     * @see Datum#isHidden() */
    public void hideSelectedPoints() {
        for (Datum d: getData()) {
            if (d.isSelected()) {
                d.setSelected(false);
                d.setHidden(true);
            }
        }
    }
    
    /** Selects all the data points within this sample.
     * @see Datum#setSelected(boolean) */
    public void selectAll() {
        for (Datum d : getData()) d.setSelected(true);
    }

    /** Selects all the visible (non-hidden) data points within this sample.
     * @see Datum#setSelected(boolean)
     * @see Datum#isHidden() */
    public void selectVisible() {
        for (Datum d : getData()) {
            if (!d.isHidden()) d.setSelected(true);
        }
    }

    /** De-selects all the data points within this sample.  */
    public void selectNone() {
        for (Datum d : getData()) d.setSelected(false);
    }
    
    /** Reports whether this sample contains any data. 
     * @return {@code true} if this sample contains any data */
    public boolean hasData() {
        return !data.isEmpty();
    }
    
    /** Returns all the data points within this sample.
     * @return all the data points within this sample */
    public List<Datum> getData() {
        return Collections.unmodifiableList(data);
    }
    
    /** Returns all the visible (non-hidden) data points within this sample.
     * @return all the visible (non-hidden) data points within this sample */
    public List<Datum> getVisibleData() {
        List<Datum> visibleData = new ArrayList<Datum>(getNumData());
        for (Datum d: getData()) if (!d.isHidden()) visibleData.add(d);
        return visibleData;
    }

    /** Returns all the selected data points within this sample.
     * @return all the selected data points within this sample */
    public List<Datum> getSelectedData() {
        LinkedList<Datum> selData = new LinkedList<Datum>();
        for (Datum d: getData()) if (d.isSelected()) selData.add(d);
        return selData;
    }
    
    /** Returns {@code true} if the selected points are contiguous.
     * This is the case if there are no unselected points between the
     * first selected point and the last selected point.
     * 
     * @return {@code true} if the selected points are contiguous
     */
    public boolean isSelectionContiguous() {
        int runEndsSeen = 0;
        boolean thisIsSelected = false, lastWasSelected = false;
        for (Datum d: getData()) {
            thisIsSelected = d.isSelected();
            if (lastWasSelected && !thisIsSelected) runEndsSeen++;
            lastWasSelected = thisIsSelected;
        }
        if (thisIsSelected) runEndsSeen++;
        return (runEndsSeen <= 1);
    }

    /** Returns the number of data points within this sample.
     * @return the number of data points within this sample */
    public int getNumData() {
        return getData().size();
    }

    /** Returns a specified data point from this sample 
     * @param i the index of the requested data point
     * @return the data point with the selected index, if it exists
     * @throws IndexOutOfBoundsException if no data point with the selected index exists
     */
    public Datum getDatum(int i) {
        return getData().get(i);
    }
    
    /** Adds a data point to this sample.
     * @param datum a data point to add to this sample
     */
    public void addDatum(Datum datum) {
        if (data.isEmpty()) {
            setSampAz(datum.getSampAz());
            setSampDip(datum.getSampDip());
            setFormAz(datum.getFormAz());
            setFormDip(datum.getFormDip());
            setMagDev(datum.getMagDev());
        }
        data.add(datum);
        if (datum.hasMagSus()) hasMsData = true;
        datum.setSample(this);
    }

    /**
     * Sets the orientation corrections for this sample's magnetic moment data.
     * @param sampleAz the sample dip azimuth
     * @param sampleDip the sample dip angle
     * @param formAz the formation dip azimuth
     * @param formDip the formation dip angle
     * @param magDev the geomagnetic field declination at the sampling site
     */
    public void setCorrections(double sampleAz, double sampleDip,
            double formAz, double formDip, double magDev) {
        this.setSampAz(sampleAz);
        this.setSampDip(sampleDip);
        this.setFormAz(formAz);
        this.setFormDip(formDip);
        this.setMagDev(magDev);
    }

    /** Reports whether this sample has any magnetic susceptibility data.
     * @return {@code true} if this sample has any magnetic susceptibility data
     */
    public boolean hasMsData() {
        return hasMsData;
    }

    /** Flags all selected data points for inclusion in principal component analysis */
    public void useSelectionForPca() {
        for (Datum d: getData()) d.setInPca(d.isSelected());
    }
    
    /** Reports whether principal component analysis should be anchored for this sample
     * @return {@code true} if principal component analysis should be anchored for this sample */
    public boolean isPcaAnchored() {
        return data.isEmpty() ? false : data.get(0).isPcaAnchored();
    }
    
    /** Sets whether principal component analysis should be anchored for this sample
     * @param pcaAnchored {@code true} to anchor principal component analysis for this sample */
    public void setPcaAnchored(boolean pcaAnchored) {
        for (Datum d: getData()) d.setPcaAnchored(pcaAnchored);
    }
    
    /**
     * Performs principal component analysis on a subset of the magnetic moment 
     * data of this sample.
     * The data points to use are determined by the result of 
     * {@link Datum#isInPca()}.
     * The results are stored within the sample and may be retrieved with
     * {@link #getPcaAnnotated()}.
     * @param correction the correction to apply to the magnetic moment data
     */
    public void doPca(Correction correction) {
        if (!hasData()) return;
        // use the first data point's anchoring status for all of them
        // (a partially anchored PCA makes no sense). This is just
        // belt-and-braces really, since they should be uniform across
        // the sample. Eventually pcaAnchored will be moved entirely
        // from Datum to its rightful home in Sample and this kind
        // of ugliness will become unnecessary.
        boolean firstDatumAnchored = getData().get(0).isPcaAnchored();
        setPcaAnchored(firstDatumAnchored);
        pca = PcaAnnotated.calculate(this, correction);
    }
    
    /** Returns the annotated results of the last PCA calculation.
     * @return the annotated results of the last PCA calculation */
    public PcaAnnotated getPcaAnnotated() {
        return pca;
    }

    /** Returns the results of the last PCA calculation.
     * @return the results of the last PCA calculation */
    public PcaValues getPcaValues() {
        return pca == null ? null : pca.getPcaValues();
    }


    /** Flags the selected data points for use in the next great-circle fit. */
    public void useSelectionForCircleFit() {
        for (Datum d: getData()) d.setOnCircle(d.isSelected());
    }

    /** Returns the current great-circle fit for this sample, if any.
     * @return the current great-circle fit for this sample, if any
     */
    public GreatCircle getGreatCircle() {
        return greatCircle;
    }
    
    /** Returns the magnetic moment vectors used for the current great-circle fit.
     * @param correction the correction to apply to the magnetic moment vectors
     * @return the magnetic moment vectors used for the current great-circle fit
     */
    public List<Vec3> getCirclePoints(Correction correction) {
        List<Vec3> result = new ArrayList<Vec3>(getData().size());
        for (Datum d: getData()) {
            if (d.isOnCircle()) result.add(d.getMoment(correction));
        }
        return result;
    }

    /** Fits a great circle to a subset of the magnetic moment vectors in 
     * this sample. A data point is used for the fit if 
     * {@link Datum#isOnCircle()} is true for it.
     * @param correction the correction to apply to the magnetic moment data
     */
    public void fitGreatCircle(Correction correction) {
        final List<Vec3> points = getCirclePoints(correction);
        if (points.size() < 2) return;
        greatCircle = new GreatCircle(points);
    }
    
    /** Returns the treatment level for the first point used in the great-circle fit. 
     * @return the treatment level for the first point used in the great-circle fit */
    public double getFirstGcStep() {
        for (Datum d: data) {
            if (d.isOnCircle()) return d.getTreatmentStep();
        }
        return -1;
    }

    /** Returns the treatment level for the last point used in the great-circle fit. 
     * @return the treatment level for the last point used in the great-circle fit */
    public double getLastGcStep() {
        double result = -1;
        for (Datum d : data) {
            if (d.isOnCircle()) result = d.getTreatmentStep();
        }
        return result;
    }
    
    /** Returns the measurement type of this sample (discrete or continuous).
     * @return the measurement type of this sample (discrete or continuous)
     */
    public MeasType getMeasType() {
        for (Datum d: getData())
            if (d.getMeasType().isActualMeasurement()) return d.getMeasType();
        return MeasType.DISCRETE;
    }

    /** Returns the sample identifier or depth. For a discrete sample 
     * this will return the name (identifier) of the sample. For a 
     * continuous sample it will return a string representation of the depth.
     * @return the sample identifier or depth
     */
    public String getNameOrDepth() {
        return nameOrDepth;
    }

    /** Returns the tray slot number for discrete samples. Not currently used.
     * @return the tray slot number for discrete samples
     */
    public int getSlotNumber() {
        return getData().get(0).getSlotNumber();
    }

    /** Returns the run number for the first data point in this sample.
     * @return the run number for the first data point in this sample */
    public int getFirstRunNumber() {
        return getData().get(0).getRunNumber();
    }

    /** Returns the run number for the last data point in this sample.
     * @return the run number for the last data point in this sample */
    public int getLastRunNumber() {
        return getData().get(getData().size()-1).getRunNumber();
    }

    /**
     * Returns the datum with the highest run number which is less
     * than the supplied run number. Intended to be used in applying
     * tray corrections. Not currently used.
     *
     * @param maxRunNumber
     * @return the Datum in this sample which has the highest
     * run number smaller than the supplied run number, or null if
     * this sample contains no such Datum.
     */
    public Datum getDatumByRunNumber(int maxRunNumber) {
        Datum result = null;
        for (Datum d: getData()) {
            if (d.getRunNumber() < maxRunNumber) {
                result = d;
            }
        }
        return result;
    }

    /** Reports whether this sample is an empty slot on the measurement tray.
     * @return {@code true} if this sample is an empty slot on the measurement tray
     */
    public boolean isEmptySlot() {
        return isEmptySlot;
    }

    /** Sets whether this sample is an empty slot on the measurement tray.
     * @param isEmptySlot {@code true} to deeclare this sample as an empty slot on the measurement tray
     */
    public void setEmptySlot(boolean isEmptySlot) {
        this.isEmptySlot = isEmptySlot;
    }

    /** Unhides all data points within this sample. */
    public void unhideAllPoints() {
        for (Datum d: getData()) d.setHidden(false);
    }

    /**
     * Returns the selected data point indices as a bit set. The data points within
     * the sample are ordered, and each bit in the bit set is set to the
     * selection state of the corresponding data point. This is useful for
     * copying and pasting selection patterns, allowing corresponding
     * points to be selected in multiple samples.
     * 
     * @return the selected data point indices as a bit set
     * @see #setSelectionBitSet(java.util.BitSet)
     */
    public BitSet getSelectionBitSet() {
        final BitSet result = new BitSet(data.size());
        for (int i=0; i<data.size(); i++) {
            final Datum datum = data.get(i);
            result.set(i, datum.isSelected());
        }
        return result;
    }
    
    /**
     * Sets the selection state of the sample's data points from a 
     * supplied bit set. For each index in the bit set, the data point
     * with the same index within the sample is selected if the bit
     * has a 1 value.
     * 
     * @param selection a template for the selection state of the data points
     * @see #getSelectionBitSet()
     */
    public void setSelectionBitSet(BitSet selection) {
        for (int i=0; i<Math.min(selection.size(), data.size()); i++) {
            final Datum datum = data.get(i);
            datum.setSelected(selection.get(i));
        }
    }

    /** Returns the site for this sample.
     * @return the site for this sample */
    public Site getSite() {
        return site;
    }

    /** Sets the site for this sample.
     * @param site the site for this sample */
    public void setSite(Site site) {
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
        Matrix scm = new Matrix(Vec3.getSampleCorrectionMatrix(toRadians(getSampAz() + getMagDev()),
               toRadians(getSampDip())));
        Matrix fcm = new Matrix(Vec3.getFormationCorrectionMatrix(toRadians(getFormAz() + getMagDev()),
               toRadians(getFormDip())));
        ams = new Tensor(k11, k22, k33, k12, k23, k13, scm, fcm);
    }

    /** Sets the AMS data for the sample using the supplied principal directions.
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
        // ams = Tensor.fromDirections(i1, d1, i2, d2, i3, d3);
        Vec3 k1 = correctFully(Vec3.fromPolarDegrees(1., i1, d1));
        Vec3 k2 = correctFully(Vec3.fromPolarDegrees(1., i2, d2));
        Vec3 k3 = correctFully(Vec3.fromPolarDegrees(1., i3, d3));
        ams = Tensor.fromDirections(k1, k2, k3);
        System.out.println(ams.toTensorComponentString());
    }
    
    private Vec3 correctFully(Vec3 v) {
       final Vec3 sc  = v.correctSample(toRadians(getSampAz() + getMagDev()),
               toRadians(getSampDip()));
       final Vec3 fc = sc.correctForm(toRadians(getFormAz() + getMagDev()),
               toRadians(getFormDip()));
        return fc;
    }

    /** Returns this sample's AMS tensor 
     * @return this sample's AMS tensor
     */
    public Tensor getAms() {
        return ams;
    }
    
    /** Returns a specified subset of demagnetization data as strings.
     * This method takes a list of {@link DatumField}s and returns a list
     * of strings. Each string in the list represents one {@link Datum}
     * in this sample, and consists of a concatenation of string representations
     * of the requested fields (in the corresponding order), delimited
     * by tab characters.
     * @param fields the fields to export
     * @return a string representation of the requested fields for each {@link Datum} in this sample
     */
    public List<String> exportFields(List<DatumField> fields) {
        final List<Datum> ds = getData();
        List<String> result = new ArrayList<String>(ds.size());
        for (Datum d: ds) {
            result.add(d.exportFieldValues(fields, "\t"));
        }
        return result;
    }

    /** Returns a list of Strings representing data pertaining to this sample.
     * (Note that this only includes sample-level data, not Datum-level
     * data such as magnetic moment measurements.)
     * @return a list of Strings representing data pertaining to this sample */
    public List<String> toStrings() {
        List<String> result = new ArrayList<String>();
        if (customFlags.size()>0) {
            result.add("CUSTOM_FLAGS\t" + customFlags.exportAsString());
        }
        if (customNotes.size()>0) {
            result.add("CUSTOM_NOTES\t" + customNotes.exportAsString());
        }
        if (site != null) {
            result.add("SITE\t" + site.getName());
        }
        return result;
    }
    
    /** Sets some of sample's fields based on a supplied string. Any string
     * produced by {@link #toStrings()} is a valid input for this method.
     * @param string a string specifying some of the sample's fields
     */
    public void fromString(String string) {
        String[] parts = string.split("\t", -1); // don't discard trailing empty strings
        if ("CUSTOM_FLAGS".equals(parts[0])) {
            List<Boolean> flags = new ArrayList<Boolean>(parts.length-1);
            for (int i=1; i<parts.length; i++) {
                flags.add(Boolean.parseBoolean(parts[i]));
            }
            customFlags = new CustomFields<Boolean>(flags);
        } else if ("CUSTOM_NOTES".equals(parts[0])) {
            List<String> notes = new ArrayList<String>(parts.length-1);
            for (int i=1; i<parts.length; i++) {
                notes.add(parts[i]);
            }
            customNotes = new CustomFields<String>(notes);
        } else if ("SITE".equals(parts[0])) {
            Site mySite = suite.getOrCreateSite(parts[1]);
            mySite.addSample(this);
        }
    }

    /** Returns this sample's custom flags.
     * @return this sample's custom flags */
    public CustomFields<Boolean> getCustomFlags() {
        return customFlags;
    }
    
    /** Returns this sample's custom notes.
     * @return this sample's custom notes */
    public CustomFields<String> getCustomNotes() {
        return customNotes;
    }

    /** Returns this sample's dip azimuth in degrees. 
     * @return this sample's dip azimuth in degrees */
    public double getSampAz() {
        return sampAz;
    }

    /** Sets this sample's dip azimuth in degrees.
     * @param sampAz this sample's dip azimuth in degrees */
    private void setSampAz(double sampAz) {
        this.sampAz = sampAz;
    }

    /** Returns this sample's dip angle in degrees. 
     * @return this sample's dip angle in degrees */
    public double getSampDip() {
        return sampDip;
    }

    private void setSampDip(double sampDip) {
        this.sampDip = sampDip;
    }

    /** Returns this sample's formation dip azimuth in degrees.
     * @return sampAz this sample's formation dip azimuth in degrees */
    public double getFormAz() {
        return formAz;
    }

    private void setFormAz(double formAz) {
        this.formAz = formAz;
    }

    /** Returns this sample's formation dip angle in degrees.
     * @return sampAz this sample's formation dip angle in degrees */
    public double getFormDip() {
        return formDip;
    }

    private void setFormDip(double formDip) {
        this.formDip = formDip;
    }

    /** Returns the geomagnetic field declination at the sampling site.
     * @return the geomagnetic field declination at the sampling site */
    public double getMagDev() {
        return magDev;
    }

    private void setMagDev(double magDev) {
        this.magDev = magDev;
    }

    /** Sets the value of a specified field for each data point in the
     * sample.
     * @param field the field to set
     * @param value the value to which to set the specified field
     */
    public void setValue(DatumField field, String value) {
        switch (field) {
            case SAMPLE_AZ: setSampAz(parseDouble(value)); break;
            case SAMPLE_DIP: setSampDip(parseDouble(value)); break;
            case FORM_AZ: setFormAz(parseDouble(value)); break;
            case FORM_DIP: setFormDip(parseDouble(value)); break;
            case MAG_DEV: setMagDev(parseDouble(value)); break;
        }
        for (Datum d: getData()) {
            d.setValue(field, value);
        }
    }
}
