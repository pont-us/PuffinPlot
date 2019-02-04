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
package net.talvi.puffinplot.window;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.plots.Plot;
import org.apache.batik.dom.GenericDOMImplementation;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.freehep.util.UserProperties;
import org.w3c.dom.DOMImplementation;

/**
 * A graphical UI component which lays out and draws one or more plots. It may
 * allow the user to resize and rearrange the plots, and provides facilities to
 * export the display to a file and to print it via the standard Java printing
 * interface. This is an abstract superclass which cannot be instantiated
 * directly; concrete subclasses must implement the {@code print} method.
 *
 * @author pont
 */
public abstract class GraphDisplay extends JPanel implements Printable {
    
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    
    private static final long serialVersionUID = -5730958004698337302L;

    /**
     * A map from internal plot names to the plots themselves.
     */
    protected Map<Class, Plot> plots;

    /**
     * A transformation applied to the graphics before painting them, intended
     * to be used for zooming in and out of the display. It is initially set to
     * the identity transformation.
     */
    protected AffineTransform zoomTransform;

    private final GdMouseListener mouseListener;
    private boolean dragPlotMode = false;
    private boolean draggingSelection = false;
    private static final AlphaComposite WEAK_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f);
    private static final AlphaComposite STRONG_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .8f);
    private final List<CurrentTreatmentStepListener>
            currentTreatmentStepListeners = new LinkedList<>();
         
    GraphDisplay() {
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(getPreferredSize());
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);
        setLayout(null);
        /*
         * LinkedHashMap because ZplotLegend has to be drawn after Zplot, since
         * it must read the order of magnitude from the axes.
         */
        plots = new LinkedHashMap<>();
        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
        mouseListener = new GdMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }

    /**
     * Returns a string representation of all this display's plot sizes 
     * and positions. This is intended to be written to a preferences object, 
     * allowing the plot layout to be saved and restored.
     * 
     * @return a string representation of all this display's plot sizes
     * and positions
     */
    public String getPlotSizeString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Plot plot: plots.values()) {
            stringBuilder.append(plot.getName());
            stringBuilder.append(" ");
            stringBuilder.append(plot.getDimensionsAsString());
            stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }

    /**
     * Returns a list of the plots which are currently visible.
     *
     * @return a list of the plots which are currently visible
     */
    public List<Plot> getVisiblePlots() {
        final Collection<Plot> allPlots = getPlots();
        final List<Plot> result = new ArrayList<>(allPlots.size());
        for (Plot plot: allPlots) {
            if (plot.isVisible()) result.add(plot);
        }
        return result;
    }

    /**
     * Returns a collection of all the plots in this graph display.
     *
     * @return all the plots in this graph display.
     */
    public Collection<Plot> getPlots() {
        return plots.values();
    }
    
    /**
     * Returns the plot with a specified class name.
     * 
     * @return the plot with the given class, or {@code null} if there is none
     */
    public Plot getPlotByClass(Class plotClass) {
        return plots.get(plotClass);
    }

    /**
     * Paints this graph display to a graphics context.
     *
     * @param g the graphics context in which to paint
     */
    @Override
    public void paint(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        final AffineTransform savedTransform = g2.getTransform();
        g2.transform(zoomTransform);
        super.paint(g2); // draws background and any components
        g2.setRenderingHints(PuffinRenderingHints.getInstance());
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();
        final List<Plot> visiblePlots = getVisiblePlots();

        if (!isDragPlotMode()) {
            if (draggingSelection) {
                final Rectangle2D r = new Rectangle2D.Double();
                r.setFrameFromDiagonal(mouseListener.startPoint,
                        mouseListener.currentDragPoint);
                final Rectangle2D rUnzoomed = new Rectangle2D.Double();
                rUnzoomed.setFrameFromDiagonal(
                        getAntiZoom().transform(mouseListener.startPoint, null),
                        getAntiZoom().transform(mouseListener.currentDragPoint,
                                null));
                for (Plot plot : visiblePlots) {
                    final boolean rightButton =
                            mouseListener.getCurrentButton() ==
                            MouseEvent.BUTTON3;
                    plot.selectByRectangle(rUnzoomed, !rightButton);
                }
                for (Plot plot : visiblePlots) plot.draw(g2);
                g2.setColor(Color.ORANGE);
                g2.setComposite(WEAK_COMPOSITE);
                g2.fill(r);
                g2.setComposite(STRONG_COMPOSITE);
                g2.draw(r);
            } else {
                for (Plot plot: visiblePlots) {
                    plot.draw(g2);
                }
            }
        } else {
            for (Plot plot: visiblePlots) {
                float fontSize = plot.getFontSize() * 1.5f;
                int margin = plot.getMargin();
                g2.setPaint(Color.ORANGE);
                g2.setComposite(WEAK_COMPOSITE);
                Rectangle2D d = plot.getDimensions();
                g2.fill(d);
                g2.fill(new Rectangle2D.Double(d.getMinX(),
                        d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMaxX() - margin,
                        d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(),
                        d.getWidth(), margin));
                g2.fill(new Rectangle2D.Double(d.getMinX(),
                        d.getMaxY() - margin, d.getWidth(), margin));
                g2.setComposite(AlphaComposite.
                        getInstance(AlphaComposite.SRC_OVER, .5f));
                plot.draw(g2);
                g2.setPaint(Color.BLUE);
                g2.setComposite(STRONG_COMPOSITE);
                String[] nameParts = plot.getNiceName().split(" ");
                float yPos = (float) d.getMinY()+margin;
                for (String namePart: nameParts) {
                    final AttributedString as = new AttributedString(namePart);
                    as.addAttribute(TextAttribute.FAMILY, "SansSerif");
                    as.addAttribute(TextAttribute.SIZE, fontSize);
                    as.addAttribute(TextAttribute.WEIGHT,
                            TextAttribute.WEIGHT_ULTRABOLD);
                    yPos += fontSize;
                    g2.drawString(as.getIterator(),
                        (float) d.getMinX()+margin, yPos);
                }
            }
        }
        
        g2.setTransform(savedTransform);
    }

    /**
     * Returns the inverse transform of the zoom transform. This can be used,
     * for example, to map mouse clicks back to the graph display's original,
     * untransformed co-ordinates.
     *
     * @return the inverse transform of the zoom transform
     */
    protected AffineTransform getAntiZoom() {
        try {
            return zoomTransform.createInverse();
        } catch (NoninvertibleTransformException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            /*
             * This is a "can't-happen" so rethrowing an error is appropriate.
             * It will be caught by the default handler.
             */
            throw new Error(ex);
        }
    }
    
    /**
     * Reports whether the plots are currently draggable by the user.
     *
     * @return {@code true} if the plots are currently draggable by the user
     */
    protected boolean isDragPlotMode() {
        return dragPlotMode;
    }

    /**
     * Sets whether the plots are draggable by the user.
     *
     * @param dragPlotMode {@code true} to make the plots draggable;
     * {@code false} to make them non-draggable
     */
    protected void setDragPlotMode(boolean dragPlotMode) {
        this.dragPlotMode = dragPlotMode;
    }

    /**
     * Resets each plot's size and position to their defaults.
     */
    public void resetLayout() {
        for (Plot plot: plots.values()) {
            plot.setDimensionsToDefault();
        }
        repaint();
    }

    /**
     * A listener to handle mouse clicks and movements on this graph display.
     */
    private class GdMouseListener
    implements MouseListener, MouseMotionListener {
        /*
         * We can't use a MouseAdapter because it crashes OS X Java with a 
         * ClassCastException. No idea why.
         */
        private Point2D startPoint;
        private Rectangle2D oldDims; // Plot's dimensions before drag started
        private Plot draggee; // the plot currently being dragged
        private static final int LEFT=1, TOP=2, RIGHT=4, BOTTOM=8, ALLSIDES=15;
        private int sides;
        private Point2D currentDragPoint;
        private TreatmentStep previousTreatmentStep;
        private int currentButton;
        
        /**
         * Handles a mouse click event.
         *
         * @param e the mouse click event
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            final Point2D position =
                    getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot: plots.values())
                if (plot.getDimensions().contains(position))
                    plot.mouseClicked(position, e);
            repaint();
        }
        
        /**
         * Handles a mouse button release event.
         *
         * @param e the mouse button release event
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            draggee = null;
            draggingSelection = false;
            repaint();
        }

        private void updateDraggingPlot() {
             // When plots overlap, pick the smallest.
            double smallestSize = Double.MAX_VALUE;
            for (Plot plot: getVisiblePlots()) {
                Rectangle2D dims = plot.getDimensions();
                if (dims.contains(startPoint)) {
                    double size = dims.getWidth() * dims.getHeight();
                    if (size < smallestSize) {
                        draggee = plot;
                        smallestSize = size;
                    }
                }
            }
            if (getDraggingPlot() != null) {
                oldDims =
                        (Rectangle2D) getDraggingPlot().getDimensions().clone();
                final double relX = (startPoint.getX() - oldDims.getMinX());
                final double relY = (startPoint.getY() - oldDims.getMinY());
                int sidesTmp = 0;
                final double margin = getDraggingPlot().getMargin();
                if (relX < margin) {
                    sidesTmp |= LEFT;
                }
                if (relX > oldDims.getWidth() - margin) {
                    sidesTmp |= RIGHT;
                }
                if (relY < margin) {
                    sidesTmp |= TOP;
                }
                if (relY > oldDims.getHeight() - margin) {
                    sidesTmp |= BOTTOM;
                }
                if (sidesTmp == 0) {
                    sidesTmp = ALLSIDES;
                }
                sides = sidesTmp;
            }
        }

        /**
         * Handles a mouse button press event.
         *
         * @param e the mouse button press event
         */
        @Override
        public void mousePressed(MouseEvent e) {
            draggee = null;
            startPoint = currentDragPoint = 
                    getAntiZoom().transform(e.getPoint(), null);
            if (isDragPlotMode()) {
                updateDraggingPlot();
            }
            else draggingSelection = true;
            currentButton = e.getButton();
        }
        
        /**
         * Handles a mouse drag event.
         *
         * @param e the mouse drag event
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            final Point2D thisPoint =
                    getAntiZoom().transform(e.getPoint(), null);
            if (isDragPlotMode() && getDraggingPlot() != null) {
                final double dx = thisPoint.getX() - startPoint.getX();
                final double dy = thisPoint.getY() - startPoint.getY();
                double x0 = oldDims.getMinX();
                double x1 = oldDims.getMaxX();
                double y0 = oldDims.getMinY();
                double y1 = oldDims.getMaxY();
                
                if ((sides & LEFT) != 0) {
                    x0 += dx;
                }
                if ((sides & TOP) != 0) {
                    y0 += dy;
                }
                if ((sides & RIGHT) != 0) {
                    x1 += dx;
                }
                if ((sides & BOTTOM) != 0) {
                    y1 += dy;
                }
                
                final double dragMargin = getDraggingPlot().getMargin();
                // Check for minimum dimensions
                if (x1 - x0 < 3 * dragMargin) {
                    x0 = getDraggingPlot().getDimensions().getMinX();
                    x1 = getDraggingPlot().getDimensions().getMaxX();
                }
                if (y1 - y0 < 3 * dragMargin) {
                    y0 = getDraggingPlot().getDimensions().getMinY();
                    y1 = getDraggingPlot().getDimensions().getMaxY();
                }
                final Rectangle2D newDimensions =
                        new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0);
                getDraggingPlot().setDimensions(newDimensions);
                repaint();
            } else {
                currentDragPoint = thisPoint;
                repaint();
            }
        }

        /**
         * Returns the plot currently being dragged, if any.
         *
         * @return the plot currently being dragged, if any
         */
        public Plot getDraggingPlot() {
            return draggee;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            final Point2D currentMovePoint = e.getPoint();
            TreatmentStep currentTreatmentStep = null;
            for (Plot plot: getPlots()) {
                if (!plot.isVisible()) {
                    continue;
                }
                final TreatmentStep step =
                        plot.getDatumForPosition(currentMovePoint);
                if (step != null) {
                    currentTreatmentStep = step;
                    break;
                }
            }
            if (currentTreatmentStep == previousTreatmentStep) {
                return;
            }
            for (CurrentTreatmentStepListener listener:
                    currentTreatmentStepListeners) {
                listener.treatmentStepChanged(currentTreatmentStep);
            }
            previousTreatmentStep = currentTreatmentStep;
        }

        /**
         * Returns the currently depressed mouse button.
         *
         * @return the currently depressed mouse button.
         */
        public int getCurrentButton() {
            return currentButton;
        }
    }

    /**
     * Prints all this display's plots to a graphics context.
     *
     * @param pf the page format for printing
     * @param graphics the graphics context to which to draw
     */
    protected void printPlots(PageFormat pf, Graphics graphics) {
        setDoubleBuffered(false);
        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        double xScale = pf.getImageableWidth() / getWidth();
        double yScale = pf.getImageableHeight() / getHeight();
        double scale = Math.min(xScale, yScale);
        g2.scale(scale, scale);
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();
        for (Plot plot: getVisiblePlots()) plot.draw(g2);
        printChildren(graphics);
        setDoubleBuffered(true);
    }
    
    /**
     * Writes the contents of this display to an SVG file using the Batik
     * library.
     *
     * @param filename the name of the file to which to write
     */
    public void saveToSvgBatik(String filename) {
        final DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();
        final String svgNS = "http://www.w3.org/2000/svg";
        final org.w3c.dom.Document document =
                domImpl.createDocument(svgNS, "svg", null);
        final org.apache.batik.svggen.SVGGraphics2D svgGenerator =
                new org.apache.batik.svggen.SVGGraphics2D(document);
        svgGenerator.setUnsupportedAttributes(null);
        paint(svgGenerator);
        //for (Plot plot: getVisiblePlots()) plot.draw(svgGenerator);
        final boolean useCssAttributes = true;
        Writer writer = null;
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(filename);
            writer = new OutputStreamWriter(outputStream, "UTF-8");
            svgGenerator.stream(writer, useCssAttributes);
        } catch (IOException ex) {
            try {
                if (writer != null) writer.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException ex2) {
                LOGGER.log(Level.SEVERE, null, ex2);
            }
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Writes the contents of this display to an SVG file using the FreeHEP
     * library.
     *
     * @param filename the name of the file to which to write
     * @throws java.io.IOException if there is an error while writing the file
     *
     */
    public void saveToSvgFreehep(String filename) throws IOException {
        final SVGGraphics2D graphics =
                new org.freehep.graphicsio.svg.SVGGraphics2D(new File(filename),
                this);
        final UserProperties userProps = new UserProperties();
        userProps.setProperty(SVGGraphics2D.TEXT_AS_SHAPES, false);
        graphics.setProperties(userProps);
        graphics.startExport();
        print(graphics);
        graphics.endExport();
        graphics.dispose();
    }
    
    /**
     * Add a listener for changes to the current datum (i.e. the datum related
     * to the point under the mouse pointer). A listener can be added multiple
     * times, and will in that case be called multiple times.
     *
     * @param listener the datum change listener to add
     */
    public void addCurrentDatumListener(CurrentTreatmentStepListener listener) {
        currentTreatmentStepListeners.add(listener);
    }
    
    /**
     * Remove a datum change listener. If the specified listener has not been
     * added, nothing will happen and no exception will be thrown. If the same
     * listener has been added multiple times and there are multiple instances
     * of it in the list of listeners, this method will only remove the first
     * instance. This method can be called repeatedly to remove multiple
     * instances of the same listener.
     *
     * @param listener the datum change listener to remove
     */
    public void removeCurrentDatumListener(
            CurrentTreatmentStepListener listener) {
        currentTreatmentStepListeners.remove(listener);
    }
}
