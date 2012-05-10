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

import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An axis for a plot.
 * 
 * @author pont
 */
final class PlotAxis {
    private final Plot plot;
    private final AxisParameters ap;

    static enum Direction {
        RIGHT("R","E", 0), UP("U","N", 1),
        LEFT("L","W", 2), DOWN("D","S", 3);
        
        private final String letter;
        private final String compassDir;
        private final int position;
        private final static Direction[] ordering = new Direction[4];

        static {
            for (Direction d: values()) ordering[d.position] = d;
        }

        private Direction(String letter, String compassDir, int position) {
            this.letter = letter;
            this.compassDir = compassDir;
            this.position = position;
        }
        
        boolean isHorizontal() {
            return (position % 2) == 0;
        }
        
        Direction labelPos(boolean farSide) {
            final Direction d = this.isHorizontal() ? DOWN : LEFT;
            return farSide ? d.opposite() : d;
        }
        
        Direction rotAcw90() {
            return ordering[(position + 1) % 4];
        }

        public Direction opposite() {
            return ordering[(position + 2) % 4];
        }

        double labelRot() {
            return this.isHorizontal() ? 0 : -Math.PI/2;
        }

        public String getCompassDir() {
            return compassDir;
        }

        public String getLetter() {
            return letter;
        }
    }

    public PlotAxis(AxisParameters axisParameters, Plot plot) {
        ap = new AxisParameters(axisParameters);
        if (ap.stepSize == null) ap.stepSize = calculateStepSize(ap.extent);
        if (ap.numSteps == null) ap.numSteps = calculateNumSteps(ap.extent, ap.stepSize);
        if (ap.magnitude == null) ap.magnitude = calculateMagnitude(getLength());
        this.plot = plot;
    }

    static PlotAxis[] makeMatchingAxes(AxisParameters[] params, Plot plot) {
        List<Double> stepSizes = new ArrayList<Double>(params.length);
        List<Integer> magnitudes = new ArrayList<Integer>(params.length);
        for (AxisParameters p: params)
            stepSizes.add(calculateStepSize(p.extent));
        double stepSize = Collections.max(stepSizes);
        for (AxisParameters p : params)
            magnitudes.add(calculateMagnitude(roundUpToNextStep(p.extent, stepSize)));
        int magnitude = Collections.max(magnitudes);

        List<PlotAxis> axes = new ArrayList<PlotAxis>(params.length);
        for (AxisParameters p: params) {
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

            public AxisParameters(AxisParameters p) {
                extent = p.extent;
                direction = p.direction;
                label = p.label;
                endLabel = p.endLabel;
                magnitudeOnTicks = p.magnitudeOnTicks;
                magnitudeOnLabel = p.magnitudeOnLabel;
                stepSize = p.stepSize;
                numSteps = p.numSteps;
                magnitude = p.magnitude;
                markedPosition = p.markedPosition;
                farSide = p.farSide;
                numberEachTick = p.numberEachTick;
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
        // if (extent==0) extent=1;
        double scaleFactor = Math.pow(10, 1-Math.floor(Math.log10(extent)));
        double extentScaledTo100 = extent * scaleFactor;
        double scaledStepSize =
                calculateStepSizeForValueFrom0To100(Math.floor(extentScaledTo100));
        return scaledStepSize / scaleFactor;
    }

    /**
     *
     * @param length >0
     * @return x such that length * 10^x is in the range [1,1000).
     */
    private static int calculateMagnitude(final double length) {
        int nf = 0;
        while (length * Math.pow(10, nf) > 1000) nf -= 3;
        while (length * Math.pow(10, nf) < 1) nf += 1;
        return -nf;
    }

    private static int calculateNumSteps(double extent, double stepSize) {
        return (int) (Math.ceil(extent/stepSize));
    }

    private static double roundUpToNextStep(double extent, double stepSize) {
        double result = 0;
        while (result < extent) result += stepSize;
        return stepSize;
    }

    private static double calculateStepSizeForValueFrom0To100(double maxValue) {
        return maxValue < 12 ? 2 :
               maxValue < 40 ? 5 :
               maxValue <= 60 ? 10 :
                               20 ;
    }

    private void putText(Graphics2D g, String textString, double x,
            double y, Direction dir, double θ, double padding) {
        AttributedString text = new AttributedString(textString);
        putText(g, text, x, y, dir, θ, padding);
    }

    private void putText(Graphics2D g, AttributedString text, double x,
            double y, Direction dir, double θ, double padding) {
        plot.applyTextAttributes(text);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout layout = new TextLayout(text.getIterator(), frc);
        Rectangle2D bounds = AffineTransform.getRotateInstance(θ).
                createTransformedShape(layout.getBounds()).getBounds2D();
        double w2 = bounds.getWidth()/2;
        double h2 = bounds.getHeight()/2;
        x -= w2;
        y += h2;
        w2 += padding;
        h2 += padding;
        switch (dir) {
        case RIGHT: x += w2; break;
        case DOWN: y += h2; break;
        case LEFT: x -= w2; break;
        case UP: y -= h2; break;
        }

        // Shape bbMoved = AffineTransform.getTranslateInstance(x, y).createTransformedShape(bounds);
        AffineTransform old = g.getTransform();
        g.translate(x - bounds.getMinX(), y - bounds.getMaxY());
        g.rotate(θ);
        // Don't use layout.draw, since that will draw the text as a glyph
        // vector, which won't be exported as text in SVG, PDF, etc.
        g.drawString(text.getIterator(), 0, 0);
        g.setTransform(old);
    }

    public void draw(Graphics2D g, double scale, int xOrig, int yOrig) {
        int x = 0, y = 0;
        double t = plot.getTickLength() / 2.0f;
        switch (ap.direction) {
        case RIGHT: x = 1; break;
        case DOWN: y = 1; break;
        case LEFT: x = -1; break;
        case UP: y = -1; break;
        }

        for (int i=1; i<=ap.numSteps; i++) {
            double pos = i*getStepSize()*scale;
            g.draw(new Line2D.Double(xOrig+x*pos+y*t, yOrig+y*pos+x*t,
                    xOrig+x*pos-y*t, yOrig+y*pos-x*t));
            if (ap.numberEachTick || i==ap.numSteps) {
                double length = getNormalizedLength() * (double) i / ap.numSteps;
                int length_int = (int) length;
                String text = Math.abs(length - length_int) < 0.0001
                        ? Integer.toString(length_int)
                        : String.format("%.1f", length);
                AttributedString as = (ap.magnitudeOnTicks && getMagnitude() != 0)
                    ? plot.timesTenToThe(text, getMagnitude())
                    : new AttributedString(text);
                putText(g, as,
                        xOrig + x * pos, yOrig + y * pos,
                        ap.direction.labelPos(ap.farSide), 0, 5);
            }
        }
        
        double xLen = x*getLength()*scale;
        double yLen = y*getLength()*scale;
        g.draw(new Line2D.Double(xOrig, yOrig, xOrig+xLen, yOrig+yLen));
        if (getLength()!=0) {
            if (ap.markedPosition != null) {
                AttributedString mark = new AttributedString
                        (String.format("%.2f", ap.markedPosition * Math.pow(10, -getMagnitude())));
                putText(g, mark,
                        xOrig + x * ap.markedPosition * scale,
                        yOrig + y * ap.markedPosition * scale,
                        ap.direction.labelPos(!ap.farSide), 0, 5);
                double pos = ap.markedPosition * scale;
                g.draw(new Line2D.Double(xOrig+x*pos+y*t, yOrig+y*pos+x*t,
                    xOrig+x*pos-y*t, yOrig+y*pos-x*t));
            }
        }
        if (ap.label != null) {
            AttributedString as = (ap.magnitudeOnLabel && getMagnitude() != 0)
                    ? plot.timesTenToThe(ap.label, getMagnitude())
                    : new AttributedString(ap.label);

            putText(g, as, xOrig + xLen / 2, yOrig + yLen / 2,
                    ap.direction.labelPos(ap.farSide), ap.direction.labelRot(), 22);
        }
        
        if (ap.endLabel != null) {
            putText(g, ap.endLabel, xOrig+xLen, yOrig+yLen, ap.direction, 0, 8);
        }
    }

    public String getEndLabel() {
        return ap.endLabel;
    }

    public PlotAxis.Direction getDirection() {
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