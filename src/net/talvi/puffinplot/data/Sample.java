package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.talvi.puffinplot.PuffinApp;

public class Sample {
    
    private final List<Datum> data;
    private final double depth;
    private final String name;
    private PcaValues pca = null;
    private FisherValues fisher = null;

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
    
    public void selectAll() {
        for (Datum d : data) d.selected = true;
    }
    
    public void selectNone() {
        for (Datum d : data) d.selected = false;
    }
    
    public List<Datum> getData() {
        return data;
    }
    
    public int getNumData() {
        return data.size();
    }

    public Datum getDatum(int i) {
        return data.get(i);
    }
    
    public void addDatum(Datum d) {
        data.add(d);
    }

    public double getDepth() {
        return depth;
    }

    public PcaValues getPca() {
        return pca;
    }
    
    public List<Point> getSelectedPoints() {
        LinkedList<Point> points = new LinkedList<Point>();
        for (Datum d: data)
            if (d.selected)
                points.add(d.getPoint(PuffinApp.getApp().getCorrection()));
        return points;
    }
    
    public void doFisher() {
        List<Point> points = getSelectedPoints();
        if (points.size() < 2)
            PuffinApp.errorDialog("Fisher error", "You must select at least two points in order " +
                    "to calculate Fisher statistics.");
        else
            fisher = FisherValues.calculate(points);
    }

    public void doPca() {
        List<Point> points = getSelectedPoints();
        if (points.size() < 2)
            PuffinApp.errorDialog("PCA error", "You must select at least two points in order "+
                    "to perform PCA.");
        else
            pca = PcaValues.calculate(points, 
                    PuffinApp.getApp().getPrefs().isPcaAnchored()
                    ? Point.ORIGIN
                    : Point.centreOfMass(points));
    }

    public String getName() {
        return name;
    }

    public FisherValues getFisher() {
        return fisher;
    }
}
