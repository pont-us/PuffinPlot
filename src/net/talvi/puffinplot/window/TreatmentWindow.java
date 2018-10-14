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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatType;

/**
 * A window allowing the user to edit treatment type.
 * 
 * @author pont
 */
public class TreatmentWindow extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private final JButton cancelButton;
    private final JButton setButton;
    private final TreatmentCombo treatmentCombo;
    private final PuffinApp app;
    
    private static class TreatmentCombo extends JComboBox {
        
        public TreatmentCombo() {
            super();
            for (TreatType t: TreatType.values()) {
                addItem(t.getNiceName());
            }
        }
        
        public TreatType getTreatmentType() {
            return TreatType.values()[getSelectedIndex()];
        }
        
    }

    /** Creates a new correction window.
     * 
     * @param app the PuffinPlot instance associated with this window
     */
    public TreatmentWindow(PuffinApp app) {
        super("Set treatment type");
        this.app = app;
        setResizable(false);
        final Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        final JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        final JLabel label = new JLabel("Set treatment type for selected samples.");
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setAlignmentY(CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        topPanel.add(label, BorderLayout.CENTER);
        contentPane.add(topPanel);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.LINE_AXIS));
        mainPanel.add(new JLabel("Treatment"));
        mainPanel.add(treatmentCombo = new TreatmentCombo());

        cancelButton = new JButton("Cancel");
        setButton = new JButton("Set");
        setButton.addActionListener(this);
        cancelButton.addActionListener(this);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(setButton);

        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPane.add(mainPanel);
        contentPane.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPane.add(buttonPanel);

        pack();
        setLocationRelativeTo(null);
    }

    /** <p>Handle an action event. The events handled are clicks on the
     * ‘Cancel’ and ‘Set’ buttons.</p>
     * 
     * @param event the action event to handle
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getSource() == cancelButton)
            setVisible(false);
        if (event.getSource() == setButton) {
            final TreatType treatType = treatmentCombo.getTreatmentType();
            for (Sample sample: app.getSelectedSamples()) {
                for (Datum datum: sample.getData()) {
                    datum.setTreatType(treatType);
                }
            }
            setVisible(false);
            app.updateDisplay();
        }
    }
}
