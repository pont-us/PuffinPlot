package net.talvi.puffinplot.plots;

import static java.lang.Math.sqrt;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;

import java.awt.Color;
import java.awt.Graphics2D;
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
