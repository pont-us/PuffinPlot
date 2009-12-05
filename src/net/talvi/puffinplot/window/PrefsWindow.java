package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.LayoutManager;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class PrefsWindow extends JFrame {

    public PrefsWindow() {
        super("Preferences");
        setPreferredSize(new Dimension(300, 400));
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx =  gbc.gridy = 0;
        gbc.weightx = gbc.weighty = 0.99;
        JTabbedPane tp = new JTabbedPane();
        add(tp, gbc);
        JPanel loadingPanel = new JPanel(false);
        loadingPanel.add(new JLabel("Blah"));
        loadingPanel.add(new JLabel("Blah"));
        loadingPanel.add(new JLabel("Blah"));
        JPanel plotsPanel = new JPanel(false);
        plotsPanel.add(new JLabel("Wiffle"));
        tp.addTab("Loading", null, loadingPanel,  "File loading");
        tp.addTab("Plots", null, plotsPanel, "Graph plotting");
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LAST_LINE_END;
        gbc.weightx = gbc.weighty = 0.01;
        gbc.fill = GridBagConstraints.NONE;
        JButton button = new JButton("Close");
        add(button, gbc);
        pack();
    }

}
