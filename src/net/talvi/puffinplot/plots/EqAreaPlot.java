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
    private boolean taperingEnabled;
    private final static boolean TAPERED_LINES = true;
    
    protected EqAreaPlot(GraphDisplay parent, PlotParams params, Preferences prefs,
            boolean taperingEnabled) {
        super(parent, params, prefs);
        this.taperingEnabled = taperingEnabled;
    }
    
    protected EqAreaPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        this(parent, params, prefs, false);
    }
    
    protected void drawAxes(Graphics2D g, int xo, int yo, int radius) {
        g.setColor(Color.WHITE);
        g.fillArc(xo - radius, yo - radius, radius * 2, radius * 2, 0, 360);
        g.setColor(Color.BLACK);
        g.drawArc(xo - radius, yo - radius, radius * 2, radius * 2, 0, 360);
        final double r = radius;
        for (int theta = 0; theta < 360; theta += decTickStep) {
            final double x = cos(toRadians(theta));
            final double y = sin(toRadians(theta));
            g.draw(new Line2D.Double(xo + x * r, yo + y * r,
                    xo + x * (r - getTickLength()),
                    yo + y * (r - getTickLength())));
        }

        final double l = getTickLength() / 2.0;
        for (int i = 0; i < incTickNum; i++) {
            Point2D p =  project(Vec3.fromPolarDegrees(1., 90 - i * (90./(double)incTickNum), 90.),
                    xo, yo, radius);
            double x = p.getX();
            g.draw(new Line2D.Double(x, yo - l, x, yo + l));
        }
        g.draw(new Line2D.Double(xo - l, yo, xo + l, yo));
    }

    protected GeneralPath vectorsToPath(List<Vec3> vectors, int xo, int yo, int radius) {
        GeneralPath path = new GeneralPath();
        boolean first = true;
        for (Vec3 v : vectors) {
            // g.setStroke(new BasicStroke(getUnitSize() * (1-(float)v.z) *20.0f));
            Point2D p = project(v, xo, yo, radius);
            if (first) {
                path.moveTo((float) p.getX(), (float) p.getY());
                first = false;
            } else {
                path.lineTo((float) p.getX(), (float) p.getY());
            }
        }
        return path;
    }

     protected void drawStandardLineSegments(Graphics2D g,
            int xo, int yo, int radius, List<Vec3> vs) {
         // determine whether we're in upper hemisphere, ignoring
         // z co-ordinates very close to zero. Assumes all segments
         // in same hemisphere.
         boolean upperHemisph = true;
         for (Vec3 v: vs) { if (v.z > 1e-10) { upperHemisph = false; break; } }
         Stroke stroke = upperHemisph ? getStroke() : getDashedStroke();
         g.setStroke(stroke);
         g.draw(vectorsToPath(vs, xo, yo, radius));
     }

     /**
      * Currently unused but should be plumbed in as a user preference
      * at some point. Draws line segments using a three-dimensional effect,
      * whereby more distant segments appear thinner and lighter in colour.
      *
      */
    private void drawTaperedLineSegments(Graphics2D g,
            int xo, int yo, int radius, List<Vec3> vs) {
         boolean first = true;
         Point2D pPrev = null;
         for (Vec3 v : vs) {
             Point2D p = project(v, xo, yo, radius);
             if (!first) {
                 //g.setColor(Color.BLACK);
                 float w = (1.0f-(float)v.z)/2f;
                 float colour = 0.3f*(1f-w);
                 g.setColor(new Color(colour,colour,colour));
                 //g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                 //        0.8f + w*0.2f));
                 float width = w * 20.0f;
                 if (width < 4) {width = 4f;}
                 g.setStroke(new BasicStroke(getUnitSize() * width,
                         BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                 g.draw(new Line2D.Double(pPrev, p));
             }
             first = false;
             pPrev = p;
         }
     }

    private void drawLineSegments(Graphics2D g,
            int xo, int yo, int radius, List<Vec3> vs) {
        if (isTaperingEnabled()) {
            drawTaperedLineSegments(g, xo, yo, radius, vs);
        } else {
            List<List<Vec3>> vss =  Vec3.interpolateEquatorPoints(vs);
            for (List<Vec3> part: vss) {
                drawStandardLineSegments(g, xo, yo, radius, part);
            }
        }
    }

    protected void drawGreatCircleSegment(Graphics2D g,
            int xo, int yo, int radius, Vec3 p1, Vec3 p2) {
        if (p1.sameHemisphere(p2)) {
            drawLineSegments(g, xo, yo, radius,
                    Vec3.spherInterpolate(p1, p2, 0.05));
        } else {
            // If we're using the solid/dashed line convention,
            // we need to split the line across the z=0 plane
            Vec3 equator = Vec3.equatorPoint(p1, p2);
            drawLineSegments(g, xo, yo, radius,
                    Vec3.spherInterpolate(p1, equator, 0.05));
            drawLineSegments(g, xo, yo, radius,
                    Vec3.spherInterpolate(equator, p2, 0.05));
         }
    }

    protected void drawGreatCircleSegment(Graphics2D g,
            int xo, int yo, int radius, Vec3 p1, Vec3 p2, Vec3 dir) {
        drawLineSegments(g, xo, yo, radius, Vec3.spherInterpDir(p1, p2, dir, 0.05));
    }

    protected void drawGreatCircle(Graphics2D g, int xo, int yo, int radius,
            Vec3 pole) {
        int n = 64;
        List<Vec3> vs = pole.greatCirclePoints(n, true);
        drawLineSegments(g, xo, yo, radius, vs);
   }

    protected static Point2D.Double project(Vec3 p, int xo, int yo, int radius) {
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

    public boolean isTaperingEnabled() {
        return taperingEnabled;
    }
}
