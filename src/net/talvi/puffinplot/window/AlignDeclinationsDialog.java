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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.stream.Stream;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.talvi.puffinplot.PuffinApp;

import static javax.swing.SwingConstants.TRAILING;
import static java.awt.GridBagConstraints.LINE_START;
import static java.awt.GridBagConstraints.LINE_END;
import static java.awt.GridBagConstraints.BOTH;
import javax.swing.JTextField;
import net.talvi.puffinplot.data.CoreSections;
import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.Suite;

/**
 * A dialog allowing the user to perform declination alignment between core
 * sections.
 *
 * This class is not a JDialog subclass; it just implements a {@code show()}
 * method which displays a dialog using a {@code JOptionPane} static method,
 * and getters to retrieve results stored after showing the dialog.
 * 
 * @author pont
 */
public class AlignDeclinationsDialog {

    private final JFrame parentFrame;
    private final PuffinApp app;
    private int endLength;
    private double targetDeclination;
    private CoreSections.TargetDeclinationType targetType;
    
    /**
     * Create a new declination alignment dialog.
     * 
     * @param app the PuffinPlot application with which this dialog is
     * associated
     */
    public AlignDeclinationsDialog(PuffinApp app) {
        this.app = app;
        this.parentFrame = app.getMainWindow();
    }
    
    /**
     * Show the dialog, and perform the alignment if appropriate.
     * This method will block until the dialog is closed.
     */
    public void show() {
        if (!canAlignSectionDeclinations()) {
            return;
        }
        
        final JTextField endLengthField =
                new JTextField("5");
        final JTextField targetDeclinationField =
                new JTextField("0");
        final JComboBox<String> targetTypeComboBox = new JComboBox<>(
                Stream.of(CoreSections.TargetDeclinationType.values())
                        .map(ret -> ret.getNiceName()).toArray(String[]::new));
        targetTypeComboBox.setSelectedIndex(0);
        
        final JPanel panel = new JPanel();
        final Insets insets = new Insets(4, 4, 4, 4);
        panel.setLayout(new GridBagLayout());
        panel.add(new JLabel("Core end length", TRAILING),
                new GridBagConstraints(0, 0, 1, 1, 0.5, 0.5,
                        LINE_END, BOTH, insets, 0, 0));
        panel.add(new JLabel("Target declination", TRAILING),
                new GridBagConstraints(0, 1, 1, 1, 0.5, 0.5,
                        LINE_END, BOTH, insets, 0, 0));
        panel.add(new JLabel("Target aligns with", TRAILING),
                new GridBagConstraints(0, 2, 1, 1, 0.5, 0.5,
                        LINE_END, BOTH, insets, 0, 0));
        panel.add(endLengthField,
                new GridBagConstraints(1, 0, 1, 1, 0.5, 0.5,
                        LINE_START, BOTH, insets, 0, 0));
        panel.add(targetDeclinationField,
                new GridBagConstraints(1, 1, 1, 1, 0.5, 0.5,
                        LINE_START, BOTH, insets, 0, 0));
        panel.add(targetTypeComboBox,
                new GridBagConstraints(1, 2, 1, 1, 0.5, 0.5,
                        LINE_START, BOTH, insets, 0, 0));
        
        final int userChoice = JOptionPane.showConfirmDialog(
                parentFrame, panel, "Align declinations",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        endLength = parseEndLength(endLengthField.getText());
        targetDeclination = parseTarget(targetDeclinationField.getText());

        if (userChoice == JOptionPane.CANCEL_OPTION
                || endLength == -1 || Double.isNaN(targetDeclination)
                || targetTypeComboBox.getSelectedIndex() == -1) {
            return;
        }
        
        targetType = CoreSections.TargetDeclinationType.values()[
                targetTypeComboBox.getSelectedIndex()];
        
        app.getCurrentSuite().alignSectionDeclinations(endLength,
                targetDeclination, targetType);
        JOptionPane.showMessageDialog(app.getMainWindow(),
                "Core section declinations successfully aligned.",
                "Core sections aligned",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private double parseTarget(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            PuffinApp.errorDialog("Invalid target declination",
                    String.format("‘%s’ is not a valid declination", s),
                    parentFrame);
            return Double.NaN;
        }
    }
    
    private int parseEndLength(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            PuffinApp.errorDialog("Invalid end length",
                    String.format("‘%s’ is not a valid length", s),
                    parentFrame);
            return -1;
        }
    }

    private boolean canAlignSectionDeclinations() {
        final Suite suite = app.getCurrentSuite();
        if (suite == null) {
            app.errorDialog("No suite loaded",
                    "PuffinPlot cannot align core sections, "
                    + "as there is no data suite loaded.");
            return false;
        }
        if (suite.getMeasurementType() != MeasurementType.CONTINUOUS) {
            app.errorDialog("Can't align core sections",
                    "Core section alignment can only be done on continuous "
                    + "suites.");
            return false;
        }
        return true;
    }
}
