package net.talvi.puffinplot.plots;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.atan;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumComparator;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;

public class ZPlot extends Plot {

    public ZPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }

    private static Rectangle2D extent(List<Datum> sample, Correction corr,
            MeasurementAxis axis1, MeasurementAxis axis2) {
        Comparator<Datum> xComp = new DatumComparator(axis1, corr);
        Comparator<Datum> yComp = new DatumComparator(axis2, corr);
        double xMin = Collections.min(sample, xComp).getPoint(corr).getComponent(axis1);
        double xMax = Collections.max(sample, xComp).getPoint(corr).getComponent(axis1);
        double yMin = Collections.min(sample, yComp).getPoint(corr).getComponent(axis2);
        double yMax = Collections.max(sample, yComp).getPoint(corr).getComponent(axis2);
        return new Rectangle2D.Double(xMin, yMin, xMax-xMin, yMax-yMin);
    }
    
    private void drawLine(Graphics2D g, double x, double y,
            double angleRad, ZplotAxes axes, Color colour) {
        Rectangle oldClip = g.getClipBounds();
        g.setClip(axes.getBounds());
        double dx = 800*sin(angleRad);
        double dy = 800*cos(angleRad);
        g.setStroke(getStroke());
        g.setColor(colour);
        g.draw(new Line2D.Double(x-dx, y+dy, x+dx, y-dy));
        g.setClip(oldClip);
    }
    
    @Override
    public String getName() {
        return "zplot";
    }

    @Override
    public String getNiceName() {
        return "Zplot";
    }

    public void draw(Graphics2D g) {
        Sample sample = params.getSample();
        if (sample==null) return;
        List<Datum> data = sample.getData();
        if (data.size() == 0) return;
        
        clearPoints();
        Correction correction = params.getCorrection();
        MeasurementAxis vVs = params.getAxis();
        
        Rectangle2D extent1 =
                extent(data, correction, MeasurementAxis.Y, MeasurementAxis.X);
        Rectangle2D extent2 =
                extent(data, correction, vVs, MeasurementAxis.MINUSZ);

        Rectangle2D dim = cropRectangle(getDimensions(), 250, 250, 200, 200);

        ZplotAxes axes = new ZplotAxes(extent1.createUnion(extent2),
                dim, vVs,this);
        
        g.setColor(Color.BLACK);
        g.setStroke(getStroke());
        axes.draw(g);

        final double scale = axes.getScale();
        final double xOffset = axes.getXOffset();
        final double yOffset = axes.getYOffset();
        
        boolean first = true;
        for (Datum d: data) {
            Vec3 p = d.getPoint(correction);
            // Plot the point in the horizontal plane
            double x1 = xOffset + p.y * scale;
            double y1 = yOffset - p.x * scale;
            addPoint(d, new Point2D.Double(x1, y1), true, first, !first);
            first = false;
        }
        first = true;
        for (Datum d: data) {
            Vec3 p = d.getPoint(correction);
            // Now plot the point in the vertical plane
            double x2 = xOffset + p.getComponent(vVs) * scale;
            double y2 = yOffset - p.getComponent(MeasurementAxis.MINUSZ) * scale;
            addPoint(d, new Point2D.Double(x2, y2), false, first, !first);
            first = false;
        }
        
        final PcaValues pca = sample.getPca();
        if (pca != null) {
            double x1 = pca.getOrigin().y * scale;
            double y1 = - pca.getOrigin().x * scale;
            drawLine(g, xOffset + x1, yOffset + y1,pca.getDecRadians(), axes, Color.BLUE);
            
            double x2 = pca.getOrigin().getComponent(vVs) * scale;
            double y2 = - pca.getOrigin().getComponent(MeasurementAxis.MINUSZ) * scale;
            double incCorr = 0;
            switch (vVs) {
                // We don't necessarily want the actual line of inclination; we
                // want the projection of that line onto the appropriate plane.
                case X:
                    incCorr = atan(sin(pca.getIncRadians()) / (cos(pca.getIncRadians()) * cos(pca.getDecRadians())));
                    break;
                case Y:
                    incCorr = atan(sin(pca.getIncRadians()) / (cos(pca.getIncRadians()) * sin(pca.getDecRadians())));
                    break;
                case H: incCorr = pca.getIncRadians(); break;
            }
            drawLine(g, xOffset + x2, yOffset + y2, Math.PI/2 + incCorr, axes, Color.BLUE);
        }
        drawPoints(g);
    }
}
