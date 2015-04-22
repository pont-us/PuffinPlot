/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.talvi.puffinplot.data.FileType;
import static java.awt.event.KeyEvent.getExtendedKeyCodeForChar;

/**
 * A dialog allowing the user to select a file type.
 * 
 * The file types presented are those from which PuffinPlot can read data.
 * 
 * @author pont
 */
public class FiletypeDialog extends JDialog {
    
    private FileType fileType = null;
    
    /**
     * Creates a new FiletypeDialog.
     * 
     * @param owner the owner of the dialog
     */
    public FiletypeDialog(Frame owner) {
        super(owner, "Select filetype", true);
        JPanel cp = new JPanel();
        cp.setLayout(new BoxLayout(cp, BoxLayout.Y_AXIS));
        cp.setBorder(new EmptyBorder(8,8,8,8));
        final JLabel jLabel = new JLabel("Please select the type of the file.");
        jLabel.setAlignmentX(CENTER_ALIGNMENT);
        cp.add(jLabel);
        cp.add(Box.createRigidArea((new Dimension(0, 16))));
        
        for (final FileType ft: FileType.values()) {
            if (ft != FileType.UNKNOWN) {
                final JButton button = new JButton(ft.getNiceName());
                button.setAlignmentX(Component.CENTER_ALIGNMENT);
                button.setMnemonic(getExtendedKeyCodeForChar(ft.getShortcut()));
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fileType = ft;
                        setVisible(false);
                        dispose();
                    }
                });
                cp.add(button);
                cp.add(Box.createRigidArea((new Dimension(0, 8))));
            }
        }
        
        cp.add(Box.createRigidArea((new Dimension(0, 16))));
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fileType = null;
                        setVisible(false);
                        dispose();
                    }
                });
        cancelButton.setAlignmentX(CENTER_ALIGNMENT);
        cancelButton.setMnemonic(KeyEvent.VK_ESCAPE);
        cp.add(cancelButton);
        setContentPane(cp);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * @return the fileType selected by the user
     */
    public FileType getFileType() {
        return fileType;
    }
    
}
