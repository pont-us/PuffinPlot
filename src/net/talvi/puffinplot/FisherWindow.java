package net.talvi.puffinplot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.talvi.puffinplot.plots.FisherEqAreaPlot;

public class FisherWindow extends JFrame {

    private final FisherEqAreaPlot plot;
    // private final JPanel contentPane;
    
    private class FisherPanel extends JPanel implements Printable {
        
        public FisherPanel() {
            setBackground(Color.WHITE);
        }
        
        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHints(PuffinRenderingHints.getInstance());
            super.paint(g2);
            plot.draw(g2);
        }

        public int print(Graphics graphics, PageFormat pf, int pageIndex) throws PrinterException {
                    pf.setOrientation(PageFormat.LANDSCAPE);
            if (pageIndex > 0) return NO_SUCH_PAGE;

            setDoubleBuffered(false);
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double xScale = pf.getImageableWidth() / getWidth();
            double yScale = pf.getImageableHeight() / getHeight();
            double scale = Math.min(xScale, yScale);
            g2.scale(scale, scale);
            g2.setPaint(Color.BLACK);
            g2.setPaintMode();
            plot.draw(g2);
            printChildren(graphics);
            setDoubleBuffered(true);
            return PAGE_EXISTS;
        }
        
    }
    
    public FisherWindow() {
        setPreferredSize(new Dimension(800, 800));
        setTitle("Fisher analysis");
        JPanel contentPane = new FisherPanel();
        plot = new FisherEqAreaPlot(null, null, new Rectangle2D.Double(50, 50, 700, 700));
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
    }
    
}
