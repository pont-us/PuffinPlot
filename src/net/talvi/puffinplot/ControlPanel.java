package net.talvi.puffinplot;

import java.awt.Dimension;
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

import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasurementAxis;

public class ControlPanel extends JPanel 
   implements ActionListener, ItemListener {

    private static final long serialVersionUID = 1L;

    private final NumField sampAzField;
    private final NumField sampDipField;
    private final NumField formAzField;
    private final NumField formDipField;
    JComboBox suiteBox;
    private CorrectionBox correctionBox;
    VVsBox vVsBox;
    // private JButton pcaButton;
    
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
        add(new JLabel("samp az"));
        add(sampAzField = new NumField(4));
        add(new JLabel("dip"));
        add(sampDipField = new NumField(3));
        add(new JLabel("form az"));
        add(formAzField = new NumField(4));
        add(new JLabel("dip"));
        add(formDipField = new NumField(3));
        add(new JLabel("mag dev"));
        add(new NumField(4));
        add(new JToolBar.Separator());
        add(new JButton(PuffinApp.getApp().getActions().pcaOnSelection));
        add(new JButton(PuffinApp.getApp().getActions().fisher));
        add(new JButton(PuffinApp.getApp().getActions().clear));
        
        suiteBox.addActionListener(this);
        
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).
                put(KeyStroke.getKeyStroke('E', modifierKey), "toggle-zplot");
        getActionMap().put("toggle-zplot", toggleZplotAction);
    }

    private class NumField extends JLabel {
        public NumField(int columns) {
            super("----");
            setMaximumSize(new Dimension(getPreferredSize().width,
                   getMaximumSize().height));
        }
        
        public void setValue(double value) {
            setText(Double.toString(value));
        }
    }
    
    private volatile boolean updatingSuites = false;
    void updateSuites() {
        updatingSuites = true;
        suiteBox.removeAllItems();
        for (Suite suite: PuffinApp.getApp().suites) {
            suiteBox.addItem(suite);
        }
        updatingSuites = false;
        Suite currentSuite = PuffinApp.getApp().getSuite();
        suiteBox.setSelectedItem(currentSuite);
    }
    
    void updateSample() {
        Datum d = PuffinApp.getApp().getSample().getDatum(0);
        sampAzField.setValue(d.getSampAz());
        sampDipField.setValue(d.getSampDip());
        formAzField.setValue(d.getFormAz());
        formDipField.setValue(d.getFormDip());
    }

    public void actionPerformed(ActionEvent e) {
        /* No way to tell if this was a user click or the box being
         * rebuilt, so we have to use this ugly variable to avoid spurious
         * changes.
         * 
         */
        if (!updatingSuites) {
            int index = suiteBox.getSelectedIndex();
            if (index > -1) PuffinApp.getApp().setSuite(index);
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
                if (cs[i] == PuffinApp.getApp().getPrefs().getCorrection())
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
        PuffinApp.getApp().getMainWindow().repaint();
    }
}
