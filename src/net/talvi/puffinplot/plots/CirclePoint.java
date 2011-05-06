package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import net.talvi.puffinplot.data.Datum;

public class CirclePoint implements PlotPoint {

    private final Shape shape;
    private final Point2D centre;

    CirclePoint(Plot plot, Datum datum, Point2D centre, double size) {
        super();
        this.centre = centre;
        double m = size * plot.getUnitSize();
        double xo = centre.getX();
        double yo = centre.getY();
        shape = new Ellipse2D.Double(xo-m, yo-m, m*2, m*2);
    }

    CirclePoint(Plot plot, Datum datum, Point2D centre) {
        this(plot, datum, centre, Plot.PLOT_POINT_SIZE);
    }

    public void draw(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.draw(shape);
        //g.setColor(Color.LIGHT_GRAY);
        g.fill(shape);
    }

    public void drawWithPossibleLine(Graphics2D g, PlotPoint prev) {
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
