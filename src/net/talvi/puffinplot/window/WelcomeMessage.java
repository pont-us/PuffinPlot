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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.window;

import java.awt.Component;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.Version;

/**
 * A welcome message for the main window.
 * 
 * This component is displayed in the main window when no
 * suite is loaded, and gives some basic information about
 * the running version of PuffinPlot.
 *
 * @author pont
 */
final class WelcomeMessage extends JPanel {

    private WelcomeMessage() {
        super();
    }
    
    private void createComponents(PuffinApp app) {
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        final Version version = PuffinApp.getInstance().getVersion();
        final String welcome = String.format(Locale.ENGLISH,
                "Welcome to PuffinPlot, version %s. This puffin hatched on %s.",
                version.getVersionString(), version.getDateString());
        add(new JLabel(welcome));
        add(new JLabel(String.format(Locale.ENGLISH,
                "PuffinPlot is copyright %s by Pontus Lurcock.",
                version.getYearRange())));
        final JPanel citePanel = new JPanel();
        citePanel.setBorder(new EmptyBorder(12, 0, 12, 12));
        citePanel.setLayout(new BoxLayout(citePanel, BoxLayout.X_AXIS));
        citePanel.add(new JLabel("If you use PuffinPlot in a published work, please "));
        final JButton citeMeButton = new JButton(app.getActions().openCiteWindow);
        citePanel.add(citeMeButton);
        // Override the default text for the action.
        citeMeButton.setText("click here to cite the PuffinPlot paper.");
        citePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(citePanel);
        add(new JLabel("PuffinPlot is distributed under the GNU General Public License."));
        add(new JLabel("Select ‘About PuffinPlot’ from the Help menu for details."));
    }
    
    /**
     * Return a new welcome message component.
     * 
     * This is a factory method to instantiate a new object.
     * The constructor is private.
     * 
     * @param app The PuffinPlot app to which this message should relate.
     * @return a new welcome message component
     */
    public static final WelcomeMessage getInstance(PuffinApp app) {
        final WelcomeMessage wm = new WelcomeMessage();
        wm.createComponents(app);
        return wm;
    }
}
