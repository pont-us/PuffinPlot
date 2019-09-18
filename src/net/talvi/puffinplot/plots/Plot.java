/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.window.PuffinRenderingHints;

import static java.awt.font.TextAttribute.SUPERSCRIPT;
import static java.awt.font.TextAttribute.SUPERSCRIPT_SUPER;
import java.util.function.Consumer;

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

    /**
     * The data points displayed by the plot.
     * 
     * The default size of 32 should accommodate the vast majority of
     * real-world demagnetization data sets without needing to be resized.
     */
    List<PlotPoint> points = new ArrayList<>(32);

    private Stroke stroke, dashedStroke;
    private float unitSize;
    private String fontFamily;
    private static final float LINE_WIDTH_IN_UNITS = 8.0f;
    private static final float TICK_LENGTH_IN_UNITS = 48.0f;
    private static final float FONT_SIZE_IN_UNITS = 120.0f;
    private static final float SLOPPY_SELECTION_RADIUS_IN_UNITS = 128.0f;
    /** the default size of a plotted data point */
    protected static final double PLOT_POINT_SIZE = 24.;
    private Map<Attribute,Object> attributeMap = new HashMap<>();
    private boolean visible;
    private final Set<SampleClickListener> sampleClickListeners =
            new HashSet<>();
    private final Set<Consumer<Boolean>> visibilityChangedListeners =
            new HashSet<>();

    /** the default sizes and positions of the plots */
    protected static final String DEFAULT_PLOT_POSITIONS =
            "equarea true 334 297 264 284 " +
            "zplot true 600 72 337 509 " +
            "demag true 334 72 264 209 " +
            "datatable true 6 144 326 437 " +
            "pcatable true 10 80 318 59 " +
            "title true 11 8 379 64 " +
            "sitetable true 421 6 335 65 " +
            "ams false 380 541 197 191 " +
            "ternaryplot false 584 541 183 188 " +
            "equarea_site false 6 541 182 193 " +
            "equarea_suite false 191 541 186 197 " +
            "sample_params_table false 9 341 254 241 " +
            "site_params_table false 519 457 318 134 " +
            "vgp_table false 619 557 318 134 " +
            "nrm_histogram false 771 542 226 115 " +
            "zplotlegend true 779 7 156 58 " +
            "suite_table false 636 76 297 91 " +
            "depth false 50 50 300 200 " +
            "vgpmap false 50 50 300 200 ";

    /**
     * Creates a plot with the supplied parameters.
     *
     * @param params the parameters of the plot
     */
    public Plot(PlotParams params) {
        this.params = params;
        final String sizesString =
                params.getSetting("plotSizes", DEFAULT_PLOT_POSITIONS);
        fontFamily = params.getSetting("plots.fontFamily", "Arial");
        try {
            setDimensionsFromPrefsString(sizesString);
        } catch (NoSuchElementException e) {
            logger.log(Level.WARNING, "Error parsing plot size", e);
            // safe to continue, default will be set below
        }
        // We may have a sizes string in prefs, but without this specific plot.
        if (dimensions == null) {
            /*
             * If the plot is not in the defaults string, this may leave
             * dimensions as null, but it can also be set afterwards by
             * the constructor of a subclass.
             */
            setDimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
        }
        unitSizeChanged();
    }
    
    private void unitSizeChanged() {
        unitSize = params.getUnitSize();
        stroke = new BasicStroke(getUnitSize() * LINE_WIDTH_IN_UNITS);
        dashedStroke = new BasicStroke(getUnitSize() * LINE_WIDTH_IN_UNITS,
                0, 0, 1, new float[]{2, 2}, 0);
        attributeMap.put(TextAttribute.FAMILY, fontFamily);
        attributeMap.put(TextAttribute.SIZE, getFontSize());
    }

    private void setDimensionsFromPrefsString(String spec) {
        final Scanner scanner = new Scanner(spec);
        scanner.useLocale(Locale.ENGLISH); // ensure "." for decimal point
        while (scanner.hasNext()) {
            final String plotName = scanner.next();
            if (getName().equals(plotName)) {
                final boolean oldVisibility = isVisible();
                final boolean newVisibility = scanner.nextBoolean();
                setVisible(newVisibility);
                if (newVisibility != oldVisibility) {
                    fireVisibilityChangedNotification(newVisibility);
                }
                final double x = scanner.nextDouble();
                final double y = scanner.nextDouble();
                final double w = scanner.nextDouble();
                final double h = scanner.nextDouble();
                dimensions = new Rectangle2D.Double(x, y, w, h);
            } else {
                for (int i = 0; i < 5; i++) {
                    scanner.next();
                }
            }
        }
    }
    
    /**
     * Returns the dimensions of this plot.
     *
     * @return the dimensions of this plot
     */
    public Rectangle2D getDimensions() {
        return dimensions;
    }

    /**
     * Sets the dimensions of this plot.
     *
     * @param dimensions dimensions the new dimensions of this plot
     */
    public void setDimensions(Rectangle2D dimensions) {
        this.dimensions = dimensions;
    }
    
    /**
     * Determines whether the points should be labelled. This method is used by
     * {@code Plot}'s drawing routines. As defined in {@code Plot}, this method
     * always returns {@code false}. Plots which use labelled points should
     * override it.
     *
     * @return {@code true} if this plot's points should be labelled
     */
    public boolean areTreatmentStepsLabelled() {
        return false;
    }

    /**
     * Resets the plot's dimensions to the default, as defined in
     * {@link #DEFAULT_PLOT_POSITIONS}.
     */
    public void setDimensionsToDefault() {
        setDimensionsFromPrefsString(DEFAULT_PLOT_POSITIONS);
    }

    /**
     * Returns the size of the margin displayed when resizing the plot.
     *
     * @return the size of the margin displayed when resizing the plot
     */
    public int getMargin() {
        return 24;
    }

    /**
     * Returns the default solid stroke style.
     *
     * @return the default solid stroke style.
     */
    public Stroke getStroke() {
        return stroke;
    }
    
    /**
     * Returns the default dashed stroke style.
     *
     * @return the default dashed stroke style.
     */
    public Stroke getDashedStroke() {
        return dashedStroke;
    }

    /**
     * Returns the size of a plot unit in Java 2D units.
     *
     * @return the size of a plot unit in Java 2D units
     */
    public final float getUnitSize() {
        return unitSize;
    }

    /**
     * Returns the standard length of an axis tick in plot units.
     *
     * @return the standard length of an axis tick in plot units
     */
    public float getTickLength() {
        return TICK_LENGTH_IN_UNITS * getUnitSize();
    }

    /**
     * Returns the standard font size in plot units.
     *
     * @return the standard font size in plot units.
     */
    public final float getFontSize() {
        return FONT_SIZE_IN_UNITS * getUnitSize();
    }

    /**
     * Returns this plot's standard text attributes.
     *
     * @return this plot's standard text attributes
     */
    public Map<? extends Attribute,?> getTextAttributes() {
        return Collections.unmodifiableMap(attributeMap);
    }

    /**
     * Applies this plot's standard text attributes to an attributed string.
     *
     * @param as the string to which to apply this plot's standard text
     * attributes
     */
    public void applyTextAttributes(AttributedString as) {
        for (Map.Entry<? extends Attribute, ?> a : attributeMap.entrySet()) {
            as.addAttribute(a.getKey(), a.getValue());
        }
    }

    /**
     * Writes a text string onto this plot.
     *
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

    /**
     * Writes an attributed text string onto this plot.
     *
     * @param graphics the graphics object to which to write the
     * @param as the text to write
     * @param x the x co-ordinate of the text
     * @param y the y co-ordinate of the text
     */
    protected void writeString(Graphics2D graphics, AttributedString as,
            float x, float y) {
        applyTextAttributes(as);
        /*
         * Don't use TextLayout.draw, since it draw with a GlyphVector and we'd
         * get shapes rather than text in SVG export etc.
         */
        graphics.drawString(as.getIterator(), x, y);
    }

    /**
     * Returns an attributed string representing a number in scientific
     * notation. By default a notation of the form
     * "1.2 &times; 10<sup>34</sup>."
     * is produced; if the rendering hints of the supplied graphics object
     * contain the key {@link PuffinRenderingHints#KEY_E_NOTATION}, a notation
     * of the form "1.2E34." is produced instead.
     *
     * @param significand the significand of the number
     * @param exponent the exponent of the number
     * @param graphics a graphics context
     * @return an attributed string representing the number in scientific
     * notation
     */
    protected AttributedString timesTenToThe(String significand,
            String exponent, Graphics2D graphics) {
        if (!graphics.getRenderingHints()
                .containsKey(PuffinRenderingHints.KEY_E_NOTATION)) {
            String text = significand;
            // 00D7 is the multiplication sign
            if (exponent.startsWith("-")) {
                exponent = "\u2212" + exponent.substring(1); // minus sign
            }
            text += " \u00D710" + exponent;
            
            final AttributedString as = new AttributedString(text);
            as.addAttribute(SUPERSCRIPT, SUPERSCRIPT_SUPER,
                    text.length() - exponent.length(), text.length());
            
            return as;
        } else {
            return new AttributedString(significand + "E" + exponent);
        }
    }

    /**
     * Returns an attributed string representing a number in scientific
     * notation. See
     * {@link #timesTenToThe(java.lang.String, java.lang.String, java.awt.Graphics2D)}
     * for details of the notation produced.
     * 
     * @see #timesTenToThe(java.lang.String, java.lang.String, java.awt.Graphics2D) 
     * 
     * @param significand the significand of the number
     * @param exponent the exponent of the number
     * @param graphics a graphics context
     * @return an attributed string representing the number in scientific
     * notation
     */
    protected AttributedString timesTenToThe(String significand, int exponent,
            Graphics2D graphics) {
        return timesTenToThe(significand, Integer.toString(exponent), graphics);
    }

    /**
     * Returns a cropped version of a specified rectangle.
     *
     * @param rectangle a rectangle
     * @param left the amount to crop at the left
     * @param right the amount to crop at the right
     * @param top the amount to crop at the top
     * @param bottom the amount to crop at the bottom
     * @return the cropped rectangle
     */
    protected Rectangle2D cropRectangle(Rectangle2D rectangle, double left,
            double right, double top, double bottom) {
        final double unit = getUnitSize();
        return new Rectangle2D.Double(
                rectangle.getMinX() + left * unit,
                rectangle.getMinY() + top * unit,
                rectangle.getWidth() - (left + right) * unit,
                rectangle.getHeight() - (top + bottom) * unit);
    }

    /**
     * Returns a string representation of this plot's dimensions. The string is
     * in the same format as the {@code plotSizes} Preferences entry from which
     * the plot reads its initial dimensions.
     *
     * @return a string representation of this plot's dimensions
     */
    public String getDimensionsAsString() {
        logger.log(Level.FINE, "Getting dimensions for {0}", getName());
        return String.format(Locale.ENGLISH, "%b %.0f %.0f %.0f %.0f ",
                isVisible(),
                dimensions.getMinX(), dimensions.getMinY(),
                dimensions.getWidth(), dimensions.getHeight());
    }

    /**
     * Draws the points in this plot's internal buffer.
     *
     * @param g the graphics object to which to draw the points
     */
    protected void drawPoints(Graphics2D g) {
        g.setStroke(getStroke());
        PlotPoint prev = null;
        for (PlotPoint point: points) {
            point.drawWithPossibleLine(g, prev, areTreatmentStepsLabelled());
            prev = point;
        }
    }
    
    /**
     * Adds a point to this plot's internal buffer.
     * 
     * @param step the datum associated with the point ({@code null} if none)
     * @param p the position of the point
     * @param filled {@code true} if the point should be filled
     * @param special {@code true} if the point should be highlighted
     * @param line {@code true} if a line should be drawn from the previous 
     * point to this one.
     * @return the point which was created
     */
    protected PlotPoint addPoint(TreatmentStep step, Point2D p, boolean filled,
                                 boolean special, boolean line) {
        final ShapePoint pp = ShapePoint.build(this, p).datum(step).
                filled(filled).lineToHere(line).special(special).
                build();
        points.add(pp);
        final int nPoints = points.size();
        if (nPoints > 1) {
            final PlotPoint prevPoint = points.get(nPoints - 2);
            final Point2D centre = prevPoint.getCentre();
            final List<Point2D> others = new ArrayList<>(2);
            others.add(points.get(nPoints - 1).getCentre());
            if (nPoints > 2) {
                others.add(points.get(nPoints - 3).getCentre());
            }
            ((ShapePoint) prevPoint)
                    .setLabelPos(Direction.safeDirection(centre, others));
        }
        return pp;
    }
    
    /**
     * Clear this plot's internal buffer of points.
     */
    protected void clearPoints() {
        points.clear();
    }
    
    /**
     * Write some text on this plot. The text is positioned relative to a
     * specified point. The parameter {@code dir} controls whether the text
     * should be offset up, down, left, or right of the point. In the axis
     * perpendicular to the offset direction, the text is centred, so for
     * example if the text is placed below the specified point, it will also be
     * centred horizontally relative to the point. An angle of rotation can also
     * be specified for the text; note that at present only rotations of 0
     * (horizontal, rightward) and pi/2 (vertical, upward) have been tested.
     *
     * @param graphics the graphics context
     * @param text the text to write
     * @param x the x position of the text
     * @param y the y position of the text
     * @param dir the location of the text relative to the given position
     * @param θ the rotation of the text, in radians (0 = horizontal)
     * @param padding the distance between the text and the given position
     */
    public void putText(Graphics2D graphics, AttributedString text, double x,
            double y, Direction dir, double θ, double padding) {
        AffineTransform initialTransform = graphics.getTransform();
        applyTextAttributes(text);

        /*
         * The obvious thing to do might be to calculate the bounding box for
         * unrotated text, then rotate the bounding box if required.
         * Unfortunately this is unreliable, since it turns out that orientation
         * can affect text layout. On Linux 64-bit OpenJDK 1.7.0_25, vertical
         * text is set closer than horizontal. So the bounding box must be
         * determined by actually creating a TextLayout for a rotated
         * FontRenderContext.
         */
        graphics.rotate(θ);
        final TextLayout layout =
                new TextLayout(text.getIterator(),
                        graphics.getFontRenderContext());
        final Rectangle2D transformedBounds = layout.getBounds();
        graphics.setTransform(initialTransform);

        /*
         * We now back-rotate the transformed bounding box to use in
         * calculations requiring the untransformed bounds.
         */
        final Rectangle2D bounds = AffineTransform.getRotateInstance(-θ).
                createTransformedShape(transformedBounds).getBounds2D();

        /*
         * Translate the co-ordinate system to the "target" point. This isn't
         * quite where we want it yet: we need to (1) offset in the chosen
         * direction, and (2) centre the text relative to the offset point. Part
         * (1) could be combined with this translate operation (though I think
         * it's conceptually clearer to separate them), but it's easier to do
         * part (2) after we've rotated into the text's co-ordinate system.
         */
        graphics.translate(x, y);
        
        /*
         * Translate the target point to the "offset" point: this is the point
         * above, below, left, or right of the target point which actually
         * defines the text's location. The offset is measured from the edge of
         * the text, not its centre, hence the additional half-width or
         * half-height offset in the same direction. Note that we're still
         * working in the unrotated co-ordinate system here.
         */
        final double halfWidth = bounds.getWidth()/2;
        final double halfHeight = bounds.getHeight()/2;
        switch (dir) {
        case RIGHT:
            graphics.translate(padding + halfWidth, 0);
            break;
        case DOWN:
            graphics.translate(0, padding + halfHeight);
            break;
        case LEFT:
            graphics.translate(-padding - halfWidth, 0);
            break;
        case UP:
            graphics.translate(0, -padding - halfHeight);
            break;
        }
        
        /*
         * Having moved to the offset point, we now rotate from the graph's
         * orientation to the text's orientation (which may of course be the
         * same).
         */
        graphics.rotate(θ);
        
        /*
         * We have to do some more translation now, in the text's rotated
         * coordinate system. The offset point is the desired location of the
         * centre of the text, but drawString wants the location of the bottom
         * left corner so we move the text half its width to the left and half
         * its height down.
         */
        graphics.translate(-transformedBounds.getWidth()/2,
                transformedBounds.getHeight()/2);
        
        /*
         * Finally we have to account for the fact that the origin of the
         * TextLayout from which the bounds were calculated is not at (0, 0).
         * (See TextLayout docs.)
         */
        graphics.translate(-transformedBounds.getMinX(),
                -transformedBounds.getMaxY());

        /*
         * We don't use layout.draw, since that will draw the text as a glyph
         * vector, which won't be exported as text in SVG, PDF, etc.
         */
        graphics.drawString(text.getIterator(), 0, 0);
        
        /*
         * Finally, we restore the initial co-ordinate system.
         */
        graphics.setTransform(initialTransform);
    }
    
    /**
     * Write some text on this plot.
     * 
     * @param g the graphics context
     * @param textString the text to write
     * @param x the x position of the text
     * @param y the y position of the text
     * @param dir the location of the text relative to the given position
     * @param θ the rotation of the text, in radians (0 = horizontal)
     * @param padding the distance between the text and the given position
     */
    public void putText(Graphics2D g, String textString, double x,
            double y, Direction dir, double θ, double padding) {
        final AttributedString text = new AttributedString(textString);
        putText(g, text, x, y, dir, θ, padding);
    }

    
    /**
     * Handles a mouse click event on the plot. If the click was on a plotted
     * point, the associated datum (if any) will have its selection state
     * toggled.
     *
     * @param position the position of the click
     * @param e the event associated with the click
     */
    public void mouseClicked(java.awt.geom.Point2D position, MouseEvent e) {
        if (!isVisible()) {
            return;
        }
        final boolean sloppy = e.isShiftDown();
        for (PlotPoint p: points) {
            final TreatmentStep step = p.getTreatmentStep();
            if (step != null && !step.isHidden()) {
                if (sloppy) {
                    if (p.isNear(position,
                            SLOPPY_SELECTION_RADIUS_IN_UNITS * getUnitSize())) {
                        step.setSelected(e.getButton() == MouseEvent.BUTTON1);
                    }
                } else {
                    if (p.getShape().contains(position)) {
                        step.toggleSelected();
                    }
                }
            } else {
                // no treatment step associated with point; check for a sample
                final Sample sample = p.getSample();
                if (sample != null && p.getShape().contains(position)) {
                    fireSampleClickNotification(sample);
                }
            }
        }
    }
    
    /**
     * Returns the datum associated with the point at the given position,
     * or null if no such datum exists.
     * 
     * @param position a position
     * @return the datum whose point is at the position, if any; otherwise null
     */
    public TreatmentStep getTreatmentStepForPosition(Point2D position) {
        for (PlotPoint point: points) {
            final TreatmentStep step = point.getTreatmentStep();
            if (step != null && !step.isHidden()
                    && point.getShape().contains(position)) {
                return step;
            }
        }
        return null;
    }

    /**
     * Sets selection state for data points in a rectangle. Only visible points
     * (i.e. those for which {@code isHidden()} is {@code false}) are affected
     * by this method.
     *
     * @param rectangle a rectangle defining which points should be selected
     * @param state {@code true} to select points, {@code false} to deselect
     */
    public void selectByRectangle(Rectangle2D rectangle, boolean state) {
        for (PlotPoint point: points) {
            if (point.getTreatmentStep() != null
                    && !point.getTreatmentStep().isHidden()
                    && point.getShape().intersects(rectangle)) {
                point.getTreatmentStep().setSelected(state);
            }
        }
    }

    /**
     * Returns an internal name for this plot.
     *
     * @return an internal name for this plot
     */
    public abstract String getName();

    /**
     * Returns a user-friendly name for this plot.
     *
     * @return a user-friendly name for this plot
     */
    public String getNiceName() {
        return getName();
    }
    
    /**
     * Draws this plot.
     *
     * @param graphics the graphics object onto which to draw this plot
     */
    public abstract void draw(Graphics2D graphics);

    /**
     * Reports whether this plot is visible.
     *
     * @return {@code true} if this plot is visible; {@code false} if it is
     * hidden
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets whether this plot should be drawn.
     *
     * @param visible {@code true} to draw this plot; {@code false} not to draw
     * it
     */
    public void setVisible(boolean visible) {
        final boolean oldVisibility = this.visible;
        this.visible = visible;
        if (this.visible != oldVisibility) {
            fireVisibilityChangedNotification(this.visible);
        }
    }
    
    /**
     * Adds a listener for sample clicks to this plot. When the user clicks
     * on a plot element representing a sample, the
     * {@link SampleClickListener#sampleClicked(net.talvi.puffinplot.data.Sample)}
     * method of the supplied listener will be invoked. The listener can be
     * removed using the
     * {@link Plot#removeSampleClickListener(net.talvi.puffinplot.plots.SampleClickListener)}
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
    
    /**
     * Adds a listener for visibility changes to this plot.
     *
     * @see
     * Plot#removeVisibilityChangedListener(Consumer)
     *
     * @param listener the listener to add
     */
    public void addVisibilityChangedListener(Consumer<Boolean> listener) {
        visibilityChangedListeners.add(listener);
    }
    
    /**
     * Removes a listener for visibility changes to this plot.
     *
     * @see Plot#addVisibilityChangedListener(Consumer)
     *
     * @param listener the listener to remove
     */
    public void removeVisibilityChangedListener(Consumer<Boolean> listener) {
        visibilityChangedListeners.remove(listener);
    }

    private void fireVisibilityChangedNotification(Boolean newVisibility) {
        visibilityChangedListeners
                .forEach(listener -> listener.accept(newVisibility));
    }
}
