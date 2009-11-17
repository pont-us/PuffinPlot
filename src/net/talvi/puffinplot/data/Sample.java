package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.talvi.puffinplot.PuffinApp;

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
        if (points.size() < 3) return;
        greatCircle = new Eigens(points, true).vectors.get(2);
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
        return len - n.charAt(len-2) == '.' ? len-5 : len-4;
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

    /**
     * @return the site
     */
    public Site getSite() {
        return site;
    }

    /**
     * @param site the site to set
     */
    public void setSite(Site site) {
        this.site = site;
    }
}
