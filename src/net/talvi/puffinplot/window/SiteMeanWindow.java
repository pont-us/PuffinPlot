package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.JFrame;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.plots.SiteEqAreaPlot;

/**
 * A window containing a site mean graph display.
 * 
 * @see SiteMeanDisplay
 * 
 * @author pont
 */
public class SiteMeanWindow extends JFrame {
    private SiteMeanDisplay graphDisplay;

    /**
     * Creates a new site mean window.
     */
    public SiteMeanWindow() {
        setPreferredSize(new Dimension(600, 600));
        setTitle("Site equal-area plot");
        GraphDisplay contentPane = graphDisplay = new SiteMeanDisplay();
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }

    /**
     * Returns the single plot in this window's graph display.
     * 
     * @return the plot in this window's graph display.
     */
    public SiteEqAreaPlot getPlot() {
        return (SiteEqAreaPlot) graphDisplay.plots.get("greatcircles");
    }
}
