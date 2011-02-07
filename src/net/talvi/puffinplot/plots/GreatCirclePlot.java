package net.talvi.puffinplot.plots;

import net.talvi.puffinplot.data.Site;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import static java.lang.Math.min;

public class GreatCirclePlot extends EqAreaPlot {
    public GreatCirclePlot(GraphDisplay parent, PlotParams params,
            Rectangle2D dimensions) {
        super(parent, params, null);
        this.dimensions = dimensions;
    }
    
    @Override
    public String getName() {
        return "greatcircles";
    }

    @Override
    public void draw(Graphics2D g) {
        Site site = params.getSample().getSite();
        if (site == null) {
            writeString(g, "No sites defined.", 100, 100);
            return;
        }
        GreatCircles circles = site.getGreatCircles();
        if (circles == null) return;
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        clearPoints();
        drawAxes(g, xo, yo, radius);
        for (GreatCircle circle: circles.getCircles()) {
            final Vec3 pole = circle.getPole();
            g.setColor(Color.BLACK);
            drawGreatCircle(g, xo, yo, radius, pole);
            boolean first = true;
            for (Vec3 p: circle.getPoints()) {
                //addPoint(null, project(p, xo, yo, radius), p.z>=0, false, !first);
                first = false;
            }
            g.setColor(Color.RED);
            drawGreatCircleSegment(g, xo, yo, radius, circles.getDirection(),
                    pole.nearestOnCircle(circles.getDirection()));
        }
        addPoint(null, project(circles.getDirection(), xo, yo, radius), true, true, false);
        drawPoints(g);
    }
}
