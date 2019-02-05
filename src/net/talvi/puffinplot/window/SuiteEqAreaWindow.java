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

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.plots.SeparateSuiteEqualAreaPlot;

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
        setTitle("Suite equal-area plot");
        final JPanel contentPane = graphDisplay = new SuiteEqAreaDisplay(
                app.getPlotParams(), app.getPrefs().getPrefs());
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
    public SeparateSuiteEqualAreaPlot getPlot() {
        return (SeparateSuiteEqualAreaPlot)
                graphDisplay.plots.get(SeparateSuiteEqualAreaPlot.class);
    }
    
}
