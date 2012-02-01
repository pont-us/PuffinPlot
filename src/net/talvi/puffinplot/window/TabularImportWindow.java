/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.talvi.puffinplot.window;

import java.awt.Dimension;
import java.util.List;
import javax.swing.*;
import net.talvi.puffinplot.PuffinApp;
import net.talvi.puffinplot.data.DatumField;
import org.apache.batik.swing.JSVGScrollPane;

/**
 *
 * @author pont
 */
public class TabularImportWindow extends JFrame {
    
    public TabularImportWindow() {
        super("Import data");
        setPreferredSize(new Dimension(400, 400));
        
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        add(new JScrollPane(new FieldChooserPane()));

        pack();
        setLocationRelativeTo(PuffinApp.getInstance().getMainWindow());
    }
    
    private static class FieldChooserPane extends JPanel {
        
        public FieldChooserPane() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            for (int i=0; i<20; i++) {
                add(new FieldChooser(i+1));
            }
        }
        
    }
    
    private static class FieldChooser extends JPanel {
        private static final long serialVersionUID = 1L;
    
        private final JComboBox fieldBox;
        private static final String[] fieldStrings;
        
        static {
            final DatumField[] fields = DatumField.values();
            fieldStrings = new String[fields.length];
            for (int i=0; i<fields.length; i++) {
                fieldStrings[i] = fields[i].getNiceName();
            }
        }
        
        public FieldChooser(int columnNumber) {
            super(false);
            setMaximumSize(new Dimension(500, 24));
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            add(Box.createRigidArea((new Dimension(8,0))));
            
            SpinnerModel spinnerModel =
                    new SpinnerNumberModel(columnNumber, 1, 1000, 1);
            JSpinner spinner = new JSpinner(spinnerModel);
            //l.setLabelFor(spinner);
            spinner.setPreferredSize(new Dimension(100, 24));
            add(spinner);

            fieldBox = new JComboBox(fieldStrings);
            fieldBox.setPreferredSize(new Dimension(300, 24));
            // box.setToolTipText(toolTip);

            add(fieldBox);
        }

        public int getColumnNumber() {
            return 0;
        }
        
        public DatumField getField() {
            return null;
        }

    }
    
}
