package net.talvi.puffinplot.plots;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.FisherValues;
import net.talvi.puffinplot.data.Point;
import net.talvi.puffinplot.data.Sample;

public class EqAreaPlot extends Plot {

    private static final int decTickStep = 10;
    private static final float decTickLength = 0.03f; // as fraction of radius
    private static final int incTickNum = 9;
    private static final float incTickLength = decTickLength;

    public EqAreaPlot(GraphDisplay parent, PlotParams params, Rectangle2D dimensions) {
        super(parent, params, dimensions);
    }

    public void draw(Graphics2D g) {
              
        int minDim = (int) Math.min(getDimensions().getWidth(), getDimensions().getHeight());
        int xo = (int) getDimensions().getCenterX();
        int yo = (int) getDimensions().getCenterY();
        
        clearPoints();
        Sample sample = params.getSample();
        if (sample == null) return;
        int radius = minDim / 2;
        
        g.setColor(Color.BLACK);

        g.drawArc(xo-radius, yo-radius, radius*2, radius*2, 0, 360);
        for (int theta=0; theta<360; theta += decTickStep) {
            double x = (Math.cos(Math.toRadians(theta)) * radius);
            double y = (Math.sin(Math.toRadians(theta)) * radius);
            g.drawLine((int) (xo + x), (int) (yo + y),
                        (int) (xo + x * (1f - decTickLength)),
                        (int) (yo + y * (1f - decTickLength)));
        }
        
        int l = (int) (radius * incTickLength / 2);
        for (int i=0; i<incTickNum; i++) {
            int x = (int) ((i * radius) / incTickNum);
            g.drawLine(xo + x, yo - l, xo + x, yo + l);
            // draws inclination ticks
        }
        g.drawLine(xo-l, yo, xo+l, yo);
        // a line crossing the last inclination tick to make
        // a cross at the centre of the plot
       
        List<Datum> data = sample.getData();
        Correction correction = params.getCorrection();
        boolean first = true;
        for (Datum d: data) {

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
            addPoint(d,
                    new Point2D.Double(xo + radius*(p.y)*L, yo + radius*(-p.x)*L), 
                    p.z>0, first, !first);
            first = false;
        }
        drawPoints(g);
        
        FisherValues fish = sample.getFisher();
        if (fish != null) {
            float xOrig = (float) getDimensions().getMinX();
            float yOrig = (float) getDimensions().getMinY();
            g.drawString(
                    String.format("D:%.1f I:%.1f",
                    fish.getMeanDirection().decDegrees(),
                    fish.getMeanDirection().incDegrees()), xOrig, yOrig);
            g.drawString(String.format("α95:%.1f",fish.getA95()), xOrig, yOrig + 15);
            g.drawString(String.format("k:%.1f",fish.getK()), xOrig, yOrig + 30);
        }
    }
}
