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

import java.util.stream.Stream;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.RpiEstimateType;
import net.talvi.puffinplot.data.Suite;

/**
 * A class to show a dialog to input settings for an RPI calculation.
 * <p>
 * This class is not a JDialog subclass; it just implements a {@code show()}
 * method which displays a dialog using a {@code JOptionPane} static method,
 * and getters to retrieve results stored after showing the dialog.
 * 
 * @author pont
 */
public class RpiDialog {

    private Suite nrm = null;
    private Suite normalizer = null;
    private RpiEstimateType estimateType = null;
    private final PuffinApp app;
    
    /**
     * Create a new RPI dialog.
     * 
     * @param app the PuffinApp from which to get the suite lists
     */
    public RpiDialog(PuffinApp app) {
        this.app = app;
    }
    
    /**
     * Show this RPI dialog.
     * <p>
     * This method will block until the user has clicked "OK" or "Cancel".
     * 
     * @return {@code true} if the user made a valid selection and clicked
     * "OK"; {@code false} otherwise
     */
    public boolean show() {
        if (app.getSuites().size() < 2) {
            app.errorDialog("Not enough suites for RPI", "An RPI calculation "
                    + "requires two open suites: one for the NRM, and one for "
                    + "the normalizer.");
            return false;
        }

        final JComboBox<Suite> nrmComboBox =
                new JComboBox<>(app.getSuites().toArray(new Suite[0]));
        final JComboBox<Suite> normalizerComboBox =
                new JComboBox<>(app.getSuites().toArray(new Suite[0]));
        final JComboBox<String> estimateTypeComboBox =
                new JComboBox<>(Stream.of(RpiEstimateType.values())
                        .map(ret -> ret.getNiceName()).toArray(String[]::new));
        nrmComboBox.setSelectedIndex(0);
        normalizerComboBox.setSelectedIndex(1);
        final JComponent[] inputs = new JComponent[] {
            new JLabel("NRM suite"), nrmComboBox,
            new JLabel("Normalizer suite"), normalizerComboBox,
            new JLabel("Normalizer type"), estimateTypeComboBox};
        
        final int userChoice = JOptionPane.showConfirmDialog(
                app.getMainWindow(),
                inputs, "Select suites to use",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        final int nrmIndex = nrmComboBox.getSelectedIndex();
        final int normalizerIndex = normalizerComboBox.getSelectedIndex();
        final int estimateTypeIndex = estimateTypeComboBox.getSelectedIndex();
        
        if (nrmIndex == normalizerIndex) {
            app.errorDialog("RPI suites must be different",
                    "You can't use the same suite for both NRM and normalizer "
                    + "in an RPI calculation.");
            return false;
        }

        if (userChoice == JOptionPane.CANCEL_OPTION
                || nrmIndex == -1 || normalizerIndex == -1
                || estimateTypeIndex == -1) {
            return false;
        }
        
        nrm = app.getSuites().get(nrmIndex);
        normalizer = app.getSuites().get(normalizerIndex);
        estimateType = RpiEstimateType.values()[estimateTypeIndex];
        return true;
    }
    
    /**
     * @return the NRM suite selected by the user, or null if none
     */
    public Suite getNrm() {
        return nrm;
    }

    /**
     * @return the normalizer suite selected by the user, or null if none
     */
    public Suite getNormalizer() {
        return normalizer;
    }

    /**
     * @return the estimate type selected by the user, or null if none
     */
    public RpiEstimateType getEstimateType() {
        return estimateType;
    }

}
