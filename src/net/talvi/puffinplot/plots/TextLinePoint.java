package net.talvi.puffinplot.plots;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import net.talvi.puffinplot.data.Datum;

public class TextLinePoint implements PlotPoint {

    private final Plot plot;
    private final Datum datum;
    private final double yPos;
    private double xSpacing;
    private Rectangle2D bbox;
    private final List<AttributedCharacterIterator> strings;
    private final double xMin;

    public TextLinePoint(Plot plot, Graphics2D g, double yOffset, Datum d,
            List<String> values, double xSpacing) {
        this.plot = plot;
        this.datum = d;
        this.yPos = yOffset + plot.getDimensions().getMinY();
        this.xSpacing = xSpacing;
        this.strings = new ArrayList<AttributedCharacterIterator>(values.size());
        double xPos = 10;
        xMin = plot.getDimensions().getMinX();
        final FontMetrics metrics = g.getFontMetrics();
        for (String s : values) {
            AttributedString as = new AttributedString(s);
            plot.applyTextAttributes(as);
            AttributedCharacterIterator ai = as.getIterator();
            strings.add(ai);
            double x = xMin + xPos;
            Rectangle2D b = metrics.getStringBounds(ai, 0, s.length(), g);
            Rectangle2D b2 = new Rectangle2D.Double(b.getMinX() + x,
                    b.getMinY() + yPos, b.getWidth(), b.getHeight());
            if (bbox == null) {
                bbox = (Rectangle2D) b2.clone();
            } else {
                bbox.add(b2);
            }
            xPos += xSpacing;
        }
    }

    public void draw(Graphics2D g) {
        if (datum != null) {
            if (datum.isSelected()) {
                plot.writeString(g, "π"/*"‣"*/, (float) xMin, (float) yPos);
            }
            if (datum.isHidden()) {
                plot.writeString(g, "-", (float) xMin, (float) yPos);
            }
            //if (datum.isOnCircle()) {
             //   plot.writeString(g, "*", (float) xMin+40, (float) yPos);
            //}
        }

        double x = xMin + 10;
        for (AttributedCharacterIterator s : strings) {
            g.drawString(s, (float) x, (float) yPos);
            x += xSpacing;
        }
    }

    public void drawWithPossibleLine(Graphics2D g, PlotPoint prev) {
        draw(g);
    }

    public Datum getDatum() {
        return datum;
    }

    public Shape getShape() {
        return bbox;
    }

    public Point2D getCentre() {
        return new Point2D.Double(bbox.getCenterX(), bbox.getCenterY());
    }

    public boolean isNear(Point2D point, double distance) {
        return bbox.contains(point);
    }
}
