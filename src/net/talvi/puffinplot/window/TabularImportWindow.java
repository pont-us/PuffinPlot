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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SpinnerNumberModel;

import net.talvi.puffinplot.data.FieldUnit;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.MomentUnit;
import net.talvi.puffinplot.data.TreatmentType;
import net.talvi.puffinplot.data.TreatmentParameter;
import net.talvi.puffinplot.data.file.FileFormat;

/**
 * A window allowing the user to define a custom file format.
 * 
 * @see FileFormat
 * 
 * @author pont
 */
public class TabularImportWindow extends JDialog {
    private static final long serialVersionUID = 1L;
    
    private final List<FieldChooser> fieldChoosers = new ArrayList<>(20);
    private final HeaderLinesPanel headerLinesPanel;
    private final EnumChooser<MeasurementType> measTypeChooser;
    private final EnumChooser<TreatmentType> treatTypeChooser;
    private final EnumChooser<MomentUnit> momentUnitChooser;
    private final EnumChooser<FieldUnit> fieldUnitChooser;
    private final FileFormat initialFormat;
    private final StringChooser separatorChooser;
    private final LabelledTextField columnWidthsBox;
    private final JCheckBox fixedWidthBox;
    private FileFormat format;
    
    /**
     * Creates a new tabular import window. The supplied preferences object
     * is used to save and restore the file format.
     * 
     * @param dialogOwner parent frame (owner) for this dialog
     * @param prefs preferences for storing configuration
     * 
     * @see FileFormat
     */
    public TabularImportWindow(Frame dialogOwner, Preferences prefs) {
        super(dialogOwner, "Import data", true);
        setPreferredSize(new Dimension(400, 500));
        this.initialFormat = FileFormat.readFromPrefs(prefs);
        final Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        final JPanel firstPanel = new JPanel();
        firstPanel.setLayout(new BoxLayout(firstPanel, BoxLayout.Y_AXIS));
        firstPanel.setBorder(
                BorderFactory.createTitledBorder("General settings"));
        headerLinesPanel = new HeaderLinesPanel();
        firstPanel.add(headerLinesPanel);
        measTypeChooser = new EnumChooser<>("Measurement type",
                new String[] {"Continuous", "Discrete"},
                new MeasurementType[] { 
                    MeasurementType.CONTINUOUS, MeasurementType.DISCRETE},
                initialFormat.getMeasurementType());
        firstPanel.add(measTypeChooser);
        treatTypeChooser = new EnumChooser<>("Treatment type",
                "Thermal#AF (3-axis)#AF (z-axis)#IRM#ARM".split("#"),
                new TreatmentType[] {
                    TreatmentType.THERMAL, TreatmentType.DEGAUSS_XYZ,
                    TreatmentType.DEGAUSS_Z, TreatmentType.IRM,
                    TreatmentType.ARM},
                initialFormat.getTreatmentType());
        firstPanel.add(treatTypeChooser);
        momentUnitChooser = new EnumChooser<>("Unit for magnetic moment",
                new String[] {"A/m", "mA/m"},
                new MomentUnit[] {MomentUnit.AM, MomentUnit.MILLIAM},
                initialFormat.getMomentUnit());
        firstPanel.add(momentUnitChooser);
        fieldUnitChooser = new EnumChooser<>("Unit for AF field",
                new String[] {"millitesla", "tesla", "gauss", "kilogauss"},
                new FieldUnit[] {FieldUnit.MILLITESLA, FieldUnit.TESLA,
                    FieldUnit.GAUSS, FieldUnit.KILOGAUSS},
                initialFormat.getFieldUnit());
        firstPanel.add(fieldUnitChooser);
        firstPanel.add(separatorChooser = new StringChooser("Column separator",
                new String[] {
                    "Comma", "Tab", "Single space",
                    "Any white space", "| (pipe)"},
                new String[] {",", "\t", " ", "\\s+", "|"},
                initialFormat.getSeparator()));
        firstPanel.add(fixedWidthBox = new JCheckBox("Use fixed-width columns",
                initialFormat.useFixedWidthColumns()));
        firstPanel.add(columnWidthsBox = new LabelledTextField("Column widths",
                initialFormat.getColumnWidthsAsString()));
        contentPane.add(firstPanel);
        
        final JScrollPane scrollPane = new JScrollPane(new FieldChooserPane(),
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JViewport headerViewport = new JViewport();
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        JLabel columnLabel = new JLabel("Column no.");
        columnLabel.setMaximumSize(new Dimension(80, 24));
        header.add(columnLabel);
        header.add(new JLabel("Column contents"));
        headerViewport.add(header);
        scrollPane.setColumnHeader(headerViewport);
        final JPanel columnPanel = new JPanel();
        columnPanel.setLayout(new BorderLayout());
        columnPanel.setBorder(
                BorderFactory.createTitledBorder("Column definitions"));
        columnPanel.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(columnPanel);
        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        final JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                format = null;
                setVisible(false);
                dispose();
            }});
        
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                format = createFileFormat();
                setVisible(false);
                dispose();
            }});
        buttonPanel.add(okButton);
        contentPane.add(buttonPanel);
        pack();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(dialogOwner);
    }

    /**
     * Returns the chosen format.
     * 
     * Note that this will be null unless the user has clicked the "OK"
     * button.
     * 
     * @return the format chosen by the user
     */
    public FileFormat getFormat() {
        return format;
    }
    
    private class StringChooser extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String[] values;
        private final JComboBox comboBox;
        
        public StringChooser(String labelText, String[] labels, String[] values,
                String initialValue) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            final JLabel label = new JLabel(labelText);
            comboBox = new JComboBox(labels);
            this.values = values;
            label.setLabelFor(comboBox);
            for (int i=0; i<values.length; i++) {
                if (values[i].equals(initialValue)) {
                    comboBox.setSelectedIndex(i);
                }
            }
            add(label);
            add(Box.createRigidArea(new Dimension(5,0)));
            add(comboBox);
        }
        
        public String getValue() {
            return values[comboBox.getSelectedIndex()];
        }
    }
    
    private class LabelledTextField extends JPanel {
        private static final long serialVersionUID = 1L;
        private final JTextField textField;
        
        public LabelledTextField(String labelText, String initialValue) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            final JLabel label = new JLabel(labelText);
            textField = new JTextField(initialValue);
            label.setLabelFor(textField);
            add(label);
            add(textField);
        }
        
        public String getValue() {
            return textField.getText();
        }
    }
    
    private class EnumChooser<T extends Enum<T>> extends JPanel {
        private static final long serialVersionUID = 1L;
        
        private final T[] values;
        private final String[] names;
        private final JComboBox comboBox;
        
        public EnumChooser(String label, String[] names, T[] values,
                T initialValue) {
            this.names = names;
            this.values = values;
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            comboBox = new JComboBox(names);
            for (int i=0; i<values.length; i++) {
                if (values[i]==initialValue) {
                    comboBox.setSelectedIndex(i);
                }
            }
            final JLabel jLabel = new JLabel(label);
            jLabel.setLabelFor(comboBox);
            add(jLabel);
            add(Box.createRigidArea(new Dimension(5,0)));
            add(comboBox);
        }
        
        public T getValue() {
            return values[comboBox.getSelectedIndex()];
        }
    }
    
    /**
     * @return the file format defined by the current settings of this window
     */
    private FileFormat createFileFormat() {
        final Map<Integer, TreatmentParameter> fieldMap =
                new HashMap<>(fieldChoosers.size());
        for (FieldChooser fieldChooser: fieldChoosers) {
            final TreatmentParameter field = fieldChooser.getField();
            if (field != null) {
                fieldMap.put(fieldChooser.getColumnNumber()-1, field);
            }
        }
        return new FileFormat(fieldMap, headerLinesPanel.getNumber(),
                measTypeChooser.getValue(),
                treatTypeChooser.getValue(),
                separatorChooser.getValue(),
                fixedWidthBox.isSelected(),
                FileFormat.convertStringToColumnWidths(
                        columnWidthsBox.getValue()),
                momentUnitChooser.getValue(),
                fieldUnitChooser.getValue());
    }
    
    private class HeaderLinesPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final SpinnerNumberModel spinnerModel;
        public HeaderLinesPanel() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            final JLabel label = new JLabel("Number of header lines to skip");
            spinnerModel = new SpinnerNumberModel(
                    initialFormat.getHeaderLines(), 0, 1000, 1);
            final JSpinner spinner = new JSpinner(spinnerModel);
            add(label);
            add(spinner);
            label.setLabelFor(spinner);
        }

        private int getNumber() {
            return spinnerModel.getNumber().intValue();
        }
    }
    
    private class FieldChooserPane extends JPanel {
        private static final long serialVersionUID = 1L;
        public FieldChooserPane() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            Iterator<Integer> columnIterator = 
                    initialFormat.getColumnMap().keySet().iterator();
            for (int i=0; i<20; i++) {
                FieldChooser fieldChooser = new FieldChooser(i+1);
                if (columnIterator.hasNext()) {
                    final int column = columnIterator.next();
                    final TreatmentParameter field =
                            initialFormat.getColumnMap().get(column);
                    fieldChooser.setContents(column+1, field);
                }
                add(fieldChooser);
                fieldChoosers.add(fieldChooser);
            }
        }
    }
    
    private static class FieldChooser extends JPanel {
        private static final long serialVersionUID = 1L;
    
        private final JComboBox fieldBox;
        private final JSpinner spinner;
        private final SpinnerNumberModel spinnerModel;
        private static final List<String> fieldStrings;
        private static final List<TreatmentParameter> fields;
        private static final String[] emptyStringArray =
                new String[] {}; // for List.toArray
        
        static {
            TreatmentParameter[] allValues = TreatmentParameter.values();
            fieldStrings = new ArrayList<>(allValues.length+1);
            fields = new ArrayList<>(allValues.length+1);
            fields.add(null);
            fieldStrings.add("[Ignore this column]");
            for (TreatmentParameter field: allValues) {
                if (field.isImportable()) {
                    fields.add(field);
                    fieldStrings.add(field.getNiceName());
                }
            }
        }
        
        public FieldChooser(int columnNumber) {
            super(false);
            setMaximumSize(new Dimension(500, 24));
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(Box.createRigidArea((new Dimension(8,0))));
            
            spinnerModel =
                    new SpinnerNumberModel(columnNumber, 1, 1000, 1);
            spinner = new JSpinner(spinnerModel);
            spinner.setMaximumSize(new Dimension(80, 24));
            add(spinner);
            fieldBox = new JComboBox(fieldStrings.toArray(emptyStringArray));
            fieldBox.setMaximumSize(new Dimension(300, 24));
            add(fieldBox);
        }

        public int getColumnNumber() {
            return spinnerModel.getNumber().intValue();
        }
        
        public TreatmentParameter getField() {
            return fields.get(fieldBox.getSelectedIndex());
        }

        private void setContents(int column, TreatmentParameter field) {
            spinnerModel.setValue(column);
            for (int i=0; i<fields.size(); i++) {
                if (fields.get(i) == field) {
                    fieldBox.setSelectedIndex(i);
                }
            }
        }
    }
}
