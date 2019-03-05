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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.talvi.puffinplot.PuffinActions;
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

    private static final Logger logger =
            Logger.getLogger("net.talvi.puffinplot");
    private static final long serialVersionUID = 1L;
    private final JTextField[] sensorLengthField = new JTextField[3];
    private final PresetsBox presetsBox;
    private final List<PlotBox> plotBoxes = new ArrayList<>(24);
    private final List<PrefTextField> prefTextFields = new ArrayList<>();
    private final PuffinApp app;
    private final PuffinPrefs prefs;
    private final static Dimension prefDim = new Dimension(100, 30);
    
    /**
     * Creates a new preferences window for a supplied PuffinPlot application.
     * 
     * @param app the PuffinPlot application associated with this window
     */
    public PrefsWindow(PuffinApp app) {
        super("Preferences");
        this.app = app;
        prefs = app.getPrefs();
        final Insets insets = new Insets(4,4,4,4);
        final int BOTH = GridBagConstraints.BOTH;
        setPreferredSize(new Dimension(500, 560));
        setLayout(new GridBagLayout());
        final JTabbedPane tp = new JTabbedPane();
        add(tp, new GridBagConstraints(0, 0, 4, 1, 0.99, 0.99,
                GridBagConstraints.LINE_START, BOTH,
                insets, 0, 0));
        final JPanel loadingPanel = new JPanel(false);
        loadingPanel.setLayout(new GridBagLayout());
        final JPanel squidPanel = new JPanel(new GridBagLayout());
        squidPanel.setBorder(
                BorderFactory.createTitledBorder("SQUID sensor lengths"));
        final String[] labels = {"x", "y", "z"};
        final List<String> lengths = prefs.getSensorLengths().getLengths();
        final JLabel presetsLabel = new JLabel("Presets");
        presetsLabel.setHorizontalAlignment(JLabel.RIGHT);
        squidPanel.add(presetsLabel,
                new GridBagConstraints(0, 0, 1, 1, 0.8, 0.8,
                        GridBagConstraints.LINE_START,
                        BOTH, insets, 0, 0));
        for (int i=0; i<3; i++) {
            sensorLengthField[i] = new JTextField(lengths.get(i), 7);
        }
        presetsBox = new PresetsBox();

        squidPanel.add(presetsBox, new GridBagConstraints(1, 0, 1, 1, 0.8, 0.8,
                        GridBagConstraints.LINE_START,
                        BOTH, insets, 1, 1));
        for (int i=0; i<3; i++) {
            final GridBagConstraints c = new GridBagConstraints();
            c.gridy = i + 1;
            c.ipadx = c.ipady = 0;
            c.anchor = GridBagConstraints.EAST;
            final JLabel label = new JLabel(labels[i]);
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
        final TwoGeeLoader.Protocol[] protocolValues =
                TwoGeeLoader.Protocol.values();
        final String[] protocolStrings = new String[protocolValues.length];
        for (int i=0; i<protocolValues.length; i++) {
            protocolStrings[i] = protocolValues[i].name();
        }
        loadingPanel.add(makeLabelledPrefComboBox("Protocol",
                "measurementProtocol", protocolStrings, "NORMAL",
                "The measurement protocol to use when reading 2G files."), gbc2);
        
        final JPanel plotsPanel = new JPanel(false);
        plotsPanel.setLayout(new BoxLayout(plotsPanel, BoxLayout.Y_AXIS));
        final JLabel plotsLabel = new JLabel("<html><b>Visible plots</b></html>");
        plotsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        plotsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        plotsPanel.add(Box.createRigidArea((new Dimension(0,8))));
        plotsPanel.add(plotsLabel);
        plotsPanel.add(Box.createRigidArea((new Dimension(0,8))));
        final JPanel plotsSubPanel = new JPanel(false);
        plotsSubPanel.setLayout(new GridLayout(0, 2));

        for (Plot plot: app.getMainWindow().getGraphDisplay().getPlots()) {
            final PlotBox pb = new PlotBox(plot);
            plotsSubPanel.add(pb);
            plotBoxes.add(pb);
        }
        plotsPanel.add(plotsSubPanel);
        
        final JPanel miscPanel = new JPanel(false);
        miscPanel.setLayout(new BoxLayout(miscPanel, BoxLayout.Y_AXIS));
        
        miscPanel.add(Box.createVerticalGlue());
        miscPanel.add(makeAlignedCheckBox(new PrefsCheckBox(
                "Label equal-area plots", "plots.labelEqualAreaPlots", false)));
        miscPanel.add(makeAlignedCheckBox(new PrefsCheckBox(
                "Label treatment steps", "plots.labelTreatmentSteps", false)));
        miscPanel.add(makeAlignedCheckBox(new PrefsCheckBox(
                "Label samples in site plots", "plots.labelSamplesInSitePlots",
                false)));
        miscPanel.add(makeAlignedCheckBox(new PrefsCheckBox(
                "Label points in suite plots", "plots.labelPointsInSuitePlots",
                false)));
        miscPanel.add(makeAlignedCheckBox(new PrefsCheckBox(
                "Highlight current sample/site", "plots.highlightCurrentSample",
                false)));
        miscPanel.add(makeAlignedCheckBox(new PrefsCheckBox(
                "Show site α95s on suite plot", "plots.showSiteA95sOnSuitePlot",
                false)));
        miscPanel.add(makeAlignedCheckBox(new MagDevCheckBox()));
        miscPanel.add(makeLabelledPrefTextField("Demag. y-axis label",
                "plots.demag.vAxisLabel", "Magnetization (A/m)"));
        miscPanel.add(makeLabelledPrefTextField("PmagPy folder",
                "data.pmagPyPath", "/usr/local/bin"));
        miscPanel.add(makeLabelledPrefTextField("Font",
                "plots.fontFamily", "Arial"));
        miscPanel.add(makeLabelledPrefComboBox("Look and feel",
                "lookandfeel",
                new String[] {"Default", "Native", "Metal", "Nimbus"},
                "Default",
                "PuffinPlot's appearance (changes take effect on restart)"));
        miscPanel.add(makeLabelledPrefTextField("GC validity",
                "data.greatcircles.validityExpr", "true"));
        miscPanel.add(makeLabelledPrefComboBox("Zplot PCA display",
                "plots.zplotPcaDisplay",
                new String[] {"Full", "Long", "Short", "None"},
                "Long", "How PCA lines are shown on the Zijderveld plot"));
        miscPanel.add(makeLabelledPrefComboBox("Sample orientation",
                "display.sampleOrientation",
                new String[] {"Azimuth/Dip", "Azimuth/Hade"},
                "Azimuth/Dip",
                "The parameters used to represent sample orientation"));
        miscPanel.add(makeLabelledPrefComboBox("Formation orientation",
                "display.formationOrientation",
                new String[] {"Dip azimuth/Dip", "Strike/Dip"},
                "Azimuth/Dip",
                "The parameters used to represent formation orientation"));
        miscPanel.add(Box.createVerticalGlue());

        tp.addTab("2G import", null, loadingPanel,
                "Settings for reading 2G data files");
        tp.addTab("Plots", null, plotsPanel, "Select which plots are shown");
        tp.addTab("Misc.", null, miscPanel, "Miscellaneous settings");

        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> {
            /*
             * window closing event isn't triggered by a setVisible(false),
             * so we have to update the SensorLengths here.
             */
            applySettings();
            setVisible(false);
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                applySettings();
            }
        });
        
        final PuffinActions actions = app.getActions();
        add(makeActionButton(actions.clearPreferences, "Clear"),
                new GridBagConstraints(
                0, 1, 1, 1, 1, .01, GridBagConstraints.LINE_START,
                BOTH, insets, 0, 0));
        add(makeActionButton(actions.importPrefs, "Import"),
                new GridBagConstraints(
                1, 1, 1, 1, 1, .01, GridBagConstraints.LINE_START,
                BOTH, insets, 0, 0));
        add(makeActionButton(actions.exportPrefs, "Export"),
                new GridBagConstraints(
                2, 1, 1, 1, 1, .01, GridBagConstraints.LINE_START,
                BOTH, insets, 0, 0));
        add(closeButton, new GridBagConstraints(
                3, 1, 1, 1, 1, .01, GridBagConstraints.LINE_END,
                BOTH, new Insets(4, 50, 4, 4), 20, 0));
        pack();
        setLocationRelativeTo(app.getMainWindow());
    }

    private JButton makeActionButton(final Action action, String name) {
        final JButton button = new JButton(name);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applySettings();
                action.actionPerformed(new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED, null));
            }
        });
        button.setToolTipText(
                (String) action.getValue(Action.SHORT_DESCRIPTION));
        button.setMaximumSize(new Dimension(300, 50));
        return button;
    }
    
    // For layout debugging.
    private void redBorder(JComponent comp) {
        comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.red),
                comp.getBorder()));
        if (comp instanceof AbstractButton) {
            ((AbstractButton) comp).setBorderPainted(true);
        }
    }
    
    private JPanel makeLabelledPrefTextField(String labelString, String pref,
            String defaultValue) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final JLabel label = new JLabel(labelString);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setPreferredSize(prefDim);
        label.setMaximumSize(new Dimension(400, 50));
        panel.add(label);
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final PrefTextField field = new PrefTextField(pref, defaultValue);
        prefTextFields.add(field);
        field.setPreferredSize(prefDim);
        field.setMaximumSize(new Dimension(800, 50));
        panel.add(field);
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        return panel;
    }
    
    private JPanel makeAlignedCheckBox(JCheckBox checkBox) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea((new Dimension(100, 0))));
        checkBox.setMaximumSize(new Dimension(800, 50));
        checkBox.setPreferredSize(prefDim);
        panel.add(checkBox);
        return panel;
    }
    
    private JPanel makeLabelledPrefComboBox(String labelString, String pref,
            String[] values, String defaultValue, String toolTip) {
        final JPanel panel = new JPanel(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final JLabel label = new JLabel(labelString);
        label.setPreferredSize(prefDim);
        label.setMaximumSize(new Dimension(400, 50));
        label.setHorizontalAlignment(JLabel.RIGHT);
        panel.add(label);
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        final JComboBox box = new PrefsComboBox(pref, values, defaultValue);
        box.setPreferredSize(prefDim);
        box.setMaximumSize(new Dimension(800, 50));
        box.setToolTipText(toolTip);
        panel.add(box);
        panel.add(Box.createRigidArea((new Dimension(8,0))));
        return panel;
    }
    
    private void applySettings() {
        presetsBox.applySettings();
        prefs.set2gProtocol(TwoGeeLoader.Protocol.valueOf(
                prefs.getPrefs().get("measurementProtocol", "NORMAL")));
        for (PlotBox plotBox: plotBoxes) {
            plotBox.applySetting();
        }
        for (PrefTextField prefBox: prefTextFields) {
            prefBox.storeValue();
        }
        try {
            prefs.getPrefs().flush();
        } catch (BackingStoreException ex) {
            logger.log(Level.WARNING, null, ex);
            app.errorDialog("PuffinPlot error",
                    "The preferences could not be saved.");
        }
        app.updateDisplay();
    }

    private class PlotBox extends JPanel {
        private static final long serialVersionUID = 1L;
        private final Plot plot;
        private final JCheckBox checkBox;
        
        public PlotBox(Plot plot) {
            super();
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            add(Box.createRigidArea((new Dimension(8, 0))));
            checkBox = new JCheckBox(plot.getNiceName(), plot.isVisible());
            add(checkBox);
            this.plot = plot;
        }

        public void applySetting() {
            plot.setVisible(checkBox.isSelected());
        }
    }

    private class PrefTextField extends JTextField {
        private static final long serialVersionUID = 1L;
        final private String key;
        
        private PrefTextField(String key, String def) {
            super(prefs.getPrefs().get(key, def));
            this.key = key;
        }
        
        public void storeValue() {
            prefs.getPrefs().put(key, getText());
        }
    }
    
    private class PrefsCheckBox extends JCheckBox{
        private static final long serialVersionUID = 1L;
        public PrefsCheckBox(String label, final String key,
            boolean defaultValue) {
            super(label, prefs.getPrefs().getBoolean(key, defaultValue));
            addItemListener(event ->
                prefs.getPrefs().putBoolean(key, isSelected())
            );
        }
    }
    
    private class MagDevCheckBox extends JCheckBox implements ItemListener {
        private static final long serialVersionUID = 1L;
        public MagDevCheckBox() {
            super("Bedding is vs. magnetic north",
                   app.getCorrection().isMagDevAppliedToFormation());
            addItemListener(this);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            app.getCorrection().setMagDevAppliedToFormation(isSelected());
        }
    }
    
    private class PrefsComboBox extends JComboBox<String>
            implements ItemListener {
        private static final long serialVersionUID = 1L;

        private final String prefsKey;
        
        public PrefsComboBox(String prefsKey, String[] items,
                String defaultValue) {
            super(items);
            this.prefsKey = prefsKey;
            final String value = prefs.getPrefs().get(prefsKey, defaultValue);
            setSelectedItem(value);
            addItemListener(this);
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            prefs.getPrefs().put(prefsKey, getSelectedItem().toString());
        }
    }

    private class PresetsBox extends JComboBox<String> implements ItemListener {
        private static final long serialVersionUID = 1L;

        public PresetsBox() {
            super(SensorLengths.getPresetNames());
            addItem("Custom");
            addItemListener(this);
            updateWith(prefs.getSensorLengths());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            String name = (String) getSelectedItem();
            if (name.equals("Custom")) {
                for (JTextField slf: sensorLengthField) slf.setEnabled(true);
            } else {
                final SensorLengths sl = SensorLengths.fromPresetName(name);
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
