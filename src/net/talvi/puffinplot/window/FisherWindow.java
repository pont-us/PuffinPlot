package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.plots.FisherPlot;

/**
 * A window to hold a {@link FisherGraphDisplay}.
 * 
 * @see FisherGraphDisplay
 * 
 * @author pont
 */
public class FisherWindow extends JFrame {
    private FisherGraphDisplay graphDisplay;

    /**
     * Creates a new Fisher window.
     */
    public FisherWindow() {
        setPreferredSize(new Dimension(600, 600));
        setTitle("Suite equal-area plot");
        JPanel contentPane = graphDisplay = new FisherGraphDisplay();
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }

    /**
     * Returns the single Fisher plot contained in this window's 
     * graph display.
     * 
     * @return the Fisher plot in this window's graph display
     */
    public FisherPlot getPlot() {
        return (FisherPlot) graphDisplay.plots.get("fisherplot");
    }
    
}
