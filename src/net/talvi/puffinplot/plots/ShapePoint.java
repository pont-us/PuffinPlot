/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStep;

import static java.lang.Math.sqrt;

/**
 * A graph point with a geometrical shape. Available shapes are
 * square, triangular, and circular.
 * 
 * @author pont
 */
class ShapePoint implements PlotPoint {

    private final Shape shape;
    private final Shape highlight;
    private final TreatmentStep treatmentStep;
    private final boolean filled;
    private final boolean lineToHere;
    private final boolean special;
    private final Point2D centre;
    private final Plot plot;
    private Direction labelPos = Direction.RIGHT;
    private static final double HIGHLIGHT_SCALE = 1.6;

    /**
     * @return the labelPos
     */
    public Direction getLabelPos() {
        return labelPos;
    }

    /**
     * @param labelPos the labelPos to set
     */
    public void setLabelPos(Direction labelPos) {
        this.labelPos = labelPos;
    }

    public static enum PointShape { 
        SQUARE, CIRCLE, TRIANGLE, DIAMOND;
        
        public static PointShape fromAmsAxis(int axis) {
            switch (axis) {
                case 0: return SQUARE;
                case 1: return TRIANGLE;
                default: return CIRCLE;
            }
        }
    }
    
    /** To deal with the large number of possible parameters,
     * we use the builder pattern. */
    public static class Builder {
        private final Point2D centre;
        private final Plot plot;
        private TreatmentStep treatmentStep = null;
        private boolean filled = false;
        private boolean lineToHere = false;
        private boolean special = false;
        private double size = Plot.PLOT_POINT_SIZE;
        private PointShape pointShape = PointShape.SQUARE;
        
        public Builder(Plot plot, Point2D centre) {
            this.plot = plot;
            this.centre = centre;
        }
        
        public Builder datum(TreatmentStep treatmentStep) {
            this.treatmentStep = treatmentStep;
            return this;
        }
        
        public Builder filled(boolean filled) {
            this.filled = filled;
            return this;
        }
        
        public Builder lineToHere(boolean lineToHere) {
            this.lineToHere = lineToHere;
            return this;
        }
        
        public Builder special(boolean special) {
            this.special = special;
            return this;
        }
        
        public Builder size(double size) {
            this.size = size;
            return this;
        }
        
        public Builder scale(double scale) {
            this.size = this.size * scale;
            return this;
        }
        
        public Builder pointShape(PointShape pointShape) {
            this.pointShape = pointShape;
            return this;
        }
        
        public Builder circle() {
            this.pointShape = PointShape.CIRCLE;
            return this;
        }
        
        public Builder triangle() {
            this.pointShape = PointShape.TRIANGLE;
            return this;
        }

        public Builder diamond() {
            this.pointShape = PointShape.DIAMOND;
            return this;
        }
        
        public ShapePoint build() {
            return new ShapePoint(this);
        }
    }
    
    public static Builder build(Plot plot, Point2D centre) {
        return new Builder(plot, centre);
    }
    
    private ShapePoint(Builder b) {
        this.plot= b.plot;
        double s = b.size * b.plot.getUnitSize();
        this.centre = b.centre;
        this.treatmentStep = b.treatmentStep;
        this.filled = b.filled;
        this.lineToHere = b.lineToHere;
        this.special = b.special;
        final double xo = centre.getX();
        final double yo = centre.getY();
        final double hs = s * HIGHLIGHT_SCALE;
        switch (b.pointShape) {
            case SQUARE:
                shape = new Rectangle2D.Double(xo - s, yo - s, 2 * s, 2 * s);
                highlight = new Rectangle2D.Double(xo - hs, yo - hs,
                        2 * hs, 2 * hs);
                break;
            case TRIANGLE:
                shape = makeTriangle(s, xo, yo);
                highlight = makeTriangle(hs, xo, yo);
                break;
            case CIRCLE:
                shape = new Ellipse2D.Double(xo-s, yo-s, s*2, s*2);
                highlight = new Ellipse2D.Double(xo-hs, yo-hs, hs*2, hs*2);
                break;
            case DIAMOND:
                shape = makeDiamond(s, xo, yo);
                highlight = makeDiamond(hs, xo, yo);
                break;
            default:
                throw new IllegalArgumentException("Unknown point shape: "+
                        b.pointShape.toString());
        }
    }
    
    private static Shape makeTriangle(double s, double xo, double yo) {
        final Path2D path = new Path2D.Double();
        path.moveTo(xo - s, yo + s * (1/sqrt(3)));
        path.lineTo(xo + s, yo + s * (1/sqrt(3)));
        path.lineTo(xo, yo - s * (2/sqrt(3)));
        path.closePath();
        return path;
    }
    
    private static Shape makeDiamond(double s, double xo, double yo) {
        // We wish the sides of the dimand to have length 2s, so
        // the diagonal needs to have length 2s*sqrt(2).
        s *= Math.sqrt(2.);
        final Path2D path = new Path2D.Double();
        path.moveTo(xo-s, yo);
        path.lineTo(xo, yo+s);
        path.lineTo(xo+s, yo);
        path.lineTo(xo, yo-s);
        path.closePath();
        return path;
    }
    
    @Override
    public void draw(Graphics2D g) {
        g.setStroke(plot.getStroke());
        if (treatmentStep != null) {
            g.setColor(getTreatmentStep().isSelected() ? Color.RED : Color.BLACK);
        }

        g.draw(shape);
        if (special) {
            g.draw(highlight);
        }
        if (filled) {
            g.fill(shape);
        }
    }

    @Override
    public void drawWithPossibleLine(Graphics2D g, PlotPoint prev,
            boolean annotate) {
        draw(g);
        g.setColor(Color.BLACK);
        if (lineToHere && prev != null) {
            g.draw(new Line2D.Double(prev.getCentre(), centre));
        }
        if (annotate && treatmentStep != null) {
            double pad = plot.getFontSize() / 3;
            final String label = treatmentStep.getFormattedTreatmentLevel();
            plot.putText(g, label, centre.getX(), centre.getY(), getLabelPos(),
                    0, pad);
        }
    }

    @Override
    public TreatmentStep getTreatmentStep() {
        return treatmentStep;
    }
    
    /* For the present, only TextLinePoints can have samples. */
    @Override
    public Sample getSample() {
        return null;
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public Point2D getCentre() {
        return centre;
    }

    @Override
    public boolean isNear(Point2D point, double distance) {
        return centre.distance(point) < distance;
    }
}
