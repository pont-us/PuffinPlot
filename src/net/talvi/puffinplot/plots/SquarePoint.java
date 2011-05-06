package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import net.talvi.puffinplot.data.Datum;

class SquarePoint implements PlotPoint {

    private final Shape shape;
    private final Shape highlight;
    private final Datum datum;
    private final boolean filled;
    private final boolean lineToHere;
    private final boolean special;
    private final Point2D centre;
    private static final double HIGHLIGHT_SCALE = 1.6;

    SquarePoint(Plot plot, Datum datum, Point2D centre, boolean filled,
            boolean lineToHere, boolean special, double size) {
        super();
        double s = size * plot.getUnitSize();
        this.centre = centre;
        this.datum = datum;
        shape = new Rectangle2D.Double(centre.getX() - s, centre.getY() - s,
                2 * s, 2 * s);
        final double hs = HIGHLIGHT_SCALE;
        highlight = new Rectangle2D.Double(centre.getX() - s * hs,
                centre.getY() - s * hs, 2 * s * hs, 2 * s * hs);
        this.filled = filled;
        this.lineToHere = lineToHere;
        this.special = special;
    }

    SquarePoint(Plot plot, Datum datum, Point2D centre, boolean filled,
            boolean lineToHere, boolean special) {
        this(plot, datum, centre, filled, lineToHere, special, Plot.PLOT_POINT_SIZE);
    }

    public void draw(Graphics2D g) {
        g.setColor(getDatum() != null && getDatum().isSelected() ? Color.RED : Color.BLACK);
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

    public Point2D getCentre() {
        return centre;
    }

    public Shape getShape() {
        return shape;
    }

    public boolean isNear(Point2D point, double distance) {
        return centre.distance(point) < distance;
    }
}
