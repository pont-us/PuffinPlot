/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStepField;

/**
 * A window allowing the user to edit orientation corrections.
 * Editable fields are provided for the sample and formation
 * orientations and for the magnetic declination.
 * 
 * @author pont
 */
public class EditSampleParametersWindow extends JFrame {

    private final static TreatmentStepField[] fields = {
        TreatmentStepField.VOLUME,
        TreatmentStepField.SAMPLE_AZ, TreatmentStepField.SAMPLE_DIP, TreatmentStepField.VIRT_SAMPLE_HADE,
        TreatmentStepField.FORM_AZ, TreatmentStepField.VIRT_FORM_STRIKE, TreatmentStepField.FORM_DIP,
        TreatmentStepField.MAG_DEV
    };
    
    private static final long serialVersionUID = 1L;
    private final JButton cancelButton;
    private final JButton setButton;
    private final Map<TreatmentStepField, JCheckBox> checkBoxMap =
            new EnumMap<>(TreatmentStepField.class);
    private final Map<TreatmentStepField, JTextField> textFieldMap =
            new EnumMap<>(TreatmentStepField.class);
    private final ActionListener actionListener;
    private final PuffinApp app;
    

    /**
     * Creates a new correction window.
     */
    public EditSampleParametersWindow(PuffinApp app) {
        super("Edit sample parameters");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        final Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        JLabel label = new JLabel("Select sample parameters to modify.");
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setAlignmentY(CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        topPanel.add(label, BorderLayout.CENTER);
        cp.add(topPanel);

        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        for (TreatmentStepField field: fields) {
            gc.gridwidth = 2;
            gc.anchor = GridBagConstraints.EAST;
            JCheckBox checkBox = new JCheckBox(field.getNiceName());
            checkBox.setHorizontalTextPosition(SwingConstants.LEFT);
            fieldPanel.add(checkBox, gc);
            gc.anchor = GridBagConstraints.WEST;
            gc.gridwidth = GridBagConstraints.REMAINDER;
            final JTextField textField = new JTextField(6);
            fieldPanel.add(textField,gc);
            checkBoxMap.put(field, checkBox);
            textFieldMap.put(field, textField);
        }

        cancelButton = new JButton("Cancel");
        setButton = new JButton("Set");
        actionListener = new EspwActionListener();
        setButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(setButton);

        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        cp.add(fieldPanel);
        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        cp.add(buttonPanel);

        pack();
        setLocationRelativeTo(app.getMainWindow());
    }

    private class EspwActionListener implements ActionListener {
        /** <p>Handle an action event. The events handled are clicks on the
         * ‘Cancel’ and ‘Set’ buttons.</p>
         *
         * @param event the action event to handle
         */
        @Override
        public void actionPerformed(ActionEvent event) {
            if (event.getSource() == cancelButton) {
                setVisible(false);
                dispose();
            }
            if (event.getSource() == setButton) {
                final List<Sample> samples = app.getSelectedSamples();
                for (TreatmentStepField field : fields) {
                    if (checkBoxMap.get(field).isSelected()) {
                        final String value = textFieldMap.get(field).getText();
                        for (Sample s: samples) {
                            s.setValue(field, value);
                        }
                    }
                }
                app.recalculateSamplesAndSites(samples);
                app.updateDisplay();
                setVisible(false);
                dispose();
            }
        }
    }
}
