package net.talvi.puffinplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.plots.DataDisplay;
import net.talvi.puffinplot.plots.DemagPlot;
import net.talvi.puffinplot.plots.EqAreaPlot;
import net.talvi.puffinplot.plots.Plot;
import net.talvi.puffinplot.plots.ZPlot;

public class GraphDisplay extends JPanel implements Printable {

    private static final long serialVersionUID = -5730958004698337302L;
    JPanel leftPanel;
    private PlotParams params;
    private Sample[] samples = null;
    private int printPageIndex = -1;
    private ZPlot zPlot;
    private EqAreaPlot eaPlot;
    private DemagPlot demagPlot;
    private DataDisplay dataDisplay;
    private static final RenderingHints renderingHints = new PRenderingHints();
    List<PlotPoint> points = new LinkedList<PlotPoint>();
    private AffineTransform zoomTransform;
    
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
 
    private class PlotPoint {

        private final Shape shape;
        private final Shape highlight;
        private final Datum datum;
        private final boolean filled;
        private final boolean lineToHere;
        private final boolean special;
        private final Point2D centre;
        private static final double highlightSize = 1.6;
        private static final double plotPointSize = 5;

        PlotPoint(Datum datum, Point2D centre,
                boolean filled, boolean lineToHere, boolean special) {
            double size = (plotPointSize / 2);
            this.centre = centre;
            this.datum = datum;
            shape = new Rectangle2D.Double(centre.getX() - size, centre.getY() - size, 2 * size, 2 * size);
            double hs = highlightSize;
            highlight = new Rectangle2D.Double(centre.getX() - size * hs, centre.getY() - size * hs,
                    2 * size * hs, 2 * size * hs);
            this.filled = filled;
            this.lineToHere = lineToHere;
            this.special = special;
        }

        void draw(Graphics2D g) {
            g.setColor(datum.selected ? Color.RED: Color.BLACK);
            g.setStroke(new BasicStroke());
            g.draw(shape);
            if (special) g.draw(highlight);
            if (filled) g.fill(shape);
        }

        public Point2D getCentre() {
            return centre;
        }
    }
        
    GraphDisplay() {
        
        params = new PlotParams() {

            public Sample getSample() {
                return samples==null
                       ? PuffinApp.app.getCurrentSample()
                        : samples[printPageIndex];
            }

            public Correction getCorrection() {
                return PuffinApp.app.currentCorrection();
            }

            public MeasurementAxis getAxis() {
                return PuffinApp.app.mainWindow.controlPanel.getAxis();
            }

            public MeasType getMeasType() {
                return PuffinApp.app.getCurrentSuite().getMeasType();
            }
            
        };
        
        setPreferredSize(new Dimension(1200, 800));
        setMaximumSize(new Dimension(1200, 800));
        zoomTransform = AffineTransform.getScaleInstance(1.0, 1.0);

        setLayout(null);
        eaPlot = new EqAreaPlot(params, this);
        zPlot = new ZPlot(params, this);
        demagPlot = new DemagPlot(params, this);
        dataDisplay = new DataDisplay(params);
        add(dataDisplay);
        dataDisplay.setLocation(600, 500);
        dataDisplay.setSize(400, 400);
        
        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
        
        addMouseListener(new GdMouseListener());
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g); // draws background

        Graphics2D g2 = (Graphics2D) g;
        
        AffineTransform savedTransform = g2.getTransform();
        g2.transform(zoomTransform);
        drawGraphs(g2);
        g2.setTransform(savedTransform);
    }
    
    private void drawGraphs(Graphics2D g2) {
                g2.setRenderingHints(renderingHints);
        points.clear();
        zPlot.paint(g2, 100, 100, 500, 400);
        demagPlot.paint(g2, 100, 550, 300, 200);
        eaPlot.paint(g2, 700, 100, 300, 300);
        
        Point2D prev = null;
        for (int i=0; i<points.size(); i++) {
            points.get(i).draw(g2);
            if (i > 0 && points.get(i).lineToHere) {
                g2.setColor(Color.BLACK);
                g2.draw(new Line2D.Double(prev, points.get(i).centre));
            }
            prev = points.get(i).centre;
        }
        for (PlotPoint s: points) s.draw(g2);
    }
    

    private class GdMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            AffineTransform antiZoom;
            try {
                antiZoom = zoomTransform.createInverse();
            } catch (NoninvertibleTransformException ex) {
                Logger.getLogger(GraphDisplay.class.getName()).log(Level.SEVERE, null, ex);
                // This is a "can't-happen" so rethrowing an error is appropriate.
                // It will be caught by the default handler.
                throw new Error(ex);
            }
            for (PlotPoint s : points) {
                if (s.shape.contains(antiZoom.transform(e.getPoint(), null))) {
                    s.datum.toggleSel();
                }
                PuffinApp.app.mainWindow.graphDisplay.repaint();
            }
        }
    }

    public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
        pf.setOrientation(PageFormat.LANDSCAPE);
        if (samples == null) samples = PuffinApp.app.getSelectedSamples();
        if (pageIndex >= samples.length) {
            samples = null; // we've finished printing
            return NO_SUCH_PAGE;
        }
        printPageIndex = pageIndex;
        setDoubleBuffered(false);
        dataDisplay.setDoubleBuffered(false);
        dataDisplay.p.setDoubleBuffered(false);
        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        //double xScale = pf.getImageableWidth() / getWidth();
        //double yScale = pf.getImageableHeight() / getHeight();
        //double scale = Math.min(xScale, yScale);
        g2.scale(0.5, 0.5);
        drawGraphs(g2);
        printChildren(graphics);
        setDoubleBuffered(true);
        dataDisplay.setDoubleBuffered(true);
        dataDisplay.p.setDoubleBuffered(true);
        return PAGE_EXISTS;
    }
    
    public void addPoint(Datum d, Point2D p, boolean filled, boolean special, boolean line) {
        points.add(new PlotPoint(d, p, filled, line, special));
    }
}
