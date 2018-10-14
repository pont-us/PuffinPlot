/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
    private final SuiteEqAreaDisplay graphDisplay;

    /**
     * Creates a new suite equal-area window.
     * 
     * @param app the application instance for which to create the window
     */
    public SuiteEqAreaWindow(PuffinApp app) {
        setPreferredSize(new Dimension(600, 600));
        setTitle("Suite equal-area plot");
        JPanel contentPane = graphDisplay = new SuiteEqAreaDisplay(app.getPrefs().getPrefs());
        contentPane.setOpaque(true); //content panes must be opaque
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(app.getMainWindow());
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
