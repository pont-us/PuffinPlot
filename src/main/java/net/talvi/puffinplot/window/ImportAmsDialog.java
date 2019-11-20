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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import net.talvi.puffinplot.PuffinApp;

/**
 * A dialog for importing AMS data from files.
 */
public class ImportAmsDialog extends JDialog {

    private final JCheckBox magneticNorthCheckBox;
    private final JCheckBox overwriteFormationCorrectionCheckBox;
    private final JCheckBox overwriteSampleCorrectionCheckBox;
    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");

    private final PuffinApp app;

    /**
     * Creates a new AMS import dialog.
     *
     * @param app the PuffinApp instance with which to associate the dialog
     */
    public ImportAmsDialog(PuffinApp app) {
        super(app.getMainWindow(), true);
        this.app = app;

        overwriteSampleCorrectionCheckBox =
                new JCheckBox("Overwrite existing sample orientations");
        overwriteFormationCorrectionCheckBox =
                new JCheckBox("Overwrite existing formation orientations");
        magneticNorthCheckBox =
                new JCheckBox("Orientations are relative to magnetic north");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Import AMS data from ASC file");

        final JButton importButton = new JButton("Import");
        importButton.addActionListener(e -> doImport());

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));

        final GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout
                        .createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(overwriteSampleCorrectionCheckBox)
                        .addComponent(overwriteFormationCorrectionCheckBox)
                        .addComponent(magneticNorthCheckBox))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(GroupLayout.Alignment.TRAILING,
                    layout.createSequentialGroup()
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(cancelButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(importButton)
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(overwriteSampleCorrectionCheckBox)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(overwriteFormationCorrectionCheckBox)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(magneticNorthCheckBox)
                    .addGap(18, 18, 18)
                    .addGroup(layout.createParallelGroup(
                        GroupLayout.Alignment.BASELINE)
                        .addComponent(importButton)
                        .addComponent(cancelButton))
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    private void doImport() {
        setVisible(false);
        try {
            final List<File> files = app.openFileDialog("Select AMS files");
            app.getCurrentSuite().importAmsFromAsc(files,
                    magneticNorthCheckBox.isSelected(),
                    overwriteSampleCorrectionCheckBox.isSelected(),
                    overwriteFormationCorrectionCheckBox.isSelected());
            app.getMainWindow().suitesChanged();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            app.errorDialog("Error importing AMS", ex.getLocalizedMessage());
        }
    }
}
