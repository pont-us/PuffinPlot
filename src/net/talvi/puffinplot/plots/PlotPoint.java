package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import net.talvi.puffinplot.data.Datum;

public interface PlotPoint {

    void draw(Graphics2D g);

    void drawWithPossibleLine(Graphics2D g, PlotPoint prev);

    Datum getDatum();

    Shape getShape();

    Point2D getCentre();

    boolean isNear(Point2D point, double distance);

}
