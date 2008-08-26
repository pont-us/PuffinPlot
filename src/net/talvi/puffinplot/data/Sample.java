package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.talvi.puffinplot.PuffinApp;

public class Sample {
    
    private List<Datum> data;
    private double depth;
    private String name;
    private PcaValues pca = null;

    public Sample(double depth) {
        this.depth = depth;
        this.data = new ArrayList<Datum>();
    }

    public Sample(String name) {
        this.name = name;
        this.data = new ArrayList<Datum>();
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

    public void doPca() {
        LinkedList<Point> points = new LinkedList<Point>();
        for (Datum d: data) if (d.selected) points.add(d.getPoint(PuffinApp.app.currentCorrection()));
        if (points.size() < 2)
            PuffinApp.app.errorDialog("PCA error", "You must select at least two points in order "+
                    "to perform PCA.");
        else
            pca = PcaValues.calculate(points, 
                    PuffinApp.app.getPrefs().isPcaAnchored()
                    ? Point.ORIGIN
                    : Point.centreOfMass(points));
    }

    public String getName() {
        return name;
    }
}
