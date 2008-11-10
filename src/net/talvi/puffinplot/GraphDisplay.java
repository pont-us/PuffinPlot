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
import java.util.HashMap;
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
import net.talvi.puffinplot.plots.FisherTable;
import net.talvi.puffinplot.plots.PcaTable;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.SampleTable;
import net.talvi.puffinplot.plots.ZPlot;

public class GraphDisplay extends JPanel implements Printable {

    private static final long serialVersionUID = -5730958004698337302L;
    private transient PlotParams params;
    private Sample[] samples = null;
    private int printPageIndex = -1;
    private HashMap<String, Plot> plots;
    private static final RenderingHints renderingHints = new PRenderingHints();
    private AffineTransform zoomTransform;
    private final GdMouseListener mouseListener;
    private boolean dragPlotMode = false;
    
    static private final class PRenderingHints extends RenderingHints {
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
        
        final PuffinApp app = PuffinApp.getApp();
        params = new PlotParams() {

            public Sample getSample() {
                // TODO: looks a bit hacky, fix or document
                return samples==null
                       ? app.getSample()
                        : samples[printPageIndex];
            }

            public Correction getCorrection() {
                return app.getCorrection();
            }

            public MeasurementAxis getAxis() {
                return app.getMainWindow().controlPanel.getAxis();
            }

            public MeasType getMeasType() {
                return app.getSuite().getMeasType();
            }
            
        };
        
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(new Dimension(1200, 800));
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);

        setLayout(null);
        plots = new HashMap<String, Plot>();
        PuffinPrefs pref = PuffinApp.getApp().getPrefs();
        plots.put("equarea", new EqAreaPlot(this, params, pref.getPlotSize("equarea")));
        plots.put("zplot", new ZPlot(this, params, pref.getPlotSize("zplot")));
        plots.put("demag", new DemagPlot(this, params, pref.getPlotSize("demag")));
        plots.put("datatable", new DataTable(this, params, pref.getPlotSize("datatable")));
        plots.put("pcatable", new PcaTable(this, params, pref.getPlotSize("pcatable")));
        plots.put("sampletable", new SampleTable(this, params, pref.getPlotSize("sampletable")));
        plots.put("fishertable", new FisherTable(this, params, pref.getPlotSize("fishertable")));
        
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
        for (Plot plot: plots.values()) plot.draw(g2);

        if (isDragPlotMode()) {
            for (Plot plot : plots.values()) {
                int margin = plot.getMargin();
                g2.setPaint(Color.ORANGE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f));
                Rectangle2D d = plot.getDimensions();
                g2.fill(d);
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMaxX() - margin, d.getMinY(), margin, d.getHeight()));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMinY(), d.getWidth(), margin));
                g2.fill(new Rectangle2D.Double(d.getMinX(), d.getMaxY() - margin, d.getWidth(), margin));
            }
        }
        
        g2.setTransform(savedTransform);
    }

    private AffineTransform getAntiZoom() {
            try {
                return zoomTransform.createInverse();
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(GraphDisplay.class.getName()).log(Level.SEVERE, null, ex);
                // This is a "can't-happen" so rethrowing an error is appropriate.
                // It will be caught by the default handler.
                throw new Error(ex);
            }
        }
    
    public boolean isDragPlotMode() {
        return dragPlotMode;
    }

    public void setDragPlotMode(boolean dragPlotMode) {
        this.dragPlotMode = dragPlotMode;
    }

    /**
     * We can't use a MouseAdapter because it crashes OS X Java with a 
     * ClassCastException. No idea why.
     */
    private class GdMouseListener 
    implements MouseListener, MouseMotionListener {
        private Point2D startPoint;
        private Rectangle2D oldDims; // Plot's dimensions before drag started
        private Plot draggee; // the plot currently being dragged
        private static final int LEFT=1, TOP=2, RIGHT=4, BOTTOM=8, ALLSIDES=15;
        private int sides;
        
        public void mouseClicked(MouseEvent e) {
            final Point2D position = getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot: plots.values())
                if (plot.getDimensions().contains(position))
                    plot.mouseClicked(position);
            repaint();
        }
        
        public void mouseReleased(MouseEvent e) {
            draggee = null;
            repaint();
        }
        
        public void mousePressed(MouseEvent e) {
            draggee = null;
            if (!isDragPlotMode()) return;            
            startPoint = getAntiZoom().transform(e.getPoint(), null);
            for (Plot plot : plots.values())
                if (plot.getDimensions().contains(startPoint))
                    draggee = plot;
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
            if (isDragPlotMode() && getDraggingPlot() != null) {
                Point2D thisPoint = getAntiZoom().transform(e.getPoint(), null);
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
            }
        }

        public Plot getDraggingPlot() {
            return draggee;
        }

        public void mouseEntered(MouseEvent arg0) {}
        public void mouseExited(MouseEvent arg0) {}
        public void mouseMoved(MouseEvent arg0) {}
    }

    public int print(Graphics graphics, PageFormat pf, int pageIndex)
            throws PrinterException {
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
        double xScale = pf.getImageableWidth() / getWidth();
        double yScale = pf.getImageableHeight() / getHeight();
        double scale = Math.min(xScale, yScale);
        g2.scale(scale, scale);
        g2.setPaint(Color.BLACK);
        g2.setPaintMode();
        for (Plot plot: plots.values()) plot.draw(g2);
        printChildren(graphics);
        setDoubleBuffered(true);
        return PAGE_EXISTS;
    }
    
    Rectangle2D getPlotSize(String plotName) {
        return plots.get(plotName).getDimensions();
    }

}
