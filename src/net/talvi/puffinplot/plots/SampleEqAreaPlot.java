package net.talvi.puffinplot.plots;

import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;

public class SampleEqAreaPlot extends EqAreaPlot {

    public SampleEqAreaPlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }
    
    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        
        clearPoints();
        final Sample sample = params.getSample();
        if (sample == null) return;
        
        drawAxes(g, xo, yo, radius);
        boolean first = true;
        for (Datum d: sample.getData()) {
            final Vec3 p = d.getPoint(params.getCorrection()).normalize();
            addPoint(d, project(p, xo, yo, radius), p.z>0, first, !first);
            first = false;
        }
        drawPoints(g);
    }
}