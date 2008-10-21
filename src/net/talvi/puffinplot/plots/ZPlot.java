package net.talvi.puffinplot.plots;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.atan;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JLabel;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumComparator;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;

public class ZPlot extends Plot {

    private static final long serialVersionUID = 1L;

    static int margin = 50;
    static double majorTickLen = 0.05;
    private PlotParams params;
    private GraphDisplay parent;
    private JLabel summaryLine;
    private JLabel pcaLine;
    
    public ZPlot(PlotParams params, GraphDisplay parent) {
        this.params = params;
        this.parent = parent;
        // withLines = true;
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
    
    private static void drawLine(Graphics2D g, double x, double y,
            double angleRad, ZplotAxes axes, Color colour) {
        Rectangle oldClip = g.getClipBounds();
        g.setClip(axes.getBounds());
        double dx = 800*sin(angleRad);
        double dy = 800*cos(angleRad);
        g.setColor(colour);
        g.draw(new Line2D.Double(x-dx, y+dy, x+dx, y-dy));
        g.setClip(oldClip);
    }
    
    public void paint(Graphics2D g, int xOffs, int yOffs, int xSize, int ySize) {

        Sample sample = params.getSample();
        if (sample==null) return;
        
        List<Datum> data = sample.getData();
        if (data.size() == 0) return;
        
        Correction correction = params.getCorrection();
        MeasurementAxis vVs = params.getAxis();
        
        Rectangle2D extent1 = extent(data, correction, MeasurementAxis.Y, MeasurementAxis.X);
        Rectangle2D extent2 = extent(data, correction, vVs, MeasurementAxis.MINUSZ);

        ZplotAxes axes = new ZplotAxes(extent1.createUnion(extent2),
                new Rectangle2D.Double(xOffs, yOffs, xSize, ySize),
                        vVs);
        
        axes.draw(g);
        
        boolean first = true;
        for (Datum d: data) {
            double x = d.getPoint(correction).y * axes.getScale();
            double y = - d.getPoint(correction).x * axes.getScale();
            parent.addPoint(d, new Point2D.Double(axes.getXOffset() + x, axes.getYOffset() + y)
                    , true, first, !first);
            first = false;
        }
        breakLine();
        
        first = true;
        for (Datum d: data) {
            double x = d.getPoint(correction).getComponent(vVs) * axes.getScale();
            double y = - d.getPoint(correction).getComponent(MeasurementAxis.MINUSZ) * axes.getScale();
            parent.addPoint(d, new Point2D.Double(axes.getXOffset() + x, axes.getYOffset() + y),
                    false, first, !first);
            first = false;
        }
        drawPoints(g);
        
        final PcaValues pca = sample.getPca();
        if (pca != null) {
            double x1 = pca.getOrigin().y * axes.getScale();
            double y1 = - pca.getOrigin().x * axes.getScale();
            drawLine(g, axes.getXOffset() + x1, axes.getYOffset() + y1,pca.getDecRadians(), axes, Color.BLUE);
            
            double x2 = pca.getOrigin().getComponent(vVs) * axes.getScale();
            double y2 = - pca.getOrigin().getComponent(MeasurementAxis.MINUSZ) * axes.getScale();
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
            drawLine(g, axes.getXOffset() + x2, axes.getYOffset() + y2, Math.PI/2 + incCorr, axes, Color.BLUE);
            g.setColor(Color.BLACK);
            g.drawString(String.format("D:%.1f I:%.1f MAD1:%.1f MAD3:%.1f",
                    pca.getDecDegrees(), pca.getIncDegrees(), pca.getMad1(), pca.getMad3()),
                    xOffs, yOffs-30);
        }
        
        String line = null;
        switch (params.getMeasType()) {
        case DISCRETE: line = "Sample: " + sample.getName();
            break;
        case CONTINUOUS: line = "Depth: " + sample.getDepth();
            break;
        }
        line = line + ", Correction: " + params.getCorrection();
        g.drawString(line, 20, 20);
    }
}
