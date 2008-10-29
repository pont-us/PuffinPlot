package net.talvi.puffinplot;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.DataTable;
import net.talvi.puffinplot.plots.DemagPlot;
import net.talvi.puffinplot.plots.EqAreaPlot;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.ZPlot;

public class GraphDisplay extends JPanel implements Printable {

    private static final long serialVersionUID = -5730958004698337302L;
    JPanel leftPanel;
    private transient PlotParams params;
    private Sample[] samples = null;
    private int printPageIndex = -1;
    private List<Plot> plots;
    public static final RenderingHints renderingHints = new PRenderingHints();
    public AffineTransform zoomTransform;
    private final GdMouseListener mouseListener;
    private final int dragMargin = 32;
    private boolean dragPlotMode = false;

    public boolean isDragPlotMode() {
        return dragPlotMode;
    }

    public void setDragPlotMode(boolean dragPlotMode) {
        this.dragPlotMode = dragPlotMode;
    }
    
    static class PRenderingHints extends RenderingHints {
        PRenderingHints() {
            super(null);
            put(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
            put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            put(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_DEFAULT);
            put(KEY_DITHERING, VALUE_DITHER_DEFAULT);
            put(KEY_FRACTIONALMETRICS, VALUE_FRACTIONALMETRICS_DEFAULT);
            put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
            put(KEY_RENDERING, VALUE_RENDER_QUALITY);
            put(KEY_STROKE_CONTROL, VALUE_STROKE_PURE);
            put(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
            // KEY_TEXT_LCD_CONTRAST left at default.
        }
    }
         
    GraphDisplay() {
        
        params = new PlotParams() {

            public Sample getSample() {
                return samples==null
                       ? PuffinApp.getApp().getCurrentSample()
                        : samples[printPageIndex];
            }

            public Correction getCorrection() {
                return PuffinApp.getApp().currentCorrection();
            }

            public MeasurementAxis getAxis() {
                return PuffinApp.getApp().getMainWindow().controlPanel.getAxis();
            }

            public MeasType getMeasType() {
                return PuffinApp.getApp().getCurrentSuite().getMeasType();
            }
            
        };
        
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(new Dimension(1200, 800));
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);

        setLayout(null);
        plots = new LinkedList<Plot>();
        plots.add(new EqAreaPlot(this, params, new Rectangle2D.Double(700, 100, 300, 300)));
        plots.add(new ZPlot(this, params, new Rectangle2D.Double(100, 100, 500, 400)));
        plots.add(new DemagPlot(this, params, new Rectangle2D.Double(100, 550, 300, 200)));
        plots.add(new DataTable(this, params, new Rectangle2D.Double(600, 500, 400, 400)));
        
        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
        
        mouseListener = new GdMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
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
        for (Plot plot: plots) plot.draw(g2);

        if (isDragPlotMode()) {
            for (Plot plot : plots) {
                g2.setPaint(Color.ORANGE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f));
                Rectangle2D d = plot.getDimensions();
                g2.fill(d);
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), dragMargin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMaxX() - dragMargin, d.getMinY(), dragMargin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), d.getWidth(), dragMargin));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMaxY() - dragMargin, d.getWidth(), dragMargin));
            }
        }
        
        g2.setTransform(savedTransform);
    }

    public AffineTransform getAntiZoom() {
            try {
                return zoomTransform.createInverse();
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(GraphDisplay.class.getName()).log(Level.SEVERE, null, ex);
                // This is a "can't-happen" so rethrowing an error is appropriate.
                // It will be caught by the default handler.
                throw new Error(ex);
            }
        }
    
    /**
     * We can't use a MouseAdapter because it crashes OS X Java with a 
     * ClassCastException. No idea why.
     */
    private class GdMouseListener 
    implements MouseListener, MouseMotionListener {
        private Point2D startPoint;
        private Rectangle2D startDimensions;
        private Plot draggingPlot;
        private static final int LEFT=1, TOP=2, RIGHT=4, BOTTOM=8, ALLSIDES=15;
        private int sides;
        
        @Override
        public void mouseClicked(MouseEvent e) {
            final Point2D position = getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot: plots)
                if (plot.getDimensions().contains(position))
                    plot.mouseClicked(position);
            repaint();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            draggingPlot = null;
            repaint();
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            draggingPlot = null;
            if (!isDragPlotMode()) return;            
            startPoint = getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot : plots)
                if (plot.getDimensions().contains(startPoint))
                    draggingPlot = plot;
            if (getDraggingPlot() != null) {
                startDimensions =
                        (Rectangle2D) getDraggingPlot().getDimensions().clone();
                double relX = (startPoint.getX() - startDimensions.getMinX());
                double relY = (startPoint.getY() - startDimensions.getMinY());
                int sidesTmp = 0;
                if (relX < dragMargin) sidesTmp |= LEFT;
                if (relX > startDimensions.getWidth() - dragMargin) sidesTmp |= RIGHT;
                if (relY < dragMargin) sidesTmp |= TOP;
                if (relY > startDimensions.getHeight() - dragMargin) sidesTmp |= BOTTOM;
                if (sidesTmp==0) sidesTmp = ALLSIDES;
                sides = sidesTmp;
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragPlotMode() && getDraggingPlot() != null) {
                Point2D thisPoint = getAntiZoom().transform(e.getPoint(), null);
                double dx = thisPoint.getX() - startPoint.getX();
                double dy = thisPoint.getY() - startPoint.getY();
                double x0 = startDimensions.getMinX();
                double x1 = startDimensions.getMaxX();
                double y0 = startDimensions.getMinY();
                double y1 = startDimensions.getMaxY();
                if ((sides & LEFT)!=0) x0+=dx;
                if ((sides & TOP)!=0) y0+=dy;
                if ((sides & RIGHT)!=0) x1+=dx;
                if ((sides & BOTTOM)!=0) y1+=dy;
                if (x1-x0 < 3*dragMargin) {
                    x0 = getDraggingPlot().getDimensions().getMinX();
                    x1 = getDraggingPlot().getDimensions().getMaxX();
                } 
                if (y1-y0 < 3*dragMargin) {
                    y0 = getDraggingPlot().getDimensions().getMinY();
                    y1 = getDraggingPlot().getDimensions().getMaxY();
                }
                    Rectangle2D newDims = new Rectangle2D.Double(x0, y0, x1 - x0, y1 - y0);
                getDraggingPlot().setDimensions(newDims);
                repaint();
            }
        }

        public Plot getDraggingPlot() {
            return draggingPlot;
        }

        public void mouseEntered(MouseEvent arg0) {
        }

        public void mouseExited(MouseEvent arg0) {
        }

        public void mouseMoved(MouseEvent arg0) {
        }
    }

    public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
        pf.setOrientation(PageFormat.LANDSCAPE);
        if (samples == null) samples = PuffinApp.getApp().getSelectedSamples();
        if (pageIndex >= samples.length) {
            samples = null; // we've finished printing
            return NO_SUCH_PAGE;
        }
        printPageIndex = pageIndex;
        setDoubleBuffered(false);
        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        //double xScale = pf.getImageableWidth() / getWidth();
        //double yScale = pf.getImageableHeight() / getHeight();
        //double scale = Math.min(xScale, yScale);
        g2.scale(0.5, 0.5);
        for (Plot plot: plots) plot.draw(g2);
        printChildren(graphics);
        setDoubleBuffered(true);
        return PAGE_EXISTS;
    }

}
