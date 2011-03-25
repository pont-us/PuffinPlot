package net.talvi.puffinplot.window;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.talvi.puffinplot.data.Sample;

/**
 *  SampleDataPanel displays information about the current sample, including
 *  custom flags.
 */
public class SampleDataPanel extends JPanel {

    private Sample sample;

    public SampleDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //pane.setVisible(true);
    }

    public void setSample(Sample s) {
        this.sample = s;
        doUpdate();
    }

    private void doUpdate() {
        removeAll(); // remove all components
        for (int i=0; i<sample.getSuite().getCustomFlagNames().size(); i++) {
            add(new SampleCheckBox(sample, i));
        }
        revalidate();
    }

    private class SampleCheckBox extends JCheckBox {
        private final Sample sample;
        private final int flagNum;
        public SampleCheckBox(Sample sample, int flagNum) {
            super(sample.getSuite().getCustomFlagNames().get(flagNum));
            this.sample = sample;
            this.flagNum = flagNum;
            addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    SampleCheckBox scb = SampleCheckBox.this;
                    scb.sample.getCustomFlags().set(scb.flagNum, isSelected());
                }
            });
            updateState();
        }

        private void updateState() {
            setSelected(sample.getCustomFlags().get(flagNum));
        }
        
    }

}
