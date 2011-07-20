package net.talvi.puffinplot.plots;

import net.talvi.puffinplot.data.FisherValues;
import java.util.ArrayList;
import net.talvi.puffinplot.data.PcaValues;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.awt.geom.Point2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import net.talvi.puffinplot.data.GreatCircle;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.data.PcaAnnotated;
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

    private void drawGreatCircles(Graphics2D g) {
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
        double maxRadius = 0;
        for (GreatCircle circle: circles.getCircles()) {
            final Vec3 pole = circle.getPole();
            g.setColor(Color.BLACK);
            //drawGreatCircle(g, xo, yo, radius, pole);
            Vec3 segmentStart = pole.nearestOnCircle(circle.getPoints().get(0));
            Vec3 segmentEnd = pole.nearestOnCircle(endpoint);
            drawGreatCircleSegment(g, xo, yo, radius, segmentStart,
                    segmentEnd,circle.lastPoint());
            boolean first = true;
            //g.setColor(Color.BLUE);
            for (Vec3 p: circle.getPoints()) {
                addPoint(null, project(p, xo, yo, radius), p.z>=0, false, false);
                drawGreatCircleSegment(g, xo, yo, radius, p,
                        pole.nearestOnCircle(p));
                first = false;
            }
            //g.setColor(Color.RED);
            final Vec3 nearestPoint = pole.nearestOnCircle(endpoint);
            double thisRadius = Math.abs(endpoint.angleTo(nearestPoint));
            if (thisRadius > maxRadius) maxRadius = thisRadius;
            drawGreatCircleSegment(g, xo, yo, radius, endpoint, nearestPoint);
        }
        // addPoint(null, project(circles.getDirection(), xo, yo, radius), true, true, false);
        Point2D meanPos = project(circles.getDirection(), xo, yo, radius);
        PlotPoint meanPoint = new CirclePoint(this, null, meanPos, PLOT_POINT_SIZE*1.5);
        meanPoint.draw(g);
        //g.setColor(Color.GREEN);
        drawLineSegments(g, xo, yo, radius,
                circles.getDirection().makeSmallCircle(Math.toDegrees(maxRadius)));
        drawLineSegments(g, xo, yo, radius,
                circles.getDirection().makeSmallCircle(circles.getA95()));
        drawPoints(g);
        List<String> ss = circles.toStrings();
        writeString(g, ss.get(3)+"/"+ss.get(4), xo-radius, yo-radius);
    }

    private void drawPcas(Graphics2D g) {
        clearPoints();
        final Site site = params.getSample().getSite();
        List<Sample> samples = site.getSamples();
        if (samples==null || samples.isEmpty()) {
            return;
        }
        final Rectangle2D dims = getDimensions();
        final int radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        final int xo = (int) dims.getCenterX();
        final int yo = (int) dims.getCenterY();
        drawAxes(g, xo, yo, radius);
        List<Vec3> pcaDirs = new ArrayList<Vec3>(samples.size());
        for (Sample s: samples) {
            PcaValues pcaValues = s.getPcaValues();
            if (pcaValues != null) {
                Vec3 v = pcaValues.getDirection();
                pcaDirs.add(s.getPcaValues().getDirection());
                addPoint(null, project(v, xo, yo, radius), v.z>0, false, false);
            }
        }
        FisherValues fisherMean = FisherValues.calculate(pcaDirs);
        drawLineSegments(g, xo, yo, radius,
                fisherMean.getMeanDirection().makeSmallCircle(fisherMean.getA95()));
        final Vec3 meanDir = fisherMean.getMeanDirection();
        PlotPoint meanPoint =
                new CirclePoint(this, null, project(meanDir, xo, yo, radius),
                PLOT_POINT_SIZE, meanDir.z>0);
        meanPoint.draw(g);
        drawPoints(g);
    }

    @Override
    public void draw(Graphics2D g) {
        final Site site = params.getSample().getSite();
        if (site == null) {
            writeString(g, "No sites defined.", 100, 100);
            return;
        }
        GreatCircles circles = site.getGreatCircles();
        if (circles != null) {
            drawGreatCircles(g);
        } else {
            drawPcas(g);
        }
    }
}
