/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2017 Pontus Lurcock.
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import static javax.swing.SwingWorker.StateValue;

/**
 *
 * @author pont
 */
public class ProgressDialog extends JFrame
        implements ActionListener, PropertyChangeListener {

    private final JProgressBar progressBar;
    private final JButton cancelButton;
    private final JPanel outerPanel;
    private final JPanel innerPanel;
    private final Worker worker;

    public class Worker extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            setProgress(0);
            for (int i=0; i<100; i += 7) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignore) {}

                setProgress(i);
            }
            setProgress(100);
            return null;
        }
    }

    private ProgressDialog(String title) {
        super(title);

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

        worker = new Worker();
    }
    
    private void setup(Component parent) {
        cancelButton.addActionListener(this);

        worker.addPropertyChangeListener(this);
        worker.execute();

        setMinimumSize(new Dimension(400, 100));
        setContentPane(outerPanel);
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));        
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        worker.cancel(true);
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

    public static ProgressDialog getInstance(String title, Component parent) {
        final ProgressDialog progressDialog = new ProgressDialog(title);
        progressDialog.setup(parent);
        
        return progressDialog;
    }
}
