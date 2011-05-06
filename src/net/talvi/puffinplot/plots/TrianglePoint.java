package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import net.talvi.puffinplot.data.Datum;
import static java.lang.Math.sqrt;

public class TrianglePoint implements PlotPoint {

    private final Shape shape;
    private final Point2D centre;

    TrianglePoint(Plot plot, Datum datum, Point2D centre, double size) {
        super();
        this.centre = centre;
        GeneralPath path = new GeneralPath();
        double m = size * plot.getUnitSize();
        double xo = centre.getX();
        double yo = centre.getY();
        path.moveTo(xo - m, yo + m * (1/sqrt(3)));
        path.lineTo(xo + m, yo + m * (1/sqrt(3)));
        path.lineTo(xo, yo - m * (2/sqrt(3)));
        path.closePath();
        shape = path;
    }

    TrianglePoint(Plot plot, Datum datum, Point2D centre) {
            this(plot, datum, centre, Plot.PLOT_POINT_SIZE);
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.draw(shape);
        // g.setColor(Color.DARK_GRAY);
        g.fill(shape);
    }

    public void drawWithPossibleLine(Graphics2D g, PlotPoint prev) {
        // TODO: implement properly!
        draw(g);
    }

    public Datum getDatum() {
        return null;
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
