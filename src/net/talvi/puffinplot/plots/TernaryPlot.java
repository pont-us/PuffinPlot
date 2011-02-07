package net.talvi.puffinplot.plots;

import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

/**
 * A general-purpose class for producing a ternary plot of
 * any appropriately structured data.
 *
 * @author pont
 */
public class TernaryPlot extends Plot {

    private final Preferences prefs;

    public TernaryPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        this.prefs = prefs;
    }

    protected void drawAxes(Graphics2D g) {
        g.setColor(Color.BLACK);
        final Rectangle2D dims = getDimensions();
        final double xo = dims.getMinX();
        final double yo = dims.getMaxY();
        final double s = dims.getWidth();
        g.draw(new Line2D.Double(xo, yo, xo+s, yo));
        g.draw(new Line2D.Double(xo, yo, xo+s*cos(toRadians(60)),
                yo-s*sin(toRadians(60))));
        g.draw(new Line2D.Double(xo+s, yo, xo+s-s*cos(toRadians(60)),
                yo-s*sin(toRadians(60))));
    }

    protected static Point2D.Double projectNormalized(double a, double b,
            double xo, double yo, double scale) {
        final double y = yo - a * scale;
        final double w = (2/sqrt(3) - a);
        final double x = xo + ((a/2) * (2/sqrt(3))  + w * b) * scale;
        return new Point2D.Double(x, y);
    }

    protected static Point2D.Double projectThreeValues(double a, double b,
            double c, double xo, double yo, double scale) {
        final double aa = abs(a);
        final double ab = abs(b);
        final double ac = abs(c);
        final double total = aa + ab + ac;
        return projectNormalized(aa/total, ab/total, xo, yo, scale);
    }

    @Override
    public String getName() {
        return "ternarytest";
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        drawAxes(g);
        final Rectangle2D dims = getDimensions();
        double h = dims.getWidth() * (sqrt(3)/2);
        Sample s = params.getSample();
        for (Datum d: s.getData()) {
            Vec3 v = d.getMoment(params.getCorrection());
            addPoint(d, projectThreeValues(v.x, v.y, v.z,
                    dims.getMinX(), dims.getMaxY(), h), false, false, true);
        }
        drawPoints(g);
    }
}
