/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.window.PlotParams;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

/**
 * An abstract superclass for Lambert azimuthal equal-area plots. It provides
 * various facilities to simplify the drawing of an equal-area plot, including
 * projection of the data points and drawing of the axes.
 *
 * @author pont
 */
public abstract class EqAreaPlot extends Plot {
    private static final int decTickStep = 10;
    private static final int incTickNum = 9;
    
    /**
     * The graphics object to which to draw the plot. It is set by
     * {@link #updatePlotDimensions(Graphics2D)}.
     */
    protected Graphics2D g;
    
    /**
     * The x co-ordinate of the projection's origin. It is set by
     * {@link #updatePlotDimensions(Graphics2D)}.
     */
    protected int xo;
    
    /**
     * The y co-ordinate of the projection's origin. It is set by
     * {@link #updatePlotDimensions(Graphics2D)}.
     */
    protected int yo;
    
    /**
     * The radius of the projection. It is set by
     * {@link #updatePlotDimensions(Graphics2D)}.
     */
    protected int radius;
    
    /**
     * The preferences governing various options for this plot.
     */
    protected Preferences prefs;
    
    /**
     * Creates a new equal-area plot with the supplies parameters.
     * 
     * @param parameters the plot parameters
     */
    protected EqAreaPlot(PlotParams parameters) {
        super(parameters);
        this.prefs = parameters.getPreferences();
    }
    
    /**
     * Sets the fields {@link #g}, {@link #radius}, {@link #xo}, and {@link #yo}
     * according to the supplied argument and the current plot dimensions. This
     * method should be called before redrawing the plot.
     *
     * @param g the field {@link #g} will be set to this value
     */
    protected void updatePlotDimensions(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        this.g = g;
        radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        xo = (int) dims.getCenterX();
        yo = (int) dims.getCenterY();
    }
    
    /**
     * Draws the axes of the plot.
     */
    protected void drawAxes() {
        g.setStroke(getStroke());
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
            final Point2D p =  project(Vec3.fromPolarDegrees(
                    1., 90 - i * (90./(double)incTickNum), 90.));
            double x = p.getX();
            g.draw(new Line2D.Double(x, yo - l, x, yo + l));
        }
        g.draw(new Line2D.Double(xo - l, yo, xo + l, yo));
        if (prefs != null &&
                prefs.getBoolean("plots.labelEqualAreaPlots", false)) {
            g.drawString(getShortName(),
                    (float) (xo + radius/2), (float) (yo + radius));
        }
    }

    /**
     * Projects a list of three-dimensional vectors into a two-dimensional
     * path in the current plot co-ordinates.
     * 
     * @param vectors the vectors to project
     * @return a path containing the projected vectors
     */
    protected Path2D.Double vectorsToPath(List<Vec3> vectors) {
        Path2D.Double path = new Path2D.Double();
        boolean first = true;
        for (Vec3 v : vectors) {
            assert(v != null);
            // g.setStroke(new BasicStroke(getUnitSize() * (1-(float)v.z) *20.0f));
            final Point2D p = project(v);
            assert(!Double.isNaN(p.getX()));
            assert(!Double.isNaN(p.getY()));
            final double x = p.getX();
            final double y = p.getY();
            assert(!Double.isNaN(x));
            assert(!Double.isNaN(y));
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }
        return path;
    }

    /**
     * Project and cache line segments. Assumes all segments in same hemisphere.
     *
     * @param vs vectors to project
     * @param cache line cache in which to store projected vector path
     */
     protected void projectLineSegments(List<Vec3> vs, LineCache cache) {
         /*
          * determine whether we're in upper hemisphere, ignoring z co-ordinates
          * very close to zero.
          */
         boolean upperHemisph = true;
         for (Vec3 v: vs) {
             if (v.z > 1e-10) {
                 upperHemisph = false;
                 break;
             }
         }
         cache.addPath(vectorsToPath(vs), upperHemisph);
         //Stroke stroke = upperHemisph ? getStroke() : getDashedStroke();
         //g.setStroke(stroke);
         //final Path2D.Double path = vectorsToPath(vs);
         //g.draw(path);
     }

     private LineCache projectLineSegments(List<Vec3> vs) {
        final List<List<Vec3>> vss;
        vss = Vec3.interpolateEquatorPoints(vs);
        final LineCache lineCache = new LineCache(getStroke(), getDashedStroke());
        for (List<Vec3> part: vss) {
            projectLineSegments(part, lineCache);
        }
        return lineCache;
    }
     
    /**
     * Projects and draws the supplied vectors.
     * 
     * @param vs the vectors to draw
     */
    protected void drawLineSegments(List<Vec3> vs) {
        final LineCache lineCache = projectLineSegments(vs);
        lineCache.draw(g);
    }
    
    /**
     * Create a projection of a great-circle segment.
     * 
     * @param v0 first endpoint of the great-circle segment
     * @param v1 second endpoint of the great-circle segment
     * @return line segments corresponding to a projection of the segment
     */
    protected LineCache projectGreatCircleSegment(Vec3 v0, Vec3 v1) {
        return projectLineSegments(Vec3.spherInterpolate(v0, v1, 0.05));
    }
    
    /**
     * Draws the projection of a specified great-circle segment. The shorter of
     * the two possible paths will be drawn.
     *
     * @param v0 one end of a great-circle segment
     * @param v1 the other end of a great-circle segment
     */
    protected void drawGreatCircleSegment(Vec3 v0, Vec3 v1) {
        drawLineSegments(Vec3.spherInterpolate(v0, v1, 0.05));
    }

    /**
     * Draws the projection of a specified great-circle segment. Of the two
     * possible paths, the one passing closer to the supplied vector {@code dir}
     * will be drawn.
     *
     * @param v0 one end of a great-circle segment
     * @param v1 the other end of a great-circle segment
     * @param dir vector used to choose which path to draw
     */
    protected void drawGreatCircleSegment(Vec3 v0, Vec3 v1, Vec3 dir) {
        assert(v0.isWellFormed());
        assert(v1.isWellFormed());
        assert(dir.isWellFormed());
        drawLineSegments(Vec3.spherInterpDir(v0, v1, dir, 0.05));
    }

    /**
     * Projects and draws a great circle
     * 
     * @param pole the pole to the great circle which is to be drawn
     * @param drawPole {@code true} to mark the pole on the plot
     */
    protected void drawGreatCircle(Vec3 pole, boolean drawPole) {
        int n = 64;
        List<Vec3> vs = pole.greatCirclePoints(n, true);
        drawLineSegments(vs);
   }

    /**
     * Projects the direction of a three-dimensional vector into plot
     * co-ordinates. The supplied vector must be well-formed (i.e. its
     * components must be finite numbers rather than NaN or infinite values).
       * 
     * @param v a well-formed vector
     * @return the projection of the supplied vector onto this plot
     */
    protected Point2D.Double project(Vec3 v) {
        /*
         * Need to convert from declination (running clockwise from Y axis) to
         * plot co-ordinates (running anticlockwise from X axis). First we flip
         * the x-axis so that we're going anticlockwise (let x' = x, y' = -y).
         * Then we perform a 90˚ anticlockwise rotation (let x'' = -y' = y, y''
         * = x' = x). Finally, we flip the y axis to take account of AWT
         * Y-coordinates running top-to-bottom rather than bottom-to-top (let
         * x''' = x'' = y, y''' = -y'' = -x).
         */
        assert(v.isWellFormed());
        final double h2 = v.x * v.x + v.y * v.y;
        final double L = (h2 > 0) ? sqrt(1 - abs(v.z)) / sqrt(h2) : 0;
        final double x = xo + radius * v.y * L;
        final double y = yo + radius * (-v.x) * L;
        assert(!Double.isNaN(x));
        assert(!Double.isNaN(y));
        return new Point2D.Double(x, y);
    }

    /**
     * Returns a short, human-readable name for this plot. This is used to label
     * the plots on the graph display during normal use. (The "nice name" is
     * used when resizing/moving.)
     *
     * @return a short, human-readable name for this plot
     */
    public abstract String getShortName();
}
