package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import static java.lang.Math.sqrt;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;
import static java.lang.Math.signum;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Vec3;

public abstract class EqAreaPlot extends Plot {
    private static final int decTickStep = 10;
    private static final int incTickNum = 9;
    private static BasicStroke stroke1 = new BasicStroke();
    private static BasicStroke stroke2 = new BasicStroke(1, 0, 0, 1, new float[]{2, 2}, 0);

    protected EqAreaPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
    }
    
    protected void drawAxes(Graphics2D g, int xo, int yo, int radius) {
        g.setColor(Color.BLACK);

        g.drawArc(xo - radius, yo - radius, radius * 2, radius * 2, 0, 360);
        for (int theta = 0; theta < 360; theta += decTickStep) {
            double x = cos(toRadians(theta));
            double y = sin(toRadians(theta));
            g.drawLine((int) (xo + x * radius), (int) (yo + y * radius),
                    (int) (xo + x * (radius - getTickLength())),
                    (int) (yo + y * (radius - getTickLength())));
        }

        final int l = (int) (getTickLength() / 2.0);
        for (int i = 0; i < incTickNum; i++) {
            int x = (int) ((i * radius) / incTickNum);
            g.drawLine(xo + x, yo - l, xo + x, yo + l);
        }
        g.drawLine(xo - l, yo, xo + l, yo);
    }

     private static void drawGreatCircleSubsegment(Graphics2D g,
            int xo, int yo, int radius, Vec3[] vs) {
         BasicStroke stroke;
         if (abs(vs[0].z) > 1e-10) stroke = vs[0].z<0 ? stroke1 : stroke2;
         else stroke = vs[vs.length-1].z<0 ? stroke1 : stroke2;
         GeneralPath path = new GeneralPath();
         boolean first = true;
         for (Vec3 v : vs) {
             Point2D p = project(v, xo, yo, radius);
             if (first) {
                 path.moveTo((float) p.getX(), (float) p.getY());
                 first = false;
             } else {
                 path.lineTo((float) p.getX(), (float) p.getY());
             }
         }
         g.setStroke(stroke);
         g.draw(path);
     }

     protected static void drawGreatCircleSegment(Graphics2D g,
            int xo, int yo, int radius, Vec3 p1, Vec3 p2) {

        if (p1.sameHemisphere(p2)) {
            drawGreatCircleSubsegment(g, xo, yo, radius,
                    Vec3.spherInterpolate(p1, p2, 0.05));
        } else {
            Vec3 equator = Vec3.equatorPoint(p1, p2);
            drawGreatCircleSubsegment(g, xo, yo, radius,
                    Vec3.spherInterpolate(p1, equator, 0.05));
            drawGreatCircleSubsegment(g, xo, yo, radius,
                    Vec3.spherInterpolate(equator, p2, 0.05));
        }
    }

    protected static Point2D project(Vec3 p, int xo, int yo, int radius) {
        final double h2 = p.x * p.x + p.y * p.y;
        final double L = (h2 > 0) ? sqrt(1 - abs(p.z)) / sqrt(h2) : 0;

        /* Need to convert from declination (running clockwise from
         * Y axis) to plot co-ordinates (running anticlockwise from X axis).
         * First we flip the x-axis so that we're going anticlockwise
         * (let x' = x, y' = -y). Then we perform a 90˚ anticlockwise rotation
         * (let x'' = -y' = y, y'' = x' = x). Finally, we flip the y axis
         * to take account of AWT Y-coordinates running top-to-bottom rather
         * than bottom-to-top (let x''' = x'' = y, y''' = -y'' = -x).
         */
        return new Point2D.Double(xo + radius * p.y * L,
                yo + radius * (-p.x) * L);
    }

}
