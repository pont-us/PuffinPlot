package net.talvi.puffinplot.plots;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

import net.talvi.puffinplot.PlotParams;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;

public abstract class Plot extends JComponent
implements MouseListener {

    double plotPointSize = 0.015;
    List<PlotPoint> points;
    AffineTransform transform;
    boolean withLines = false;
    private boolean breakLine = false;
    public static final RenderingHints renderingHints = new PRenderingHints();
    final protected PlotParams params;
    
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
    
    class PlotPoint {

        private final Shape shape;
        private final Shape highlight;
        private final Datum datum;
        private final boolean filled;
        private final boolean noLine;
        private final boolean special;
        private final Point2D centre;
        private static final double highlightSize = 1.6;
        
        PlotPoint(Datum datum, Point2D centre,
                boolean filled, boolean noLine, boolean special) {
            double size = ( getVirtualWidth() * plotPointSize / 2 );
            this.centre = centre;
            this.datum = datum;
            shape = new Rectangle2D.Double(centre.getX()-size, centre.getY()-size, 2*size, 2*size);
            double hs = highlightSize;
            highlight = new Rectangle2D.Double(centre.getX()-size*hs, centre.getY()-size*hs,
                    2*size*hs, 2*size*hs);
            this.filled = filled;
            this.noLine = noLine;
            this.special = special;
        }
        
        void draw(Graphics2D g) {
            if (datum.selected) g.setColor(Color.RED);
            else g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke());
            g.draw(shape);
            if (special) g.draw(highlight);
            if (filled) g.fill(shape);
        }

        public Point2D getCentre() {
            return centre;
        }
    }
    
    public Plot(PlotParams params) {
        super();
        this.params = params;
        setOpaque(false);
        setMaximumSize(new Dimension(600, 600));
        setPreferredSize(new Dimension(600, 600));
        setVisible(true);
        points = new LinkedList<PlotPoint>();
        addMouseListener(this);
    }
    
    void clearPoints() {
        points.clear();
    }
    
    void addPoint(Datum d, Point2D p, boolean filled, boolean special) {
        points.add(new PlotPoint(d, p, filled, breakLine, special));
        breakLine = false;
    }
    
    void addPoint(Datum d, Point2D p, boolean filled) {
        addPoint(d, p, filled, false);
    }

    void breakLine() {
        breakLine  = true;
    }
    
    void addPoint(Datum d, double x, double y, boolean filled) {
        addPoint(d, new Point2D.Double(x,y), filled);
    }
    
    void addPoint(Datum d, double x, double y, boolean filled, boolean special) {
        addPoint(d, new Point2D.Double(x,y), filled, special);
    }
    
    void drawPoints(Graphics2D g) {
        Point2D prev = null;
        for (int i=0; i<points.size(); i++) {
            points.get(i).draw(g);
            if (withLines) {
                if (i>0 && !points.get(i).noLine) {
                    g.setColor(Color.BLACK);
                    g.draw(new Line2D.Double(prev, points.get(i).centre));
                }
                prev = points.get(i).centre;
            }
        }
        for (PlotPoint s: points) s.draw(g);
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {
        for (PlotPoint s: points) {
            try {
                if (s.shape.contains(transform.inverseTransform(e.getPoint(), null))) {
                    s.datum.toggleSel();
                }
            } catch (NoninvertibleTransformException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        PuffinApp.app.mainWindow.graphDisplay.repaint();
    }
    
    protected int getVirtualWidth() { return 500; }
    
    protected int getVirtualHeight() { return 1000; }
}
