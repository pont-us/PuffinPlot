package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import net.talvi.puffinplot.data.Sample;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = -9075963924200708871L;
    private final MainGraphDisplay graphDisplay;
    public final ControlPanel controlPanel;
    private JScrollPane scrollPane;
    SampleChooser sampleChooser;
    private final MainMenuBar menuBar;
    private JLabel welcomeMessage;
    private final SampleDataPanel sampleDataPanel;
    public final JSplitPane splitPane;
    private int splitPaneDividerWidth;

    public MainWindow() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new CloseListener());
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
        mainPanel.add(welcomeMessage =
                new JLabel("Welcome to PuffinPlot. This puffin hatched on "+
                PuffinApp.getInstance().getBuildProperty("build.date") +"."),
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

    public SampleChooser getSampleChooser() {
        return sampleChooser;
    }
    
    public void updateSampleDataPanel() {
        splitPane.setDividerSize(sampleDataPanel.isVisible() ? 
                splitPaneDividerWidth : 0);
        splitPane.resetToPreferredSizes();
    }
    
    public void suitesChanged() {
        sampleChooser.updateSuite();
        controlPanel.updateSuites();
        final int numSuites = PuffinApp.getInstance().getSuites().size();
        welcomeMessage.setVisible(numSuites == 0);
        splitPane.setVisible(numSuites > 0);
        repaint();
    }

    public void sampleChanged() {
        controlPanel.updateSample();
        final Sample sample = PuffinApp.getInstance().getSample();
        if (sample==null) return;
        sampleDataPanel.setSample(sample);
        //pack();
        repaint(100);
        getMainMenuBar().sampleChanged();
    }
    
    public MainMenuBar getMainMenuBar() {
        return menuBar;
    }

    public MainGraphDisplay getGraphDisplay() {
        return graphDisplay;
    }
    
    static class CloseListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            PuffinApp.getInstance().getActions().quit.
            actionPerformed(new ActionEvent(this, 0, "window close"));
        }
    }
    
}
