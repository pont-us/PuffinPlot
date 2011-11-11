package net.talvi.puffinplot.plots;

import net.talvi.puffinplot.data.FisherValues;
import java.util.ArrayList;
import net.talvi.puffinplot.data.PcaValues;
import java.util.List;
import java.awt.geom.Point2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Sample;
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
            Rectangle2D dimensions, Preferences prefs) {
        super(parent, params, prefs, true);
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
    public String getNiceName() {
        return "Great circles";
    }

    private void drawGreatCircles(final Graphics2D g, final Site site,
            int xo, int yo, int radius) {
        GreatCircles circles = site.getGreatCircles();
        if (circles == null) return;
        final Vec3 endpoint = circles.getDirection();
        double maxRadius = 0;
        for (GreatCircle circle: circles.getCircles()) {
            final Vec3 pole = circle.getPole();
            g.setColor(Color.BLACK);
            final Vec3 segmentStart = pole.nearestOnCircle(circle.getPoints().get(0));
            final Vec3 segmentEnd = pole.nearestOnCircle(endpoint);
            drawGreatCircleSegment(segmentStart,
                    segmentEnd,circle.lastPoint());
            for (Vec3 p: circle.getPoints()) {
                addPoint(null, project(p), p.z>=0, false, false);
                drawGreatCircleSegment(p, pole.nearestOnCircle(p));
            }
            final Vec3 nearestPoint = pole.nearestOnCircle(endpoint);
            final double thisRadius = Math.abs(endpoint.angleTo(nearestPoint));
            if (thisRadius > maxRadius) maxRadius = thisRadius;
            drawGreatCircleSegment(endpoint, nearestPoint);
        }
        final Vec3 meanDir = circles.getDirection();
        PlotPoint meanPoint =
                ShapePoint.build(this, project(meanDir)).
                circle().scale(1.5).filled(meanDir.z>0).build();
        meanPoint.draw(g);
        drawLineSegments(circles.getDirection().makeSmallCircle(Math.toDegrees(maxRadius)));
        drawLineSegments(circles.getDirection().makeSmallCircle(circles.getA95()));
        List<String> ss = circles.toStrings();
        writeString(g, ss.get(3)+"/"+ss.get(4), xo-radius, yo-radius);
    }

    private void drawPcas(final Graphics2D g, final Site site,
            int xo, int yo, int radius) {
        List<Sample> samples = site.getSamples();
        if (samples==null || samples.isEmpty()) {
            return;
        }
        List<Vec3> pcaDirs = new ArrayList<Vec3>(samples.size());
        for (Sample s: samples) {
            PcaValues pcaValues = s.getPcaValues();
            if (pcaValues != null) {
                Vec3 v = pcaValues.getDirection();
                pcaDirs.add(s.getPcaValues().getDirection());
                addPoint(null, project(v), v.z>0, false, false);
            }
        }
        FisherValues fisherMean = FisherValues.calculate(pcaDirs);
        drawLineSegments(fisherMean.getMeanDirection().makeSmallCircle(fisherMean.getA95()));
        final Vec3 meanDir = fisherMean.getMeanDirection();
        PlotPoint meanPoint =
                ShapePoint.build(this, project(meanDir)).
                circle().scale(1.5).filled(meanDir.z>0).build();
        meanPoint.draw(g);
    }

    @Override
    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample==null) return;
        final Site site = sample.getSite();
        if (site == null) {
            writeString(g, "No sites defined.", 100, 100);
            return;
        }
        updatePlotDimensions(g);
        drawAxes();
        clearPoints();
        GreatCircles circles = site.getGreatCircles();
        if (circles != null) {
            drawGreatCircles(g, site, xo, yo, radius);
        } else {
            drawPcas(g, site, xo, yo, radius);
        }
        drawPoints(g);
    }
}
