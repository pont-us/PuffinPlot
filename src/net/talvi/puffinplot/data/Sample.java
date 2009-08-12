package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.talvi.puffinplot.PuffinApp;

public class Sample {
    
    private final List<Datum> data;
    private final double depth;
    private final String name;
    private FisherValues fisher = null;
    private boolean emptySlot = false;
    private PcaAnnotated pcaAnnotated;
    private MDF mdf;
    private boolean hasMsData = false;

    private Sample(double depth, String name) {
        this.depth = depth;
        this.name = name;
        this.data = new ArrayList<Datum>();
    }

    public Sample(double depth) {
        this(depth, null);
    }

    public Sample(String name) {
        this(Double.NaN, name);
    }
    
    public void clear() {
        pcaAnnotated = null;
        fisher = null;
        mdf = null;
        selectNone();
    }

    public void calculateMdf(boolean useEmptyCorrection) {
        mdf = MDF.calculate(getVisibleData(), useEmptyCorrection);
    }

    public MDF getMidpoint() {
        return mdf;
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

    public double getDepth() {
        return depth;
    }

    public boolean hasMsData() {
        return hasMsData;
    }

    public PcaAnnotated getPca() {
        return pcaAnnotated;
    }

    public PcaValues getPcaValues() {
        return pcaAnnotated == null ? null : pcaAnnotated.getPcaValues();
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
                points.add(d.getPoint(app.getCorrection(),
                        app.isEmptyCorrectionActive()));
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
        pcaAnnotated = PcaAnnotated.calculate(this);
    }

    public void doPca() {
        doPca(getData().get(0).isPcaAnchored());
    }
    
    public String getName() {
        return name;
    }

    public MeasType getMeasType() {
        for (Datum d: getData())
            if (d.getMeasType().isActualMeasurement()) return d.getMeasType();
        return MeasType.UNKNOWN;
    }

    public String getNameOrDepth() {
        return getMeasType() == MeasType.CONTINUOUS ?
                    String.format("%f", getDepth()) :
                    getName();
    }

    private int getSiteSplit() {
        String n = getName();
        int len = n.length();
        return len - n.charAt(len-2) == '.' ? 4 : 3;
    }

    public String getSiteId() {
        return getName().substring(0, getSiteSplit());
    }

    public String getSampleId() {
        return getName().substring(getSiteSplit());
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

    public boolean isEmptySlot() {
        return emptySlot;
    }

    public void setEmptySlot(boolean isEmptySlot) {
        this.emptySlot = isEmptySlot;
    }

    public void unhideAllPoints() {
        for (Datum d: getData()) d.setHidden(false);
    }

    public void copySelectionFrom(Sample s) {
        List<Datum> otherData = s.getData();
        for (int i=0; i<getNumData(); i++)
            getData().get(i).setSelected(otherData.get(i).isSelected());
    }
}
