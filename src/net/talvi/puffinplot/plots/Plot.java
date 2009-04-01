package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.HashMap;
import static java.text.AttributedCharacterIterator.Attribute;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.GraphDisplay;
import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.data.Datum;

public abstract class Plot
{
    private final GraphDisplay parent;
    protected final PlotParams params;
    protected Rectangle2D dimensions;
    private List<PlotPoint> points = new LinkedList<PlotPoint>();
    private static final float UNIT_SCALE = (float) 0.0001f;
    private Stroke stroke;
    private float unitSize;
    private static final float TICK_LENGTH_IN_UNITS = 48.0f;
    private static final float FONT_SIZE_IN_UNITS = 100.0f;
    private Map<Attribute,Object> attributeMap
     = new HashMap<Attribute, Object>();

    protected static final String DEFAULT_PLOT_POSITIONS =
            "zplot 407 32 610 405 pcatable 518 708 195 67 " +
            "sampletable 24 13 215 39 fishertable 837 60 155 60 " +
            "datatable 43 324 349 441 demag 50 69 323 213 " +
            "equarea 685 439 338 337";

    public Rectangle2D getDimensions() {
        return dimensions;
    }

    public void setDimensions(Rectangle2D dimensions) {
        this.dimensions = dimensions;
    }

    public void setDimensionsToDefault() {
        this.dimensions = dimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
    }

    public int getMargin() {
        return 24;
    }

    public Stroke getStroke() {
        return stroke;
    }
    
    public float getUnitSize() {
        return unitSize;
    }

    public float getTickLength() {
        return TICK_LENGTH_IN_UNITS * getUnitSize();
    }

    public float getFontSize() {
        return FONT_SIZE_IN_UNITS * getUnitSize();
    }

    public Map<? extends Attribute,?>
            getTextAttributes() {
        return attributeMap;
    }

    protected void writeString(Graphics2D g, String text, float x, float y) {
        AttributedString as = new AttributedString(text);
        as.addAttributes(getTextAttributes(), 0, text.length());
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout layout = new TextLayout(as.getIterator(), frc);
        layout.draw(g, x, y);
    }
    
    private class PlotPoint {

        private final Shape shape;
        private final Shape highlight;
        private final Datum datum;
        private final boolean filled;
        private final boolean lineToHere;
        private final boolean special;
        private final Point2D centre;
        private static final double HIGHLIGHT_SCALE = 1.6;
        private static final double PLOT_POINT_SIZE = 24;

        PlotPoint(Datum datum, Point2D centre,
                boolean filled, boolean lineToHere, boolean special) {
            double size = PLOT_POINT_SIZE * getUnitSize();
            this.centre = centre;
            this.datum = datum;
            shape = new Rectangle2D.Double(centre.getX() - size,
                    centre.getY() - size, 2 * size, 2 * size);
            final double hs = HIGHLIGHT_SCALE;
            highlight = new Rectangle2D.Double(centre.getX() - size * hs,
                    centre.getY() - size * hs,
                    2 * size * hs, 2 * size * hs);
            this.filled = filled;
            this.lineToHere = lineToHere;
            this.special = special;
        }

        void draw(Graphics2D g) {
            g.setColor(datum!=null && datum.isSelected() ? Color.RED: Color.BLACK);
            g.draw(shape);
            if (special) g.draw(highlight);
            if (filled) g.fill(shape);
        }

        public Point2D getCentre() {
            return centre;
        }
    }
    
    public Plot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        this.params = params;
        this.parent = parent;
        String sizesString = DEFAULT_PLOT_POSITIONS;
        if (prefs != null) sizesString = prefs.get("plotSizes", DEFAULT_PLOT_POSITIONS);
        this.dimensions = dimensionsFromPrefsString(sizesString);
        float maxDim = 800;
        if (parent != null) {
            Dimension dims = parent.getMaximumSize();
            maxDim = (float) Math.max(dims.getWidth(), dims.getHeight());
        }
        unitSize = maxDim * UNIT_SCALE;
        stroke = new BasicStroke(getUnitSize() * 8);
        // TODO fix this; parent should never be null (see FisherEqAreaPlot).

        attributeMap.put(TextAttribute.FAMILY, "SansSerif");
        attributeMap.put(TextAttribute.SIZE, getFontSize());

    }

    private Rectangle2D dimensionsFromPrefsString(String spec) {
        final Scanner scanner = new Scanner(spec);
        while (scanner.hasNext()) {
            String plotName = scanner.next();
            if (getName().equals(plotName)) {
                return
                    new Rectangle2D.Double(scanner.nextDouble(),
                    scanner.nextDouble(), scanner.nextDouble(),
                    scanner.nextDouble());
            } else {
                for (int i=0; i<4; i++) scanner.next();
            }
        }
        return null;
    }

    public String dimensionsAsString() {
        Rectangle2D r = dimensions;
        return String.format("%f %f %f %f ",
                r.getMinX(), r.getMinY(),
                r.getWidth(), r.getHeight());
    }

    protected void drawPoints(Graphics2D g) {
        g.setStroke(getStroke());
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
        
    protected void addPoint(Datum d, Point2D p, boolean filled,
            boolean special, boolean line) {
        points.add(new PlotPoint(d, p, filled, line, special));
    }
    
    protected void clearPoints() {
        points.clear();
    }
    
    public void mouseClicked(java.awt.geom.Point2D position, MouseEvent e) {
        if (e.isShiftDown()) {
            for (PlotPoint s : points) {
                if (s.centre.distance(position) < 24)
                    s.datum.setSelected(e.getButton() == MouseEvent.BUTTON1);
            }
        } else {
            for (PlotPoint s : points) {
                if (s.shape.contains(position)) s.datum.toggleSel();
            }
        }
    }

    public abstract String getName();
        
    public abstract void draw(Graphics2D g);
}
