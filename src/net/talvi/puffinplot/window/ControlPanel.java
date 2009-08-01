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

import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;

public class ControlPanel extends JPanel 
   implements ActionListener, ItemListener {

    private static final long serialVersionUID = 1L;

    private final JLabel correctionField;
    JComboBox suiteBox;
    private CorrectionBox correctionBox;
    VVsBox vVsBox;
    // private JButton pcaButton;
    private static final PuffinApp app = PuffinApp.getInstance();
    
    private Action toggleZplotAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            vVsBox.toggle();
        }
    };
    
    public ControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(suiteBox = new JComboBox(new String[] {"no samples loaded"}));
        add(correctionBox = new CorrectionBox());
        add(vVsBox = new VVsBox());

        add(new JToolBar.Separator());
        add(correctionField = new JLabel());
        add(new JToolBar.Separator());
        add(new JButton(app.getActions().pcaOnSelection));
        add(new JButton(app.getActions().fisher));
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
        return correctionBox.getCorrection();
    }
    
    public void setCorrection(Correction c) {
        correctionBox.setCorrection(c);
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
    
    class CorrectionBox extends JComboBox {
        
        private static final long serialVersionUID = 1L;

        CorrectionBox() {
            super(new String[] {"uncorrected", "samp. corr.", "form. corr."});
            Correction[] cs = Correction.values();
            for (int i=0; i<cs.length; i++)
                if (cs[i] == app.getPrefs().getCorrection())
                    setSelectedIndex(i);
            addItemListener(ControlPanel.this);
        }
        
        public void setCorrection(Correction c) {
            Correction[] cs = Correction.values();
            for (int i=0; i<cs.length; i++) if (c==cs[i]) setSelectedIndex(i);
        }
        
        public Correction getCorrection() {
            return Correction.values()[getSelectedIndex()];
        }
        
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == correctionBox) app.redoCalculations();
        app.getMainWindow().repaint();
    }
}
