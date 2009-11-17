package net.talvi.puffinplot.plots;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import static java.lang.Math.sqrt;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.abs;
import static java.lang.Math.toRadians;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Vec3;

public abstract class EqAreaPlot extends Plot {
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

     private void drawLineSegments(Graphics2D g,
            int xo, int yo, int radius, Vec3[] vs) {
         boolean upper = true;
         for (Vec3 v: vs) {
             if (v.z > 1e-10) {
                 upper = false;
                 break;
             }
         }
         Stroke stroke = upper ? getStroke() : getDashedStroke();

         GeneralPath path = new GeneralPath();
         boolean first = true;
         for (Vec3 v : vs) {
             // g.setStroke(new BasicStroke(getUnitSize() * (1-(float)v.z) *20.0f));
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

     /**
      * Currently unused but should be plumbed in as a user preference
      * at some point. Draws line segments using an unconventional
      * three-dimensional effect, whereby more distant segments thinner
      * and lighter in colour.
      *
      */
    private void drawTaperedLineSegments(Graphics2D g,
            int xo, int yo, int radius, Vec3[] vs) {
         boolean first = true;
         Point2D pPrev = null;
         for (Vec3 v : vs) {
             Point2D p = project(v, xo, yo, radius);
             if (!first) {
                 g.setColor(Color.BLACK);
                 float w = (1.0f-(float)v.z)/2f;
                 g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                         0.6f + w*0.4f));
                 g.setStroke(new BasicStroke(getUnitSize() * w * 30.0f,
                         BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                 g.draw(new Line2D.Double(pPrev, p));
             }
             first = false;
             pPrev = p;
         }
     }

    protected void drawGreatCircleSegment(Graphics2D g,
            int xo, int yo, int radius, Vec3 p1, Vec3 p2) {
        if (p1.sameHemisphere(p2)) {
            drawLineSegments(g, xo, yo, radius,
                    Vec3.spherInterpolate(p1, p2, 0.05));
        } else {
            Vec3 equator = Vec3.equatorPoint(p1, p2);
            drawLineSegments(g, xo, yo, radius,
                    Vec3.spherInterpolate(p1, equator, 0.05));
            drawLineSegments(g, xo, yo, radius,
                    Vec3.spherInterpolate(equator, p2, 0.05));
         }
    }

    protected void drawGreatCircle(Graphics2D g, int xo, int yo, int radius,
            Vec3 pole) {
        int n = 48;
        List<Vec3> vs = pole.greatCirclePoints(n);
        List<Vec3> bottom = new ArrayList<Vec3>(vs.size());
        List<Vec3> top = new ArrayList<Vec3>(vs.size());
        int i=1;
        int state = 0;
        Vec3 prev = vs.get(0);
        // 0: seeking start of top; 1: processing top;
        // 2: processing bottom; 3: finish
        while (state < 3) {
            Vec3 v = vs.get(i);
            i = (i+1) % n;
            switch (state) {
                case 0:
                    if (prev.z < 0 && v.z >= 0) {
                        top.add(Vec3.equatorPoint(prev, v));
                        top.add(v);
                        state = 1;
                    }
                    break;
                case 1:
                    if (v.z < 0) {
                        Vec3 eq = Vec3.equatorPoint(prev, v);
                        top.add(eq);
                        bottom.add(eq);
                        bottom.add(v);
                        state = 2;
                    } else {
                        top.add(v);
                    }
                    break;
                case 2:
                    if (v.z >= 0) {
                        bottom.add(top.get(0));
                        state = 3;
                    } else {
                        bottom.add(v);
                    }
                    break;
            }
            prev = v;
        }
        drawLineSegments(g, xo, yo, radius, top.toArray(new Vec3[]{}));
        drawLineSegments(g, xo, yo, radius, bottom.toArray(new Vec3[]{}));
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
