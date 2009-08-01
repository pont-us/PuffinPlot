package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.HashMap;
import static java.text.AttributedCharacterIterator.Attribute;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;
import static java.awt.font.TextAttribute.SUPERSCRIPT;
import static java.awt.font.TextAttribute.SUPERSCRIPT_SUPER;

public abstract class Plot
{
    private final GraphDisplay parent;
    protected final PlotParams params;
    protected Rectangle2D dimensions;
    List<PlotPoint> points = new LinkedList<PlotPoint>();
    private static final float UNIT_SCALE = (float) 0.0001f;
    private Stroke stroke, dashedStroke;
    private float unitSize;
    private static final float LINE_WIDTH_IN_UNITS = 8.0f;
    private static final float TICK_LENGTH_IN_UNITS = 48.0f;
    private static final float FONT_SIZE_IN_UNITS = 100.0f;
    private static final float SLOPPY_SELECTION_RADIUS_IN_UNITS = 128.0f;
    private Map<Attribute,Object> attributeMap
     = new HashMap<Attribute, Object>();
    private static final TextAttribute SUPERSCRIPT_KEY;
    private static final Object SUPERSCRIPT_VALUE;

    protected static final String DEFAULT_PLOT_POSITIONS =
            "demag 374 85 348 311 zplot 736 85 456 697 zplotlegend 1060 30 " +
            "130 49 sampletable 14 14 462 57 fishertable 550 14 178 61 " +
            "pcatable 736 14 193 64 equarea 376 398 346 389 datatable 14 83 " +
            "356 706 ";

    static {
        /* The superscript attribute doesn't always work properly:
         * under Linux/Java 1.5 , it appears a little too high. Under
         * any (current) Apple Java, it appears halfway to Mongolia.
         * Thus, for broken JREs, we fall back on a manual text transform
         * based on the TextAttribute.SUPERSCRIPT Javadoc. The vertical
         * offset is determined by experiment, and does not accord
         * with the value suggested by the TextAttribute Javadoc.
         */
        if (PuffinApp.MAC_OS_X ||
                System.getProperty("java.version", "UNKNOWN").
                matches("^1\\.5.*")) {
            SUPERSCRIPT_KEY = TextAttribute.TRANSFORM;
            final double scale = 2d / 3d;
            AffineTransform at = AffineTransform.getTranslateInstance(0, -5);
            at.concatenate(AffineTransform.getScaleInstance(scale, scale));
            SUPERSCRIPT_VALUE = new TransformAttribute(at);
        } else {
            SUPERSCRIPT_KEY = TextAttribute.SUPERSCRIPT;
            SUPERSCRIPT_VALUE = TextAttribute.SUPERSCRIPT_SUPER;
        }
    }

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
    
    public Stroke getDashedStroke() {
        return dashedStroke;
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

    public Map<? extends Attribute,?> getTextAttributes() {
        return attributeMap;
    }

    public void applyTextAttributes(AttributedString as) {
        for (Map.Entry<? extends Attribute, ?> a : attributeMap.entrySet())
            as.addAttribute(a.getKey(), a.getValue());
    }

    protected void writeString(Graphics2D g, String text, float x, float y) {
        writeString(g, new AttributedString(text), x, y);
    }

    protected void writeString(Graphics2D g, AttributedString as, float x, float y) {
        applyTextAttributes(as);
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout layout = new TextLayout(as.getIterator(), frc);
        layout.draw(g, x, y);
    }

    /**
     *  Used for "x10^?" when exponent is unknown.
     */
    protected AttributedString timesTenToThe(String text, String exponent) {
        // 00D7 is the multiplication sign
        text += " \u00D710" + exponent;
        AttributedString as = new AttributedString(text);
        as.addAttribute(SUPERSCRIPT_KEY, SUPERSCRIPT_VALUE,
                text.length() - exponent.length(), text.length());
        return as;
    }

    protected AttributedString timesTenToThe(String text, int exponent) {
        return timesTenToThe(text, Integer.toString(exponent));
    }

    protected Rectangle2D cropRectangle(Rectangle2D r, double left,
            double right, double top, double bottom) {
        final double u = getUnitSize();
        return new Rectangle2D.Double(
                r.getMinX() + left * u,
                r.getMinY() + top * u,
                r.getWidth() - (left + right) * u,
                r.getHeight() - (top + bottom) * u);
    }
    
    public Plot(GraphDisplay parent, PlotParams params, Preferences prefs) {
        this.params = params;
        this.parent = parent;
        String sizesString = DEFAULT_PLOT_POSITIONS;
        if (prefs != null) sizesString = prefs.get("plotSizes", DEFAULT_PLOT_POSITIONS);
        dimensions = dimensionsFromPrefsString(sizesString);
        // We may have a sizes string in the prefs but without this specific plot
        if (dimensions==null) dimensions = dimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
        float maxDim = 800;
        // TODO fix this; parent should never be null (see FisherEqAreaPlot).
        if (parent != null) {
            Dimension dims = parent.getMaximumSize();
            maxDim = (float) Math.max(dims.getWidth(), dims.getHeight());
        }
        unitSize = maxDim * UNIT_SCALE;
        stroke = new BasicStroke(getUnitSize() * LINE_WIDTH_IN_UNITS);
        dashedStroke = new BasicStroke(getUnitSize() * LINE_WIDTH_IN_UNITS,
                0, 0, 1, new float[]{2, 2}, 0);

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
        PlotPoint prev = null;
        for (PlotPoint point: points) {
            point.drawWithPossibleLine(g, prev);
            prev = point;
        }
    }
        
    protected void addPoint(Datum d, Point2D p, boolean filled,
            boolean special, boolean line) {
        points.add(new SquarePoint(this, d, p, filled, line, special));
    }
    
    protected void clearPoints() {
        points.clear();
    }
    
    public void mouseClicked(java.awt.geom.Point2D position, MouseEvent e) {
        final boolean sloppy = e.isShiftDown();
        for (PlotPoint p : points) {
            final Datum d = p.getDatum();
            if (d == null || d.isHidden()) continue;
            if (sloppy) {
                if (p.isNear(position, SLOPPY_SELECTION_RADIUS_IN_UNITS * getUnitSize()))
                    d.setSelected(e.getButton() == MouseEvent.BUTTON1);
                } else {
                if (p.getShape().contains(position))
                    d.toggleSel();
            }
            }
    }

    public void selectByRectangle(Rectangle2D r) {
        for (PlotPoint point: points) {
            if (point.getDatum() != null && 
                    !point.getDatum().isHidden() &&
                    point.getShape().intersects(r))
                point.getDatum().setSelected(true);
        }
    }

    public abstract String getName();

    public String getNiceName() {
        return getName();
    }
        
    public abstract void draw(Graphics2D g);
}
