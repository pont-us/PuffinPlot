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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;

/**
 * A component allowing a user to choose the current sample and the selected
 * samples. For discrete suites, it shows a list of sample names. For continuous
 * suites, it shows a slider control representing the depth within the core.
 *
 * @author pont
 */
public class SampleChooser extends JPanel {

    private static final long serialVersionUID = 7533359714843605451L;
    private final PuffinApp app;
    private final DepthSlider depthSlider;
    private final SampleList sampleList;
    private final JScrollPane samplePane;

    private final Action nextAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            depthSlider.changeValueBy(1);
            sampleList.changeIndexBy(1);
        }
    };
    
    private final Action prevAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;
        @Override
        public void actionPerformed(ActionEvent e) {
            depthSlider.changeValueBy(-1);
            sampleList.changeIndexBy(-1);
        }
    };
    
    SampleChooser(PuffinApp app) {
        this.app = app;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(depthSlider = new DepthSlider(app));
        sampleList = new SampleList(new DefaultListModel<>());
        samplePane = new JScrollPane(sampleList);
        samplePane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(samplePane);
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke('N', modifierKey), "next");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke('B', modifierKey), "previous");
        getActionMap().put("next", nextAction);
        getActionMap().put("previous", prevAction);
        setVisibility(false, false);
    }
    
    /**
     * Returns all the currently selected samples.
     * 
     * @return all the currently selected samples
     */
    public List<Sample> getSelectedSamples() {
        final List<Sample> samples;
        final Suite suite = app.getCurrentSuite();
        if (suite == null) return Collections.emptyList();
        switch (suite.getMeasurementType()) {
        case DISCRETE:
            List names = sampleList.getSelectedValuesList();
            samples = new ArrayList<>(names.size());
            for (Object name : names) {
                samples.add(suite.getSampleByName((String) name));
            }
            break;
        case CONTINUOUS:
            int start = depthSlider.getRangeStart();
            int end = depthSlider.getRangeEnd();
            int value = depthSlider.getValue();
            if (start > -1) {
                samples = new ArrayList<>(end - start + 2);
                if (value < start) samples.add(suite.getSampleByIndex(value));
                for (int i = start; i <= end; i++) {
                    samples.add(suite.getSampleByIndex(i));
                }
                if (value > end) samples.add(suite.getSampleByIndex(value));
            } else {
                samples = Collections.singletonList(suite.getCurrentSample());
            }
            break;
        default:
            throw new Error("Unknown measurement type.");
        }
        return samples;
    }

    private class SampleList extends JList<String> {
        private static final long serialVersionUID = 1L;

        final DefaultListModel<String> model;

        SampleList(DefaultListModel<String> model) {
            super(model);
            this.model = model;
            setAlignmentY(0);
            addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (getSelectedIndex() != -1) {
                        app.getCurrentSuite().
                                setCurrentSampleIndex(getSelectedIndex());
                    }
                    app.updateDisplay();
                }
            });
        }
        
        void changeIndexBy(int delta) {
            if (isVisible()) {
                int proposedValue = getSelectedIndex() + delta;
                if (proposedValue >= 0 &&
                        proposedValue < getModel().getSize()) {
                    setSelectedIndex(proposedValue);
                }
            }
        }
    }

    private void setVisibility(boolean slider, boolean list) {
        depthSlider.setVisible(slider);
        samplePane.setVisible(list);
    }

    /**
     * Redraws the sample chooser to reflect a change in the Puffin
     * application's current suite.
     */
    public void updateSuite() {
        final Suite suite = app.getCurrentSuite();
        if (suite == null) {
            setVisibility(false, false);
            return;
        }
        switch (suite.getMeasurementType()) {
            case CONTINUOUS:
                depthSlider.setForSuite(suite);
                setVisibility(true, false);
                break;
            case DISCRETE:
                final DefaultListModel<String> model = sampleList.model;
                model.clear();
                for (Sample sample: suite.getSamples()) {
                    model.addElement(sample.getNameOrDepth());
                }
                sampleList.setSelectedIndex(0);
                setVisibility(false, true);
                break;
            case UNSET:
                setVisibility(false, false);
                break;
            default:
                throw new RuntimeException("No such measurement type.");
        }
    }

    /**
     * For long core suites, returns the current depth index. Note that this is
     * not the depth itself, but an index into the list of data points.
     *
     * @return the current depth index for long core suites
     */
    public int getDepthIndex() {
        return depthSlider.getValue();
    }
    
    /**
     * Updates the displayed value from the current PuffinPlot application.
     * 
     * This sample chooser's display is updated to show the currently
     * selected sample in the currently displayed suite of the PuffinPlot
     * application supplied to this SampleChooser's constructor.
     */
    public void updateValueFromSuite() {
        final Suite suite = app.getCurrentSuite();
        final int index = suite.getCurrentSampleIndex();
        switch (suite.getMeasurementType()) {
            case CONTINUOUS:
                depthSlider.setValue(index);
                break;
            case DISCRETE:
                sampleList.setSelectedIndex(index);
                sampleList.ensureIndexIsVisible(index);
                break;
            default:
                throw new RuntimeException("No such measurement type.");
        }
    }
}
