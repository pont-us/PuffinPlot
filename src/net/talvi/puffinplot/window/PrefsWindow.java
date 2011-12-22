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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.BorderFactory;
import javax.swing.Box;
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
import net.talvi.puffinplot.data.file.TwoGeeLoader;
import net.talvi.puffinplot.plots.Plot;

/**
 * A window which allows the user to change PuffinPlot's preferences.
 * 
 * @author pont
 */
public class PrefsWindow extends JFrame {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private static final long serialVersionUID = 1L;
    private final JTextField[] sensorLengthField = new JTextField[3];
    private final PresetsBox presetsBox;
    private final JTextField protocolBox;
    private List<PlotBox> plotBoxes = new ArrayList<PlotBox>(24);
    private List<PrefBox> prefBoxes = new ArrayList<PrefBox>();
    private final PuffinPrefs prefs =
            PuffinApp.getInstance().getPrefs();
    
    /**
     * Creates a new preferences window.
     */
    public PrefsWindow() {
        super("Preferences");
        final Insets insets = new Insets(0,0,0,0);
        final int BOTH = GridBagConstraints.BOTH;
        setPreferredSize(new Dimension(300, 500));
        setLayout(new GridBagLayout());
        JTabbedPane tp = new JTabbedPane();
        add(tp, new GridBagConstraints(0, 0, 2, 1, 0.99, 0.99, GridBagConstraints.LINE_START, BOTH,
                insets, 0, 0));
        JPanel loadingPanel = new JPanel(false);
        loadingPanel.setLayout(new GridBagLayout());
        JPanel squidPanel = new JPanel(new GridBagLayout());
        squidPanel.setBorder(BorderFactory.createTitledBorder("SQUID sensor lengths"));
        String[] labels = {"x", "y", "z"};
        List<String> lengths = prefs.getSensorLengths().getLengths();
        squidPanel.add(new JLabel("Presets"),
                new GridBagConstraints(0, 0, 1, 1, 0.8, 0.8, GridBagConstraints.LINE_START,
                BOTH, insets, 0, 0));
        for (int i=0; i<3; i++) sensorLengthField[i] = new JTextField(lengths.get(i), 7);
        presetsBox = new PresetsBox();

        squidPanel.add(presetsBox,
                new GridBagConstraints(1, 0, 1, 1, 0.8, 0.8, GridBagConstraints.LINE_START,
                BOTH, insets, 1, 1));
        for (int i=0; i<3; i++) {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridy = i + 1;
            c.ipadx = c.ipady = 0;
            c.anchor = GridBagConstraints.EAST;
            JLabel label = new JLabel(labels[i]);
            label.setHorizontalTextPosition(SwingConstants.LEFT);
            squidPanel.add(label, c);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 1;
            squidPanel.add(sensorLengthField[i], c);
        }
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridwidth = 3;
        gbc2.anchor = GridBagConstraints.LINE_START;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 0.8;
        gbc2.weighty = 0;
        loadingPanel.add(squidPanel, gbc2);
        gbc2.gridy = 5;
        gbc2.gridx = 0;
        gbc2.gridwidth = 1;
        gbc2.weightx = 0.25;
        gbc2.anchor = GridBagConstraints.LINE_END;
        loadingPanel.add(new JLabel("Protocol"), gbc2);
        gbc2.gridx = 1;
        gbc2.gridwidth = 2;
        gbc2.weightx = 0.75;
        gbc2.anchor = GridBagConstraints.LINE_START;
        protocolBox = new JTextField("NORMAL", 32);
        protocolBox.setPreferredSize(new Dimension(200, 32));
        loadingPanel.add(protocolBox, gbc2);
        protocolBox.setToolTipText("If you don't know what this does, you " +
                "probably shouldn't touch it.");
        JPanel plotsPanel = new JPanel(false);
        plotsPanel.setLayout(new BoxLayout(plotsPanel, BoxLayout.Y_AXIS));
        plotsPanel.add(new JLabel("Visible plots"));
        for (Plot plot: PuffinApp.getInstance().getMainWindow().getGraphDisplay().getPlots()) {
            PlotBox pb = new PlotBox(plot);
            plotsPanel.add(pb);
            plotBoxes.add(pb);
        }
        JPanel miscPanel = new JPanel(false);
        miscPanel.setLayout(new BoxLayout(miscPanel, BoxLayout.Y_AXIS));
        
        miscPanel.add(Box.createVerticalGlue());
        miscPanel.add(makeLabelledPrefBox("Demag y axis",
                "plots.demag.vAxisLabel", "Magnetization (A/m)"));
        miscPanel.add(makeLabelledPrefBox("PmagPy folder",
                "data.pmagPyPath", "/usr/local/bin"));
        miscPanel.add(makeLabelledPrefBox("Font",
                "plots.fontFamily", "Arial"));
        miscPanel.add(makeLabelledPrefComboBox("Look and feel",
                "lookandfeel", new String[] {"Default", "Native", "Metal", "Nimbus"},
                "Default"));
        miscPanel.add(Box.createVerticalGlue());

        tp.addTab("Loading", null, loadingPanel, "File loading");
        tp.addTab("Plots", null, plotsPanel, "Active plots");
        tp.addTab("Misc.", null, miscPanel, "Miscellaneous");

        JButton button = new JButton("Close");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // window closing event isn't triggered by a setVisible(false),
                // so we have to update the SensorLengths here.
                applySettings();
                setVisible(false);
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                applySettings();
            }
        });
        add(button, new GridBagConstraints(
                0, 1, 1, 1, 0.01, 0.01, GridBagConstraints.LINE_END,
                GridBagConstraints.VERTICAL, insets, 4, 4));
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }

    private JPanel makeLabelledPrefBox(String labelString, String pref,
            String defaultValue) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        final JLabel label = new JLabel(labelString);
        label.setMaximumSize(new Dimension(200, 30));
        panel.add(label);
        JTextField field = new PrefBox(pref, defaultValue);
        field.setMaximumSize(new Dimension(300, 50));
        panel.add(field);
        return panel;
    }
    
    private JPanel makeLabelledPrefComboBox(String labelString, String pref,
            String[] values, String defaultValue) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        final JLabel label = new JLabel(labelString);
        label.setMaximumSize(new Dimension(200, 30));
        panel.add(label);
        PrefsComboBox box = new PrefsComboBox(pref, values, defaultValue);
        box.setMaximumSize(new Dimension(300, 50));
        panel.add(box);
        return panel;
    }
    
    private void applySettings() {
        presetsBox.applySettings();
        prefs.set2gProtocol(TwoGeeLoader.Protocol.valueOf(protocolBox.getText()));
        for (PlotBox plotBox: plotBoxes) plotBox.applySetting();
        for (PrefBox prefBox: prefBoxes) prefBox.storeValue();
        try {
            prefs.getPrefs().flush();
        } catch (BackingStoreException ex) {
            logger.log(Level.WARNING, null, ex);
            PuffinApp.getInstance().errorDialog("PuffinPlot error",
                    "The preferences could not be saved.");
        }
        PuffinApp.getInstance().updateDisplay();
    }

    private class PlotBox extends JCheckBox {
        private static final long serialVersionUID = 1L;
        private final Plot plot;
        public PlotBox(Plot plot) {
            super(plot.getNiceName(), plot.isVisible());
            this.plot = plot;
        }

        public void applySetting() {
            plot.setVisible(isSelected());
        }
    }

    private class PrefBox extends JTextField {
        private static final long serialVersionUID = 1L;
        final private String key;
        public PrefBox(String key, String def) {
            super(prefs.getPrefs().get(key, def));
            this.key = key;
            prefBoxes.add(this);
        }
        public void storeValue() {
            prefs.getPrefs().put(key, getText());
        }
    }
    
    private class PrefsComboBox extends JComboBox implements ItemListener {
        private static final long serialVersionUID = 1L;

        private String prefsKey;
        
        public PrefsComboBox(String prefsKey, String[] items,
                String defaultValue) {
            super(items);
            this.prefsKey = prefsKey;
            final String value = prefs.getPrefs().get(prefsKey, defaultValue);
            setSelectedItem(value);
            addItemListener(this);
        }
        
        public void applySettings() {
            prefs.getPrefs().put(prefsKey, getSelectedItem().toString());
        }

        public void itemStateChanged(ItemEvent e) {
            applySettings();
        }
    }

    private class PresetsBox extends JComboBox implements ItemListener {
        private static final long serialVersionUID = 1L;

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
            applySettings();
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
        
        public void applySettings() {
            String name = (String) getSelectedItem();
            prefs.setSensorLengths(name.equals("Custom")
                    ? SensorLengths.fromStrings(sensorLengthField[0].getText(),
                        sensorLengthField[1].getText(),
                        sensorLengthField[2].getText())
                    : SensorLengths.fromPresetName(name));
        }
    }
}
