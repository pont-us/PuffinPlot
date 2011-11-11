package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.JFrame;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.GreatCircles;
import net.talvi.puffinplot.plots.SiteEqAreaPlot;

public class GreatCircleWindow extends JFrame {
    private GreatCircleDisplay graphDisplay;

    public GreatCircleWindow() {
        setPreferredSize(new Dimension(600, 600));
        setTitle("Great circles");
        GraphDisplay contentPane = graphDisplay = new GreatCircleDisplay();
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }

    public SiteEqAreaPlot getPlot() {
        return (SiteEqAreaPlot) graphDisplay.plots.get("greatcircles");
    }
}
