package net.talvi.puffinplot.data;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.talvi.puffinplot.PuffinApp;
import static java.lang.Math.toRadians;

public class Sample {
    
    private final List<Datum> data;
    private Site site;
    private final String nameOrDepth;
    private FisherValues fisher = null;
    private boolean isEmptySlot = false;
    public Vec3 greatCircle;
    private PcaAnnotated pca;
    private MDF mdf;
    private boolean hasMsData = false;
    private Matrix ams;
    public List<Vec3> amsAxes;

    public Sample(String name) {
        this.nameOrDepth = name;
        this.data = new ArrayList<Datum>();
    }
    
    public void clear() {
        pca = null;
        fisher = null;
        mdf = null;
        greatCircle = null;
        selectNone();
        for (Datum d: getData()) d.setOnCircle(false);
    }

    public void calculateMdf(Correction correction) {
        mdf = MDF.calculate(getVisibleData(), correction);
    }

    public MDF getMDF() {
        return mdf;
    }

    public double getNRM(Correction correction) {
        return data.get(0).getIntensity(correction);
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

    public List<Datum> getAllData() {
        return data;
    }

    public int getNumData() {
        return getData().size();
    }

    public Datum getDatum(int i) {
        return getData().get(i);
    }
    
    public void addDatum(Datum d) {
        data.add(d);
        if (d.hasMagSus()) hasMsData = true;
        d.setSample(this);
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

    public List<Vec3> getSelectedPoints() {
        LinkedList<Vec3> points = new LinkedList<Vec3>();
        PuffinApp app = PuffinApp.getInstance();
        for (Datum d: getData())
            if (d.isSelected())
                points.add(d.getMoment(app.getCorrection()));
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
    
    public void calculateFisher() {
        List<Vec3> points = getSelectedPoints();
        if (points.size() > 1)
            fisher = FisherValues.calculate(points);
    }

    public void doPca(boolean anchored) {
        for (Datum d: getData()) d.setPcaAnchored(anchored);
        pca = PcaAnnotated.calculate(this);
    }

    public void useSelectionForCircleFit() {
        for (Datum d : getData()) d.setOnCircle(d.isSelected());
    }

    public void useSelectionForPca() {
        for (Datum d : getData()) d.setInPca(d.isSelected());
    }

    public List<Vec3> getCirclePoints() {
        List<Vec3> result = new ArrayList<Vec3>(getData().size());
        PuffinApp app = PuffinApp.getInstance();
        for (Datum d: getData()) {
            if (d.isOnCircle()) result.add(d.getMoment(app.getCorrection()));
        }
        return result;
    }

    public void fitGreatCircle() {
        List<Vec3> points = getCirclePoints();
        if (points.size() < 2) return;
        greatCircle = Eigens.fromVectors(points, true).vectors.get(2);
    }

    public void doPca() {
        doPca(getData().get(0).isPcaAnchored());
    }

    public MeasType getMeasType() {
        for (Datum d: getData())
            if (d.getMeasType().isActualMeasurement()) return d.getMeasType();
        return MeasType.UNKNOWN;
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

    public FisherValues getFisher() {
        return fisher;
    }
    
    public boolean isPcaAnchored() {
        return getData().get(0).isPcaAnchored();
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

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public void setAmsFromTensor(double k11, double k22, double k33,
            double k12, double k23, double k13) {
        double[] elts = {k11, k12, k13, k12, k22, k23, k13, k23, k33};
        ams = new Matrix(elts, 3);
        Eigens amsEigens = new Eigens(ams);
        // For the present, we just keep the directionsq
        amsAxes = amsEigens.vectors;
    }

    public void setAmsDirections(double i1, double d1, double i2, double d2, double i3, double d3) {
        amsAxes = new ArrayList<Vec3>(3);
        amsAxes.add(Vec3.fromPolarDegrees(1, i1, d1));
        amsAxes.add(Vec3.fromPolarDegrees(1, i2, d2));
        amsAxes.add(Vec3.fromPolarDegrees(1, i3, d3));
    }

    public Vec3 getAmsAxis(int axis, Correction c) {
        /* This is a horrendous fudge to deal with AGICO's... interesting
         * policies on data retention. AMS tensors from SAFYR are
         * sample corrected, so we just apply a magnetic declination
         * correction here.
         *
         * Addendum: we're now using incs and decs from ASC file without
         * sample correction, so I've re-added it here.
         */
        // Vec3 result = amsEigens.vectors.get(axis);
        Vec3 result = amsAxes.get(axis);
        Datum d = getData().get(0);
        double sampAz = d.getSampAz();
        double sampDip = d.getSampDip();
        double formAz = d.getFormAz();
        double formDip = d.getFormDip();
        double magDev = d.getMagDev();
       result = result.correctSample(toRadians(sampAz + magDev),
               toRadians(sampDip));
       result = result.correctForm(toRadians(formAz + magDev),
               toRadians(formDip));
        return result;
    }

    public Matrix getAms() {
        return ams;
    }
}
