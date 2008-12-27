package net.talvi.puffinplot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.talvi.puffinplot.plots.FisherEqAreaPlot;

public class FisherWindow extends JFrame {

    private FisherEqAreaPlot plot;
    
    private class FisherPanel extends JPanel {
        
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
        
    }
    
    public FisherWindow() {
        setPreferredSize(new Dimension(800, 800));
        JPanel newContentPane = new FisherPanel();
        plot = new FisherEqAreaPlot(null, null, new Rectangle2D.Double(50, 50, 700, 700));
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);
        pack();
        setVisible(true);
    }
    
}
