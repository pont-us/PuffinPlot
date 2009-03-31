package net.talvi.puffinplot;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import javax.print.PrintService;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import net.talvi.puffinplot.data.Sample;

public class PuffinActions {

    private PuffinApp app;

    PuffinActions(PuffinApp app) {
        this.app = app;
    }
    
    public final Action about = new AbstractAction("About PuffinPlot") {

        public void actionPerformed(ActionEvent e) {
            app.getAboutBox().setLocationRelativeTo(app.getMainWindow());
            app.getAboutBox().setVisible(true);
        }
    };
    
    public final Action open = new AbstractAction("Open…") {

        public void actionPerformed(ActionEvent e) {

            boolean useSwingChooser = true; // !PuffinApp.MAC_OS_X;

            File[] files = null;

            if (useSwingChooser) {
                JFileChooser chooser = new JFileChooser();
                chooser.setMultiSelectionEnabled(true);
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                int choice = chooser.showOpenDialog(app.getMainWindow());
                if (choice == JFileChooser.APPROVE_OPTION)
                    files = chooser.getSelectedFiles();
            } else {
                FileDialog fd = new FileDialog(app.getMainWindow(), "Open file(s)",
                        FileDialog.LOAD);
                fd.setVisible(true);
                String filename = fd.getFile();
                if (filename != null) {
                    File file = new File(fd.getDirectory(), fd.getFile());
                    files = new File[]{file};
                }
            }
            if (files != null)
                app.openFiles(files);
        }
    };
    
    public final Action saveCalcs = new AbstractAction("Save PCA/Fisher…") {

        public void actionPerformed(ActionEvent arg0) {
            boolean useSwingChooser = !PuffinApp.MAC_OS_X;

            File file = null;

            if (useSwingChooser) {
                JFileChooser chooser = new JFileChooser();
                int choice = chooser.showSaveDialog(app.getMainWindow());
                if (choice == JFileChooser.APPROVE_OPTION)
                    file = chooser.getSelectedFile();
            } else {
                FileDialog fd = new FileDialog(app.getMainWindow(),
                        "Save PCA/Fisher", FileDialog.SAVE);
                fd.setVisible(true);
                String filename = fd.getFile();
                if (filename != null) {
                    file = new File(fd.getDirectory(), fd.getFile());
                }
            }
            if (file != null) app.getSuite().saveCalculations(file);
        }
        
    };
    
    public final Action save = new AbstractAction("Save…") {

        public void actionPerformed(ActionEvent arg0) {
            boolean useSwingChooser = !PuffinApp.MAC_OS_X;

            File file = null;

            if (useSwingChooser) {
                JFileChooser chooser = new JFileChooser();
                int choice = chooser.showSaveDialog(app.getMainWindow());
                if (choice == JFileChooser.APPROVE_OPTION)
                    file = chooser.getSelectedFile();
            } else {
                FileDialog fd = new FileDialog(app.getMainWindow(), "Save PCA/Fisher",
                        FileDialog.SAVE);
                fd.setVisible(true);
                String filename = fd.getFile();
                if (filename != null) {
                    file = new File(fd.getDirectory(), fd.getFile());
                }
            }
            if (file != null) app.getSuite().save(file);
        }
        
    };
    
    public final Action pageSetup = new AbstractAction("Page Setup…") {
        public void actionPerformed(ActionEvent arg0) {
            app.showPageSetupDialog();
        }
    };
    
    public final Action flipSample = new AbstractAction("Flip sample(s)") {
        public void actionPerformed(ActionEvent arg0) {
            for (Sample s: app.getSelectedSamples()) s.flip();
        }
    };
    
    public final Action pcaOnSelection = new AbstractAction("PCA") {
        public void actionPerformed(ActionEvent e) {
            for (Sample sample: app.getSelectedSamples())
                if (sample.getSelectedPoints().size()>1)
                    sample.doPca(false);
            app.updateDisplay();
        }
    };
    
    public final Action anchoredPcaOnSelection = new AbstractAction("PCA (anchored)") {
        public void actionPerformed(ActionEvent e) {
            for (Sample sample: app.getSelectedSamples())
                if (sample.getSelectedPoints().size()>1)
                    sample.doPca(true);
            app.updateDisplay();
        }
    };
    
    public final Action fisher = new AbstractAction("Fisher on sample") {
        public void actionPerformed(ActionEvent e) {
            app.getSample().doFisher();
            app.getMainWindow().repaint();
        }
    };
    
    public final Action fisherBySite = new AbstractAction("Fisher by site") {
        public void actionPerformed(ActionEvent e) {
            app.getFisherWindow().getPlot().setGroupedBySite(true);
            app.getSuite().doFisherOnPcasBySite();
            app.getFisherWindow().setVisible(true);
        }
    };
        
    public final Action fisherBySample = new AbstractAction("Fisher on all") {
        public void actionPerformed(ActionEvent e) {
            app.getSuite().doFisherOnAllPcas();
            app.getFisherWindow().getPlot().setGroupedBySite(false);
            app.getFisherWindow().setVisible(true);
        }
    };
    
    public final Action editCorrections = new AbstractAction("Corrections…") {
        public void actionPerformed(ActionEvent e) {
            app.getCorrectionWindow().setVisible(true);
        }
    };
    
    public final Action clear = new AbstractAction("Clear") {
        public void actionPerformed(ActionEvent e) {
            app.getSample().clear();
            app.getMainWindow().repaint();
        }
    };
    
    public final Action selectAll = new AbstractAction("Select all") {
        public void actionPerformed(ActionEvent e) {
            app.getSample().selectAll();
            app.getMainWindow().repaint();
        }
    };
    
    public final Action prefs = new AbstractAction("Preferences…") {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(app.getMainWindow(), "This is the preferences dialog.");
        }
    };
    
    public final Action print = new AbstractAction("Print…") {

        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(app.getMainWindow().getGraphDisplay(),
                             app.getCurrentPageFormat());

            PrintService[] services = PrinterJob.lookupPrintServices();

            if (services.length > 0) {
                try {
                    job.setPrintService(services[0]);
                    /* Note: if we pass an attribute set to printDialog(),
                     * it forces the use of a cross-platform Swing print
                     * dialog rather than the default native one.
                    */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };
    
    public final Action printFisher = new AbstractAction("Print Fisher…") {

        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((Printable) app.getFisherWindow().getContentPane(),
                    app.getCurrentPageFormat());

            PrintService[] services = PrinterJob.lookupPrintServices();

            if (services.length > 0) {
                try {
                    job.setPrintService(services[0]);
                    /* Note: if we pass an attribute set to printDialog(),
                     * it forces the use of a cross-platform Swing print
                     * dialog rather than the default native one.
                    */
                    if (job.printDialog()) job.print();
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }
        }
    };
    
    public final Action quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            PuffinApp.getInstance().getPrefs().save();
            System.exit(0);
        }
    };
}
