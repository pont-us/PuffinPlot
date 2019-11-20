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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentType;

/**
 * Lets the user select options for IAPD file import.
 *
 * @author pont
 */
public class IapdImportDialog extends JDialog {
    
    private MeasurementType measurementType = null;
    private TreatmentType treatmentType = null;
    private boolean okPressed = false;
        
    /**
     * Creates a new IapdImportDialog.
     * 
     * @param parent the parent frame of the dialog
     */
    public IapdImportDialog(Frame parent) {
        super(parent, "IAPD import options", true);
        
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(4, 4));
        contentPane.setBorder(new EmptyBorder(8,8,8,8));
        setContentPane(contentPane);
        
        final JPanel optionsPanel = new JPanel();
        optionsPanel.setBorder(new EmptyBorder(8,8,8,8));
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.add(Box.createRigidArea(new Dimension(200, 0)));
        final JPanel mtPanel = new JPanel();
        mtPanel.setLayout(new GridLayout(0, 1));
        mtPanel.setBorder(BorderFactory.createTitledBorder("Measurement type"));
        final ButtonGroup mtGroup = new ButtonGroup();
        //mtPanel.setAlignmentX((float) 0.0);
        for (MeasurementType mt: MeasurementType.values()) {
            if (!mt.isActualMeasurement()) {
                continue;
            }
            final JRadioButton button = new JRadioButton(mt.getNiceName());
            button.setAlignmentX(LEFT_ALIGNMENT);
            button.setSelected(mt == MeasurementType.DISCRETE);
            button.putClientProperty(IapdImportDialog.class, mt);
            mtPanel.add(button);
            mtGroup.add(button);
        }
        final JPanel ttPanel = new JPanel();
        ttPanel.setLayout(new GridLayout(0, 1));
        ttPanel.setBorder(BorderFactory.createTitledBorder("Treatment type"));

        final ButtonGroup ttGroup = new ButtonGroup();
        for (TreatmentType tt: TreatmentType.values()) {
            if (tt == TreatmentType.UNKNOWN || tt == TreatmentType.NONE) {
                continue;
            }
            final JRadioButton button = new JRadioButton(tt.getNiceName());
            button.setSelected(tt == TreatmentType.DEGAUSS_XYZ);
            button.putClientProperty(IapdImportDialog.class, tt);
            ttPanel.add(button);
            ttGroup.add(button);
        }

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());
        final JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }});
        
        buttonPanel.add(Box.createHorizontalStrut(12));
        final JButton okButton = new JButton("Open file");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed = true;
                final Enumeration<AbstractButton> mtEn = mtGroup.getElements();
                while (mtEn.hasMoreElements()) {
                    AbstractButton ab = mtEn.nextElement();
                    if (ab.isSelected()) {
                        measurementType = (MeasurementType) ab.getClientProperty(IapdImportDialog.class);
                    }
                }
                final Enumeration<AbstractButton> ttEn = ttGroup.getElements();
                while (ttEn.hasMoreElements()) {
                    AbstractButton ab = ttEn.nextElement();
                    if (ab.isSelected()) {
                        treatmentType = (TreatmentType) ab.getClientProperty(IapdImportDialog.class);
                    }
                }
                setVisible(false);
                dispose();
            }});
        buttonPanel.setAlignmentX(RIGHT_ALIGNMENT);
        buttonPanel.add(okButton);
        
        optionsPanel.add(mtPanel);
        optionsPanel.add(ttPanel);
        getContentPane().add(optionsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    /**
     * @return the measurement type
     */
    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    /**
     * @return the treatment type
     */
    public TreatmentType getTreatmentType() {
        return treatmentType;
    }

    /**
     * @return true iff OK was clicked by the user
     */
    public boolean wasOkPressed() {
        return okPressed;
    }
}
