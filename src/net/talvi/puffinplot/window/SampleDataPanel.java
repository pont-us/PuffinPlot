package net.talvi.puffinplot.window;

import java.awt.Dimension;
import javax.swing.Box;
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
class SampleDataPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private Sample sample;

    public SampleDataPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //pane.setVisible(true);
    }

    public void setSample(Sample s) {
        this.sample = s;
        doUpdate();
    }

    public void doUpdate() {
        removeAll(); // remove all components
        if (sample==null) return;
        Suite suite = sample.getSuite();
        add(Box.createRigidArea(new Dimension(0,12)));
        int totalObjects = 0;
        for (int i=0; i<suite.getCustomFlagNames().size(); i++) {
            add(new SampleCheckBox(sample, i));
            totalObjects++;
        }
        for (int i=0; i < suite.getCustomNoteNames().size(); i++) {
            add(new JLabel(suite.getCustomNoteNames().get(i)));
            add(new SampleField(sample, i));
            totalObjects++;
        }
        if (totalObjects>0) {
            setMinimumSize(new Dimension(50,100));
            setPreferredSize(new Dimension(150,100));
            setVisible(true);
        } else {
            setVisible(false);
        }
        revalidate();
    }

    private class SampleCheckBox extends JCheckBox {
        private static final long serialVersionUID = 1L;
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
            Boolean state = sample.getCustomFlags().get(flagNum);
            if (state != null) setSelected(state);
        }
    }

    private class SampleField extends JTextField {
        private static final long serialVersionUID = 1L;
        //private final Sample sample;
        //private final int flagNum;
        public SampleField(final Sample sample, final int noteNum) {
            super(sample.getCustomNotes().get(noteNum)==null ? "?" :
                sample.getCustomNotes().get(noteNum));
            setMaximumSize(new Dimension(2000, 30));
            //this.sample = sample;
            //this.flagNum = noteNum;
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
