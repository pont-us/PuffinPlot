package net.talvi.puffinplot;

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

    public final GraphDisplay graphDisplay;
    public final ControlPanel controlPanel;
    private JScrollPane jsp;
    SampleChooser sampleChooser;
    private JLabel welcomeMessage;
    
    public MainWindow() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new CloseListener());
        setPreferredSize(new Dimension(800,400));
        setJMenuBar(new MainMenuBar());
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        // mainPanel.add(graphDisplay = new GraphDisplay(), BorderLayout.CENTER);
        graphDisplay = new GraphDisplay();
        jsp = new JScrollPane(graphDisplay);
        jsp.setMaximumSize(new Dimension(1000,700));
        mainPanel.add(jsp);
        mainPanel.add(welcomeMessage =
                new JLabel("Welcome to PuffinPlot."), BorderLayout.NORTH);
        graphDisplay.setVisible(false);
        mainPanel.add(sampleChooser = new SampleChooser(), BorderLayout.WEST);
        Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.add(controlPanel = new ControlPanel());
        cp.add(mainPanel);
        setMaximumSize(jsp.getMaximumSize());
        pack();
        
//        addComponentListener(new java.awt.event.ComponentAdapter() {
//            public void componentResized(ComponentEvent event) {
//              setSize(
//                Math.min(1200, getWidth()),
//                Math.min(600, getHeight()));
//            }
//          });
    }
    
    void suitesChanged() {
        sampleChooser.updateSuite();
        controlPanel.updateSuites();
        graphDisplay.setVisible(true);
        welcomeMessage.setVisible(false);
        repaint();
    }
    
    static class CloseListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            PuffinApp.getApp().actions.quit.
            actionPerformed(new ActionEvent(this, 0, "window close"));
        }
    }
    
}
