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
import net.talvi.puffinplot.data.MeasurementAxis;
import net.talvi.puffinplot.data.Sample;
import static net.talvi.puffinplot.data.Correction.Rotation;

/**
 * The control panel provides a user interface for common operations.
 * These include suite selection, orientation correction, and Zijderveld
 * plot axis selection. The control panel sits at the top of the main window.
 * The control panel also displays some sample data.
 * 
 * @author pont
 */
public class ControlPanel extends JPanel 
   implements ActionListener, ItemListener {

    private static final long serialVersionUID = 1L;
    private final JLabel correctionField;
    JComboBox suiteBox;
    private RotationBox rotationBox;
    VVsBox vVsBox;
    private static final PuffinApp app = PuffinApp.getInstance();
    
    private Action toggleZplotAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;
        public void actionPerformed(ActionEvent e) {
            vVsBox.toggle();
        }
    };
    private final JRadioButton trayButton;
    private final JRadioButton emptyButton;
    
    /** Creates a new control panel */
    public ControlPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(suiteBox = new JComboBox(new String[] {"no files loaded"}));
        add(rotationBox = new RotationBox());
        trayButton = emptyButton = null;
//         Tray correction applied on loading; empty-slot correction
//         needs re-architecting if it's to be used -- 2011-10-09.
//        add(new JLabel("T"));
//        add(trayButton = new JRadioButton());
//        trayButton.addItemListener(this);
//        add(new JLabel("E"));
//        add(emptyButton = new JRadioButton());
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
        app.getMainWindow().updateSampleDataPanel();
    }
    
    /** Updates this control panel's sample information display. */
    public void updateSample() {
        Sample s = app.getSample();
        if (s != null) 
            correctionField.setText(String.format(
                    "Samp. %.1f/%.1f Form. %.1f/%.1f Dev. %.1f Anc:%s",
                    s.getSampAz(), s.getSampDip(), s.getFormAz(), s.getFormDip(),
                    s.getMagDev(), s.isPcaAnchored() ? "Y" : "N"));
    }

    /** Handles action events. Currently the only action event 
     * handled is that produced by a user selecting a suite from
     * the suite selection combo box.
     * @param e the action event to handle
     */
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
    
    /** Returns the currently selected vertical projection for the 
     * Zijderveld plot. 
     * @return  the currently selected vertical projection for the 
     * Zijderveld plot
     */
    public MeasurementAxis getAxis() {
        return vVsBox.axis();
    }
    
    /** Returns the correction to apply to magnetic moment data. 
     * @return the correction to apply to magnetic moment data */
    public Correction getCorrection() {
        /* Tray correction is applied on loading, and empty slot correction
         * is currently unused, so these fields are set to false in the
         * Correction. Originally these wre read from the user checkboxes
         * trayButton and emptyButton, which have now been removed.
         */
        return new Correction(false, false,
                rotationBox.getRotation());
    }
    
    /** Sets the correction to apply to magnetic moment data. 
     * @param c the correction to apply to magnetic moment data */
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
    
    private class RotationBox extends JComboBox {
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

    /** Handles changes in the state of user interface components.
     * For instance, this method will be called if the user
     * selects a different correction to be applied to magnetic
     * moment data.
     * 
     * @param e the event corresponding to the change
     */
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
