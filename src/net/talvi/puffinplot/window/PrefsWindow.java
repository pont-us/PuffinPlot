package net.talvi.puffinplot.window;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.PuffinPrefs;
import net.talvi.puffinplot.data.SensorLengths;
import net.talvi.puffinplot.plots.Plot;

public class PrefsWindow extends JFrame {

    private final JTextField[] sensorLengthField = new JTextField[3];
    private final PuffinPrefs prefs =
            PuffinApp.getInstance().getPrefs();
    
    public PrefsWindow() {
        super("Preferences");
        final Insets insets = new Insets(4,4,4,4);
        final int CENTER = GridBagConstraints.CENTER, BOTH = GridBagConstraints.BOTH;
        setPreferredSize(new Dimension(300, 400));
        setLayout(new GridBagLayout());
        JTabbedPane tp = new JTabbedPane();
        add(tp, new GridBagConstraints(0, 0, 1, 1, 0.99, 0.99, CENTER, BOTH,
                insets, 4, 4));
        JPanel loadingPanel = new JPanel(false);
        loadingPanel.setLayout(new GridBagLayout());
        JPanel squidPanel = new JPanel(new GridBagLayout());
        squidPanel.setBorder(BorderFactory.createTitledBorder("SQUID sensor lengths"));
        String[] labels = {"x", "y", "z"};
        List<String> lengths = PuffinApp.getInstance().getPrefs().
                getSensorLengths().getLengths();
        squidPanel.add(new JLabel("Presets"),
                new GridBagConstraints(0, 0, 1, 1, 0.5, 0.5, CENTER,
                BOTH, insets, 4, 4));
        for (int i=0; i<3; i++) sensorLengthField[i] = new JTextField(lengths.get(i), 7);
        final PresetsBox presetsBox = new PresetsBox();

        squidPanel.add(presetsBox,
                new GridBagConstraints(1, 0, 1, 1, 0.5, 0.5, CENTER,
                BOTH, insets, 4, 4));
        for (int i=0; i<3; i++) {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridy = i + 1;
            c.ipadx = c.ipady = 2;
            c.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(labels[i]);
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            squidPanel.add(label, c);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 1;
            squidPanel.add(sensorLengthField[i], c);
        }
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridwidth = 2;
        gbc2.anchor = GridBagConstraints.LINE_START;
        gbc2.fill = BOTH;
        loadingPanel.add(squidPanel, gbc2);
        gbc2.gridy = 5;
        loadingPanel.add(new JCheckBox("Use 2-position protocol"), gbc2);
        JPanel plotsPanel = new JPanel(false);
        plotsPanel.setLayout(new BoxLayout(plotsPanel, BoxLayout.Y_AXIS));
        plotsPanel.add(new JLabel("Visible plots"));
        for (Plot plot: PuffinApp.getInstance().getMainWindow().getGraphDisplay().getPlots()) {
            plotsPanel.add(new JCheckBox(plot.getNiceName(), plot.isVisible()));
        }
        tp.addTab("Loading", null, loadingPanel,  "File loading");
        tp.addTab("Plots", null, plotsPanel, "Graph plotting");
        JButton button = new JButton("Close");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // window closing event isn't triggered by a setVisible(false),
                // so we have to update the SensorLengths here.
                presetsBox.set();
                setVisible(false);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                presetsBox.set();
            }
        });
        add(button, new GridBagConstraints(
                0, 1, 1, 1, 0.01, 0.01, GridBagConstraints.LINE_END,
                GridBagConstraints.VERTICAL, insets, 4, 4));
        pack();
    }
    
    private class PresetsBox extends JComboBox implements ItemListener {

        public PresetsBox() {
            super(SensorLengths.getPresetNames());
            addItem("Custom");
            addItemListener(this);
            updateWith(prefs.getSensorLengths());
        }

        public void itemStateChanged(ItemEvent e) {
            String name = (String) getSelectedItem();
            SensorLengths sl = null;
            if (name.equals("Custom")) {
                for (JTextField slf: sensorLengthField) slf.setEnabled(true);
            } else {
                sl = SensorLengths.fromPresetName(name);
                for (int i=0; i<3; i++) {
                    JTextField slf = sensorLengthField[i];
                    slf.setEnabled(false);
                    slf.setText(sl.getLengths().get(i));
                }
            }
            set();
        }
        
        private void updateWith(SensorLengths sl) {
            String preset = sl.getPreset();
            setSelectedItem(preset != null ? sl.getPreset() : "Custom");
            for (int i = 0; i < 3; i++) {
                JTextField slf = sensorLengthField[i];
                slf.setEnabled(preset == null);
                slf.setText(sl.getLengths().get(i));
            }
        }
        
        public void set() {
            String name = (String) getSelectedItem();
            prefs.setSensorLengths(name.equals("Custom")
                    ? SensorLengths.fromStrings(sensorLengthField[0].getText(),
                        sensorLengthField[1].getText(),
                        sensorLengthField[2].getText())
                    : SensorLengths.fromPresetName(name));
        }
    }
}
