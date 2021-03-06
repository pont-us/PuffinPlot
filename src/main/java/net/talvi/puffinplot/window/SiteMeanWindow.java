/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import net.talvi.puffinplot.plots.PlotParams;
import javax.swing.JFrame;

/**
 * A window containing a site mean graph display.
 * 
 * @see SiteMeanDisplay
 * 
 * @author pont
 */
public class SiteMeanWindow extends JFrame {
    private final SiteMeanDisplay graphDisplay;

    /**
     * Creates a new site mean window.
     * 
     * @param params parameters controlling the site mean graph in this
     * window's graph display
     */
    public SiteMeanWindow(PlotParams params) {
        setTitle("Site equal-area plot");
        graphDisplay = new SiteMeanDisplay(params);
        setContentPane(graphDisplay);
        pack();
        setLocationRelativeTo(null); // centre on screen
    }
}
