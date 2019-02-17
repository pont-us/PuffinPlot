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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
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
    
    private FileType selectedFileType = null;
    
    /**
     * Creates a new FiletypeDialog.
     * 
     * @param owner the owner of the dialog
     */
    public FiletypeDialog(Frame owner) {
        super(owner, "Select filetype", true);
        
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setBorder(new EmptyBorder(8,8,8,8));
        final JLabel introText =
                new JLabel("Please select the type of the file.");
        introText.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(introText);
        contentPane.add(Box.createRigidArea((new Dimension(0, 16))));
        
        for (final FileType buttonFileType: FileType.values()) {
            if (buttonFileType != FileType.UNKNOWN) {
                final JButton button =
                        new JButton(buttonFileType.getNiceName());
                button.setAlignmentX(Component.CENTER_ALIGNMENT);
                makeButtonShortcuts(button, 
                        buttonFileType.getShortcut());
                button.addActionListener(event -> {
                    selectedFileType = buttonFileType;
                    setVisible(false);
                    dispose();
                });
                contentPane.add(button);
                contentPane.add(Box.createRigidArea((new Dimension(0, 8))));
            }
        }
        
        contentPane.add(Box.createRigidArea((new Dimension(0, 16))));
        
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> {
            selectedFileType = null;
            setVisible(false);
            dispose();
        });
        cancelButton.setAlignmentX(CENTER_ALIGNMENT);
        makeButtonShortcuts(cancelButton, 'n');
        // Add escape key as an additional shortcut for the Cancel button.
        getRootPane().registerKeyboardAction(
                event -> cancelButton.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        contentPane.add(cancelButton);
        
        setContentPane(contentPane);
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    /**
     * Create two keyboard shortcuts for a button: one for a keypress with
     * alt held down, the other without.
     * 
     * @param button the button for which to create the shortcuts
     * @param character the character corresponding to the shortcut key
     */
    private void makeButtonShortcuts(final JButton button,
            final int character) {
        final int keyCode = getExtendedKeyCodeForChar(character);
        // Create the "alt" shortcut
        button.setMnemonic(keyCode);
        // Create the "plain" shortcut
        getRootPane().registerKeyboardAction(
                event -> button.doClick(),
                KeyStroke.getKeyStroke(keyCode, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * @return the selectedFileType selected by the user
     */
    public FileType getSelectedFileType() {
        return selectedFileType;
    }
}
