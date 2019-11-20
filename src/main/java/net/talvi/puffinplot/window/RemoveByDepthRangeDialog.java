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

import java.awt.Dialog;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.Util;

import static javax.swing.GroupLayout.Alignment;

/**
 * A dialog for removing samples within a specified depth range.
 */
public class RemoveByDepthRangeDialog extends JDialog {

    private final PuffinApp app;
    private final JButton cancelButton;
    private final JScrollPane scrollPane;
    private final JTextArea infoText;
    private final JLabel rangeBottomLabel;
    private final JTextField rangeBottomText;
    private final JLabel rangeTopLabel;
    private final JTextField rangeTopText;
    private final JButton removeButton;

    /**
     * Creates a new remove depth range dialog
     * 
     * @param app the PuffinApp instance with which to associate this dialog
     */
    public RemoveByDepthRangeDialog(PuffinApp app) {
        super(app.getMainWindow(), "Remove samples by depth",
                Dialog.ModalityType.APPLICATION_MODAL);
        this.app = app;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        rangeTopText = new JTextField("0", 5);
        rangeTopLabel = new JLabel("Top of range", SwingConstants.RIGHT);
        rangeTopLabel.setLabelFor(rangeTopText);

        rangeBottomText = new JTextField("0", 5);
        rangeBottomLabel = new JLabel("Bottom of range", SwingConstants.RIGHT);
        rangeBottomLabel.setLabelFor(rangeBottomText);

        infoText = new JTextArea(
                "Remove all samples outside the specified range of depths.",
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

        removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeSpecifiedSamples());

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));

        final GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rangeTopLabel,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rangeTopText,
                                GroupLayout.PREFERRED_SIZE,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(rangeBottomLabel,
                                GroupLayout.DEFAULT_SIZE,
                                GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(rangeBottomText,
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
                    .addComponent(rangeTopLabel)
                    .addComponent(rangeTopText, GroupLayout.PREFERRED_SIZE,
                            GroupLayout.DEFAULT_SIZE,
                            GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(
                        Alignment.LEADING)
                    .addComponent(rangeBottomLabel)
                    .addComponent(rangeBottomText, GroupLayout.PREFERRED_SIZE,
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

    private void removeSpecifiedSamples() {                                             
        final Double top = Util.tryToParseDouble(
                app.getMainWindow(), rangeTopText.getText());
        if (top == null) {
            return;
        }
        final Double bottom = Util.tryToParseDouble(
                app.getMainWindow(), rangeBottomText.getText());
        if (bottom == null) {
            return;
        }
        app.getCurrentSuite().removeSamplesOutsideDepthRange(top, bottom);
        app.getMainWindow().suitesChanged();
        setVisible(false);
    }                                            
}
