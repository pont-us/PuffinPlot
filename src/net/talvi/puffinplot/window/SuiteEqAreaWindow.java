package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.plots.SeparateSuiteEaPlot;

/**
 * A window to hold a {@link SuiteEqAreaDisplay}.
 * 
 * @see SuiteEqAreaDisplay
 * 
 * @author pont
 */
public class SuiteEqAreaWindow extends JFrame {
    private SuiteEqAreaDisplay graphDisplay;

    /**
     * Creates a new suite equal-area window.
     */
    public SuiteEqAreaWindow() {
        setPreferredSize(new Dimension(600, 600));
        setTitle("Suite equal-area plot");
        JPanel contentPane = graphDisplay = new SuiteEqAreaDisplay();
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }

    /**
     * Returns the single equal-area plot contained in this window's 
     * graph display.
     * 
     * @return the equal-area plot in this window's graph display
     */
    public SeparateSuiteEaPlot getPlot() {
        return (SeparateSuiteEaPlot) graphDisplay.plots.get("fisherplot");
    }
    
}
