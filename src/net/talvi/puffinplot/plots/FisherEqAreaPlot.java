package net.talvi.puffinplot.plots;

import static java.lang.Math.min;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Vec3;

public class FisherEqAreaPlot extends EqAreaPlot {

    public FisherEqAreaPlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }
    
    @Override
    public void draw(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        
        clearPoints();
        drawAxes(g, xo, yo, radius);
        List<FisherValues> fishers = PuffinApp.getApp().getSuite().getFishers();
        if (fishers==null) return;
        
        boolean first = true;
        for (FisherValues fisher: fishers) {
            final Vec3 v = fisher.getMeanDirection();
            addPoint(null, project(v, xo, yo, radius), v.z>0, first, !first);
            first = false;
        }
        drawPoints(g);
    }

}
