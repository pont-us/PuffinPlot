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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.TransferHandler;

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Sample;

/**
 * The main window of the PuffinPlot application. The window is chiefly occupied
 * by {@link MainGraphDisplay}, which shows a configurable selection of plots.
 * Various controls and information panels are arranged around the edges.
 * * 
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
    private final JPanel mainPanel;
    private final JPanel statusBar;
    private final JLabel statusLabel;

    /**
     * Creates a new main window.
     * 
     * @param app The PuffinPlot instance associated with this window
     */
    private MainWindow(PuffinApp app) {
        this.app = app;
        menuBar = new MainMenuBar(app);
        graphDisplay = new MainGraphDisplay(app);
        scrollPane = new JScrollPane(getGraphDisplay());
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                scrollPane, sampleDataPanel = new SampleDataPanel());
        splitPaneDividerWidth = splitPane.getDividerSize();
        mainPanel = new JPanel();
        controlPanel = new ControlPanel(app);
        welcomeMessage = WelcomeMessage.getInstance(app);
        sampleChooser = new SampleChooser(app);
        statusBar = new JPanel();
        statusLabel = new JLabel("Status bar");        
    }
    
    private void addComponents() {
    
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                MainWindow.this.app.quit();}});
        setTitle("PuffinPlot");
        setPreferredSize(new Dimension(1000, 700));
        setJMenuBar(menuBar);
        
        statusBar.setLayout(new FlowLayout(FlowLayout.LEADING));
        statusBar.add(statusLabel);
        
        mainPanel.setLayout(new BorderLayout());
                mainPanel.add(welcomeMessage,
                BorderLayout.NORTH);
        mainPanel.add(sampleChooser, BorderLayout.WEST);
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        splitPane.setResizeWeight(1.0);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(0);
        mainPanel.add(splitPane);
        splitPane.setVisible(false);
        
        final Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.add(controlPanel);
        contentPane.add(mainPanel);
        setMaximumSize(scrollPane.getMaximumSize());
        
        setTransferHandler(handler);
        
        graphDisplay.addCurrentTreatmentStepListener(d ->
                statusLabel.setText(d == null ? "|" : d.toSummaryString()));
        
        pack();
        setLocationRelativeTo(null); // centre on screen
    }
    
    /**
     * Creates and returns a new main window object.
     * The constructor is private.
     * 
     * @param app the PuffinApp instance to associate with the window
     * @return a new main window object
     */
    public static MainWindow getInstance(PuffinApp app) {
        final MainWindow mw = new MainWindow(app);
        mw.addComponents();
        return mw;
    }

    /**
     * Returns this window's sample chooser.
     *
     * @return this window's sample chooser
     */
    public SampleChooser getSampleChooser() {
        return sampleChooser;
    }
    
    /**
     * Updates this window's sample data panel. This forces the display to
     * reflect any changes in the current data.
     */
    public void updateSampleDataPanel() {
        splitPane.setDividerSize(sampleDataPanel.isVisible() ? 
                splitPaneDividerWidth : 0);
        splitPane.resetToPreferredSizes();
    }
    
    /**
     * Informs this window that the list of currently loaded data suites has
     * changed. This method should also be called when the name of a suite
     * changes, and when samples are added to or removed from the current suite.
     * Calling this method allows this window to update its display accordingly.
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
     * Informs this window that the current sample has changed. Calling this
     * method allows this window to update its display accordingly.
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
     *
     * @return this window's menu bar
     */
    public MainMenuBar getMainMenuBar() {
        return menuBar;
    }

    /**
     * Returns this window's graph display.
     *
     * @return this window's graph display
     */
    public MainGraphDisplay getGraphDisplay() {
        return graphDisplay;
    }

    /**
     * Returns the control panel for the plots displayed in this window.
     *
     * @return the control panel for the plots displayed in this window
     */
    public ControlPanel getControlPanel() {
        return controlPanel;
    }
    
    private final TransferHandler handler = new TransferHandler() {
        @Override
        public boolean canImport(TransferSupport trSupp) {
            return trSupp.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        public boolean importData(TransferSupport trSupp) {
            if (!canImport(trSupp)) {
                return false;
            }
            
            final Transferable transferable = trSupp.getTransferable();

            try {
                final List<File> files = (List<File>)
                        transferable.getTransferData(
                                DataFlavor.javaFileListFlavor);
                app.openFiles(files, true);
                
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }

            return true;
        }
    };
}
