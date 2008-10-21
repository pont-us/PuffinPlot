package net.talvi.puffinplot;

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
import javax.swing.JPanel;

import javax.swing.KeyStroke;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.MeasurementAxis;

public class ControlPanel extends JPanel 
        implements FileOpenedListener, ActionListener, ItemListener {

    private static final long serialVersionUID = 1L;

    JComboBox coreBox;
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
        add(coreBox = new JComboBox(new String[] {"no samples loaded"}));
        add(correctionBox = new CorrectionBox());
        add(vVsBox = new VVsBox());
        add(new JButton(PuffinApp.getApp().actions.pca));
        add(new JButton(PuffinApp.getApp().actions.fisher));
        add(new JButton(PuffinApp.getApp().actions.clear));
        PuffinApp.getApp().addFileOpenedListener(this);
        coreBox.addActionListener(this);
        
        int modifierKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('E', modifierKey), "toggle-zplot");
        getActionMap().put("toggle-zplot", toggleZplotAction);
    }
    
    void updateCoreBox() {
        coreBox.removeAllItems();
        for (Suite suite: PuffinApp.getApp().suites) {
            coreBox.addItem(suite.getName());
        }
    }

    public void fileOpened() {
        updateCoreBox();
    }

    public void actionPerformed(ActionEvent e) {
        int index = coreBox.getSelectedIndex();
        if (index > -1) PuffinApp.getApp().setCurrentSuite(index);        
    }
    
    public MeasurementAxis getAxis() {
        return vVsBox.axis();
    }
    
    public Correction getCorrection() {
        return correctionBox.correction();
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
            addItemListener(ControlPanel.this);
        }
        
        public Correction correction() {
            switch (getSelectedIndex()) {
            case 0: return Correction.NONE;
            case 1: return Correction.SAMPLE;
            case 2: return Correction.FORMATION;
            default: throw new Error("unknown correction");
            }
        }
        
    }

    public void itemStateChanged(ItemEvent e) {
        PuffinApp.getApp().mainWindow.repaint();
    }
}
