package net.talvi.puffinplot;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import javax.print.PrintService;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class PuffinActions {

    private PuffinApp app;

    PuffinActions(PuffinApp app) {
        this.app = app;
    }
    
    public final Action about = new AbstractAction("About PuffinPlot") {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(app.mainWindow, "This is the about box.");
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
                int choice = chooser.showOpenDialog(app.mainWindow);
                if (choice == JFileChooser.APPROVE_OPTION)
                    files = chooser.getSelectedFiles();
            } else {
                FileDialog fd = new FileDialog(app.mainWindow, "Open file",
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
    
    public final Action pageSetup = new AbstractAction("Page Setup…") {

        public void actionPerformed(ActionEvent arg0) {
            app.showPageSetupDialog();
        }
    };
    
    public final Action pca = new AbstractAction("PCA") {

        public void actionPerformed(ActionEvent e) {
            app.getCurrentSample().doPca();
            app.mainWindow.repaint();
        }
    };
    
    public final Action prefs = new AbstractAction("Preferences…") {

        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(app.mainWindow, "This is the preferences dialog.");
        }
    };
    
    public final Action print = new AbstractAction("Print…") {

        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pf = new PageFormat();
            pf.setOrientation(PageFormat.LANDSCAPE);
            job.setPrintable(app.mainWindow.graphDisplay,
                    app.getCurrentPageFormat());
            // PageFormat pf = job.pageDialog(job.defaultPage());

            // NB must explicitly request orientation to work around
            // Java bug -- see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6633656 ,
            // https://bugs.launchpad.net/ubuntu/+source/cupsys/+bug/156191

            // Oops, even this doesn't help. Ah well, it works as long
            // as a default orientation is set in the CUPS setup.
            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            aset.add(OrientationRequested.LANDSCAPE);
            aset.add(new Copies(1));
            aset.add(new JobName("PuffinPlot", null));

            PrintService[] services =
                    PrinterJob.lookupPrintServices();

            if (services.length > 0) {
                System.out.println("selected printer " + services[0].getName());
                try {
                    job.setPrintService(services[0]);
                    // job.pageDialog(aset);
                    if (job.printDialog(aset)) {
                        job.print(aset);
                    }
                } catch (PrinterException pe) {
                    System.err.println(pe);
                }
            }

        }
    };
    
    public final Action quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            System.out.println("quit");
            System.exit(0);
        }
    };
}