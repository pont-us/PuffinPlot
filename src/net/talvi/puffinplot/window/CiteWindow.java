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
import javax.swing.event.HyperlinkListener;

import net.talvi.puffinplot.PuffinApp;

/**
 * A window which displays information on how to cite PuffinPlot.
 * 
 * @author pont
 */
public class CiteWindow extends JFrame {

    private static final String risText = 
            "TY  - JOUR\n" +
            "T1  - PuffinPlot: A versatile, user-friendly program for paleomagnetic analysis\n" +
            "A1  - Lurcock, P. C.\n" +
            "A1  - Wilson, G. S.\n" +
            "Y1  - 2012/06/26\n" +
            "JO  - Geochem. Geophys. Geosyst.\n" +
            "VL  - 13\n" +
            "SP  - Q06Z45\n" +
            "SN  - 1525-2027\n" +
            "KW  - graphical data display\n" +
            "KW  - magnetostratigraphy\n" +
            "KW  - paleomagnetism\n" +
            "KW  - data processing\n" +
            "KW  - u channel\n" +
            "KW  - computer software\n" +
            "KW  - 0520 Computational Geophysics: Data analysis: algorithms and implementation\n" +
            "KW  - 0530 Computational Geophysics: Data presentation and visualization (1994)\n" +
            "KW  - 1594 Geomagnetism and Paleomagnetism: Instruments and techniques\n" +
            "N2  - PuffinPlot is a user-friendly desktop application for analysis of paleomagnetic data, offering a unique combination of features. It runs on several operating systems, including Windows, Mac OS X, and Linux; supports both discrete and long core data; and facilitates analysis of very weakly magnetic samples. As well as interactive graphical operation, PuffinPlot offers batch analysis for large volumes of data, and a Python scripting interface for programmatic control of its features. Available data displays include demagnetization/intensity, Zijderveld, equal-area (for sample, site, and suite level demagnetization data, and for magnetic susceptibility anisotropy data), a demagnetization data table, and a natural remanent magnetization intensity histogram. Analysis types include principal component analysis, Fisherian statistics, and great-circle path intersections. The results of calculations can be exported as CSV (comma-separated value) files; graphs can be printed, and can also be saved as publication-quality vector files in SVG or PDF format. PuffinPlot is free, and the program, user manual, and fully documented source code may be downloaded from http://code.google.com/p/puffinplot/.\n" +
            "UR  - http://dx.doi.org/10.1029/2012GC004098\n" +
            "DO  - 10.1029/2012GC004098\n" +
            "PB  - AGU\n";
    
    private static final String bibTexText =
            "@article{lurcock2012puffinplot,\n" +
            "  author = {Lurcock, P. C. and Wilson, G. S.},\n" +
            "  title = {PuffinPlot: A versatile, user-friendly program for paleomagnetic\n" +
            "    analysis},\n" +
            "  journal = {Geochemistry, Geophysics, Geosystems},\n" +
            "  year = {2012},\n" +
            "  month = {Jun},\n" +
            "  day = {26},\n" +
            "  publisher = {AGU},\n" +
            "  volume = {13},\n" +
            "  pages = {Q06Z45},\n" +
            "  keywords = {graphical data display; magnetostratigraphy; paleomagnetism;\n" +
            "    data processing; u channel; computer software; 0520 Computational\n" +
            "    Geophysics: Data analysis: algorithms and implementation; 0530\n" +
            "    Computational Geophysics: Data presentation and visualization (1994);\n" +
            "    1594 Geomagnetism and Paleomagnetism: Instruments and techniques},\n" +
            "  abstract = {PuffinPlot is a user-friendly desktop application for analysis\n" +
            "    of paleomagnetic data, offering a unique combination of features. It runs\n" +
            "    on several operating systems, including Windows, Mac OS X, and Linux;\n" +
            "    supports both discrete and long core data; and facilitates analysis of\n" +
            "    very weakly magnetic samples. As well as interactive graphical operation,\n" +
            "    PuffinPlot offers batch analysis for large volumes of data, and a Python\n" +
            "    scripting interface for programmatic control of its features. Available\n" +
            "    data displays include demagnetization/intensity, Zijderveld, equal-area\n" +
            "    (for sample, site, and suite level demagnetization data, and for magnetic\n" +
            "    susceptibility anisotropy data), a demagnetization data table, and a\n" +
            "    natural remanent magnetization intensity histogram. Analysis types\n" +
            "    include principal component analysis, Fisherian statistics, and\n" +
            "    great-circle path intersections. The results of calculations can be\n" +
            "    exported as CSV (comma-separated value) files; graphs can be printed, and\n" +
            "    can also be saved as publication-quality vector files in SVG or PDF\n" +
            "    format. PuffinPlot is free, and the program, user manual, and fully\n" +
            "    documented source code may be downloaded from\n" +
            "    http://code.google.com/p/puffinplot/.},\n" +
            "  issn = {1525-2027},\n" +
            "  doi = {10.1029/2012GC004098},\n" +
            "  url = {http://dx.doi.org/10.1029/2012GC004098}\n" +
            "}\n";
    
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
        heading.setFont(heading.getFont().deriveFont(heading.getFont().getSize() + 8f));
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(heading);
        final JLabel label1 = new JLabel(""
                + "<html><p>If you make use of PuffinPlot in published "
                + "work, please cite the PuffinPlot paper:</p></html>");
        label1.setBorder(new EmptyBorder(12, 0, 12, 0));
        label1.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(label1);
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
        citePane.setText("<html>Lurcock, P. C. and G. S. Wilson (2012), "
                + "PuffinPlot: A versatile, user-friendly program for "
                + "paleomagnetic analysis, <i>Geochemistry, Geophysics, "
                + "Geosystems</i>, 13, Q06Z45, doi:10.1029/2012GC004098."
                + "<br><br>\nURL: "
                + "<a href=\"http://doi.org/10.1029/2012GC004098\">"
                + "http://doi.org/10.1029/2012GC004098</a>"
                + "</html>");
        citePane.setPreferredSize(null);
        
        citePane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    app.openWebPage(e.getURL().toString());
                }
            }
        });

        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(citePane);
        scrollPane.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(scrollPane);

        final JButton onlinePaperButton = new JButton();
        onlinePaperButton.setText("Click here to locate the PuffinPlot paper online.");
        onlinePaperButton.setAlignmentX(CENTER_ALIGNMENT);
        onlinePaperButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                app.openWebPage("http://doi.org/10.1029/2012GC004098");
            }});
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
        buttonPanel.add(new CiteButton("RIS", risText));
        buttonPanel.add(new CiteButton("BibTeX", bibTexText));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        contentPane.add(buttonPanel);
        
        final JButton closeButton = new JButton();
        closeButton.setText("Close this window");
        closeButton.setAlignmentX(CENTER_ALIGNMENT);
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
        contentPane.add(closeButton);
        
        pack();
        // Centre over main window. Must be done after pack() -- see
        // http://stackoverflow.com/questions/3480102 .
        setLocationRelativeTo(app.getMainWindow());
    }
    
    private class CiteButton extends JButton {
        public CiteButton(String type, final String data) {
            super("Copy "+type+" data to clipboard");
            final Clipboard clipboard = 
                    Toolkit.getDefaultToolkit().getSystemClipboard();
            addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                final StringSelection sel = new StringSelection(data);
                clipboard.setContents(sel, sel);
            }
        });
        }
    }
}
