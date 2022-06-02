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

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.MultiRange;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStepOperation;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.Component;

import static javax.swing.GroupLayout.Alignment;

/**
 * A dialog for editing treatment steps within specified ranges
 */
public class TreatmentRangesDialog extends JDialog {

    private final PuffinApp app;
    private final JButton cancelButton;
    private final JScrollPane scrollPane;
    private final JTextArea infoText;
    private final JLabel operationLabel;
    private final JComboBox<TreatmentStepOperation> operationCombo;
    private final JLabel rangesLabel;
    private final JTextField rangesText;
    private final JButton removeButton;

    /**
     * Creates a new edit treatment step range dialog
     *
     * @param app the PuffinApp instance with which to associate this dialog
     */
    public TreatmentRangesDialog(PuffinApp app) {
        super(app.getMainWindow(), "Select treatment steps by treatment level",
                ModalityType.APPLICATION_MODAL);
        this.app = app;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        rangesText = new JTextField("0", 10);
        rangesLabel = new JLabel("Treatment level range(s) ",
                SwingConstants.RIGHT);
        rangesLabel.setLabelFor(rangesText);

        operationCombo = new JComboBox<>(TreatmentStepOperation.values());
        operationCombo.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                final Object name = ((TreatmentStepOperation) value).getDisplayName();
                return super.getListCellRendererComponent(
                        list, name, index, isSelected, cellHasFocus);
            }
        });

        operationLabel = new JLabel("Operation", SwingConstants.RIGHT);
        operationLabel.setLabelFor(operationCombo);

        infoText = new JTextArea(
                "Operate on specified ranges of treatment steps " +
                        "in all selected samples.",
                5, 20);
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setFocusable(false);
        infoText.setOpaque(false);
        infoText.setBorder(BorderFactory.createEmptyBorder());

        scrollPane = new JScrollPane(infoText,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        removeButton = new JButton("OK");
        removeButton.addActionListener(e -> operateOnSpecifiedSteps());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));

        final GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rangesLabel,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rangesText,
                                GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(operationLabel,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(operationCombo,
                                GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE))
                    .addGroup(Alignment.TRAILING,
                            layout.createSequentialGroup()
                        .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE,
                                242, Short.MAX_VALUE))
                    .addGroup(Alignment.TRAILING,
                            layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(cancelButton)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton)))
                .addContainerGap()
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPane, 32, 32, GroupLayout.DEFAULT_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(
                        Alignment.BASELINE)
                    .addComponent(rangesLabel)
                    .addComponent(rangesText, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(
                        Alignment.LEADING)
                    .addComponent(operationLabel)
                    .addComponent(operationCombo, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(removeButton)
                    .addComponent(cancelButton))
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    /**
     * Show this dialog, centred over the PuffinPlot app's main window.
     */
    public void showOverMainWindow() {
        setLocationRelativeTo(app.getMainWindow());
        setVisible(true);
    }

    private void operateOnSpecifiedSteps() {
        final MultiRange multirange =
                MultiRange.fromString(rangesText.getText());
        final TreatmentStepOperation op =
                (TreatmentStepOperation) operationCombo.getSelectedItem();
        for (Sample sample: app.getSelectedSamples()) {
            sample.getStepsInRanges(multirange).forEach(step -> op.apply(step));
        }
        app.updateDisplay();
        setVisible(false);
    }                                            
}
