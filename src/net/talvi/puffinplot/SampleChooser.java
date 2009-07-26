package net.talvi.puffinplot;

import com.sun.org.apache.bcel.internal.generic.SIPUSH;
import net.talvi.puffinplot.data.Suite;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import net.talvi.puffinplot.data.Sample;

public class SampleChooser extends JPanel {

    private static final long serialVersionUID = 7533359714843605451L;
    private DepthSlider depthSlider;
    private SampleList sampleList;
    private JScrollPane samplePane;

    private Action nextAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            depthSlider.changeValueBy(1);
            sampleList.changeIndexBy(1);
        }
    };
    
    private Action prevAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            depthSlider.changeValueBy(-1);
            sampleList.changeIndexBy(-1);
        }
    };
    
    SampleChooser() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(depthSlider = new DepthSlider());
        samplePane = new JScrollPane(sampleList = new SampleList());
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
     * 
     * @return Selected samples if current suite is discrete; a length-1 
     * array containing the current sample if it's continuous.
     * 
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
            samples = Collections.singletonList(suite.getCurrentSample());
            break;
        default:
            throw new Error("Unknown measurement type.");
        }
        return samples;
    }

    private class SampleList extends JList {
        
        SampleList() {
            setAlignmentY(0);
            addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (getSelectedValue() != null)
                    PuffinApp.getInstance().getSuite().
                            setCurrentName((String) getSelectedValue());
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
    
    private class DepthSlider extends JSlider {

        private static final long serialVersionUID = 1L;

        DepthSlider() {
            super(VERTICAL, 0, 0, 0);
            setMinimum(0);
            setValue(0);
            setInverted(true);
            setMajorTickSpacing(10);
            setMinorTickSpacing(1);
            setPaintTicks(true);
            setSnapToTicks(true);
            addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    PuffinApp.getInstance().getSuite().setCurrentDepthIndex(getValue());
                    PuffinApp.getInstance().updateDisplay();
                }
            });
        }

        void setForSuite(Suite suite) {
            setMaximum(suite.getNumSamples() - 1);
            setValue(suite.getCurrentDepthIndex());
        }
        
        void changeValueBy(int delta) {
            if (isVisible()) {
                int proposedValue = getValue() + delta;
                if (proposedValue >= getMinimum() &&
                        proposedValue <= getMaximum())
                    setValue(proposedValue);
            }
        }
    }

    private void setVisibility(boolean slider, boolean list) {
        depthSlider.setVisible(slider);
        samplePane.setVisible(list);
    }

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
                sampleList.setListData(suite.getNameArray());
                sampleList.setSelectedIndex(0);
                setVisibility(false, true);
                break;
            default:
                throw new RuntimeException("No such measurement type.");
        }
    }

    public int getDepthIndex() {
        return depthSlider.getValue();
    }
//    public String getSampleName() {
//        return sampleList.getModel().getElementAt(sampleList.getSelectedIndex()).toString();
//    }
}
