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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;

import net.talvi.puffinplot.PuffinApp;

/**
 * A window which displays information on how to cite PuffinPlot.
 * 
 * @author pont
 */
public class CiteWindow extends JFrame {

    private static final String RIS_TEXT = 
            "TY  - JOUR\n"
            + "T1  - New Developments in the PuffinPlot Paleomagnetic Data Analysis Program\n"
            + "AU  - Lurcock, P. C.\n"
            + "AU  - Florindo, F.\n"
            + "PY  - 2019\n"
            + "DO  - 10.1029/2019GC008537\n"
            + "JO  - Geochemistry, Geophysics, Geosystems\n"
            + "SP  - 5578\n"
            + "EP  - 5587\n"
            + "VL  - 20\n"
            + "IS  - 11\n"
            + "SN  - 1525-2027\n"
            + "UR  - https://doi.org/10.1029/2019GC008537\n";
    
    private static final String BIBTEX_TEXT =
        "@article{lurcock2019new,\n"
        + "  author = {Lurcock, Pontus Conrad and Florindo, Fabio},\n"
        + "  title = {New developments in the {PuffinPlot} paleomagnetic data\n"
        + "    analysis program},\n"
        + "  year = {2019},\n"
        + "  volume = {20},\n"
        + "  number = {11},\n"
        + "  pages = {5578--5587},\n"
        + "  month = oct,\n"
        + "  day = {30},\n"
        + "  journal = {Geochemistry, Geophysics, Geosystems},\n"
        + "  doi = {10.1029/2019GC008537},\n"
        + "  url = {https://doi.org/10.1029/2019GC008537},\n"
        + "  keywords = {computer software; data processing; graphical data display;\n"
        + "    magnetostratigraphy; paleomagnetism; reproducible research},\n"
        + "  abstract = {PuffinPlot is a program for paleomagnetic data analysis and\n"
        + "    plotting, first released in 2012 and under continuous development\n"
        + "    since then. It is free, cross-platform software and provides both a\n"
        + "    graphical desktop interface for interactive use and an\n"
        + "    application-programmer interface for scripting. We present a major new\n"
        + "    release of the program, describe significant new features added since\n"
        + "    the first release, and demonstrate their application to real-world\n"
        + "    data. New features include automatic magnetic declination realignment,\n"
        + "    relative paleointensity calculation, virtual geomagnetic pole\n"
        + "    determination, calculation of inclination-only statistics, support for\n"
        + "    reproducible research via the export of self-contained data bundles,\n"
        + "    and support for reading a number of popular paleomagnetic file\n"
        + "    formats. We also discuss the application of unit tests in ensuring\n"
        + "    PuffinPlot's long-term reliability and outline directions for future\n"
        + "    development of the software.}\n"
        + "}\n";
    
    /**
     * Creates a new CiteWindow.
     * 
     * @param app the PuffinPlot instance associated with this window
     */
    public CiteWindow(final PuffinApp app) {
        setMinimumSize(new Dimension(400, 200));
        setPreferredSize(new java.awt.Dimension(600, 400));
        
        final JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        
        final JLabel heading = new JLabel("How to cite PuffinPlot");
        heading.setFont(heading.getFont().
                deriveFont(heading.getFont().getSize() + 8f));
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(heading);
        
        final JLabel introLabel = new JLabel("<html><p>If you make use of "
                + "PuffinPlot in published work, please cite this "
                + "paper:</p></html>");
        introLabel.setBorder(new EmptyBorder(12, 0, 12, 0));
        introLabel.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(introLabel);
        
        /*
         * From http://www.coderanch.com/t/338648/GUI/java/Multiple-lines-JLabel
         * ‘One bit of caution if you use word wrap: I've noticed that
         * JTextArea's with word wrap on are very greedy about space if you
         * don't put them inside a scroll pane. If you allow them to grow by
         * enlarging the window, they refuse to shrink back to their original
         * size if you make the window smaller again! The easy workaround is to
         * put them in a scroll pane and disable scrolling.’
         *
         */
        final JTextPane citePane = new JTextPane();
        citePane.setEditable(false);
        citePane.setContentType("text/html");
        citePane.setText("<html>Lurcock, P. C., & Florindo, F. (2019). "
                + "New developments in the PuffinPlot "
                + "paleomagnetic data analysis program. "
                + "<i>Geochemistry, Geophysics, Geosystems</i>, 20(11), 5578–5587. "
                + "doi:10.1029/2019GC008537, "
                + "<a href=\"https://doi.org/10.1029/2019GC008537\">"
                + "https://doi.org/10.1029/2019GC008537</a></html>"
        );
        citePane.setPreferredSize(null);
        citePane.addHyperlinkListener(event -> {
            if (HyperlinkEvent.EventType.ACTIVATED == event.getEventType()) {
                app.openWebPage(event.getURL().toString());
            }
        });

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(citePane);
        scrollPane.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(scrollPane);

        final JButton onlinePaperButton = new JButton();
        onlinePaperButton.setText(
                "Click here to locate the PuffinPlot paper online.");
        onlinePaperButton.setAlignmentX(CENTER_ALIGNMENT);
        onlinePaperButton.addActionListener(event ->
            app.openWebPage("https://doi.org/10.1029/2019GC008537")
        );
        contentPane.add(onlinePaperButton);
        contentPane.add(Box.createVerticalGlue());
        
        final JLabel label2 = new JLabel();
        label2.setAlignmentX(CENTER_ALIGNMENT);
        label2.setText("<html><p>"
                + "If you are using reference management software "
                + "(e.g. Zotero, Mendeley, BibTeX, or Papers)"
                + ", you can "
                + "use the buttons below to copy a citation record to "
                + "your computer's clipboard.</p></html>");
        contentPane.add(label2);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(new CiteButton("RIS", RIS_TEXT));
        buttonPanel.add(new CiteButton("BibTeX", BIBTEX_TEXT));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(buttonPanel);
        
        final JButton closeButton = new JButton();
        closeButton.setText("Close this window");
        closeButton.setAlignmentX(CENTER_ALIGNMENT);
        closeButton.addActionListener(event -> setVisible(false));
        contentPane.add(closeButton);
        
        pack();
        
        /*
         * Centre over main window. Must be done after pack() -- see
         * http://stackoverflow.com/questions/3480102 .
         */
        setLocationRelativeTo(app.getMainWindow());
    }
    
    private class CiteButton extends JButton {
        public CiteButton(String type, final String data) {
            super("Copy "+type+" data to clipboard");
            final Clipboard clipboard = 
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            addActionListener(event -> {
                final StringSelection sel = new StringSelection(data);
                clipboard.setContents(sel, sel);
            });
        }
    }
}
