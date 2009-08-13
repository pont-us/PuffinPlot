package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
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
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.DatumField;

public class CorrectionWindow extends JFrame implements ActionListener {

    private final static DatumField[] fields = {
        DatumField.SAMPLEAZ, DatumField.SAMPLEDIP, DatumField.FORMAZ,
        DatumField.FORMDIP, DatumField.MAGDEV
    };
    private JButton cancelButton;
    private JButton setButton;
    HashMap<DatumField, JCheckBox> checkBoxMap = new HashMap<DatumField, JCheckBox>();
    HashMap<DatumField, JTextField> textFieldMap = new HashMap<DatumField, JTextField>();
    

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
        for (DatumField field : fields) {
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
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == cancelButton)
            setVisible(false);
        if (event.getSource() == setButton) {
            List<Sample> samples = PuffinApp.getInstance().getSelectedSamples();
            for (DatumField field : fields) {
                if (checkBoxMap.get(field).isSelected()) {
                    String value = textFieldMap.get(field).getText();
                    for (Sample s: samples)
                        for (Datum d: s.getData()) d.setValue(field, value);
                }
            }
            setVisible(false);
        }
    }
}