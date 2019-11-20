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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.talvi.puffinplot.window;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import net.talvi.puffinplot.PuffinApp;

import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;

/**
 * An ‘about this program’ dialog box giving brief information
 * about PuffinPlot.
 * 
 * @author  pont
 */
public class AboutBox extends JDialog {
    
   
    /**
     * Creates a new about box.
     *
     * @param app the PuffinPlot instance associated with this dialog
     */
    public AboutBox(PuffinApp app) {
        super(app.getMainWindow(), "About PuffinPlot",
                ModalityType.APPLICATION_MODAL);

        setPreferredSize(new Dimension(600, 360));
        final BoxLayout layout =
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        getContentPane().setLayout(layout);

        getContentPane().add(Box.createVerticalStrut(8));
        final JLabel heading = new JLabel("PuffinPlot", SwingConstants.CENTER);
        heading.setFont(heading.getFont()
                .deriveFont(heading.getFont().getSize() + 8F));
        heading.setAlignmentX(0.5F);
        getContentPane().add(heading);

        final JTextArea textArea = new JTextArea(getMainText(app));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(8, 8, 8, 8));

        final JScrollPane scrollPane = new JScrollPane(textArea,
                VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        getContentPane().add(scrollPane);

        final JLabel versionInfo = new JLabel(
                getVersionString(app), SwingConstants.CENTER);
        versionInfo.setAlignmentX(0.5F);
        getContentPane().add(versionInfo);
        getContentPane().add(Box.createVerticalStrut(8));

        final JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(0.5F);
        closeButton.setPreferredSize(new Dimension(120, 30));
        closeButton.addActionListener(e -> setVisible(false));
        getContentPane().add(closeButton);
        getContentPane().add(Box.createVerticalStrut(8));

        pack();
    }

    private static String getMainText(PuffinApp app) {
        final String fmt =
                "PuffinPlot is a program for showing, exploring, and analysing "
                + "palæomagnetic data. Copyright %s Pontus "
                + "Lurcock (pont@talvi.net). "
                + "PuffinPlot is free software: you can redistribute it and/or "
                + "modify it under the terms of the GNU General Public License "
                + "as published by the Free Software Foundation, either version "
                + "3 of the License, or (at your option) any later version. "
                + "PuffinPlot is distributed in the hope that it will be useful, "
                + "but WITHOUT ANY WARRANTY; without even the implied warranty "
                + "of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. "
                + "Please see the file LICENCE (supplied with PuffinPlot) "
                + "or go to http://www.gnu.org/licenses/gpl.html "
                + "for details.\n\n%s\n"
                + "System information: OS %s %s (%s), Java %s (%s)";
        return String.format(fmt, app.getVersion().getYearRange(),
                getVersionString(app), gsp("os.name"), gsp("os.version"),
                gsp("os.arch"), gsp("java.version"), gsp("java.vendor"));
    }
    
    private static String getVersionString(PuffinApp app) {
        return String.format("Version: %s. Date: %s.",
                app.getVersion().getVersionString(),
                app.getVersion().getDateString());
    }

    private static String gsp(String property) {
        return System.getProperty(property);
    }
}
