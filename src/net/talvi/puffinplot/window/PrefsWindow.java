package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.plots.Plot;

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

        loadingPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridwidth = 2;
        gbc2.anchor = GridBagConstraints.LINE_START;
        loadingPanel.add(new JLabel("SQUID sensor lengths"), gbc2);
        String[] labels = {"x", "y", "z"};
        for (int i=0; i<3; i++) {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridy = i + 1;
            c.ipadx = c.ipady = 2;
            c.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(labels[i]);
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            loadingPanel.add(label, c);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 1;
            final JTextField textField = new JTextField(6);
            loadingPanel.add(textField, c);
        }
        gbc2.gridy = 5;
        loadingPanel.add(new JCheckBox("Use 2-position protocol"), gbc2);

        JPanel plotsPanel = new JPanel(false);
        plotsPanel.setLayout(new BoxLayout(plotsPanel, BoxLayout.Y_AXIS));
        plotsPanel.add(new JLabel("Visible plots"));
        for (Plot plot: PuffinApp.getInstance().getMainWindow().getGraphDisplay().getVisiblePlots()) {
            plotsPanel.add(new JLabel(plot.getNiceName()));
        }
        tp.addTab("Loading", null, loadingPanel,  "File loading");
        tp.addTab("Plots", null, plotsPanel, "Graph plotting");
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LAST_LINE_END;
        gbc.weightx = gbc.weighty = 0.01;
        gbc.fill = GridBagConstraints.NONE;
        JButton button = new JButton("Close");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        add(button, gbc);
        pack();
    }

}
