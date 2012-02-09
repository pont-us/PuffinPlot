/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.atan;
import static java.util.Collections.min;
import static java.util.Collections.max;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.Comparator;
import java.util.List;

import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumComparator;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Vec3;
import net.talvi.puffinplot.data.Sample;

/**
 * A Zijderveld plot for a sample's demagnetization data. The vertical
 * projection can be set to show east vs. up or north vs. up,
 * and can also be set for a modified Zijderveld plot, where each data
 * point is projected onto the vertical plane containing the origin and
 * itself.
 * 
 * @author pont
 */
public class ZPlot extends Plot {

    private ZplotAxes axes;
    private final ZplotLegend legend;

    /** Creates a Zijderveld plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public ZPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        legend = new ZplotLegend(parent, params, prefs);
    }

    private static Rectangle2D extent(List<Datum> sample, Correction c,
            MeasurementAxis axis1, MeasurementAxis axis2) {
        final Comparator<Datum> xComp = new DatumComparator(axis1, c);
        final Comparator<Datum> yComp = new DatumComparator(axis2, c);
        final double xMin = min(sample, xComp).getMoment(c).getComponent(axis1);
        final double xMax = max(sample, xComp).getMoment(c).getComponent(axis1);
        final double yMin = min(sample, yComp).getMoment(c).getComponent(axis2);
        final double yMax = max(sample, yComp).getMoment(c).getComponent(axis2);
        return new Rectangle2D.Double(xMin, yMin, xMax-xMin, yMax-yMin);
    }
    
    private void drawLine(Graphics2D g, double x, double y,
            double angleRad, ZplotAxes axes, Color colour) {
        final Rectangle oldClip = g.getClipBounds();
        g.setClip(axes.getBounds());
        final double dx = 800*sin(angleRad);
        final double dy = 800*cos(angleRad);
        g.setStroke(getStroke());
        g.setColor(colour);
        g.draw(new Line2D.Double(x-dx, y+dy, x+dx, y-dy));
        g.setClip(oldClip);
    }
    
    @Override
    public String getName() {
        return "zplot";
    }

    @Override
    public String getNiceName() {
        return "Zplot";
    }

    public void draw(Graphics2D g) {
        final Sample sample = params.getSample();
        if (sample==null) return;
        final List<Datum> data = sample.getVisibleData();
        if (data.isEmpty()) return;
        
        clearPoints();
        final Correction correction = params.getCorrection();
        final MeasurementAxis vVs = params.getAxis();

        final Rectangle2D extent1 =
                extent(data, correction, MeasurementAxis.Y, MeasurementAxis.X);
        final Rectangle2D extent2 =
                extent(data, correction, vVs, MeasurementAxis.MINUSZ);

        final Rectangle2D dim = cropRectangle(getDimensions(), 250, 250, 200, 200);

        axes = new ZplotAxes(extent1.createUnion(extent2), dim, vVs, this);
        
        g.setColor(Color.BLACK);
        g.setStroke(getStroke());
        axes.draw(g);

        final double scale = axes.getScale();
        final double xOffset = axes.getXOffset();
        final double yOffset = axes.getYOffset();
        
        boolean first = true;
        for (Datum d: data) {
            final Vec3 p = d.getMoment(correction);
            // Plot the point in the horizontal plane
            final double x1 = xOffset + p.y * scale;
            final double y1 = yOffset - p.x * scale;
            addPoint(d, new Point2D.Double(x1, y1), true, first, !first);
            first = false;
        }
        first = true;
        for (Datum d: data) {
            Vec3 p = d.getMoment(correction);
            // Now plot the point in the vertical plane
            final double x2 = xOffset + p.getComponent(vVs) * scale;
            final double y2 = yOffset - p.getComponent(MeasurementAxis.MINUSZ) * scale;
            addPoint(d, new Point2D.Double(x2, y2), false, first, !first);
            first = false;
        }
        
        final PcaValues pca = sample.getPcaValues();
        if (pca != null) {
            final double incRad = pca.getDirection().getIncRad();
            final double decRad = pca.getDirection().getDecRad();
            final double x1 = pca.getOrigin().y * scale;
            final double y1 = - pca.getOrigin().x * scale;
            drawLine(g, xOffset + x1, yOffset + y1,
                    decRad, axes, Color.BLUE);
            
            final double x2 = pca.getOrigin().getComponent(vVs) * scale;
            final double y2 = - pca.getOrigin().getComponent(MeasurementAxis.MINUSZ) * scale;
            double incCorr = 0;

            switch (vVs) {
                // We don't necessarily want the actual line of inclination; we
                // want the projection of that line onto the appropriate plane.
                case X:
                    incCorr = atan(sin(incRad) / (cos(incRad) * cos(decRad)));
                    break;
                case Y:
                    incCorr = atan(sin(incRad) / (cos(incRad) * sin(decRad)));
                    break;
            }
            if (vVs== MeasurementAxis.X || vVs == MeasurementAxis.Y) {
                /* If we're plotting vertical projections vs. `H', there's
                 * no meaningful way to display the vertical component of the
                 * PCA: the projection plane is changing with every point
                 * so there is no meaningful plane onto which the PCA line
                 * can be projected.
                 */
                drawLine(g, xOffset + x2, yOffset + y2, Math.PI/2 + incCorr, axes, Color.BLUE);
            }
        }
        drawPoints(g);
    }

    /** Returns the legend for this plot. 
     * @return the legend for this plot */
    public ZplotLegend getLegend() {
        return legend;
    }

    /** A legend for a Zijderveld plot. It shows the units for the
     * axes, and gives a key for the filled and unfilled points. */
    public class ZplotLegend extends Plot {

        /** Creates a legend for the Zijderveld plot containing this class.
         * 
         * @param parent the graph display containing the legend
         * @param params the parameters of the legend
         * @param prefs the preferences containing the legend configuration
         */
        private ZplotLegend(GraphDisplay parent, PlotParams params,
                Preferences prefs) {
            super(parent, params, prefs);
        }

        @Override
        public String getName() {
            return "zplotlegend";
        }

        @Override
        public String getNiceName() {
            return "Zplot key";
        }

        @Override
        public int getMargin() {
            return 12;
        }

        @Override
        public void draw(Graphics2D g) {
            final Rectangle2D dims = getDimensions();
            clearPoints();
            final double xOrig = dims.getMinX() + getMargin() + getUnitSize() * 50;
            final double yOrig = dims.getMinY() + getMargin();
            final double textOffs = 25 * getUnitSize();
            final double lineOffs = 150 * getUnitSize();

            g.setColor(Color.BLACK);
            addPoint(null, new Point2D.Double(xOrig, yOrig), false, false, false);
            addPoint(null, new Point2D.Double(xOrig, yOrig + lineOffs), true, false, false);
            writeString(g, "vertical", (float) xOrig + 50 * getUnitSize(),
                    (float) (yOrig + textOffs));
            writeString(g, "horizontal", (float) (xOrig + 50 * getUnitSize()),
                    (float) (yOrig + lineOffs + textOffs));
            final AttributedString units = axes != null
                    ? timesTenToThe("Units: A/m", axes.getMagnitude())
                    : timesTenToThe("Units: A/m", "?");
            writeString(g, units, (float) xOrig,
                    (float) (yOrig + 2 * lineOffs + textOffs));
            drawPoints(g);
        }
    }
}
