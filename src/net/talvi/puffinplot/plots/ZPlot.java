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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.text.AttributedString;
import java.util.ArrayList;
import static java.util.Collections.max;
import static java.util.Collections.min;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.Util;
import net.talvi.puffinplot.data.*;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

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
    private final Preferences prefs;

    /** Creates a Zijderveld plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
    public ZPlot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        super(parent, params, prefs);
        legend = new ZplotLegend(parent, params, prefs);
        this.prefs = prefs;
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
    
    private void drawPcaLine(Graphics2D g, double x, double y,
            double angleRad, ZplotAxes axes, Color colour, Rectangle2D clip,
            double scale) {
        // Note that line clipping is done ‘manually’. The previous implementation
        // just used g.setClip(axes.getBounds()) (saving and restoring the
        // previous clip rectangle), but this caused problems, chiefly
        // that the lines would appear at full length in SVG and PDF exports.
        // Unfortunately there seems to be no appropriate clipping
        // function in the Java libraries, so I have added a clipping routine
        // to the Util class.
        // SAFE_LENGTH is intended always to reach the edges of the plot.
        final double SAFE_LENGTH = 2000;
        final double dx = SAFE_LENGTH * sin(angleRad);
        final double dy = SAFE_LENGTH * cos(angleRad);
        g.setStroke(getStroke());
        g.setColor(colour);
        Line2D line = Util.clipLineToRectangle(
                new Line2D.Double(x-dx, y+dy, x+dx, y-dy), clip);
        line = Util.scaleLine(line, scale);
        g.draw(line);
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
        
        // We keep track of the points used for the PCA calculation in 
        // order to calculate the length for the short-format PCA fit line.
        final List<Point2D> pcaPointsH = new ArrayList<Point2D>(data.size()+1);
        final List<Point2D> pcaPointsV = new ArrayList<Point2D>(data.size()+1);
        
        boolean first = true;
        for (Datum d: data) {
            final Vec3 v = d.getMoment(correction);
            // Plot the point in the horizontal plane
            final double x1 = xOffset + v.y * scale;
            final double y1 = yOffset - v.x * scale;
            final Point2D point = new Point2D.Double(x1, y1);
            addPoint(d, point, true, first, !first);
            if (d.isInPca()) pcaPointsH.add(point);
            first = false;
        }
        first = true;
        for (Datum d: data) {
            Vec3 v = d.getMoment(correction);
            // Now plot the point in the vertical plane
            final double x2 = xOffset + v.getComponent(vVs) * scale;
            final double y2 = yOffset - v.getComponent(MeasurementAxis.MINUSZ) * scale;
            final Point2D point = new Point2D.Double(x2, y2);
            addPoint(d, point, false, first, !first);
            if (d.isInPca()) pcaPointsV.add(point);
            first = false;
        }
        
        final PcaValues pca = sample.getPcaValues();
        final String pcaStyle = prefs.get("plots.zplotPcaDisplay", "Long");
        final double lineScale = "Long".equals(pcaStyle) ? 0.9 : 1.0;
        if (pca != null && !"None".equals(pcaStyle)) {
            if (pca.isAnchored()) {
                final Point2D origin = new Point2D.Double(axes.getXOffset(), axes.getYOffset());
                pcaPointsH.add(origin);
                pcaPointsV.add(origin);
            }
            Rectangle2D clipRectangle = axes.getBounds(); // if "Short" will overwrite
            
            final double incRad = pca.getDirection().getIncRad();
            final double decRad = pca.getDirection().getDecRad();
            final double x1 = pca.getOrigin().y * scale;
            final double y1 = - pca.getOrigin().x * scale;
            if ("Short".equals(pcaStyle)) {
                clipRectangle = Util.envelope(pcaPointsH);
            }
            drawPcaLine(g, xOffset + x1, yOffset + y1,
                    decRad, axes, Color.BLUE, clipRectangle, lineScale);
            
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
                if ("Short".equals(pcaStyle)) {
                    clipRectangle = Util.envelope(pcaPointsV);
                }
                drawPcaLine(g, xOffset + x2, yOffset + y2, Math.PI/2 + incCorr,
                        axes, Color.BLUE, clipRectangle, lineScale);
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
