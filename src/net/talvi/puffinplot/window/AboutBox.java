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
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.talvi.puffinplot.PuffinApp;

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
        super(app.getMainWindow(), "About PuffinPlot", true);

        setMinimumSize(new Dimension(700, 400));
        setModal(true);
        getContentPane().setLayout(
                new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        final JLabel heading = new JLabel("PuffinPlot", SwingConstants.CENTER);
        heading.setFont(heading.getFont()
                .deriveFont(heading.getFont().getSize() + 8F));
        heading.setAlignmentX(0.5F);
        heading.setMaximumSize(new Dimension(5000, 25));
        heading.setPreferredSize(null);
        getContentPane().add(heading);

        final JTextArea textArea = new JTextArea(getMainText(app), 5, 20);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setPreferredSize(null);

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(textArea);
        getContentPane().add(scrollPane);

        final JLabel versionInfo = new JLabel(
                String.format("Version: %s. Date: %s.",
                        app.getVersion().getVersionString(),
                        app.getVersion().getDateString()),
                SwingConstants.CENTER);
        versionInfo.setAlignmentX(0.5F);
        versionInfo.setMaximumSize(new Dimension(500, 30));
        versionInfo.setMinimumSize(new Dimension(45, 30));
        versionInfo.setPreferredSize(new Dimension(500, 30));
        getContentPane().add(versionInfo);

        final JButton closeButton = new JButton("Close");
        closeButton.setAlignmentX(0.5F);
        closeButton.setMaximumSize(new Dimension(120, 29));
        closeButton.setPreferredSize(new Dimension(500, 29));
        closeButton.addActionListener(e -> setVisible(false));
        getContentPane().add(closeButton);

        pack();
    }


    private String getMainText(PuffinApp app) {
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
                + "for details.\n\n"
                + "System information: OS %s %s (%s), Java %s (%s)";
        return String.format(fmt, app.getVersion().getYearRange(),
                gsp("os.name"), gsp("os.version"),
                gsp("os.arch"), gsp("java.version"), gsp("java.vendor"));
    }

    private String gsp(String property) {
        return System.getProperty(property);
    }

}
