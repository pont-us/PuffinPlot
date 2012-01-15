package net.talvi.puffinplot.window;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.Printable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import net.talvi.puffinplot.plots.Plot;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;

/**
 * <p>A graphical UI component which lays out and draws one or more plots.
 * It may allow the user to resize and rearrange the plots,
 * and provides facilities to export the display to a 
 * file and to print it via the standard Java printing interface.
 * This is an abstract superclass which cannot be instantiated
 * directly; concrete subclasses must implement the {@code print}
 * method.</p>
 * 
 * @author pont
 */
public abstract class GraphDisplay extends JPanel implements Printable {
    private static final long serialVersionUID = -5730958004698337302L;
    /** A map from internal plot names to the plots themselves. */
    protected Map<String,Plot> plots;
    private static final RenderingHints renderingHints = PuffinRenderingHints.getInstance();
    /** A transformation applied to the graphics before painting them,
     * intended to be used for zooming in and out of the display.
     * It is initially set to the identity transformation. */
    protected AffineTransform zoomTransform;
    private final GdMouseListener mouseListener;
    private boolean dragPlotMode = false;
    private boolean draggingSelection = false;
    private static final AlphaComposite WEAK_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f);
    private static final AlphaComposite STRONG_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .8f);
         
    GraphDisplay() {
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(getPreferredSize());
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);

        setLayout(null);
        // LinkedHashMap because ZplotLegend has to be drawn after
        // Zplot, since it must read the order of magnitude from the
        // axes.
        plots = new LinkedHashMap<String,Plot>();

        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
        
        mouseListener = new GdMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }

    /**
     * <p>Returns a string representation of all this display's plot sizes 
     * and positions. This is intended to be written to a preferences object, 
     * allowing the plot layout to be saved and restored.</p>
     * 
     * @return a string representation of all this display's plot sizes
     * and positions
     */
    public String getPlotSizeString() {
        StringBuilder sb = new StringBuilder();
        for (Plot plot: plots.values()) {
            sb.append(plot.getName());
            sb.append(" ");
            sb.append(plot.getDimensionsAsString());
            sb.append(" ");
        }
        return sb.toString();
    }

    /** Returns a list of the plots which are currently visible.
     * @return a list of the plots which are currently visible */
    public List<Plot> getVisiblePlots() {
        final Collection<Plot> ps = getPlots();
        final List<Plot> result = new ArrayList<Plot>(ps.size());
        for (Plot p: ps) if (p.isVisible()) result.add(p);
        return result;
    }

    /** Returns a collection of all the plots in this graph display. 
     * @return all the plots in this graph display. */
    public Collection<Plot> getPlots() {
        return plots.values();
    }

    /** Paints this graph display to a graphics context. 
     * @param g the graphics context in which to paint */
    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform savedTransform = g2.getTransform();
        g2.transform(zoomTransform);
        super.paint(g2); // draws background and any components
        g2.setRenderingHints(renderingHints);
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();
        final List<Plot> visiblePlots = getVisiblePlots();

        if (!isDragPlotMode()) {
            if (draggingSelection) {
                Rectangle2D r = new Rectangle2D.Double();
                r.setFrameFromDiagonal(mouseListener.startPoint,
                        mouseListener.currentDragPoint);
                Rectangle2D rUnzoomed = new Rectangle2D.Double();
                rUnzoomed.setFrameFromDiagonal(getAntiZoom().transform(mouseListener.startPoint, null),
                        getAntiZoom().transform(mouseListener.currentDragPoint, null));
                for (Plot plot : visiblePlots) plot.selectByRectangle(rUnzoomed);
                for (Plot plot : visiblePlots) plot.draw(g2);
                g2.setColor(Color.ORANGE);
                g2.setComposite(WEAK_COMPOSITE);
                g2.fill(r);
                g2.setComposite(STRONG_COMPOSITE);
                g2.draw(r);
            } else {
                for (Plot plot : visiblePlots) plot.draw(g2);
            }
        } else {
            for (Plot plot : visiblePlots) {
                float fontSize = plot.getFontSize() * 1.5f;
                int margin = plot.getMargin();
                g2.setPaint(Color.ORANGE);
                g2.setComposite(WEAK_COMPOSITE);
                Rectangle2D d = plot.getDimensions();
                g2.fill(d);
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMaxX() - margin, d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), d.getWidth(), margin));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMaxY() - margin, d.getWidth(), margin));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
                plot.draw(g2);
                g2.setPaint(Color.BLUE);
                g2.setComposite(STRONG_COMPOSITE);
                String[] nameParts = plot.getNiceName().split(" ");
                float yPos = (float) d.getMinY()+margin;
                for (String namePart: nameParts) {
                    final AttributedString as = new AttributedString(namePart);
                    as.addAttribute(TextAttribute.FAMILY, "SansSerif");
                    as.addAttribute(TextAttribute.SIZE, fontSize);
                    as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_ULTRABOLD);
                    yPos += fontSize;
                    g2.drawString(as.getIterator(),
                        (float) d.getMinX()+margin, yPos);
                }
            }
        }
        
        g2.setTransform(savedTransform);
    }

    /** <p>Returns the inverse transform of the zoom transform.
     * This can be used, for example, to map mouse clicks back
     * to the graph display's original, untransformed co-ordinates.</p>
     * @return the inverse transform of the zoom transform */
    protected AffineTransform getAntiZoom() {
        try {
            return zoomTransform.createInverse();
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(GraphDisplay.class.getName()).log(Level.SEVERE, null, ex);
            // This is a "can't-happen" so rethrowing an error is appropriate.
            // It will be caught by the default handler.
            throw new Error(ex);
        }
    }
    
    /** Reports whether the plots are currently draggable by the user. 
     * @return {@code true} if the plots are currently draggable by the user */
    protected boolean isDragPlotMode() {
        return dragPlotMode;
    }

    /** Sets whether the plots are draggable by the user.
     * @param dragPlotMode {@code true} to make the plots draggable;
     * {@code false} to make them non-draggable */
    protected void setDragPlotMode(boolean dragPlotMode) {
        this.dragPlotMode = dragPlotMode;
    }

    /** Resets each plot's size and position to their defaults. */
    public void resetLayout() {
        for (Plot plot: plots.values()) plot.setDimensionsToDefault();
        repaint();
    }

    /** A listener to handle mouse clicks and movements on this 
      * graph display.*/
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
        
        /** Handles a mouse click event.
         * @param e the mouse click event */
        public void mouseClicked(MouseEvent e) {
            final Point2D position = getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot: plots.values())
                if (plot.getDimensions().contains(position))
                    plot.mouseClicked(position, e);
            repaint();
        }
        
        /** Handles a mouse button release event.
         * @param e the mouse button release event */
        public void mouseReleased(MouseEvent e) {
            draggee = null;
            draggingSelection = false;
            repaint();
        }

        private void updateDraggingPlot() {
            double smallestSize = Double.MAX_VALUE; // when plots overlap, pick the smallest
            for (Plot plot : getVisiblePlots()) {
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
                double relX = (startPoint.getX() - oldDims.getMinX());
                double relY = (startPoint.getY() - oldDims.getMinY());
                int sidesTmp = 0;
                double margin = getDraggingPlot().getMargin();
                if (relX < margin) sidesTmp |= LEFT;
                if (relX > oldDims.getWidth() - margin) sidesTmp |= RIGHT;
                if (relY < margin) sidesTmp |= TOP;
                if (relY > oldDims.getHeight() - margin) sidesTmp |= BOTTOM;
                if (sidesTmp==0) sidesTmp = ALLSIDES;
                sides = sidesTmp;
            }
        }

        /** Handles a mouse button press event.
         * @param e the mouse button press event */
        public void mousePressed(MouseEvent e) {
            draggee = null;
            startPoint = currentDragPoint = getAntiZoom().transform(e.getPoint(), null);
            if (isDragPlotMode()) updateDraggingPlot();
            else draggingSelection = true;
        }
        
        /** Handles a mouse drag event. 
         * @param e the mouse drag event */
        public void mouseDragged(MouseEvent e) {
            Point2D thisPoint = getAntiZoom().transform(e.getPoint(), null);
            if (isDragPlotMode() && getDraggingPlot() != null) {
                double dx = thisPoint.getX() - startPoint.getX();
                double dy = thisPoint.getY() - startPoint.getY();
                double x0 = oldDims.getMinX();
                double x1 = oldDims.getMaxX();
                double y0 = oldDims.getMinY();
                double y1 = oldDims.getMaxY();
                if ((sides & LEFT)!=0) x0+=dx;
                if ((sides & TOP)!=0) y0+=dy;
                if ((sides & RIGHT)!=0) x1+=dx;
                if ((sides & BOTTOM)!=0) y1+=dy;
                
                double dragMargin = getDraggingPlot().getMargin();
                // Check for minimum dimensions
                if (x1-x0 < 3*dragMargin) {
                    x0 = getDraggingPlot().getDimensions().getMinX();
                    x1 = getDraggingPlot().getDimensions().getMaxX();
                } 
                if (y1-y0 < 3*dragMargin) {
                    y0 = getDraggingPlot().getDimensions().getMinY();
                    y1 = getDraggingPlot().getDimensions().getMaxY();
                }
                Rectangle2D newDims =
                        new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0);
                getDraggingPlot().setDimensions(newDims);
                repaint();
            } else {
                currentDragPoint = thisPoint;
                repaint();
            }
        }

        /** Returns the plot currently being dragged, if any. 
         * @return the plot currently being dragged, if any */
        public Plot getDraggingPlot() {
            return draggee;
        }

        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}
        public void mouseMoved(MouseEvent e) {}
    }

    /** Prints all this display's plots to a graphics context.
     * @param pf the page format for printing
     * @param graphics the graphics context to which to draw */
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

    /** Writes the contents of this display to an SVG file.
     * @param filename the name of the file to which to write */
    public void saveToSvg(String filename) {
        // Get a DOMImplementation.
        DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();
        // Create an instance of org.w3c.dom.Document.
        String svgNS = "http://www.w3.org/2000/svg";
        org.w3c.dom.Document document =
                domImpl.createDocument(svgNS, "svg", null);
        // Create an instance of the SVG Generator.
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        // svgGenerator.setUnsupportedAttributes(null);
        // TODO work out why setUnsupportedAttributes doesn't work.
        paint(svgGenerator);
        //for (Plot plot: getVisiblePlots()) plot.draw(svgGenerator);
        // Finally, stream out SVG to the standard output using
        // UTF-8 encoding.
        boolean useCSS = true; // we want to use CSS style attributes
        Writer out;
        try {
            out = new FileWriter(filename);
            svgGenerator.stream(out, useCSS);
        } catch (IOException ex) {
            Logger.getLogger(GraphDisplay.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
