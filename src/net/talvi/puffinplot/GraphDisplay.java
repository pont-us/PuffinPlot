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
import javax.swing.JPanel;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
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
    private static final RenderingHints renderingHints = new PRenderingHints();
    List<PlotPoint> points = new LinkedList<PlotPoint>();
    private boolean breakLine;
    private boolean withLines;
    
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
        private final boolean noLine;
        private final boolean special;
        private final Point2D centre;
        private static final double highlightSize = 1.6;
        double plotPointSize = 5;

        PlotPoint(Datum datum, Point2D centre,
                boolean filled, boolean noLine, boolean special) {
            double size = (/*getVirtualWidth() * */ plotPointSize / 2);
            this.centre = centre;
            this.datum = datum;
            shape = new Rectangle2D.Double(centre.getX() - size, centre.getY() - size, 2 * size, 2 * size);
            double hs = highlightSize;
            highlight = new Rectangle2D.Double(centre.getX() - size * hs, centre.getY() - size * hs,
                    2 * size * hs, 2 * size * hs);
            this.filled = filled;
            this.noLine = noLine;
            this.special = special;
        }

        void draw(Graphics2D g) {
            if (datum.selected)
                g.setColor(Color.RED);
            else
                g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke());
            g.draw(shape);
            if (special)
                g.draw(highlight);
            if (filled)
                g.fill(shape);
        }

        public Point2D getCentre() {
            return centre;
        }
    }

    protected int getVirtualWidth() { return 2970; }
   
    protected int getVirtualHeight() { return 2100; }
        
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

        eaPlot = new EqAreaPlot(params, this);
        zPlot = new ZPlot(params, this);
        demagPlot = new DemagPlot(params, this);

        setOpaque(true);
        setBackground(Color.WHITE);
        setVisible(true);
        
        addMouseListener(new GdMouseListener());
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g); // draws background
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHints(renderingHints);
        points.clear();
        zPlot.paint(g2, 100, 500, 300, 300);
        demagPlot.paint(g2, 100, 100, 300, 300);
        eaPlot.paint(g2, 600, 100, 300, 300);
        
        Point2D prev = null;
        for (int i=0; i<points.size(); i++) {
            points.get(i).draw(g2);
            if (withLines) {
                if (i>0 && !points.get(i).noLine) {
                    g2.setColor(Color.BLACK);
                    g2.draw(new Line2D.Double(prev, points.get(i).centre));
                }
                prev = points.get(i).centre;
            }
        }
        for (PlotPoint s: points) s.draw(g2);
    }
    

    private class GdMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
        for (PlotPoint s: points) {
                if (s.shape.contains(e.getPoint())) {
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
        Graphics2D g2 = (Graphics2D) graphics;
        g2.translate(pf.getImageableX(), pf.getImageableY());
        //double xScale = pf.getImageableWidth() / getWidth();
        //double yScale = pf.getImageableHeight() / getHeight();
        //double scale = Math.min(xScale, yScale);
        g2.scale(0.5, 0.5);
        // leftPanel.setDoubleBuffered(false);
        //g2.setRenderingHints(renderingHints);
        g2.setColor(Color.BLACK);
        g2.drawString("TEST", 250, 250);

        points.clear();
        zPlot.paint(g2, 100, 500, 300, 300);
        demagPlot.paint(g2, 100, 100, 300, 300);
        eaPlot.paint(g2, 600, 100, 300, 300);
        for (PlotPoint s: points) s.draw(g2);
        // printChildren(g);
        // leftPanel.setDoubleBuffered(true);
        return PAGE_EXISTS;
    }
    
    public void addPoint(Datum d, double x, double y, boolean filled) {
        addPoint(d, new Point2D.Double(x,y), filled, false);
    }
    
    public void addPoint(Datum d, double x, double y, boolean filled, boolean special) {
        addPoint(d, new Point2D.Double(x,y), filled, special);
    }
    
    public void addPoint(Datum d, Point2D p, boolean filled, boolean special) {
        points.add(new PlotPoint(d, p, filled, breakLine, special));
        breakLine = false;
    }
}
