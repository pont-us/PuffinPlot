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

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * An axis for a plot.
 * 
 * @author pont
 */
final class PlotAxis {
    
    /* Distance between axis and edge of axis label. */
    private static final int LABEL_PADDING = 23;
    
    private final Plot plot;
    private final AxisParameters ap;

    public PlotAxis(AxisParameters axisParameters, Plot plot) {
        ap = new AxisParameters(axisParameters);
        if (ap.stepSize == null) {
            ap.stepSize = calculateStepSize(ap.extent);
        }
        if (ap.numSteps == null) {
            ap.numSteps = calculateNumSteps(ap.extent, ap.stepSize);
        }
        if (ap.magnitude == null) {
            ap.magnitude = calculateMagnitude(getLength());
        }
        this.plot = plot;
    }

    static PlotAxis[] makeMatchingAxes(AxisParameters[] params, Plot plot) {
        final List<Double> stepSizes = new ArrayList<>(params.length);
        final List<Integer> magnitudes = new ArrayList<>(params.length);
        for (AxisParameters p: params)
            stepSizes.add(calculateStepSize(p.extent));
        double stepSize = Collections.max(stepSizes);
        for (AxisParameters p : params)
            magnitudes.add(
                    calculateMagnitude(roundUpToNextStep(p.extent, stepSize)));
        int magnitude = Collections.max(magnitudes);

        final List<PlotAxis> axes = new ArrayList<>(params.length);
        for (AxisParameters p : params) {
            AxisParameters newP = new AxisParameters(p);
            newP.stepSize = stepSize;
            newP.magnitude = magnitude;
            axes.add(new PlotAxis(newP, plot));
        }

        return axes.toArray(new PlotAxis[] {});
      }

    public final static class AxisParameters {
        public double extent = 1;
        public Direction direction = Direction.UP;
        public String label = null;
        public String endLabel = null;
        public boolean magnitudeOnTicks = false;
        public boolean magnitudeOnLabel = true;
        public Double stepSize = null;
        public Integer numSteps = null;
        public Integer magnitude = null;
        public Double markedPosition = null;
        // farside: true for an axis at top or right.
        // used to draw label on correct side.
        public boolean farSide = false;
        public boolean numberEachTick = false;
        public double startValue = 0;
        
        public AxisParameters(double extent, Direction direction) {
            this.extent = extent;
            this.direction = direction;
        }
        
        public AxisParameters(AxisParameters other) {
            extent = other.extent;
            direction = other.direction;
            label = other.label;
            endLabel = other.endLabel;
            magnitudeOnTicks = other.magnitudeOnTicks;
            magnitudeOnLabel = other.magnitudeOnLabel;
            stepSize = other.stepSize;
            numSteps = other.numSteps;
            magnitude = other.magnitude;
            markedPosition = other.markedPosition;
            farSide = other.farSide;
            numberEachTick = other.numberEachTick;
        }
        
        public AxisParameters withEndLabel(String endLabel) {
            this.endLabel = endLabel;
            return this;
        }
        
        public AxisParameters withLabel(String label) {
            this.label = label;
            return this;
        }
        
        public AxisParameters withNumberEachTick() {
            this.numberEachTick = true;
            return this;
        }
        
        public AxisParameters withStartValue(double startValue) {
            this.startValue = startValue;
            return this;
        }
        
    }

    private static double calculateStepSize(double extent) {
        final double scaleFactor =
                Math.pow(10, 1-Math.floor(Math.log10(extent)));
        final double extentScaledTo100 = extent * scaleFactor;
        final double scaledStepSize = calculateStepSizeForValueFrom0To100(
                Math.floor(extentScaledTo100));
        return scaledStepSize / scaleFactor;
    }

    /**
     *
     * @param length >0
     * @return x such that length * 10^x is in the range [1,1000).
     */
    private static int calculateMagnitude(final double length) {
        int nf = 0;
        while (length * Math.pow(10, nf) > 1000) {
            nf -= 3;
        }
        while (length * Math.pow(10, nf) < 1) {
            nf += 1;
        }
        return -nf;
    }

    private static int calculateNumSteps(double extent, double stepSize) {
        return (int) (Math.ceil(extent / stepSize));
    }

    private static double roundUpToNextStep(double extent, double stepSize) {
        double result = 0;
        while (result < extent) {
            result += stepSize;
        }
        return stepSize;
    }

    private static double calculateStepSizeForValueFrom0To100(double maxValue) {
        return maxValue < 12 ? 2 :
               maxValue < 40 ? 5 :
               maxValue <= 60 ? 10 :
                               20 ;
    }

    public void draw(Graphics2D graphics, double scale, int xOrig, int yOrig) {
        int x = 0, y = 0;
        double t = plot.getTickLength() / 2.0f;
        switch (ap.direction) {
            case RIGHT:
                x = 1;
                break;
            case DOWN:
                y = 1;
                break;
            case LEFT:
                x = -1;
                break;
            case UP:
                y = -1;
                break;
        }

        for (int i=1; i<=ap.numSteps; i++) {
            double pos = i * getStepSize() * scale;
            graphics.draw(new Line2D.Double(
                    xOrig + x * pos + y * t, yOrig + y * pos + x * t,
                    xOrig + x * pos - y * t, yOrig + y * pos - x * t));
            if (ap.numberEachTick || i==ap.numSteps) {
                double length =
                        getNormalizedLength() * (double) i / ap.numSteps;
                int length_int = (int) length;
                String text = Math.abs(length - length_int) < 0.0001
                        ? Integer.toString(length_int)
                        : String.format(Locale.ENGLISH, "%.1f", length);
                AttributedString as =
                        (ap.magnitudeOnTicks && getMagnitude() != 0)
                        ? plot.timesTenToThe(text, getMagnitude(), graphics)
                        : new AttributedString(text);
                plot.putText(graphics, as,
                        xOrig + x * pos, yOrig + y * pos,
                        ap.direction.labelPos(ap.farSide), 0, 5);
            }
        }
        
        final double xLen = x * getLength() * scale;
        final double yLen = y * getLength() * scale;
        graphics.draw(new Line2D.Double(xOrig, yOrig, xOrig+xLen, yOrig+yLen));
        if (getLength() != 0) {
            if (ap.markedPosition != null) {
                final AttributedString mark = new AttributedString(
                                String.format(Locale.ENGLISH, "%.2f",
                                        ap.markedPosition *
                                                Math.pow(10, -getMagnitude())));
                plot.putText(graphics, mark,
                        xOrig + x * ap.markedPosition * scale,
                        yOrig + y * ap.markedPosition * scale,
                        ap.direction.labelPos(!ap.farSide), 0, 5);
                final double pos = ap.markedPosition * scale;
                graphics.draw(new Line2D.Double(
                        xOrig + x * pos + y * t, yOrig + y * pos + x * t,
                        xOrig + x * pos - y * t, yOrig + y * pos - x * t));
            }
        }
        if (ap.label != null) {
            AttributedString as = (ap.magnitudeOnLabel && getMagnitude() != 0)
                    ? plot.timesTenToThe(ap.label, getMagnitude(), graphics)
                    : new AttributedString(ap.label);

            plot.putText(graphics, as, xOrig + xLen / 2, yOrig + yLen / 2,
                    ap.direction.labelPos(ap.farSide),
                    ap.direction.labelRot(), LABEL_PADDING);
        }
        
        if (ap.endLabel != null) {
            plot.putText(graphics, ap.endLabel, xOrig + xLen, yOrig + yLen,
                    ap.direction, 0, 8);
        }
    }

    public String getEndLabel() {
        return ap.endLabel;
    }

    public Direction getDirection() {
        return ap.direction;
    }
    
    public double getStepSize() {
        return ap.stepSize;
    }
        
    double getLength() {
        return ap.stepSize * ap.numSteps;
    }
    
    double getNormalizedLength() {
        return getLength() * Math.pow(10, -getMagnitude());
    }

    int getMagnitude() {
        return ap.magnitude;
    }
}
