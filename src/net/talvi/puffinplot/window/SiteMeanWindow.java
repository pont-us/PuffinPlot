/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
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
