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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import net.talvi.puffinplot.data.MedianDestructiveField;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.plots.PlotAxis.AxisParameters;

/**
 * Demagnetization plot. This is a simple biplot with demagnetization step on
 * the x axis and magnetic moment intensity on the y axis. When magnetic
 * susceptibility data is available, it is also overlaid on the plot.
 *
 * @author pont
 */
public class DemagPlot extends Plot {

    /**
     * Creates a demagnetization plot with the supplied parameters
     *
     * @param params the parameters of the plot
     */
    public DemagPlot(PlotParams params) {
        super(params);
    }

    /**
     * Returns this plot's internal name.
     *
     * @return this plot's internal name
     */
    @Override
    public String getName() {
        return "demag";
    }

    /**
     * Returns this plot's user-friendly name.
     *
     * @return this plot's user-friendly name
     */
    @Override
    public String getNiceName() {
        return "Demag.";
    }
    
    /**
     * Draws this plot.
     *
     * @param g the graphics object to which to draw the plot
     */
    @Override
    public void draw(Graphics2D g) {
        clearPoints();
        
        final Sample sample = params.getSample();
        if (sample == null) {
            return;
        }
        
        final List<TreatmentStep> steps = sample.getVisibleTreatmentSteps();
        if (steps.isEmpty()) {
            return;
        }

        g.setColor(Color.BLACK);
        final double maxDemag = TreatmentStep.maxTreatmentLevel(steps);

        /*
         * If all the measurements have the same demag level, we'll just plot
         * them in sequence to avoid giving them all the same x co-ordinate.
         */
        boolean xBySequence = false;

        double xAxisLength;
        if (maxDemag == 0) {
            xAxisLength = steps.size() > 1 ? steps.size() : 1;
            xBySequence = true;
        } else {
            xAxisLength = maxDemag;
        }
        
        final TreatmentType treatmentType =
                sample.getTreatmentStepByIndex(sample.getNumberOfSteps() - 1).
                        getTreatmentType();
        final String xAxisLabel;
        double demagRescale = 1;
        if (xBySequence) {
            xAxisLabel = "Measurement number";
        } else {
            String unit = treatmentType.getUnit();
            if (treatmentType.isMagneticField()) {
                unit = "m" + unit;
                demagRescale = 1000;
            }
            xAxisLabel = String.format(Locale.ENGLISH,
                    "%s (%s)", treatmentType.getAxisLabel(), unit);
        }
        
        final AxisParameters xAxisParams = 
                new AxisParameters(xAxisLength * demagRescale, Direction.RIGHT).
                withLabel(xAxisLabel).withNumberEachTick().withTickAtZero();
        
        final MedianDestructiveField midpoint = sample.getMdf();
        if (midpoint != null && midpoint.isHalfIntReached()) {
            xAxisParams.markedPosition =
                    midpoint.getDemagLevel() * demagRescale;
        }
        
        final PlotAxis xAxis = new PlotAxis(xAxisParams, this);
        final AxisParameters yAxisParameters =
                new AxisParameters(correctedMaxIntensity(steps), Direction.UP).
                withLabel(vAxisLabel()).withNumberEachTick();
        /*
         * If there's mag sus data, we omit the tick (and, more importantly,
         * the label) at zero to make room for a legend point making it clear
         * which point style represents remanence.
         */
        yAxisParameters.hasTickAtZero = !sample.hasMagSusData();
        final PlotAxis yAxis = new PlotAxis(yAxisParameters, this);

        final Rectangle2D dim = cropRectangle(getDimensions(),
                320, 320, 50, 290);
        final double xScale = dim.getWidth() / xAxis.getLength();
        final double yScale = dim.getHeight() / yAxis.getLength();
        yAxis.draw(g, yScale, (int) dim.getMinX(), (int) dim.getMaxY());
        xAxis.draw(g, xScale, (int) dim.getMinX(), (int) dim.getMaxY());
        
        
        int i = 0;
        for (TreatmentStep step: steps) {
            final double demagLevel = step.getTreatmentLevel() * demagRescale;
            final double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : demagLevel) * xScale;
            addPoint(step, new Point2D.Double(xPos,
                    dim.getMaxY() - step.getIntensity() * yScale),
                    true, false, i > 0);
            i++;
        }

        if (midpoint != null && xAxisParams.markedPosition != null) {
            final double xPos =
                    dim.getMinX() + xScale * xAxisParams.markedPosition;
            final double yPos =
                    dim.getMaxY() - midpoint.getIntensity() * yScale;
            g.draw(new Line2D.Double(dim.getMinX(), yPos,
                    xPos, yPos));
            g.draw(new Line2D.Double(xPos, dim.getMaxY() - getFontSize() * 1.5,
                    xPos, yPos));
        }

        if (sample.hasMagSusData()) {
            /*
             * Add legend points on the left and right axes to show which
             * point type is associated with which data set.
             */
            addPoint(null,
                    new Point2D.Double(dim.getMinX() - 10, dim.getMaxY()),
                    true, false, false);
            addPoint(null,
                    new Point2D.Double(dim.getMaxX() + 10, dim.getMaxY()),
                    false, false, false);
            
            final PlotAxis msAxis = makeMagSusAxis(steps);
            final double msScale = dim.getHeight() / msAxis.getLength();
            msAxis.draw(g, msScale, (int) dim.getMaxX(), (int) dim.getMaxY());
            i = 0;
            boolean first = true;
            for (TreatmentStep step: steps) {
                final double demagLevel =
                        step.getTreatmentLevel() * demagRescale;
                final double xPos = dim.getMinX() +
                    (xBySequence ? (i + 1) : demagLevel) * xScale;
                if (step.hasMagSus()) {
                    double magSus = step.getMagSus();
                    if (magSus < 0) {
                        magSus = 0;
                    }
                    addPoint(step, new Point2D.Double(xPos,
                            dim.getMaxY() - magSus * msScale),
                            false, false, !first);
                    first = false;
                }
                i++;
            }
        }
        drawPoints(g);
    }

    private String vAxisLabel() {
        return params.getSetting("plots." + getName() + ".vAxisLabel",
                "Magnetization (A/m)");
    }

    /**
     * @param steps some treatment steps
     * @return the maximum magnetic intensity of the steps if this value is not
     *   0; 1 if the maximum intensity is 0
     */
    private static double correctedMaxIntensity(
            final Collection<TreatmentStep> steps) {
        double maxIntens = TreatmentStep.maxIntensity(steps);
        if (maxIntens == 0) {
            maxIntens = 1;
        }
        return maxIntens;
    }

    private PlotAxis makeMagSusAxis(Collection<TreatmentStep> data) {
        double maxMagSus = TreatmentStep.maxMagSus(data);
        
        if (maxMagSus <= 0) {
            maxMagSus = 1;
        }
        final AxisParameters msAxisParams =
                new AxisParameters(maxMagSus, Direction.UP).
                        withNumberEachTick();
        msAxisParams.label = "Mag. sus. (S.I.)";
        msAxisParams.farSide = true;
        final PlotAxis msAxis = new PlotAxis(msAxisParams, this);
        return msAxis;
    }
}
