package net.talvi.puffinplot.window;

import java.awt.Component;
import java.util.EventListener;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
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
        for (String name: sample.getCustomFlagNames()) {
            add(new SampleRadioButton(sample, name));
        }
        revalidate();
    }

    private class SampleRadioButton extends JRadioButton {
        private final Sample sample;
        private final String flagName;
        public SampleRadioButton(Sample sample, String flagName) {
            super(flagName);
            this.sample = sample;
            this.flagName = flagName;
            addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    SampleRadioButton srb = SampleRadioButton.this;
                    srb.sample.setCustomFlag(srb.flagName, isSelected());
                }
            });
            updateState();
        }

        private void updateState() {
            setSelected(sample.getCustomFlag(flagName));
        }
        
    }

}
