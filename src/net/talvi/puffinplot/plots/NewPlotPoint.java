/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot.plots;

import java.awt.geom.Line2D;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.data.Datum;
import static java.lang.Math.sqrt;

public class NewPlotPoint implements PlotPoint {

    private final Shape shape;
    private final Shape highlight;
    private final Datum datum;
    private final boolean filled;
    private final boolean lineToHere;
    private final boolean special;
    private final Point2D centre;
    private final Plot plot;
    private static final double HIGHLIGHT_SCALE = 1.6;

    public enum PointShape { 
        SQUARE, CIRCLE, TRIANGLE;
        
        public static PointShape fromAmsAxis(int axis) {
            switch (axis) {
                case 0: return SQUARE;
                case 1: return TRIANGLE;
                default: return CIRCLE;
            }
        }
    }
    
    public static class Builder {
        
        private final Point2D centre;
        private final Plot plot;
        private Datum datum = null;
        private boolean filled = false;
        private boolean lineToHere = false;
        private boolean special = false;

        private double size = Plot.PLOT_POINT_SIZE;
        private PointShape pointShape = PointShape.SQUARE;
        
        public Builder(Plot plot, Point2D centre) {
            this.plot = plot;
            this.centre = centre;
        }
        
        public Builder datum(Datum datum) {
            this.datum = datum;
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
        
        public NewPlotPoint build() {
            return new NewPlotPoint(this);
        }
    }
    
    public static Builder build(Plot plot, Point2D centre) {
        return new Builder(plot, centre);
    }
    
    private NewPlotPoint(Builder b) {
        this.plot= b.plot;
        double s = b.size * b.plot.getUnitSize();
        this.centre = b.centre;
        this.datum = b.datum;
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
            default:
                throw new IllegalArgumentException("Unknown point shape: "+
                        b.pointShape.toString());
        }
    }
    
    private Shape makeTriangle(double s, double xo, double yo) {
        GeneralPath path = new GeneralPath();
        path.moveTo(xo - s, yo + s * (1/sqrt(3)));
        path.lineTo(xo + s, yo + s * (1/sqrt(3)));
        path.lineTo(xo, yo - s * (2/sqrt(3)));
        path.closePath();
        return path;
    }
    
    public void draw(Graphics2D g) {
        g.setStroke(plot.getStroke());
        g.setColor(getDatum() != null && getDatum().isSelected() ?
                Color.RED : Color.BLACK);
        g.draw(shape);
        if (special) g.draw(highlight);
        if (filled) g.fill(shape);
    }

    public void drawWithPossibleLine(Graphics2D g, PlotPoint prev) {
        draw(g);
        g.setColor(Color.BLACK);
        if (lineToHere && prev != null) {
            g.draw(new Line2D.Double(prev.getCentre(), centre));
        }
    }

    public Datum getDatum() {
        return datum;
    }

    public Shape getShape() {
        return shape;
    }

    public Point2D getCentre() {
        return centre;
    }

    public boolean isNear(Point2D point, double distance) {
        return centre.distance(point) < distance;
    }
    
}
