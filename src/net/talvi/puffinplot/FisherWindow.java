package net.talvi.puffinplot;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.talvi.puffinplot.plots.FisherEqAreaPlot;

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

    public FisherEqAreaPlot getPlot() {
        return (FisherEqAreaPlot) graphDisplay.plots.get("fisherplot");
    }
    
}
