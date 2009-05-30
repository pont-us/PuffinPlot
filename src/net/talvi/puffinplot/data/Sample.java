package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Suite;

public class Sample {
    
    private final List<Datum> data;
    private final double depth;
    private final String name;
    private PcaValues pca = null;
    private FisherValues fisher = null;
    private boolean emptySlot = false;
    private static final int SAMPLE_ID_LENGTH = 4;

    public Sample(double depth) {
        this.depth = depth;
        this.name = null;
        this.data = new ArrayList<Datum>();
    }

    public Sample(String name) {
        this.name = name;
        this.depth = Double.NaN;
        this.data = new ArrayList<Datum>();
    }
    
    public void clear() {
        pca = null;
        fisher = null;
        selectNone();
    }
    
    /*
     * Rotates all data 180 degrees about the X axis.
     */
    public void flip() {
        for (Datum d: data) d.rotX180();
    }

    public void hideSelectedPoints() {
        for (Datum d: data) {
            if (d.isSelected()) {
                d.setSelected(false);
                d.setHidden(true);
            }
        }
    }
    
    public void selectAll() {
        for (Datum d : data) d.setSelected(true);
    }
    
    public void selectNone() {
        for (Datum d : data) d.setSelected(false);
    }

    public List<Datum> getVisibleData() {
        List<Datum> visibleData = new ArrayList<Datum>(data.size());
        for (Datum d: data) if (!d.isHidden()) visibleData.add(d);
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
        data.add(d);
        d.setSample(this);
    }

    public double getDepth() {
        return depth;
    }

    public PcaValues getPca() {
        return pca;
    }
    
    public List<Vec3> getSelectedPoints() {
        LinkedList<Vec3> points = new LinkedList<Vec3>();
        PuffinApp app = PuffinApp.getInstance();
        for (Datum d: data)
            if (d.isSelected())
                points.add(d.getPoint(app.getCorrection(),
                        app.isEmptyCorrectionActive()));
        return points;
    }
    
    public void doFisher() {
        List<Vec3> points = getSelectedPoints();
        if (points.size() > 1)
            fisher = FisherValues.calculate(points);
    }

    public void doPca(boolean anchored) {
        for (Datum d: data) d.setPcaAnchored(anchored);
        List<Vec3> points = getSelectedPoints();
        if (points.size() > 1)
            pca = PcaValues.calculate(points, 
                    anchored
                    ? Vec3.ORIGIN
                    : Vec3.centreOfMass(points));
    }

    public void doPca() {
        doPca(data.get(0).isPcaAnchored());
    }
    
    public String getName() {
        return name;
    }

    private int getSiteSplit() {
        return getName().length() - SAMPLE_ID_LENGTH;
    }

    public String getSiteId() {
        return getName().substring(1, getSiteSplit());
    }

    public String getSampleId() {
        return getName().substring(getSiteSplit());
    }

    public FisherValues getFisher() {
        return fisher;
    }
    
    public boolean isPcaAnchored() {
        return data.get(0).isPcaAnchored();
    }
    
    public void setPcaAnchored(boolean pcaAnchored) {
        for (Datum d: data) d.setPcaAnchored(pcaAnchored);
    }

    public boolean isEmptySlot() {
        return emptySlot;
    }

    public void setEmptySlot(boolean isEmptySlot) {
        this.emptySlot = isEmptySlot;
    }

    public void unhideAllPoints() {
        for (Datum d: data) d.setHidden(false);
    }
}
