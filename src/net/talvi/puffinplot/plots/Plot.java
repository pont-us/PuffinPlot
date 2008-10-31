package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;

public abstract class Plot
{
    protected final GraphDisplay parent;
    protected final PlotParams params;
    private Rectangle2D dimensions;
    private List<PlotPoint> points = new LinkedList<PlotPoint>();
    private static final Stroke stroke = new BasicStroke();

    public Rectangle2D getDimensions() {
        return dimensions;
    }

    public void setDimensions(Rectangle2D dimensions) {
        this.dimensions = dimensions;
    }
    
    private static class PlotPoint {

        private final Shape shape;
        private final Shape highlight;
        private final Datum datum;
        private final boolean filled;
        private final boolean lineToHere;
        private final boolean special;
        private final Point2D centre;
        private static final double highlightSize = 1.6;
        private static final double plotPointSize = 5;

        PlotPoint(Datum datum, Point2D centre,
                boolean filled, boolean lineToHere, boolean special) {
            double size = (plotPointSize / 2);
            this.centre = centre;
            this.datum = datum;
            shape = new Rectangle2D.Double(centre.getX() - size, centre.getY() - size, 2 * size, 2 * size);
            double hs = highlightSize;
            highlight = new Rectangle2D.Double(centre.getX() - size * hs, centre.getY() - size * hs,
                    2 * size * hs, 2 * size * hs);
            this.filled = filled;
            this.lineToHere = lineToHere;
            this.special = special;
        }

        void draw(Graphics2D g) {
            g.setColor(datum.selected ? Color.RED: Color.BLACK);
            g.draw(shape);
            if (special) g.draw(highlight);
            if (filled) g.fill(shape);
        }

        public Point2D getCentre() {
            return centre;
        }
    }
    
    public Plot(GraphDisplay parent, PlotParams params, Rectangle2D dimensions) {
        this.params = params;
        this.parent = parent;
        this.dimensions = dimensions;
    }
    
    protected void drawPoints(Graphics2D g) {
        g.setStroke(stroke);
        Point2D prev = null;
        for (int i=0; i<points.size(); i++) {
            points.get(i).draw(g);
            if (i > 0 && points.get(i).lineToHere) {
                g.setColor(Color.BLACK);
                g.draw(new Line2D.Double(prev, points.get(i).centre));
            }
            prev = points.get(i).centre;
        }
    }
        
    protected void addPoint(Datum d, Point2D p, boolean filled, boolean special, boolean line) {
        points.add(new PlotPoint(d, p, filled, line, special));
    }
    
    protected void clearPoints() {
        points.clear();
    }
    
    public void mouseClicked(java.awt.geom.Point2D position) {
        for (PlotPoint s : points) {
            if (s.shape.contains(position)) {
                s.datum.toggleSel();
            }
        }
    }
    
    public abstract void draw(Graphics2D g);
}
