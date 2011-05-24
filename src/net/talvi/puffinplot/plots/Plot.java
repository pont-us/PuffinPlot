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
import java.text.AttributedString;
import java.util.HashMap;
import static java.text.AttributedCharacterIterator.Attribute;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;
import static java.awt.font.TextAttribute.SUPERSCRIPT;
import static java.awt.font.TextAttribute.SUPERSCRIPT_SUPER;

public abstract class Plot
{
    private static final Logger logger = Logger.getLogger(Plot.class.getName());
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
    protected static final double PLOT_POINT_SIZE = 24.;
    private Map<Attribute,Object> attributeMap
     = new HashMap<Attribute, Object>();
    private static final TransformAttribute MAC_SUPERSCRIPT_TRANSFORM;
    private boolean visible;

    protected static final String DEFAULT_PLOT_POSITIONS =
            "demag true 374 85 348 311 zplot true 736 85 456 697 " +
            "zplotlegend true 1060 14 130 49 sampletable true 14 14 462 57 " +
            "fishertable true 550 14 178 61 pcatable true 736 14 193 64 " +
            "equarea true 376 398 346 389 datatable true 14 83 356 543 " +
            "ams false 222 633 147 154 ternarytest false 500 500 300 300 " +
            "greatcircles false 422 633 147 154 " +
            "suite_directions false 422 733 147 154 "+
            "nrm_histogram false 422 733 147 154";

    static {
        final AffineTransform at = AffineTransform.getTranslateInstance(0, 0.18);
        at.concatenate(AffineTransform.getScaleInstance(0.8, 0.8));
        MAC_SUPERSCRIPT_TRANSFORM = new TransformAttribute(at);
    }

    public Rectangle2D getDimensions() {
        return dimensions;
    }

    public void setDimensions(Rectangle2D dimensions) {
        this.dimensions = dimensions;
    }

    public void setDimensionsToDefault() {
        setDimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
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

    public final float getUnitSize() {
        return unitSize;
    }

    public float getTickLength() {
        return TICK_LENGTH_IN_UNITS * getUnitSize();
    }

    public final float getFontSize() {
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
        if (text != null && !"".equals(text)) {
            writeString(g, new AttributedString(text), x, y);
        }
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
        if (!PuffinApp.MAC_OS_X) {
            // 00D7 is the multiplication sign
            text += " \u00D710" + exponent;
            AttributedString as = new AttributedString(text);
            as.addAttribute(SUPERSCRIPT, SUPERSCRIPT_SUPER,
                    text.length() - exponent.length(), text.length());
            return as;
        } else {
            /* This is a fairly horrendous workaround for some fairly
             * horrendous bugs in Apple Java text rendering. Superscript
             * attributes don't work properly; the obvious workaround for
             * that (manually creating a scale-and-translate attribute)
             * doesn't work either, since for some reason the transform
             * is applied once to the first character in the run, then
             * twice to subsequent characters. To get around this, we
             * (1) create a transform which, when applied twice over,
             * produces an acceptable superscript effect, and (2) insert
             * a zero-width space as the first character in the run,
             * which "soaks up" the single application of the transform.
             * Much tedious trial and error went into this; test any changes
             * thoroughly!
             */
            text += " \u00D710\u200B" + exponent;
            AttributedString as = new AttributedString(text);
            as.addAttribute(TextAttribute.TRANSFORM, MAC_SUPERSCRIPT_TRANSFORM,
                    text.length() - exponent.length() - 1, text.length());
            return as;
        }
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
        String sizesString = DEFAULT_PLOT_POSITIONS;
        if (prefs != null) sizesString = prefs.get("plotSizes", DEFAULT_PLOT_POSITIONS);
        try {
            setDimensionsFromPrefsString(sizesString);
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, "Error parsing plot size", e);
            // safe to continue, default will be set below
        }
        // We may have a sizes string in the prefs but without this specific plot
        if (dimensions==null) setDimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
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
        // attributeMap.put(TextAttribute.FAMILY, "SansSerif");
        attributeMap.put(TextAttribute.FAMILY, "DeliciousGraph");
        // attributeMap.put(TextAttribute.SIZE, getFontSize());
        attributeMap.put(TextAttribute.SIZE, 120 * getUnitSize());
    }

    private void setDimensionsFromPrefsString(String spec) {
        final Scanner scanner = new Scanner(spec);
        scanner.useLocale(Locale.ENGLISH); // use `.' for decimals
        while (scanner.hasNext()) {
            String plotName = scanner.next();
            if (getName().equals(plotName)) {
                setVisible(scanner.nextBoolean());
                double x = scanner.nextDouble();
                double y = scanner.nextDouble();
                double w = scanner.nextDouble();
                double h = scanner.nextDouble();
                dimensions = new Rectangle2D.Double(x, y, w, h);
            } else {
                for (int i=0; i<5; i++) scanner.next();
            }
        }
    }

    public String dimensionsAsString() {
        Rectangle2D r = dimensions;
        // Explicit locale to ensure . for decimal separator
        return String.format(Locale.ENGLISH, "%b %g %g %g %g ",
                isVisible(),
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
