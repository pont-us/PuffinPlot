/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import static java.awt.font.TextAttribute.SUPERSCRIPT;
import static java.awt.font.TextAttribute.SUPERSCRIPT_SUPER;
import java.awt.font.TransformAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.window.GraphDisplay;
import net.talvi.puffinplot.window.PlotParams;

/**
 * An abstract superclass for all plots and other data displays.
 * Any class that needs to write or draw to one of PuffinPlot's
 * graph display areas should extend this class. Plot's subclasses
 * include both graphical plots and textual displays such as
 * the plot title and data table.
 * 
 * @author pont
 */
public abstract class Plot
{
    private static final Logger logger = Logger.getLogger(Plot.class.getName());
    
    /** the plot parameters */
    protected final PlotParams params;
    /** the plot's dimensions */
    protected Rectangle2D dimensions;
    /** the data points displayed by the plot */
    List<PlotPoint> points = new LinkedList<PlotPoint>();
    private static final float UNIT_SCALE = 0.0001f;
    private Stroke stroke, dashedStroke;
    private float unitSize;
    private static final float LINE_WIDTH_IN_UNITS = 8.0f;
    private static final float TICK_LENGTH_IN_UNITS = 48.0f;
    private static final float FONT_SIZE_IN_UNITS = 120.0f;
    private static final float SLOPPY_SELECTION_RADIUS_IN_UNITS = 128.0f;
    /** the default size of a plotted data point */
    protected static final double PLOT_POINT_SIZE = 24.;
    private Map<Attribute,Object> attributeMap =
            new HashMap<Attribute, Object>();
    private static final TransformAttribute MAC_SUPERSCRIPT_TRANSFORM;
    private boolean visible;
    private final boolean useAppleSuperscriptHack;
    private final Set<SampleClickListener> sampleClickListeners =
            new HashSet<SampleClickListener>();

    /** the default sizes and positions of the plots */
    protected static final String DEFAULT_PLOT_POSITIONS =
            "equarea true 339 319 309 302 "
            + "zplot true 647 70 446 614 "
            + "demag true 337 99 308 204 "
            + "datatable true 8 72 326 543 "
            + "pcatable true 338 5 191 65 "
            + "title true 4 5 328 59 "
            + "sitetable true 533 4 321 66 "
            + "ams false 379 634 193 177 "
            + "ternaryplot false 573 616 204 191 "
            + "equarea_site false 6 616 182 193 "
            + "equarea_suite false 192 615 186 197 "
            + "nrm_histogram false 778 679 315 131 "
            + "sample_params_table false 478 379 315 331 "
            + "site_params_table false 778 379 315 331 "
            + "zplotlegend true 856 6 132 63";

    static {
        final AffineTransform at = AffineTransform.getTranslateInstance(0, 0.18);
        at.concatenate(AffineTransform.getScaleInstance(0.8, 0.8));
        MAC_SUPERSCRIPT_TRANSFORM = new TransformAttribute(at);
    }

    /** Creates a plot with the supplied parameters.
     * 
     * @param parent the graph display containing the plot
     * @param params the parameters of the plot
     * @param prefs the preferences containing the plot configuration
     */
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
        final String fontFamily = prefs.get("plots.fontFamily", "Arial");
        attributeMap.put(TextAttribute.FAMILY, fontFamily);
        attributeMap.put(TextAttribute.SIZE, getFontSize());
        useAppleSuperscriptHack = PuffinApp.getInstance().isOnOsX() &&
                (PuffinApp.getInstance().getOsxPointVersion() < 6 ||
                System.getProperty("java.version").startsWith("1.5"));
                
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
    
    /** Returns the dimensions of this plot. 
     * @return the dimensions of this plot */
    public Rectangle2D getDimensions() {
        return dimensions;
    }

    /** Sets the dimensions of this plot. 
     * @param dimensions dimensions the new dimensions of this plot */
    public void setDimensions(Rectangle2D dimensions) {
        this.dimensions = dimensions;
    }

    /** Resets the plot's dimensions to the default,
     * as defined in {@link #DEFAULT_PLOT_POSITIONS}. */
    public void setDimensionsToDefault() {
        setDimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
    }

    /** Returns the size of the margin displayed when resizing the plot. 
     * @return the size of the margin displayed when resizing the plot */
    public int getMargin() {
        return 24;
    }

    /** Returns the default solid stroke style.
     * @return the default solid stroke style. */
    public Stroke getStroke() {
        return stroke;
    }
    
    /** Returns the default dashed stroke style.
     * @return the default dashed stroke style. */
    public Stroke getDashedStroke() {
        return dashedStroke;
    }

    /** Returns the size of a plot unit in Java 2D units.
     * @return the size of a plot unit in Java 2D units */
    public final float getUnitSize() {
        return unitSize;
    }

    /** Returns the standard length of an axis tick in plot units. 
     * @return the standard length of an axis tick in plot units */
    public float getTickLength() {
        return TICK_LENGTH_IN_UNITS * getUnitSize();
    }

    /** Returns the standard font size in plot units.
     * @return the standard font size in plot units. */
    public final float getFontSize() {
        return FONT_SIZE_IN_UNITS * getUnitSize();
    }

    /** Returns this plot's standard text attributes. 
     * @return this plot's standard text attributes */
    public Map<? extends Attribute,?> getTextAttributes() {
        return Collections.unmodifiableMap(attributeMap);
    }

    /** Applies this plot's standard text attributes to an attributed string.
     * @param as the string to which to apply this plot's standard text 
     * attributes
     */
    public void applyTextAttributes(AttributedString as) {
        for (Map.Entry<? extends Attribute, ?> a : attributeMap.entrySet())
            as.addAttribute(a.getKey(), a.getValue());
    }

    /** Writes a text string onto this plot. 
     * @param g the graphics object to which to write the 
     * @param text the text to write
     * @param x the x co-ordinate of the text
     * @param y the y co-ordinate of the text
     */
    protected void writeString(Graphics2D g, String text, float x, float y) {
        if (text != null && !"".equals(text)) {
            writeString(g, new AttributedString(text), x, y);
        }
    }

    /** Writes an attributed text string onto this plot. 
     * @param g the graphics object to which to write the 
     * @param as the text to write
     * @param x the x co-ordinate of the text
     * @param y the y co-ordinate of the text
     */
    protected void writeString(Graphics2D g, AttributedString as, float x, float y) {
        applyTextAttributes(as);
        // Don't use TextLayout.draw, since it draw with a GlyphVector
        // and we'd get shapes rather than text in SVG export etc.
        g.drawString(as.getIterator(), x, y);
    }

    /**
     * Returns an attributed string representing a number in scientific
     * notation. For example, a significand of 12 and an exponent of 34
     * would produce the string 12 &times; 10<sup>34</sup>.
     * 
     * @param significand the significand of the number
     * @param exponent the exponent of the number
     * @return an attributed string representing the number in scientific notation
     */
    protected AttributedString timesTenToThe(String significand, String exponent) {
        String text = significand;
        if (!useAppleSuperscriptHack) {
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
             * 
             * Works for Java 1.5.0 on OS X 10.5.8 PPC, tested 2011-12-08.
             * Now that recent Macs can do superscript properly, it's
             * probably not worth individually crafting and testing 
             * workarounds for every Java/OS X/architecture combination.
             * I will trust that this one is acceptable until/unless
             * I get a bug report about it.
             */
            text += " \u00D710\u200B" + exponent;
            AttributedString as = new AttributedString(text);
            as.addAttribute(TextAttribute.TRANSFORM, MAC_SUPERSCRIPT_TRANSFORM,
                    text.length() - exponent.length() - 1, text.length());
            return as;
        }
    }

    /**
     * Returns an attributed string representing a number in scientific
     * notation. For example, a significand of 12 and an exponent of 34
     * would produce the string 12 &times; 10<sup>34</sup>.
     * 
     * @param significand the significand of the number
     * @param exponent the exponent of the number
     * @return an attributed string representing the number in scientific notation
     */
    protected AttributedString timesTenToThe(String significand, int exponent) {
        return timesTenToThe(significand, Integer.toString(exponent));
    }

    /** Returns a cropped version of a specified rectangle. 
     * @param r a rectangle
     * @param left the amount to crop at the left
     * @param right the amount to crop at the right
     * @param top the amount to crop at the top
     * @param bottom the amount to crop at the bottom
     * @return the cropped rectangle
     */
    protected Rectangle2D cropRectangle(Rectangle2D r, double left,
            double right, double top, double bottom) {
        final double u = getUnitSize();
        return new Rectangle2D.Double(
                r.getMinX() + left * u,
                r.getMinY() + top * u,
                r.getWidth() - (left + right) * u,
                r.getHeight() - (top + bottom) * u);
    }

    /** Returns a string representation of this plot's dimensions.
     * The string is in the same format as the {@code plotSizes} 
     * Preferences entry from which the plot reads its initial
     * dimensions.
     * @return a string representation of this plot's dimensions
     */
    public String getDimensionsAsString() {
        logger.log(Level.FINE, "Getting dimensions for {0}", getName());
        Rectangle2D r = dimensions;
        // Explicit locale to ensure . for decimal separator
        return String.format(Locale.ENGLISH, "%b %g %g %g %g ",
                isVisible(),
                r.getMinX(), r.getMinY(),
                r.getWidth(), r.getHeight());
    }

    /** Draws the points in this plot's internal buffer. 
     * @param g the graphics object to which to draw the points */
    protected void drawPoints(Graphics2D g) {
        g.setStroke(getStroke());
        PlotPoint prev = null;
        for (PlotPoint point: points) {
            point.drawWithPossibleLine(g, prev);
            prev = point;
        }
    }
    
    /**
     * Adds a point to this plot's internal buffer.
     * 
     * @param d the datum associated with the point ({@code null} if none)
     * @param p the position of the point
     * @param filled {@code true} if the point should be filled
     * @param special {@code true} if the point should be highlighted
     * @param line {@code true} if a line should be drawn from the previous 
     * point to this one.
     */
    protected void addPoint(Datum d, Point2D p, boolean filled,
            boolean special, boolean line) {
        points.add(ShapePoint.build(this, p).datum(d).
                filled(filled).lineToHere(line).special(special).
                build());
    }
    
    /** Clear this plot's internal buffer of points. */
    protected void clearPoints() {
        points.clear();
    }
    
    /** Handles a mouse click event on the plot. If the click was on 
     * a plotted point, the associated datum (if any) will have its
     * selection state toggled.
     * 
     * @param position the position of the click
     * @param e the event associated with the click
     */
    public void mouseClicked(java.awt.geom.Point2D position, MouseEvent e) {
        final boolean sloppy = e.isShiftDown();
        for (PlotPoint p : points) {
            final Datum d = p.getDatum();
            if (d != null && !d.isHidden()) {
            if (sloppy) {
                if (p.isNear(position, SLOPPY_SELECTION_RADIUS_IN_UNITS * getUnitSize()))
                    d.setSelected(e.getButton() == MouseEvent.BUTTON1);
                } else {
                if (p.getShape().contains(position))
                    d.toggleSel();
            }
            } else /* no datum associated with point; check for a sample */ {
                final Sample sample = p.getSample();
                if (sample != null && p.getShape().contains(position)) {
                    fireSampleClickNotification(sample);
                }
            }
        }
    }

    /** Sets selection state for data points in a rectangle.
     * Only visible points (i.e. those for which {@code isHidden()} is 
     * {@code false}) are affected by this method.
     * @param rectangle a rectangle defining which points should be selected
     * @param state {@code true} to select points, {@code false} to deselect
     */
    public void selectByRectangle(Rectangle2D rectangle, boolean state) {
        for (PlotPoint point: points) {
            if (point.getDatum() != null && 
                    !point.getDatum().isHidden() &&
                    point.getShape().intersects(rectangle))
                point.getDatum().setSelected(state);
        }
    }

    /** Returns an internal name for this plot.
     * @return an internal name for this plot */
    public abstract String getName();

    /** Returns a user-friendly name for this plot.
     * @return  a user-friendly name for this plot */
    public String getNiceName() {
        return getName();
    }
    
    /** Draws this plot.
     * @param g the graphics object onto which to draw this plot */
    public abstract void draw(Graphics2D g);

    /** Reports whether this plot is visible. 
     * @return {@code true} if this plot is visible; {@code false} if it is hidden
     */
    public boolean isVisible() {
        return visible;
    }

    /** Sets whether this plot should be drawn. 
     * @param visible {@code true} to draw this plot; {@code false} not to draw it
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Adds a listener for sample clicks to this plot. When the user clicks
     * on a plot element representing a sample, the
     * {@link SampleClickListener#sampleClicked(net.talvi.puffinplot.data.Sample)}
     * method of the supplied listener will be invoked. The listener can be
     * removed using the {@link Plot#removeSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener)}
     * method.
     * 
     * @see SampleClickListener
     * @see Plot#removeSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener) 
     * 
     * @param listener the listener to add
     */
    public void addSampleClickListener(SampleClickListener listener) {
        sampleClickListeners.add(listener);
    }
    
    /**
     * Removes a sample click listener which was previously added to this plot.
     * If the listener was never added to the plot, no action is taken and
     * no exception is raised.
     * 
     * @see Plot#addSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener) 
     * @see SampleClickListener
     * 
     * @param listener the listener to remove
     */
    public void removeSampleClickListener(SampleClickListener listener) {
        sampleClickListeners.remove(listener);
    }
    
    private void fireSampleClickNotification(Sample sample) {
        for (SampleClickListener listener: sampleClickListeners) {
            listener.sampleClicked(sample);
        }
    }
}
