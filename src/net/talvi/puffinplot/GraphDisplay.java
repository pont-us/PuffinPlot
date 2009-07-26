package net.talvi.puffinplot;

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
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.Printable;
import java.text.AttributedString;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import net.talvi.puffinplot.plots.Plot;

public abstract class GraphDisplay extends JPanel implements Printable {

    private static final long serialVersionUID = -5730958004698337302L;
    protected Map<String,Plot> plots;
    private static final RenderingHints renderingHints = PuffinRenderingHints.getInstance();
    protected AffineTransform zoomTransform;
    private final GdMouseListener mouseListener;
    private boolean dragPlotMode = false;
    private static final AlphaComposite WEAK_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f);
    private static final AlphaComposite STRONG_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .8f);
         
    GraphDisplay() {
        
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(getPreferredSize());
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);

        setLayout(null);
        plots = new HashMap<String,Plot>();

        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
        
        mouseListener = new GdMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
    }

    public String getPlotSizeString() {
        StringBuilder sb = new StringBuilder();
        for (Plot plot: plots.values()) {
            sb.append(plot.getName());
            sb.append(" ");
            sb.append(plot.dimensionsAsString());
            sb.append(" ");
        }
        return sb.toString();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        AffineTransform savedTransform = g2.getTransform();
        g2.transform(zoomTransform);
        super.paint(g2); // draws background and any components
        g2.setRenderingHints(renderingHints);
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();

        if (!isDragPlotMode()) {
            for (Plot plot: plots.values()) plot.draw(g2);
            if (mouseListener.pointSelectionCorner != null &&
                    mouseListener.startPoint != null) {
                Rectangle2D r = new Rectangle2D.Double();
                r.add(mouseListener.pointSelectionCorner);
                r.add(mouseListener.startPoint);
                //g2.draw(r);
                //g2.draw(new Line2D.Double(mouseListener.startPoint,
                //        mouseListener.pointSelectionCorner));
            }
        } else {
            for (Plot plot : plots.values()) {
                float fontSize = plot.getFontSize() * 2;
                int margin = plot.getMargin();
                g2.setPaint(Color.ORANGE);
                g2.setComposite(WEAK_COMPOSITE);
                Rectangle2D d = plot.getDimensions();
                g2.fill(d);
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMaxX() - margin, d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), d.getWidth(), margin));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMaxY() - margin, d.getWidth(), margin));
                AttributedString as = new AttributedString(plot.getNiceName());
                as.addAttribute(TextAttribute.FAMILY, "SansSerif");
                as.addAttribute(TextAttribute.SIZE, fontSize);
                as.addAttribute(TextAttribute.WEIGHT, TextAttribute.WEIGHT_ULTRABOLD);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
                plot.draw(g2);
                g2.setPaint(Color.BLUE);
                g2.setComposite(STRONG_COMPOSITE);
                g2.drawString(as.getIterator(),
                        (float) d.getMinX()+margin, (float) d.getMinY()+margin+fontSize);
            }
        }
        
        g2.setTransform(savedTransform);
    }

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
    
    protected boolean isDragPlotMode() {
        return dragPlotMode;
    }

    protected void setDragPlotMode(boolean dragPlotMode) {
        this.dragPlotMode = dragPlotMode;
    }

    void resetLayout() {
        for (Plot plot: plots.values()) plot.setDimensionsToDefault();
        repaint();
    }

    /**
     * We can't use a MouseAdapter because it crashes OS X Java with a 
     * ClassCastException. No idea why.
     */
    protected class GdMouseListener
    implements MouseListener, MouseMotionListener {
        private Point2D startPoint;
        private Rectangle2D oldDims; // Plot's dimensions before drag started
        private Plot draggee; // the plot currently being dragged
        private static final int LEFT=1, TOP=2, RIGHT=4, BOTTOM=8, ALLSIDES=15;
        private int sides;
        private Point2D pointSelectionCorner;
        
        public void mouseClicked(MouseEvent e) {
            final Point2D position = getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot: plots.values())
                if (plot.getDimensions().contains(position))
                    plot.mouseClicked(position, e);
            repaint();
        }
        
        public void mouseReleased(MouseEvent e) {
            draggee = null;
            repaint();
        }
        
        public void mousePressed(MouseEvent e) {
            draggee = null;
            startPoint = getAntiZoom().transform(e.getPoint(), null);
            if (!isDragPlotMode()) return;            
            double smallestSize = Double.MAX_VALUE; // when plots overlap, pick the smallest
            for (Plot plot : plots.values()) {
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
                pointSelectionCorner = thisPoint;
                repaint();
            }
        }

        public Plot getDraggingPlot() {
            return draggee;
        }

        public void mouseEntered(MouseEvent arg0) {}
        public void mouseExited(MouseEvent arg0) {}
        public void mouseMoved(MouseEvent arg0) {}
    }

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
        for (Plot plot: plots.values()) plot.draw(g2);
        printChildren(graphics);
        setDoubleBuffered(true);
    }
}
