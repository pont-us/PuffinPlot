/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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

import net.talvi.puffinplot.*;
import net.talvi.puffinplot.data.Suite;
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
import net.talvi.puffinplot.data.Sample;

/**
 * A component allowing a user to choose the current sample and
 * the selected samples.
 * For discrete suites, it shows a list of sample names.
 * For continuous suites, it shows a slider control representing
 * the depth within the core.
 * 
 * @author pont
 */
public class SampleChooser extends JPanel {

    private static final long serialVersionUID = 7533359714843605451L;
    private DepthSlider depthSlider;
    private SampleList sampleList;
    private JScrollPane samplePane;

    private Action nextAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            depthSlider.changeValueBy(1);
            sampleList.changeIndexBy(1);
        }
    };
    
    private Action prevAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            depthSlider.changeValueBy(-1);
            sampleList.changeIndexBy(-1);
        }
    };
    
    SampleChooser() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(depthSlider = new DepthSlider());
        samplePane = new JScrollPane(sampleList = new SampleList(new DefaultListModel()));
        samplePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(samplePane);
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('N', modifierKey), "next");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('B', modifierKey), "previous");
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
        List<Sample> samples;
        Suite suite = PuffinApp.getInstance().getSuite();
        if (suite == null) return Collections.emptyList();
        switch (suite.getMeasType()) {
        case DISCRETE:
            Object[] names = sampleList.getSelectedValues();
            samples = new ArrayList<Sample>(names.length);
            for (int i = 0; i < names.length; i++)
                samples.add(suite.getSampleByName((String) names[i]));
            break;
        case CONTINUOUS:
            int start = depthSlider.getRangeStart();
            int end = depthSlider.getRangeEnd();
            int value = depthSlider.getValue();
            if (start > -1) {
                samples = new ArrayList<Sample>(end - start + 2);
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

    private class SampleList extends JList {
        private static final long serialVersionUID = 1L;

        DefaultListModel model;

        SampleList(DefaultListModel model) {
            super(model);
            this.model = model;
            setAlignmentY(0);
            addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (getSelectedIndex() != -1)
                    PuffinApp.getInstance().getSuite().
                            setCurrentSampleIndex(getSelectedIndex());
                    PuffinApp.getInstance().updateDisplay();
            }
            });
        }
        
        void changeIndexBy(int delta) {
            if (isVisible()) {
                int proposedValue = getSelectedIndex() + delta;
                if (proposedValue >= 0 && proposedValue < getModel().getSize())
                    setSelectedIndex(proposedValue);
            }
        }
    }

    private void setVisibility(boolean slider, boolean list) {
        depthSlider.setVisible(slider);
        samplePane.setVisible(list);
    }

    /**
     * Redraws the sample chooser to reflect a change in the
     * Puffin application's current suite.
     */
    public void updateSuite() {
        Suite suite = PuffinApp.getInstance().getSuite();
        if (suite == null) {
            setVisibility(false, false);
            return;
        }
        switch (suite.getMeasType()) {
            case CONTINUOUS:
                depthSlider.setForSuite(suite);
                setVisibility(true, false);
                break;
            case DISCRETE:
                DefaultListModel model = sampleList.model;
                model.clear();
                for (Sample s: suite.getSamples())
                    model.addElement(s.getNameOrDepth());
                sampleList.setSelectedIndex(0);
                setVisibility(false, true);
                break;
            default:
                throw new RuntimeException("No such measurement type.");
        }
    }

    /**
     * For long core suites, returns the current depth index.
     * Note that this is not the depth itself, but an index into the list
     * of data points.
     * 
     * @return the current depth index for long core suites
     */
    public int getDepthIndex() {
        return depthSlider.getValue();
    }
    
    public void updateValueFromSuite() {
        final Suite suite = PuffinApp.getInstance().getSuite();
        final int index = suite.getCurrentSampleIndex();
        switch (suite.getMeasType()) {
            case CONTINUOUS:
                depthSlider.setValue(index);
                break;
            case DISCRETE:
                sampleList.setSelectedIndex(index);
                break;
            default:
                throw new RuntimeException("No such measurement type.");
        }
    }
}
