package net.talvi.puffinplot.window;

import net.talvi.puffinplot.*;
import net.talvi.puffinplot.data.Suite;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JRadioButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import static net.talvi.puffinplot.data.Correction.Rotation;

public class ControlPanel extends JPanel 
   implements ActionListener, ItemListener {

    private static final long serialVersionUID = 1L;
    private final JLabel correctionField;
    JComboBox suiteBox;
    private RotationBox rotationBox;
    VVsBox vVsBox;
    private static final PuffinApp app = PuffinApp.getInstance();
    
    private Action toggleZplotAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            vVsBox.toggle();
        }
    };
    private final JRadioButton trayButton;
    private final JRadioButton emptyButton;
    
    public ControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(suiteBox = new JComboBox(new String[] {"no files loaded"}));
        add(rotationBox = new RotationBox());
        add(new JLabel("T"));
        add(trayButton = new JRadioButton());
        trayButton.addItemListener(this);
        add(new JLabel("E"));
        add(emptyButton = new JRadioButton());
        add(vVsBox = new VVsBox());
        add(new JToolBar.Separator());
        add(correctionField = new JLabel());
        add(new JToolBar.Separator());
        add(new JButton(app.getActions().selectAll));
        add(new JButton(app.getActions().pcaOnSelection));
        add(new JButton(app.getActions().clear));
        
        suiteBox.addActionListener(this);
        
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke('E', modifierKey), "toggle-zplot");
        getActionMap().put("toggle-zplot", toggleZplotAction);
    }
    
    private volatile boolean updatingSuites = false;
    void updateSuites() {
        updatingSuites = true;
        suiteBox.removeAllItems();
        for (Suite suite: app.getSuites()) {
            suiteBox.addItem(suite);
        }
        updatingSuites = false;
        Suite currentSuite = app.getSuite();
        if (currentSuite != null) suiteBox.setSelectedItem(currentSuite);
    }
    
    public void updateSample() {
        Sample s = app.getSample();
        Datum d = null;
        if (s != null) d = s.getDatum(0);
        if (d != null)
            correctionField.setText(String.format(
                    "Samp. %.1f/%.1f Form. %.1f/%.1f Dev. %.1f Anc:%s",
                    d.getSampAz(), d.getSampDip(), d.getFormAz(), d.getFormDip(),
                    d.getMagDev(), d.isPcaAnchored() ? "Y" : "N"));
    }

    public void actionPerformed(ActionEvent e) {
        /* No way to tell if this was a user click or the box being
         * rebuilt, so we have to use this ugly variable to avoid spurious
         * changes.
         */
        if (!updatingSuites) {
            int index = suiteBox.getSelectedIndex();
            if (index > -1) app.setSuite(index);
        }
    }
    
    public MeasurementAxis getAxis() {
        return vVsBox.axis();
    }
    
    public Correction getCorrection() {
        return new Correction(trayButton.isSelected(),
                emptyButton.isSelected(),
                rotationBox.getRotation());
    }
    
    public void setCorrection(Correction c) {
        rotationBox.setRotation(c.getRotation());
    }
    
    private class VVsBox extends JComboBox {
        
        private static final long serialVersionUID = 1L;

        VVsBox() {
            super(new String[] {"V vs. N", "V vs. E", "V vs. H"});
            addItemListener(ControlPanel.this);
        }
        
        public MeasurementAxis axis() {
            switch (getSelectedIndex()) {
            case 0: return MeasurementAxis.X;
            case 1: return MeasurementAxis.Y;
            case 2: return MeasurementAxis.H;
            default: throw new RuntimeException("unknown axis");
            }
        }
        
        void toggle() {
            setSelectedIndex((getSelectedIndex()+1) % getModel().getSize());
        }
    }
    
    class RotationBox extends JComboBox {
        private static final long serialVersionUID = 1L;

        RotationBox() {
            super(new String[] {"uncorrected", "samp. corr.", "form. corr."});
            addItemListener(ControlPanel.this);
        }
        
        public void setRotation(Rotation c) {
            setSelectedIndex(c == Rotation.FORMATION ? 2 :
                c == Rotation.SAMPLE ? 1 : 0);
        }
        
        public Rotation getRotation() {
            int i = getSelectedIndex();
            return                i == 0 ? Rotation.NONE :
                                  i == 1 ? Rotation.SAMPLE :
                               /* i == 2*/ Rotation.FORMATION;
        }
    }

    public void itemStateChanged(ItemEvent e) {
        final Object s = e.getSource();
        if (s == rotationBox || s == trayButton || s == emptyButton ||
                s == vVsBox) {
            app.setCorrection(getCorrection());
            app.redoCalculations();
            app.getMainWindow().repaint();
        }
    }
}
