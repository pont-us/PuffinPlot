package net.talvi.puffinplot.plots;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.talvi.puffinplot.data.Datum;

/**
 * <p>A <q>data point</q> which actually consists of a line of text.</p>
 * 
 * @author pont
 */
class TextLinePoint implements PlotPoint {

    private final Plot plot;
    private final Datum datum;
    private final double yPos;
    private List<Double> xSpacing;
    private Rectangle2D bbox;
    private final List<AttributedCharacterIterator> strings;
    private final double xMin;

    public TextLinePoint(Plot plot, Graphics2D g, double yOffset, Datum d,
            List<String> values, List<Double> xSpacing) {
        this.plot = plot;
        this.datum = d;
        this.yPos = yOffset + plot.getDimensions().getMinY();
        this.xSpacing = xSpacing;
        this.strings = new ArrayList<AttributedCharacterIterator>(values.size());
        double xPos = 10;
        xMin = plot.getDimensions().getMinX();
        final FontMetrics metrics = g.getFontMetrics();
        for (int i=0; i<values.size(); i++) {
            final String s = values.get(i);
            final double space = xSpacing.get(i);
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
            xPos += space;
        }
    }

    public void draw(Graphics2D g) {
        double us = plot.getUnitSize();
        xSpacing =
            Arrays.asList(360*us, 420*us, 420*us, 620*us, 580*us);
        final FontMetrics fontMetrics = g.getFontMetrics();
        if (datum != null) {
            if (datum.isSelected()) {
                plot.writeString(g, "*", (float) xMin, (float) yPos);
            }
            if (datum.isHidden()) {
                plot.writeString(g, "-", (float) xMin, (float) yPos);
            }
        }

        double x = xMin + 10;
        for (int i=0; i<strings.size(); i++) {
            final AttributedCharacterIterator s = strings.get(i);
            final Rectangle2D bounds =
                    fontMetrics.getStringBounds(s, s.getBeginIndex(), s.getEndIndex(), g);
            final double space = xSpacing.get(i);
            final double xOffset = space - bounds.getWidth();
            g.drawString(s, (float) (x + xOffset), (float) yPos);
            x += space;
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
