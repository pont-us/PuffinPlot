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
import javax.swing.*;
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
    // private final JTextField protocolBox;
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
        setPreferredSize(new Dimension(400, 500));
        setLayout(new GridBagLayout());
        JTabbedPane tp = new JTabbedPane();
        add(tp, new GridBagConstraints(0, 0, 2, 1, 0.99, 0.99,
                GridBagConstraints.LINE_START, BOTH,
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
        loadingPanel.add(makeLabelledPrefComboBox("Read magnetization from",
                "readTwoGeeMagFrom",
                new String[] {"X/Y/Z", "Dec/Inc/Intensity"},
                "X/Y/Z",
                "Magnetization can be read either from x/y/z moments or from "
                + "declination/inclination/intensity."),
                gbc2);
        
        gbc2 = new GridBagConstraints();
        gbc2.gridy = 1;
        gbc2.gridwidth = 3;
        gbc2.anchor = GridBagConstraints.LINE_START;
        gbc2.fill = GridBagConstraints.BOTH;
        gbc2.weightx = 0.8;
        gbc2.weighty = 0;
        loadingPanel.add(squidPanel, gbc2);
        
        gbc2.gridy = 6;
        gbc2.gridx = 0;
        gbc2.gridwidth = 1;
        gbc2.weightx = 0.25;
        gbc2.anchor = GridBagConstraints.LINE_END;
        final TwoGeeLoader.Protocol[] protocolValues = TwoGeeLoader.Protocol.values();
        final String[] protocolStrings = new String[protocolValues.length];
        for (int i=0; i<protocolValues.length; i++) {
            protocolStrings[i] = protocolValues[i].name();
        }
        loadingPanel.add(makeLabelledPrefComboBox("Protocol",
                "measurementProtocol", protocolStrings, "NORMAL",
                "The measurement protocol to use when reading 2G files."), gbc2);
        
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
        
        miscPanel.add(new MagDevTickBox());
        miscPanel.add(makeLabelledPrefBox("Demag y axis",
                "plots.demag.vAxisLabel", "Magnetization (A/m)"));
        miscPanel.add(makeLabelledPrefBox("PmagPy folder",
                "data.pmagPyPath", "/usr/local/bin"));
        miscPanel.add(makeLabelledPrefBox("Font",
                "plots.fontFamily", "Arial"));
        miscPanel.add(makeLabelledPrefComboBox("Look and feel",
                "lookandfeel", new String[] {"Default", "Native", "Metal", "Nimbus"},
                "Default", "PuffinPlot's appearance (changes take effect on restart)"));
        miscPanel.add(makeLabelledPrefBox("GC validity",
                "data.greatcircles.validityExpr", "N>=3 and a95<3.5 and k>3"));
        miscPanel.add(makeLabelledPrefComboBox("Zplot PCA display",
                "plots.zplotPcaDisplay", new String[] {"Full", "Long", "Short", "None"},
                "Long", "How PCA lines are shown on the Zijderveld plot"));
        miscPanel.add(makeLabelledPrefComboBox("Sample orientation",
                "display.sampleOrientation",
                new String[] {"Azimuth/Dip", "Azimuth/Hade"},
                "Azimuth/Dip", "The parameters used to represent sample orientation"));
        miscPanel.add(makeLabelledPrefComboBox("Formation orientation",
                "display.formationOrientation",
                new String[] {"Dip azimuth/Dip", "Strike/Dip"},
                "Azimuth/Dip", "The parameters used to represent formation orientation"));
        miscPanel.add(Box.createVerticalGlue());

        tp.addTab("2G import", null, loadingPanel, "Settings for reading 2G data files");
        tp.addTab("Plots", null, plotsPanel, "Select which plots are shown");
        tp.addTab("Misc.", null, miscPanel, "Miscellaneous settings");

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
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
        add(closeButton, new GridBagConstraints(
                0, 1, 1, 1, 0.01, 0.01, GridBagConstraints.LINE_END,
                GridBagConstraints.VERTICAL, insets, 4, 4));
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }

    private JPanel makeLabelledPrefBox(String labelString, String pref,
            String defaultValue) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final JLabel label = new JLabel(labelString);
        label.setMaximumSize(new Dimension(200, 30));
        panel.add(label);
        JTextField field = new PrefBox(pref, defaultValue);
        field.setMaximumSize(new Dimension(300, 50));
        panel.add(field);
        return panel;
    }
    
    private JPanel makeLabelledTickBox(String labelString, String pref,
            String defaultValue) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final JLabel label = new JLabel(labelString);
        label.setMaximumSize(new Dimension(200, 30));
        panel.add(label);
        JTextField field = new PrefBox(pref, defaultValue);
        field.setMaximumSize(new Dimension(300, 50));
        panel.add(field);
        return panel;
    }
    
    private JPanel makeLabelledPrefComboBox(String labelString, String pref,
            String[] values, String defaultValue, String toolTip) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final JLabel label = new JLabel(labelString);
        label.setMaximumSize(new Dimension(200, 30));
        panel.add(label);
        PrefsComboBox box = new PrefsComboBox(pref, values, defaultValue);
        box.setMaximumSize(new Dimension(300, 50));
        box.setToolTipText(toolTip);
        panel.add(box);
        return panel;
    }
    
    private void applySettings() {
        presetsBox.applySettings();
        prefs.set2gProtocol(TwoGeeLoader.Protocol.valueOf(
                prefs.getPrefs().get("measurementProtocol", "NORMAL")));
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
    
    private class MagDevTickBox extends JCheckBox implements ItemListener {
        private static final long serialVersionUID = 1L;
        public MagDevTickBox() {
            super("Bedding is vs. magnetic north",
                   PuffinApp.getInstance().getCorrection().isMagDevAppliedToFormation());
            addItemListener(this);
        }

        public void itemStateChanged(ItemEvent e) {
            PuffinApp.getInstance().getCorrection().setMagDevAppliedToFormation(isSelected());
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

        public void itemStateChanged(ItemEvent e) {
            prefs.getPrefs().put(prefsKey, getSelectedItem().toString());
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
