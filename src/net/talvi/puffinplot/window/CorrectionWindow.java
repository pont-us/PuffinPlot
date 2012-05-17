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
import javax.swing.*;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.Sample;

/**
 * A window allowing the user to edit orientation corrections.
 * Editable fields are provided for the sample and formation
 * orientations and for the magnetic declination.
 * 
 * @author pont
 */
public class CorrectionWindow extends JFrame implements ActionListener {

    private final static DatumField[] fields = {
        DatumField.SAMPLE_AZ, DatumField.SAMPLE_DIP, DatumField.VIRT_SAMPLE_HADE,
        DatumField.FORM_AZ, DatumField.VIRT_FORM_STRIKE, DatumField.FORM_DIP, 
        DatumField.MAG_DEV
    };
    
    private static final long serialVersionUID = 1L;
    private JButton cancelButton;
    private JButton setButton;
    private Map<DatumField, JCheckBox> checkBoxMap =
            new EnumMap<DatumField, JCheckBox>(DatumField.class);
    private Map<DatumField, JTextField> textFieldMap =
            new EnumMap<DatumField, JTextField>(DatumField.class);
    

    /** Creates a new correction window. */
    public CorrectionWindow() {
        super("Edit corrections");
        setResizable(false);
        final Container cp = getContentPane();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));

        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        JPanel topBit = new JPanel();
        topBit.setLayout(new BorderLayout());
        JLabel label = new JLabel("Select values to modify.");
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setAlignmentY(CENTER_ALIGNMENT);
        
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        topBit.add(label, BorderLayout.CENTER);
        cp.add(topBit);

        JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        for (DatumField field: fields) {
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
        setButton.addActionListener(this);
        cancelButton.addActionListener(this);
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPane.add(setButton);

        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        cp.add(fieldPanel);
        cp.add(Box.createRigidArea(new Dimension(0, 10)));
        cp.add(buttonPane);

        pack();
        setLocationRelativeTo(null);
    }

    /** <p>Handle an action event. The events handled are clicks on the
     * <q>Cancel</q> and <q>Set</q> buttons.</p>
     * 
     * @param event the action event to handle
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == cancelButton)
            setVisible(false);
        if (event.getSource() == setButton) {
            final PuffinApp app = PuffinApp.getInstance();
            List<Sample> samples = app.getSelectedSamples();
            for (DatumField field : fields) {
                if (checkBoxMap.get(field).isSelected()) {
                    String value = textFieldMap.get(field).getText();
                    for (Sample s: samples) {
                        s.setValue(field, value);
                    }
                }
            }
            setVisible(false);
            app.updateDisplay();
        }
    }
}
