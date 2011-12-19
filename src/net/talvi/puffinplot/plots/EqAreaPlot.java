package net.talvi.puffinplot.plots;

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
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Vec3;
import static java.lang.Math.min;

/**
 * An abstract superclass for Lambert azimuthal equal-area plots.
 * It provides various facilities to simplify the drawing of an
 * equal-area plot, including projection of the data points and drawing
 * of the axes.
 * 
 * @author pont
 */
public abstract class EqAreaPlot extends Plot {
    private static final int decTickStep = 10;
    private static final int incTickNum = 9;
    private boolean taperingEnabled;
    
    /** The graphics object to which to draw the plot.
     *  It is set by {@link #updatePlotDimensions(Graphics2D)}. */
    protected Graphics2D g;
    
    /** The x co-ordinate of the projection's origin.
     *  It is set by {@link #updatePlotDimensions(Graphics2D)}. */
    protected int xo;
    
    /** The y co-ordinate of the projection's origin.
     *  It is set by {@link #updatePlotDimensions(Graphics2D)}. */
    protected int yo;
    
    /** The radius of the projection.
     *  It is set by {@link #updatePlotDimensions(Graphics2D)}. */
    protected int radius;
    
    /**
     * Creates a new equal-area plot with the supplies parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the plot parameters
     * @param prefs the preferences containing the plot configuration
     * @param taperingEnabled {@code true} to taper plotted lines according to z position
     */
    protected EqAreaPlot(GraphDisplay parent, PlotParams params, Preferences prefs,
            boolean taperingEnabled) {
        super(parent, params, prefs);
        this.taperingEnabled = taperingEnabled;
    }
    
    /**
     * Creates a new equal-area plot with the supplies parameters.
     * Plotted lines will not be tapered.
     * 
     * @param parent the graph display containing the plot
     * @param params the plot parameters
     * @param prefs the preferences containing the plot configuration
     */
    protected EqAreaPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        this(parent, params, prefs, false);
    }
    
    /**
     * Sets the fields {@link #g}, {@link #radius}, {@link #xo},
     * and {@link #yo} according to the supplied argument and the
     * current plot dimensions.
     * This method should be called before redrawing the plot.
     * @param g the field {@link g} will be set to this value
     */
    protected void updatePlotDimensions(Graphics2D g) {
        final Rectangle2D dims = getDimensions();
        this.g = g;
        radius = (int) (min(dims.getWidth(), dims.getHeight()) / 2);
        xo = (int) dims.getCenterX();
        yo = (int) dims.getCenterY();
    }
    
    /** Draws the axes of the plot. */
    protected void drawAxes() {
        g.setStroke(getStroke());
        //g.setColor(Color.WHITE);
        //g.fillArc(xo - radius, yo - radius, radius * 2, radius * 2, 0, 360);
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
            Point2D p =  project(Vec3.fromPolarDegrees(1., 90 - i * (90./(double)incTickNum), 90.));
            double x = p.getX();
            g.draw(new Line2D.Double(x, yo - l, x, yo + l));
        }
        g.draw(new Line2D.Double(xo - l, yo, xo + l, yo));
    }

    /**
     * Projects a list of three-dimensional vectors into a two-dimensional
     * path in the current plot co-ordinates.
     * 
     * @param vectors the vectors to project
     * @return a path containing the projected vectors
     */
    protected GeneralPath vectorsToPath(List<Vec3> vectors) {
        GeneralPath path = new GeneralPath();
        boolean first = true;
        for (Vec3 v : vectors) {
            // g.setStroke(new BasicStroke(getUnitSize() * (1-(float)v.z) *20.0f));
            Point2D p = project(v);
            if (first) {
                path.moveTo((float) p.getX(), (float) p.getY());
                first = false;
            } else {
                path.lineTo((float) p.getX(), (float) p.getY());
            }
        }
        return path;
    }

    /**
     * Draw non-tapered line segments. Assumes all segments in same hemisphere.
     * @param vs vectors to project
     */
     private void drawStandardLineSegments(List<Vec3> vs) {
         // determine whether we're in upper hemisphere, ignoring
         // z co-ordinates very close to zero. 
         boolean upperHemisph = true;
         for (Vec3 v: vs) { if (v.z > 1e-10) { upperHemisph = false; break; } }
         Stroke stroke = upperHemisph ? getStroke() : getDashedStroke();
         g.setStroke(stroke);
         g.draw(vectorsToPath(vs));
     }

     /**
      * Draws line segments using a three-dimensional effect,
      * whereby more distant segments appear thinner and lighter in colour.
      * Should be made selectable by a user preference at some point. 
      */
    private void drawTaperedLineSegments(List<Vec3> vs) {
         boolean first = true;
         Point2D pPrev = null;
         for (Vec3 v : vs) {
             final Point2D p = project(v);
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

    /**
     * Projects and draws the supplied vectors.
     * 
     * @param vs the vectors to draw
     */
    protected void drawLineSegments(List<Vec3> vs) {
        if (isTaperingEnabled()) {
            drawTaperedLineSegments(vs);
        } else {
            List<List<Vec3>> vss =  Vec3.interpolateEquatorPoints(vs);
            for (List<Vec3> part: vss) {
                drawStandardLineSegments(part);
            }
        }
    }
    
    /** Draws the projection of a specified great-circle segment.
     * The shorter of the two possible paths will be drawn.
     * @param v0 one end of a great-circle segment
     * @param v1 the other end of a great-circle segment
     */
    protected void drawGreatCircleSegment(Vec3 v0, Vec3 v1) {
        drawLineSegments(Vec3.spherInterpolate(v0, v1, 0.05));
    }

    /** Draws the projection of a specified great-circle segment.
     * Of the two possible paths, the one passing closer
     * to the supplied vector {@code dir} will be drawn.
     * @param v0 one end of a great-circle segment
     * @param v1 the other end of a great-circle segment
     * @param dir vector used to choose which path to draw
     */
    protected void drawGreatCircleSegment(Vec3 v0, Vec3 v1, Vec3 dir) {
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
     * Projects the direction of a three-dimensional vector into plot co-ordinates.
     * 
     * @param v a vector
     * @return the projection of the supplied vector onto this plot
     */
    protected Point2D.Double project(Vec3 v) {
        final double h2 = v.x * v.x + v.y * v.y;
        final double L = (h2 > 0) ? sqrt(1 - abs(v.z)) / sqrt(h2) : 0;
        /* Need to convert from declination (running clockwise from
         * Y axis) to plot co-ordinates (running anticlockwise from X axis).
         * First we flip the x-axis so that we're going anticlockwise
         * (let x' = x, y' = -y). Then we perform a 90˚ anticlockwise rotation
         * (let x'' = -y' = y, y'' = x' = x). Finally, we flip the y axis
         * to take account of AWT Y-coordinates running top-to-bottom rather
         * than bottom-to-top (let x''' = x'' = y, y''' = -y'' = -x).
         */
        return new Point2D.Double(xo + radius * v.y * L,
                yo + radius * (-v.x) * L);
    }

    /** Reports whether tapered lines are enabled for this plot. 
     * @return {@code true} if tapered lines are enabled for this plot
     */
    public boolean isTaperingEnabled() {
        return taperingEnabled;
    }
}
