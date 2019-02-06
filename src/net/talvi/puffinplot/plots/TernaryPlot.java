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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

/**
 * An experimental ternary plot for a sample's magnetic moment data. Each point
 * on the plot represents a magnetic moment measurement. Each of the ternary
 * plot's axes corresponds to one of the three magnetic moment measurement axes
 * (x, y, and z). A path on the plot thus shows how the relative intensities of
 * the three orthogonal magnetization components vary during treatment. For a
 * normal palaeomagnetic study, this is unlikely to be useful. It is designed
 * for use with Lowrie's (1990) technique of thermal demagnetization of a
 * composite triaxial IRM. In this case the three axes of the graph represent
 * three different coercivity components, and the path of points for a sample
 * represents the relative response of those components to thermal
 * demagnetization.
 *
 * @author pont
 */
public class TernaryPlot extends Plot {

    /** Creates a ternary plot with the supplied parameters.
     * 
     * @param params the parameters of the plot
     */
    public TernaryPlot(PlotParams params) {
        super(params);
    }

    private void drawAxes(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setStroke(getStroke());
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

    private static Point2D.Double projectNormalized(double a, double b,
            double xo, double yo, double scale) {
        final double y = yo - a * scale;
        final double w = (2/sqrt(3) - a);
        final double x = xo + ((a/2) * (2/sqrt(3))  + w * b) * scale;
        return new Point2D.Double(x, y);
    }

    private static Point2D.Double projectThreeValues(double a, double b,
            double c, double xo, double yo, double scale) {
        final double aa = abs(a);
        final double ab = abs(b);
        final double ac = abs(c);
        final double total = aa + ab + ac;
        return projectNormalized(aa/total, ab/total, xo, yo, scale);
    }

    @Override
    public String getName() {
        return "ternaryplot";
    }
    
    @Override
    public String getNiceName() {
        return "Ternary demag.";
    }

    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        drawAxes(g);
        final Rectangle2D dims = getDimensions();
        double h = dims.getWidth() * (sqrt(3)/2);
        Sample sample = params.getSample();
        if (sample==null) return;
        for (TreatmentStep step: sample.getTreatmentSteps()) {
            final Vec3 v = step.getMoment(params.getCorrection());
            addPoint(step, projectThreeValues(v.x, v.y, v.z,
                    dims.getMinX(), dims.getMaxY(), h), false, false, true);
        }
        drawPoints(g);
    }
}
