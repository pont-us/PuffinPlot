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
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import net.talvi.puffinplot.Util;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.PcaValues;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentStepMomentComparator;
import net.talvi.puffinplot.data.Vec3;

import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.util.Collections.max;
import static java.util.Collections.min;

/**
 * A Zijderveld plot for a sample's demagnetization data. The vertical
 * projection can be set to show east vs. up or north vs. up, and can also be
 * set for a modified Zijderveld plot, where each data point is projected onto
 * the vertical plane containing the origin and itself.
 *
 * @author pont
 */
public class ZPlot extends Plot {

    private ZplotAxes axes;
    private final ZplotLegend legend;
    private final Preferences prefs;

    /**
     * Creates a Zijderveld plot with the supplied parameters.
     *
     * @param params the parameters of the plot
     */
    public ZPlot(PlotParams params) {
        super(params);
        legend = new ZplotLegend(params);
        this.prefs = params.getPreferences();
    }

    private static Rectangle2D extent(List<TreatmentStep> sample, Correction c,
            MeasurementAxis axis1, MeasurementAxis axis2) {
        final Comparator<TreatmentStep> xComp =
                new TreatmentStepMomentComparator(axis1, c);
        final Comparator<TreatmentStep> yComp =
                new TreatmentStepMomentComparator(axis2, c);
        final double xMin = min(sample, xComp).getMoment(c).getComponent(axis1);
        final double xMax = max(sample, xComp).getMoment(c).getComponent(axis1);
        final double yMin = min(sample, yComp).getMoment(c).getComponent(axis2);
        final double yMax = max(sample, yComp).getMoment(c).getComponent(axis2);
        return new Rectangle2D.Double(xMin, yMin, xMax-xMin, yMax-yMin);
    }
    
    /**
     * Draws a line indicating a PCA direction.
     * 
     * @param g graphics context
     * @param x TODO
     * @param y TODO
     * @param angleRad TODO
     * @param colour colour of line
     * @param clip clipping rectangle (null indicates an infinitely small
     * clipping rectangle, and no line will be drawn)
     * @param scale TODO
     */
    private void drawPcaLine(Graphics2D g, double x, double y,
            double angleRad, Color colour, Rectangle2D clip,
            double scale) {
        /*
         * Line clipping is done ‘manually’. The previous implementation just
         * used g.setClip(axes.getBounds()) (saving and restoring the previous
         * clip rectangle), but this caused problems, chiefly that the lines
         * would appear at full length in SVG and PDF exports. Unfortunately
         * there seems to be no appropriate clipping function in the Java
         * libraries, so I have added a clipping routine to the Util class.
         */
        
        // SAFE_LENGTH is intended always to reach the edges of the plot.
        if (clip == null) return;
        final double SAFE_LENGTH = 10000;
        final double dx = SAFE_LENGTH * sin(angleRad);
        final double dy = SAFE_LENGTH * cos(angleRad);
        g.setStroke(getStroke());
        g.setColor(colour);
        final Line2D line = Util.clipLineToRectangle(
                new Line2D.Double(x-dx, y+dy, x+dx, y-dy), clip);
        /*
         * If the clipping rectangle is null, or the line does not intersect the
         * rectangle, the clipped line will be null. Util.scaleLine will handle
         * this gracefully but Graphics2D may not, so we check for a null line
         * here.
         */
        if (line != null) {
            g.draw(Util.scaleLine(line, scale));
        }
    }
    
    @Override
    public String getName() {
        return "zplot";
    }

    @Override
    public String getNiceName() {
        return "Zplot";
    }
        
    @Override
    public boolean areTreatmentStepsLabelled() {
        if (prefs==null) return false;
        else return prefs.getBoolean("plots.labelTreatmentSteps", false);
    }

    @Override
    public void draw(Graphics2D graphics) {
        final Sample sample = params.getSample();
        if (sample==null) {
            return;
        }
        final List<TreatmentStep> steps = sample.getVisibleTreatmentSteps();
        if (steps.isEmpty()) {
            return;
        }
        
        clearPoints();
        final Correction correction = params.getCorrection();
        final MeasurementAxis vProjXax = params.getVprojXaxis();
        final MeasurementAxis hProjXax = params.getHprojXaxis();
        final MeasurementAxis hProjYax = params.getHprojYaxis();
        
        final Rectangle2D extent1 =
                extent(steps, correction, hProjXax, hProjYax);
        final Rectangle2D extent2 =
                extent(steps, correction, vProjXax, MeasurementAxis.MINUSZ);
        final Rectangle2D axisDimensions =
                cropRectangle(getDimensions(), 250, 250, 200, 200);
                
        final MeasurementAxis[] hProjAxes = {
            hProjXax,
            hProjYax.opposite(),
            hProjXax.opposite(),
            hProjYax
        };
        
        axes = new ZplotAxes(extent1.createUnion(extent2), axisDimensions,
                vProjXax, hProjAxes, this);
        
        graphics.setColor(Color.BLACK);
        graphics.setStroke(getStroke());
        axes.draw(graphics);

        final double scale = axes.getScale();
        final double xOffset = axes.getXOffset();
        final double yOffset = axes.getYOffset();
        
        /*
         * We keep track of the points used for the PCA calculation in order to
         * calculate the length for the short-format PCA fit line.
         */
        final List<Point2D> pcaPointsH = new ArrayList<>(steps.size()+1);
        final List<Point2D> pcaPointsV = new ArrayList<>(steps.size()+1);

        
        boolean first = true;
        for (TreatmentStep step: steps) {
            final Vec3 v = step.getMoment(correction);
            // Plot the point in the horizontal plane
            final double x = xOffset + v.getComponent(hProjXax) * scale;
            final double y = yOffset - v.getComponent(hProjYax) * scale;
            final Point2D point = new Point2D.Double(x, y);
            addPoint(step, point, true, first, !first);
            if (step.isInPca()) {
                pcaPointsH.add(point);
            }
            first = false;
        }
        
        first = true;
        for (TreatmentStep step: steps) {
            Vec3 v = step.getMoment(correction);
            // Now plot the point in the vertical plane.
            final double x = xOffset + v.getComponent(vProjXax) * scale;
            final double y =
                    yOffset - v.getComponent(MeasurementAxis.MINUSZ) * scale;
            final Point2D point = new Point2D.Double(x, y);
            addPoint(step, point, false, first, !first);
            if (step.isInPca()) {
                pcaPointsV.add(point);
            }
            first = false;
        }
        
        final PcaValues pca = sample.getPcaValues();
        final String pcaStyle = prefs.get("plots.zplotPcaDisplay", "Long");
        final double lineScale = "Long".equals(pcaStyle) ? 0.9 : 1.0;
        if (pca != null && !"None".equals(pcaStyle)) {
            if (pca.isAnchored()) {
                final Point2D origin = new Point2D.Double(axes.getXOffset(),
                        axes.getYOffset());
                pcaPointsH.add(origin);
                pcaPointsV.add(origin);
            }
            /*
             * If the PCA line style is "Short", this clip rectangle will be
             * overwritten.
             */
            Rectangle2D clipRectangle = axes.getBounds();
            
            final double incRad = pca.getDirection().getIncRad();
            final double decRad = pca.getDirection().getDecRad();
            final double x1 = pca.getOrigin().getComponent(hProjXax) * scale;
            final double y1 = - pca.getOrigin().getComponent(hProjYax) * scale;
            if ("Short".equals(pcaStyle)) {
                clipRectangle = Util.envelope(pcaPointsH);
            }
            
            drawPcaLine(graphics, xOffset + x1, yOffset + y1,
                    transformDeclination(decRad, hProjAxes),
                    Color.BLUE, clipRectangle, lineScale);
            
            final double x2 = pca.getOrigin().getComponent(vProjXax) * scale;
            final double y2 = -pca.getOrigin().
                    getComponent(MeasurementAxis.MINUSZ) * scale;
            double incCorr = 0;

            switch (vProjXax) {
                /*
                 * We don't necessarily want the actual line of inclination; we
                 * want the projection of that line onto the appropriate plane.
                 */
                case X:
                    incCorr = atan(sin(incRad) / (cos(incRad) * cos(decRad)));
                    break;
                case Y:
                    incCorr = atan(sin(incRad) / (cos(incRad) * sin(decRad)));
                    break;
            }
            if (vProjXax == MeasurementAxis.X ||
                    vProjXax == MeasurementAxis.Y) {
                /*
                 * If we're plotting vertical projections vs. `H', there's no
                 * meaningful way to display the vertical component of the PCA:
                 * the projection plane is changing with every point so there is
                 * no meaningful plane onto which the PCA line can be projected.
                 */
                if ("Short".equals(pcaStyle)) {
                    clipRectangle = Util.envelope(pcaPointsV);
                }
                drawPcaLine(graphics, xOffset + x2, yOffset + y2,
                        Math.PI/2 + incCorr,
                        Color.BLUE, clipRectangle, lineScale);
            }
        }
        drawPoints(graphics);
    }

    /**
     * Projects a declination for plotting on non-standard axes.
     * 
     * If the declination can't be projected onto the provided axes,
     * Double.NaN will be returned.
     * 
     * @param dec the declination in radians
     * @param axes the axes on which it is to be plotted (right, down, left, up)
     * @return an angle in radians measured clockwise from vertical, which will
     * represent the declination on the specified axes
     */
    private double transformDeclination(double dec, MeasurementAxis[] axes) {
        assert(axes.length == 4);

        /*
         * First, find the North (==MeasurementAxis.X) axis, so we can add an
         * offset to plot the declination relative to it.
         */
        final int northIndex =
                java.util.Arrays.asList(axes).indexOf(MeasurementAxis.X);
        
        // If there's no North axis, give up.
        if (northIndex == -1) {
            return Double.NaN;
        }
        
        /*
         * Now try to find an East (==MeasurementAxis.Y) axis adjacent to the
         * North axis.
         */
        int direction = 0;
        if (axes[(northIndex+3)%4] == MeasurementAxis.Y) {
            direction = -1; // East axis anticlockwise of North axis
        } else if (axes[(northIndex+1)%4] == MeasurementAxis.Y) {
            direction = 1; // East axis anticlockwise of North axis
        } else {
            return Double.NaN;
        }
        
        /*
         * When calculating the final angle we need to add 1 to the north index
         * since the axes array starts with the right (positive-X) axis.
         */
        return ((Math.PI/2)*(northIndex+1) + direction*dec) % (2*Math.PI);
    }
    
    /**
     * Returns the legend for this plot.
     *
     * @return the legend for this plot
     */
    public ZplotLegend getLegend() {
        return legend;
    }

    /**
     * A legend for a Zijderveld plot. It shows the units for the axes, and
     * gives a key for the filled and unfilled points.
     */
    public class ZplotLegend extends Plot {

        /**
         * Creates a legend for the Zijderveld plot containing this class.
         *
         * @param parent the graph display containing the legend
         * @param params the parameters of the legend
         * @param prefs the preferences containing the legend configuration
         */
        private ZplotLegend(PlotParams params) {
            super(params);
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
        public void draw(Graphics2D graphics) {
            final Rectangle2D dims = getDimensions();
            clearPoints();
            final double xOrig =
                    dims.getMinX() + getMargin() + getUnitSize() * 50;
            final double yOrig = dims.getMinY() + getMargin();
            final double textOffs = 25 * getUnitSize();
            final double lineOffs = 150 * getUnitSize();

            graphics.setColor(Color.BLACK);
            addPoint(null, new Point2D.Double(xOrig, yOrig),
                    false, false, false);
            addPoint(null, new Point2D.Double(xOrig, yOrig + lineOffs),
                    true, false, false);
            writeString(graphics, "vertical",
                    (float) xOrig + 50 * getUnitSize(),
                    (float) (yOrig + textOffs));
            writeString(graphics, "horizontal",
                    (float) (xOrig + 50 * getUnitSize()),
                    (float) (yOrig + lineOffs + textOffs));
            final AttributedString units = axes != null
                    ? timesTenToThe("Units: A/m", axes.getMagnitude(), graphics)
                    : timesTenToThe("Units: A/m", "?", graphics);
            writeString(graphics, units, (float) xOrig,
                    (float) (yOrig + 2 * lineOffs + textOffs));
            drawPoints(graphics);
        }
    }
}
