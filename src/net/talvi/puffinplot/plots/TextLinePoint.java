package net.talvi.puffinplot.plots;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import net.talvi.puffinplot.data.Datum;

public class TextLinePoint implements PlotPoint {
    private final Plot plot;
    private final Datum datum;
    private final double yPos;
    private final List<String> values;
    private double xSpacing;
    private Rectangle2D bbox;
    private FontRenderContext frc;
    private List<TextLayout> layouts;
    private double xMin;

    public TextLinePoint(Plot plot, FontRenderContext frc, double yOffset, Datum d,
            List<String> values, double xSpacing) {
        this.plot = plot;
        this.datum = d;
        this.yPos = yOffset + plot.getDimensions().getMinY();
        this.values = values;
        this.xSpacing = xSpacing;
        this.frc = frc;

        double xPos = 10;
        xMin = plot.getDimensions().getMinX();
        layouts = new ArrayList<TextLayout>(values.size());
        for (String s : values) {
            AttributedString as = new AttributedString(s);
            plot.applyTextAttributes(as);
            TextLayout layout = new TextLayout(as.getIterator(), frc);
            double x = xMin + xPos;
            Rectangle2D b = layout.getBounds();
            Rectangle2D b2 = new Rectangle2D.Double(b.getMinX() + x,
                    b.getMinY() + yPos, b.getWidth(), b.getHeight());
            if (bbox == null) bbox = (Rectangle2D) b2.clone();
            else bbox.add(b2);
            layouts.add(layout);
            xPos += xSpacing;
        }
    }

    public void draw(Graphics2D g) {
        if (datum != null) {
            if (datum.isSelected()) plot.writeString(g, "‣", (float) xMin, (float) yPos);
            if (datum.isHidden()) plot.writeString(g, "-", (float) xMin, (float) yPos);
        }

        double x = xMin + 10;
        for (TextLayout l: layouts) {
            l.draw(g, (float) x, (float) yPos);
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
