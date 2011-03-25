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

public class MainWindow extends JFrame {

    private static final long serialVersionUID = -9075963924200708871L;

    private final GraphDisplay graphDisplay;
    public final ControlPanel controlPanel;
    private JScrollPane jsp;
    SampleChooser sampleChooser;
    private final MainMenuBar menuBar;
    private JLabel welcomeMessage;
    private final SampleDataPanel sampleDataPanel;

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
        jsp = new JScrollPane(getGraphDisplay());
        // jsp.setMaximumSize(new Dimension(1000,700));
        mainPanel.add(jsp);
        mainPanel.add(welcomeMessage =
                new JLabel("Welcome to PuffinPlot. This puffin hatched on "+
                PuffinApp.getInstance().getBuildDate()+"."), BorderLayout.NORTH);
        graphDisplay.setVisible(false);
        mainPanel.add(sampleChooser = new SampleChooser(), BorderLayout.WEST);
        mainPanel.add(sampleDataPanel = new SampleDataPanel(), BorderLayout.EAST);
        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.add(controlPanel = new ControlPanel());
        cp.add(mainPanel);
        setMaximumSize(jsp.getMaximumSize());
        pack();
    }

    public SampleChooser getSampleChooser() {
        return sampleChooser;
    }
    
    public void suitesChanged() {
        sampleChooser.updateSuite();
        controlPanel.updateSuites();
        int numSuites = PuffinApp.getInstance().getSuites().size();
        welcomeMessage.setVisible(numSuites == 0);
        getGraphDisplay().setVisible(numSuites > 0);
        repaint();
    }

    public void sampleChanged() {
        controlPanel.updateSample();
        sampleDataPanel.setSample(PuffinApp.getInstance().getSample());
        //pack();
        repaint(100);
        getMainMenuBar().sampleChanged();
    }
    
    public MainMenuBar getMainMenuBar() {
        return menuBar;
    }

    public GraphDisplay getGraphDisplay() {
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
