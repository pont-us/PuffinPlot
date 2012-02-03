/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.file.FileFormat;
import org.apache.batik.swing.JSVGScrollPane;

/**
 *
 * @author pont
 */
public class TabularImportWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private List<FieldChooser> fieldChoosers = new ArrayList<FieldChooser>(20);
    private HeaderLinesPanel headerLinesPanel;
    private final EnumChooser<MeasType> measTypeChooser;
    private final EnumChooser<TreatType> treatTypeChooser;
    
    public TabularImportWindow(final PuffinApp app) {
        super("Import data");
        final Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        headerLinesPanel = new HeaderLinesPanel();
        contentPane.add(headerLinesPanel);
        measTypeChooser = new EnumChooser<MeasType>("Measurement type",
                new String[] {"Continuous", "Discrete"},
                new MeasType[] {MeasType.CONTINUOUS, MeasType.DISCRETE},
                MeasType.CONTINUOUS);
        contentPane.add(measTypeChooser);

        treatTypeChooser = new EnumChooser<TreatType>("Treatment type",
                "Thermal#AF (3-axis)#AF (z-axis)#IRM#ARM".split("#"),
                new TreatType[] {TreatType.THERMAL, TreatType.DEGAUSS_XYZ,
                TreatType.DEGAUSS_Z, TreatType.IRM, TreatType.ARM},
                TreatType.DEGAUSS_XYZ);
        contentPane.add(treatTypeChooser);        
        
        final JScrollPane scrollPane = new JScrollPane(new FieldChooserPane(),
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        JViewport headerViewport = new JViewport();
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.add(new JLabel("Column number"));
        header.add(new JLabel("Column contents"));
        headerViewport.add(header);
        scrollPane.setColumnHeader(headerViewport);
        final JPanel columnPanel = new JPanel();
        columnPanel.setLayout(new BorderLayout());
        columnPanel.setBorder(BorderFactory.createTitledBorder("Column definitions"));
        columnPanel.add(scrollPane, BorderLayout.CENTER);
        contentPane.add(columnPanel);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        final JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }});
        
        final JButton importButton = new JButton("Choose files…");
        importButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final FileFormat format = getFileFormat();
                setVisible(false);
                dispose();
                app.importTabularDataWithFormat(format);
            }});
        buttonPanel.add(importButton);
        contentPane.add(buttonPanel);
        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }
    
    private class EnumChooser<T extends Enum<T>> extends JPanel {
        
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
            add(comboBox);
        }
        
        public T getValue() {
            return values[comboBox.getSelectedIndex()];
        }
    }
    
    public FileFormat getFileFormat() {
        final Map<Integer, DatumField> fieldMap =
                new HashMap<Integer,DatumField>(fieldChoosers.size());
        for (FieldChooser fieldChooser: fieldChoosers) {
            fieldMap.put(fieldChooser.getColumnNumber()-1, fieldChooser.getField());
        }
        return new FileFormat(fieldMap, headerLinesPanel.getNumber(),
                measTypeChooser.getValue(),
                treatTypeChooser.getValue(),
                "\\s");
    }
    
    private class HeaderLinesPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final SpinnerNumberModel spinnerModel;
        public HeaderLinesPanel() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            final JLabel label = new JLabel("Number of header lines to skip");
            spinnerModel =
                    new SpinnerNumberModel(1, 0, 1000, 1);
                    //new SpinnerNumberModel(0, 0, 1000, 1);
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
        public FieldChooserPane() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            for (int i=0; i<4; i++) {
                FieldChooser fieldChooser = new FieldChooser(i+1);
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
        private static final List<DatumField> fields;
        private static final String[] emptyStringArray =
                new String[] {}; // for List.toArray
        
        static {
            DatumField[] allValues = DatumField.values();
            fieldStrings = new ArrayList<String>(allValues.length);
            fields = new ArrayList<DatumField>(allValues.length);
            for (DatumField field: allValues) {
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
            //l.setLabelFor(spinner);
            spinner.setMaximumSize(new Dimension(100, 24));
            add(spinner);

            fieldBox = new JComboBox(fieldStrings.toArray(emptyStringArray));
            fieldBox.setMaximumSize(new Dimension(300, 24));
            // box.setToolTipText(toolTip);

            add(fieldBox);
        }

        public int getColumnNumber() {
            return spinnerModel.getNumber().intValue();
        }
        
        public DatumField getField() {
            return fields.get(fieldBox.getSelectedIndex());
        }
    }
}
