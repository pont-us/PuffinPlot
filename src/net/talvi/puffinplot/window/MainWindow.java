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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Sample;

/**
 * The main window of the PuffinPlot application. The window is chiefly
 * occupied by {@link MainGraphDisplay}, which shows a configurable 
 * selection of plots. Various controls and information panels are arranged
 * around the edges.
 * 
 * @see MainGraphDisplay
 * @see MainMenuBar
 * @see ControlPanel
 * @see SampleChooser
 * 
 * @author pont
 */
public final class MainWindow extends JFrame {

    private static final long serialVersionUID = -9075963924200708871L;
    private final MainGraphDisplay graphDisplay;
    private final ControlPanel controlPanel;
    private final JScrollPane scrollPane;
    private final SampleChooser sampleChooser;
    private final MainMenuBar menuBar;
    private final JPanel welcomeMessage;
    private final SampleDataPanel sampleDataPanel;
    private final JSplitPane splitPane;
    private final int splitPaneDividerWidth;
    private final PuffinApp app;

    /**
     * Creates a new main window.
     */
    public MainWindow() {
        app = PuffinApp.getInstance();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {app.quit();}});
        setTitle("PuffinPlot");
        setPreferredSize(new Dimension(800,400));
        menuBar = new MainMenuBar();
        setJMenuBar(menuBar);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        // mainPanel.add(graphDisplay = new GraphDisplay(), BorderLayout.CENTER);
        graphDisplay = new MainGraphDisplay();
        scrollPane = new JScrollPane(getGraphDisplay());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                scrollPane, sampleDataPanel = new SampleDataPanel());
        splitPane.setResizeWeight(1.0);
        splitPane.setOneTouchExpandable(true);
        splitPaneDividerWidth = splitPane.getDividerSize();
        splitPane.setDividerSize(0);
        // jsp.setMaximumSize(new Dimension(1000,700));
        mainPanel.add(splitPane);
        mainPanel.add(welcomeMessage = new WelcomeMessage(),
                BorderLayout.NORTH);
        splitPane.setVisible(false);
        mainPanel.add(sampleChooser = new SampleChooser(), BorderLayout.WEST);
        // mainPanel.add(sampleDataPanel = new SampleDataPanel(), BorderLayout.EAST);
        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.add(controlPanel = new ControlPanel());
        cp.add(mainPanel);
        setMaximumSize(scrollPane.getMaximumSize());
        pack();
        setLocationRelativeTo(null); // centre on screen
    }
    
    private class WelcomeMessage extends JPanel {
        public WelcomeMessage() {
            super();
            setBorder(new EmptyBorder(12, 12, 12, 12));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            final PuffinApp.Version version = app.getVersion();
            final String welcome =
                    String.format(Locale.ENGLISH,
                            "Welcome to PuffinPlot, version %s. "
                                    + "This puffin hatched on %s.",
                            version.getVersionString(),
                    version.getDateString());
            add(new JLabel(welcome));
            add(new JLabel(String.format(Locale.ENGLISH,
                    "PuffinPlot is copyright %s by Pontus Lurcock.",
                    version.getYearRange())));
            final JPanel citePanel = new JPanel();
            citePanel.setBorder(new EmptyBorder(12, 0, 12, 12));
            citePanel.setLayout(new BoxLayout(citePanel, BoxLayout.X_AXIS));
            citePanel.add(new JLabel("If you use PuffinPlot in a published work, please "));
            JButton citeMeButton =
                    new JButton("click here to cite the PuffinPlot paper.");
            citePanel.add(citeMeButton);
            citeMeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    PuffinApp.getInstance().getCiteWindow().setVisible(true);
                }
            });
            citePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(citePanel);
            add(new JLabel("PuffinPlot is distributed under the GNU General Public License."));
            add(new JLabel("Select ‘About PuffinPlot’ from the Help menu for details."));
            
        }
    }

    /**
     * Returns this window's sample chooser.
     * @return this window's sample chooser
     */
    public SampleChooser getSampleChooser() {
        return sampleChooser;
    }
    
    /**
     * Updates this window's sample data panel. This forces the display
     * to reflect any changes in the current data.
     */
    public void updateSampleDataPanel() {
        splitPane.setDividerSize(sampleDataPanel.isVisible() ? 
                splitPaneDividerWidth : 0);
        splitPane.resetToPreferredSizes();
    }
    
    /**
     * Informs this window that the list of currently loaded data suites
     * has changed. Calling this method allows this window to update its display
     * accordingly.
     */
    public void suitesChanged() {
        sampleChooser.updateSuite();
        getControlPanel().updateSuites();
        final int numSuites = app.getSuites().size();
        welcomeMessage.setVisible(numSuites == 0);
        splitPane.setVisible(numSuites > 0);
        repaint();
    }

    /**
     * Informs this window that the current sample has changed.
     * Calling this method allows this window to update its display
     * accordingly.
     */
    public void sampleChanged() {
        getControlPanel().updateSample();
        final Sample sample = app.getSample();
        if (sample==null) return;
        sampleDataPanel.setSample(sample);
        repaint(100);
        getMainMenuBar().sampleChanged();
    }
    
    /**
     * Returns this window's menu bar.
     * @return this window's menu bar
     */
    public MainMenuBar getMainMenuBar() {
        return menuBar;
    }

    /**
     * Returns this window's graph display.
     * @return this window's graph display
     */
    public MainGraphDisplay getGraphDisplay() {
        return graphDisplay;
    }

    /**
     * Returns the control panel for the plots displayed in this window.
     * @return the control panel for the plots displayed in this window
     */
    public ControlPanel getControlPanel() {
        return controlPanel;
    }
}
