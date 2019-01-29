/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */

package net.talvi.puffinplot.window;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import static javax.swing.SwingWorker.StateValue;

/**
 * A dialog which shows progress for a supplied SwingWorker task. The
 * constructor is private so the class cannot be instantiated directly, but a
 * static method is provided to create an instance and show it.
 *
 * @author pont
 */
public class ProgressDialog extends JDialog
        implements ActionListener, PropertyChangeListener {

    private final JProgressBar progressBar;
    private final JButton cancelButton;
    private final JPanel outerPanel;
    private final JPanel innerPanel;
    private final SwingWorker<Void, Void> worker;

    private ProgressDialog(String title, Frame owner, SwingWorker<Void, Void> worker) {
        super(owner, title, true);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("start");

        outerPanel = new JPanel(new BorderLayout());
        innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(progressBar);
        innerPanel.add(new Box.Filler(new Dimension(5, 2),
                new Dimension(5, 16),
                new Dimension(5, 40)));
        cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        innerPanel.add(cancelButton);
        outerPanel.add(innerPanel, BorderLayout.PAGE_START);
        outerPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        outerPanel.setOpaque(true);

        this.worker = worker;
    }
    
    private void setup(Component parent) {
        cancelButton.addActionListener(this);

        worker.addPropertyChangeListener(this);
        worker.execute();

        setMinimumSize(new Dimension(400, 100));
        setContentPane(outerPanel);
        pack();
        setLocationRelativeTo(parent);
   }

    @Override
    public void actionPerformed(ActionEvent evt) {
        worker.cancel(true);
        setVisible(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if ("progress".equals(event.getPropertyName())) {
            final int progress = (Integer) event.getNewValue();
            progressBar.setValue(progress);
        } else if ("state".equals(event.getPropertyName())) {
            final StateValue sv = (StateValue) event.getNewValue();
            if (sv == StateValue.DONE) {
                setVisible(false);
            }
        }
    }

    /**
     * Create and show a progress dialog for a supplied SwingWorker.
     * 
     * The progress dialog will be displayed modally (i.e. blocking
     * user interaction while it is open), and this call will block
     * until the task has finished running.
     * 
     * @param title Title for the dialog
     * @param parent Parent window for the dialog
     * @param worker task whose progress the dialog should monitor
     */
    public static void showDialog(String title, Frame parent,
            SwingWorker<Void, Void> worker) {
        final ProgressDialog progressDialog = new ProgressDialog(title, parent, worker);
        progressDialog.setup(parent);
        progressDialog.setVisible(true);
        // This will block until the dialog closes.
    }
}
