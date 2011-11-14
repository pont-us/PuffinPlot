package net.talvi.puffinplot.data;

import java.util.Collection;
import Jama.Matrix;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import static java.lang.Math.toRadians;
import static java.lang.Double.parseDouble;

public class Sample {
    
    private final List<Datum> data;
    private Site site;
    private final String nameOrDepth;
    private boolean isEmptySlot = false;
    private GreatCircle greatCircle;
    private PcaAnnotated pca;
    private MDF mdf;
    private boolean hasMsData = false;
    private Tensor ams;
    private double magSusJump = 0; // temperature at which mag. sus. jumps
    private CustomFields<Boolean> customFlags;
    private CustomFields<String> customNotes;
    private final Suite suite;
    private double sampAz = Double.NaN, sampDip = Double.NaN;
    private double formAz = Double.NaN, formDip = Double.NaN;
    private double magDev = Double.NaN;

    public Sample(String name, Suite suite) {
        this.nameOrDepth = name;
        this.suite = suite;
        this.data = new ArrayList<Datum>();
        this.customFlags = new CustomFields<Boolean>();
        this.customNotes = new CustomFields<String>();
    }
    
    public void clear() {
        pca = null;
        mdf = null;
        greatCircle = null;
        selectNone();
        for (Datum d: getData()) d.setOnCircle(false);
    }

    public void calculateMdf(Correction correction) {
        mdf = MDF.calculate(getVisibleData());
    }

    public MDF getMDF() {
        return mdf;
    }

    public double getNRM() {
        if (data.isEmpty()) return Double.NaN;
        return data.get(0).getIntensity();
    }

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

    public double getMagSusJump() {
        return magSusJump;
    }

    /*
     * Rotates all data 180 degrees about the X axis.
     */
    public void flip() {
        for (Datum d: getData()) d.rotX180();
    }

    public void hideSelectedPoints() {
        for (Datum d: getData()) {
            if (d.isSelected()) {
                d.setSelected(false);
                d.setHidden(true);
            }
        }
    }
    
    public void selectAll() {
        for (Datum d : getData()) d.setSelected(true);
    }

    public void selectVisible() {
        for (Datum d : getData()) {
            if (!d.isHidden()) d.setSelected(true);
        }
    }


    public void selectNone() {
        for (Datum d : getData()) d.setSelected(false);
    }

    public List<Datum> getVisibleData() {
        List<Datum> visibleData = new ArrayList<Datum>(getNumData());
        for (Datum d: getData()) if (!d.isHidden()) visibleData.add(d);
        return visibleData;
    }

    public List<Datum> getData() {
        return data;
    }

    public int getNumData() {
        return getData().size();
    }

    public Datum getDatum(int i) {
        return getData().get(i);
    }
    
    public void addDatum(Datum d) {
        if (data.isEmpty()) {
            setSampAz(d.getSampAz());
            setSampDip(d.getSampDip());
            setFormAz(d.getFormAz());
            setFormDip(d.getFormDip());
            setMagDev(d.getMagDev());
        }
        data.add(d);
        if (d.hasMagSus()) hasMsData = true;
        d.setSample(this);
    }

    public void setCorrections(double sampleAz, double sampleDip,
            double formAz, double formDip, double magDev) {
        this.setSampAz(sampleAz);
        this.setSampDip(sampleDip);
        this.setFormAz(formAz);
        this.setFormDip(formDip);
        this.setMagDev(magDev);
    }

    public boolean hasMsData() {
        return hasMsData;
    }

    public PcaAnnotated getPca() {
        return pca;
    }

    public PcaValues getPcaValues() {
        return pca == null ? null : pca.getPcaValues();
    }

    public List<Datum> getSelectedData() {
        LinkedList<Datum> selData = new LinkedList<Datum>();
        for (Datum d: getData()) if (d.isSelected()) selData.add(d);
        return selData;
    }

    public List<Vec3> getSelectedPoints(Correction correction) {
        LinkedList<Vec3> points = new LinkedList<Vec3>();
        for (Datum d: getData())
            if (d.isSelected())
                points.add(d.getMoment(correction));
        return points;
    }

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
    
    public void doPca(boolean anchored, Correction correction) {
        if (!hasData()) return;
        for (Datum d: getData()) d.setPcaAnchored(anchored);
        pca = PcaAnnotated.calculate(this, correction);
    }

    public void useSelectionForCircleFit() {
        for (Datum d : getData()) d.setOnCircle(d.isSelected());
    }

    public void useSelectionForPca() {
        for (Datum d : getData()) d.setInPca(d.isSelected());
    }

    public List<Vec3> getCirclePoints(Correction correction) {
        List<Vec3> result = new ArrayList<Vec3>(getData().size());
        for (Datum d: getData()) {
            if (d.isOnCircle()) result.add(d.getMoment(correction));
        }
        return result;
    }

    public void fitGreatCircle(Correction correction) {
        final List<Vec3> points = getCirclePoints(correction);
        if (points.size() < 2) return;
        greatCircle = new GreatCircle(points);
    }

    public void doPca(Correction correction) {
        if (!hasData()) return;
        doPca(getData().get(0).isPcaAnchored(), correction);
    }

    public MeasType getMeasType() {
        for (Datum d: getData())
            if (d.getMeasType().isActualMeasurement()) return d.getMeasType();
        return MeasType.DISCRETE;
    }

    public String getNameOrDepth() {
        return nameOrDepth;
    }

    private void checkDiscrete() {
        if (!getMeasType().isDiscrete())
            throw new UnsupportedOperationException("Only discrete measurements can have sites.");
    }

    private int getSiteSplit() {
        checkDiscrete();
        String n = getNameOrDepth();
        int len = n.length();
        int splitAt =  len - n.charAt(len-2) == '.' ? len-5 : len-4;
        return (splitAt >= 0) ? splitAt : 0;
    }

    public String getSiteId() {
        checkDiscrete();
        return getNameOrDepth().substring(0, getSiteSplit());
    }

    public String getSampleId() {
        checkDiscrete();
        return getNameOrDepth().substring(getSiteSplit());
    }
    
    public boolean isPcaAnchored() {
        return data.isEmpty() ? false : data.get(0).isPcaAnchored();
   }
    
    public void setPcaAnchored(boolean pcaAnchored) {
        for (Datum d: getData()) d.setPcaAnchored(pcaAnchored);
    }

    public int getSlotNumber() {
        return getData().get(0).getSlotNumber();
    }

    public int getFirstRunNumber() {
        return getData().get(0).getRunNumber();
    }

    public int getLastRunNumber() {
        return getData().get(getData().size()-1).getRunNumber();
    }

    /**
     * Returns the datum with the highest run number which is less
     * than the supplied run number. Intended to be used in applying
     * tray corrections.
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

    public boolean isEmptySlot() {
        return isEmptySlot;
    }

    public void setEmptySlot(boolean isEmptySlot) {
        this.isEmptySlot = isEmptySlot;
    }

    public void unhideAllPoints() {
        for (Datum d: getData()) d.setHidden(false);
    }

    public void copySelectionFrom(Sample s) {
        List<Datum> otherData = s.getData();
        for (int i=0; i<Math.min(getNumData(),s.getNumData()); i++) {
            getData().get(i).setSelected(otherData.get(i).isSelected());
        }
    }
    
    public BitSet getSelectionBitSet() {
        final BitSet result = new BitSet(data.size());
        for (int i=0; i<data.size(); i++) {
            final Datum datum = data.get(i);
            result.set(i, datum.isSelected());
        }
        return result;
    }
    
    public void setSelectionBitSet(BitSet selection) {
        for (int i=0; i<Math.min(selection.size(), data.size()); i++) {
            final Datum datum = data.get(i);
            datum.setSelected(selection.get(i));
        }
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public void setAmsFromTensor(double k11, double k22, double k33,
            double k12, double k23, double k13) {
        Matrix scm = new Matrix(Vec3.getSampleCorrectionMatrix(toRadians(getSampAz() + getMagDev()),
               toRadians(getSampDip())));
        Matrix fcm = new Matrix(Vec3.getFormationCorrectionMatrix(toRadians(getFormAz() + getMagDev()),
               toRadians(getFormDip())));
        ams = new Tensor(k11, k22, k33, k12, k23, k13, scm, fcm);
    }

    public void setAmsDirections(double i1, double d1, double i2, double d2,
            double i3, double d3) {
        // ams = Tensor.fromDirections(i1, d1, i2, d2, i3, d3);
        Vec3 k1 = correctFully(Vec3.fromPolarDegrees(1., i1, d1));
        Vec3 k2 = correctFully(Vec3.fromPolarDegrees(1., i2, d2));
        Vec3 k3 = correctFully(Vec3.fromPolarDegrees(1., i3, d3));
        ams = Tensor.fromDirections(k1, k2, k3);
        System.out.println(ams.toTensorComponentString());
    }
    
    public Vec3 correctFully(Vec3 v) {
       final Vec3 sc  = v.correctSample(toRadians(getSampAz() + getMagDev()),
               toRadians(getSampDip()));
       final Vec3 fc = sc.correctForm(toRadians(getFormAz() + getMagDev()),
               toRadians(getFormDip()));
        return fc;
    }

    public Vec3 correctSample(Vec3 v) {
        final Vec3 sc = v.correctSample(toRadians(getSampAz() + getMagDev()),
               toRadians(getSampDip()));
        return sc;
    }

    public Vec3 getAmsAxis(int axis) {
        return ams.getAxis(axis);
    }

    public Tensor getAms() {
        return ams;
    }

    public List<String> exportFields(Collection<DatumField> fields) {
        final List<Datum> ds = getData();
        List<String> result = new ArrayList<String>(ds.size());
        for (Datum d: ds) {
            result.add(d.exportFields(fields, "\t"));
        }
        return result;
    }

    public Suite getSuite() {
        return suite;
    }
    
    /**
     * Produce a list of Strings representing data pertaining to this sample.
     * (Sample-level data only; Datum-level stuff handled separately.)
     */
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
    
    /*
     * Set internal data based on a supplied string.
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

    /**
     * @return the customFlags
     */
    public CustomFields<Boolean> getCustomFlags() {
        return customFlags;
    }

    public CustomFields<String> getCustomNotes() {
        return customNotes;
    }

    public boolean hasData() {
        return !data.isEmpty();
    }

    public double getSampAz() {
        return sampAz;
    }

    public void setSampAz(double sampAz) {
        this.sampAz = sampAz;
    }

    public double getSampDip() {
        return sampDip;
    }

    public void setSampDip(double sampDip) {
        this.sampDip = sampDip;
    }

    public double getFormAz() {
        return formAz;
    }

    public void setFormAz(double formAz) {
        this.formAz = formAz;
    }

    public double getFormDip() {
        return formDip;
    }

    public void setFormDip(double formDip) {
        this.formDip = formDip;
    }

    public double getMagDev() {
        return magDev;
    }

    public void setMagDev(double magDev) {
        this.magDev = magDev;
    }

    public double getFirstGcStep() {
        for (Datum d: data) {
            if (d.isOnCircle()) return d.getTreatmentStep();
        }
        return -1;
    }

    public double getLastGcStep() {
        double result = -1;
        for (Datum d : data) {
            if (d.isOnCircle()) result = d.getTreatmentStep();
        }
        return result;
    }

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

    public GreatCircle getGreatCircle() {
        return greatCircle;
    }
}
