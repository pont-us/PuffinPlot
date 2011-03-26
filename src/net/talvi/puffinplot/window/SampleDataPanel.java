package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Suite;

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
        Suite suite = sample.getSuite();
        for (int i=0; i<suite.getCustomFlagNames().size(); i++) {
            add(new SampleCheckBox(sample, i));
        }
        for (int i = 0; i < suite.getCustomNoteNames().size(); i++) {
            add(new JLabel(suite.getCustomNoteNames().get(i)));
            add(new SampleField(sample, i));
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

    private class SampleField extends JTextField {
        private final Sample sample;
        private final int flagNum;
        public SampleField(final Sample sample, final int noteNum) {
            super(sample.getCustomNotes().get(noteNum));
            setMaximumSize(new Dimension(200, 30));
            this.sample = sample;
            this.flagNum = noteNum;
            getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    sample.getCustomNotes().set(noteNum, getText());
                }

                public void removeUpdate(DocumentEvent e) {
                    sample.getCustomNotes().set(noteNum, getText());
                }

                public void changedUpdate(DocumentEvent e) {
                    sample.getCustomNotes().set(noteNum, getText());
                }
            });
        }
    }
}
