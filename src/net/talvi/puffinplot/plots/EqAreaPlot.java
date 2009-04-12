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
     // as fraction of radius
    private static final int decTickStep = 10;
    private static final int incTickNum = 9;

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

    protected static void drawGreatCircleSegment(Graphics2D g,
            int xo, int yo, int radius, Vec3 p1, Vec3 p2) {
        BasicStroke stroke1 = new BasicStroke();
        BasicStroke stroke2 = new BasicStroke(1, 0, 0, 1, new float[] {2, 2}, 0);
        Vec3[] points = Vec3.spherInterpolate(p1, p2, 0.05);
        GeneralPath path = null;
        int i = 0;
        while (i < points.length) {
            path = new GeneralPath();
            Point2D p = null;
            p = project(points[i], xo, yo, radius);
            path.moveTo((float) p.getX(), (float) p.getY());
            do {
                i++;
                if (i >= points.length) break;
                p = project(points[i], xo, yo, radius);
                path.lineTo((float) p.getX(), (float) p.getY());
            } while ((signum(points[i - 1].z) == signum(points[i].z)));

            g.setStroke(points[i-1].z < 0 ? stroke1 : stroke2);
            g.draw(path);
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
