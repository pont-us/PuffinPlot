package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;

public class DemagPlot extends Plot {

    public DemagPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    public String getName() {
        return "demag";
    }

    @Override
    public String getNiceName() {
        return "Demag.";
    }

    public void draw(Graphics2D g) {
        clearPoints();
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getVisibleData();
        if (data.size() == 0) return;

        Rectangle2D dim = cropRectangle(getDimensions(), 270, 200, 50, 230);
        boolean emptyC = params.isEmptyCorrectionActive();

        g.setColor(Color.BLACK);
        double maxIntens = 0;
        double maxDemag = 0;
        for (Datum d: data) {
            if (d.getDemagLevel() > maxDemag) maxDemag = d.getDemagLevel();
            if (d.getIntensity(emptyC) > maxIntens) maxIntens = d.getIntensity(emptyC);
        }

        if (maxDemag == 0) maxDemag = 1;
        if (maxIntens == 0) maxIntens = 1;
        
        PlotAxis vAxis = new PlotAxis(maxIntens, PlotAxis.Direction.UP,
                PlotAxis.saneStepSize(maxIntens),
                "Intensity", null, this);
        PlotAxis hAxis = new PlotAxis(maxDemag, PlotAxis.Direction.RIGHT,
                PlotAxis.saneStepSize(maxDemag),
                sample.getDatum(sample.getNumData()-1).getTreatType().getAxisLabel(), null, this);
        
        double hScale = dim.getWidth() / hAxis.getLength();
        double vScale = dim.getHeight() / vAxis.getLength();
        
        vAxis.draw(g, vScale, (int)dim.getMinX(), (int)dim.getMaxY());
        hAxis.draw(g, hScale, (int)dim.getMinX(), (int)dim.getMaxY());
        
        boolean first = true;
        for (Datum d: data) {
            addPoint(d, new Point2D.Double(dim.getMinX() + d.getDemagLevel() * hScale,
                    dim.getMaxY() - d.getIntensity(emptyC) * vScale), true, false, !first);
            first = false;
        }
        drawPoints(g);
    }
}
