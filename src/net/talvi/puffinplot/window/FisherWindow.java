package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.talvi.puffinplot.plots.FisherPlot;

public class FisherWindow extends JFrame {
    private FisherGraphDisplay graphDisplay;

    public FisherWindow() {
        setPreferredSize(new Dimension(600, 600));
        setTitle("Fisher analysis");
        JPanel contentPane = graphDisplay = new FisherGraphDisplay();
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
    }

    public FisherPlot getPlot() {
        return (FisherPlot) graphDisplay.plots.get("fisherplot");
    }
    
}
