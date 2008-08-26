package net.talvi.puffinplot.plots;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Point;
import net.talvi.puffinplot.data.Sample;

public class EqAreaPlot extends Plot {

    private static final long serialVersionUID = 1L;

    private static int decTickStep = 10;
    private static float decTickLength = 0.03f; // as fraction of radius
    private static int incTickNum = 9;
    private static float incTickLength = decTickLength;
    private static int margin = 10;

    public EqAreaPlot(PlotParams params) {
        super(params);
        withLines = true;
    }
    
    private int diameter() {
        // return Math.min(getSize().width, getSize().height) - margin*2;
        return Math.min(getVirtualWidth(), getVirtualHeight()) - margin*2;
    }
    
    void paintAxes(Graphics2D g) {
        g.setColor(Color.BLACK);
        int radius = diameter() / 2;
        g.drawArc(-radius, -radius, diameter(), diameter(), 0, 360);
        for (int theta=0; theta<360; theta += decTickStep) {
            double x = (Math.cos(Math.toRadians(theta)) * radius);
            double y = (Math.sin(Math.toRadians(theta)) * radius);
            g.drawLine((int) x, (int) y,
                        (int) (x * (1f - decTickLength)),
                        (int) (y * (1f - decTickLength)));
        }
        int l = (int) (radius * incTickLength / 2);
        for (int i=0; i<incTickNum; i++) {
            int x = (int) ((i * radius) / incTickNum);
            g.drawLine(x, -l, x, l);
        }
        g.drawLine(-l, 0, l, 0);
    }
    
    @Override
    protected int getVirtualWidth() { return 1000; }
    
    @Override
    protected int getVirtualHeight() { return 1000; }
    
    @Override
    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        g.setRenderingHints(renderingHints);
        
        double minDim = Math.min(getWidth(), getHeight());
        
        transform = AffineTransform.getScaleInstance
                (minDim / getVirtualWidth(),
                (minDim / getVirtualHeight()));
        transform.concatenate(AffineTransform.getTranslateInstance(getVirtualWidth()/2,
                getVirtualHeight()/2));
        g.transform(transform);
        
        clearPoints();
        Sample sample = params.getSample();
        if (sample==null) return; 
        
        paintAxes(g);
        List<Datum> data = sample.getData();
        Correction correction = params.getCorrection();
        boolean first = true;
        for (Datum d: data) {
            int radius = diameter() / 2;
            Point p = d.getPoint(correction).normalize();

            double h2 = p.x*p.x + p.y*p.y;
            double L = (h2 > 0) ? sqrt(1-abs(p.z)) / sqrt(h2) : 0;
            
            /* Need to convert from declination (running clockwise from
             * Y axis) to plot co-ordinates (running anticlockwise from X axis).
             * First we flip the x-axis so that we're going anticlockwise
             * (let x' = x, y' = -y). Then we perform a 90˚ anticlockwise rotation
             * (let x'' = -y' = y, y'' = x' = x). Finally, we flip the y axis
             * to take account of AWT Y-coordinates running top-to-bottom rather
             * than bottom-to-top (let x''' = x'' = y, y''' = -y'' = -x).
             */
            addPoint(d, radius*(p.y)*L, radius*(-p.x)*L, p.z>0, first);
            first = false;
        }
        drawPoints(g);
    }
}
