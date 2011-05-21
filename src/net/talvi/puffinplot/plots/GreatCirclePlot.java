package net.talvi.puffinplot.plots;

import java.awt.geom.Point2D;
import java.util.prefs.Preferences;
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
        super(parent, params, null, true);
        this.dimensions = dimensions;
    }

    public GreatCirclePlot(GraphDisplay parent, PlotParams params,
            Preferences prefs) {
        super(parent, params, prefs);
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
        final Vec3 endpoint = circles.getDirection();
        for (GreatCircle circle: circles.getCircles()) {
            final Vec3 pole = circle.getPole();
            g.setColor(Color.BLACK);
            //drawGreatCircle(g, xo, yo, radius, pole);
            Vec3 segmentStart = pole.nearestOnCircle(circle.getPoints().get(0));
            Vec3 segmentEnd = pole.nearestOnCircle(endpoint);
            drawGreatCircleSegment(g, xo, yo, radius, segmentStart,
                    segmentEnd,circle.lastPoint());
            boolean first = true;
            g.setColor(Color.BLUE);
            for (Vec3 p: circle.getPoints()) {
                addPoint(null, project(p, xo, yo, radius), p.z>=0, false, false);
                drawGreatCircleSegment(g, xo, yo, radius, p,
                        pole.nearestOnCircle(p));
                first = false;
            }
            g.setColor(Color.RED);
            drawGreatCircleSegment(g, xo, yo, radius, endpoint,
                    pole.nearestOnCircle(endpoint));
        }
        // addPoint(null, project(circles.getDirection(), xo, yo, radius), true, true, false);
        Point2D meanPos = project(circles.getDirection(), xo, yo, radius);
        PlotPoint meanPoint = new CirclePoint(this, null, meanPos, PLOT_POINT_SIZE*1.5);
        meanPoint.draw(g);
        drawPoints(g);
    }
}
