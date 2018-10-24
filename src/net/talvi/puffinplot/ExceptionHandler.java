/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
package net.talvi.puffinplot;

import java.awt.Component;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * An uncaught exception handler for a PuffinApp instance. This class
 * is designed to be instantiated and set as the default uncaught exception
 * handler. It handles an uncaught exception by writing a crash report file
 * (including log data and a build date from PuffinApp) and showing
 * a dialog box with options to quit or continue. If a further uncaught
 * exception occurs while the first one is being handled, {@code 
 * ExceptionHandler} will terminate the JVM at once to avoid the risk of an
 * infinite loop.
 */
class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    
    /**
     * This is a flag to prevent re-entrant calls to the
     * {@code uncaughtException} method. The method is synchronized,
     * which prevents it being called concurrently from more than one
     * thread, but it's possible that a method called from
     * {@code uncaughtException} itself (or something further down the
     * call chain) will throw an exception which will again be caught.
     * Even if the second exception occurs in another thread,
     * {@code uncaughtException} may be called again in its initial
     * thread, as there's no guarantee that it will be called in the
     * thread that caused the exception -- that's clear from the fact
     * that the source thread is supplied as a parameter.
     * <p>
     * This flag is set on entry to the method and cleared on exit. If it is
     * true on entry, {@code uncaughtException} terminates the JVM
     * immediately to avoid getting trapped in a recursive loop.
     * <p>
     * The {@code volatile} modifier isn't really be necessary since the
     * {@code synchronized} modifier on {@code uncaughtException} should
     * ensure that it's never at risk of concurrent access, but it doesn't
     * do any harm and adds an extra layer of safety.
     */
    private volatile boolean handlingException = false;
    private PuffinApp app;
    
    /**
     * Associate this exception handler with a specified PuffinPlot application
     * instance. If the exception handler method in this class is called,
     * the supplied {@code PuffinApp} is used to provide log messages
     * and a build date for the crash report file, and a parent window for
     * positioning the crash dialog.
     * 
     * @param app 
     */
    public void setApp(PuffinApp app) {
        this.app = app;
    }
    
    @Override
    public synchronized void uncaughtException(Thread thread,
            Throwable exception) {
        System.out.println("entering "+thread);
        if (handlingException) {
            /*
             * We don't even risk trying to print a stack trace here:
             * an infinite recursive loop of {@code uncaughtException}
             * calls is VERY undesirable, so we try to exit as quickly
             * and safely as possible.
             */
            System.exit(1);
        }
        handlingException = true;
        final File errorFile = new File(System.getProperty("user.home"),
                "PUFFIN-ERROR.txt");
        try (PrintWriter writer = new PrintWriter(errorFile)) {
            writer.println("PuffinPlot error file");
            writer.println("Build date: " + (app == null ?
                    "unknown" : app.getBuildProperty("build.date")));
            final Date now = new Date();
            final SimpleDateFormat df =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            writer.println("Crash date: " + df.format(now));
            for (String prop : new String[]{"java.version", "java.vendor",
                "os.name", "os.arch", "os.version", "user.name"}) {
                writer.println(String.format(Locale.ENGLISH,
                        "%-16s%s", prop,
                        System.getProperty(prop)));
            }
            writer.println("Locale: " + Locale.getDefault().toString());
            exception.printStackTrace(writer);
            if (app != null) {
                writer.println("\nLog messages: \n");
                writer.append(PuffinApp.flushLogAndGetStream().toString());
                writer.flush();
            } else {
                writer.println("\nNo log messages (app unset)\n");
            }
            if (unhandledErrorDialog()) {
                System.exit(1);
            }
        } catch (Throwable secondaryException) {
            /*
             * This should catch anything thrown directly while attempting
             * to handle the initial exception. It won't help if there's
             * an exception in another thread, but the handlingException
             * flag should take care of that.
             *
             * We explicitly pass System.err rather than using the
             * no-argument method to make it clear (to humans and static
             * analysis tools) that this is not a temporary hack -- at
             * this stage it's probably the best we can do.
             */
            secondaryException.printStackTrace(System.err);
            exception.printStackTrace(System.err);
            /*
             * If the exception handler itself is throwing exceptions, it's
             * best to exit ASAP rather than risk getting trapped in an
             * infinite loop.
             */
            System.exit(1);
        }
        handlingException = false;
        System.out.println("leaving "+thread);
    }
    
    private boolean unhandledErrorDialog() {
        final JLabel message = new JLabel(
                "<html><body style=\"width: 400pt; font-weight: normal;\">" +
                "<p><b>An unexpected error occurred.</b></p>" +
                "<p>We apologize for the inconvenience. Please report this " +
                "error to puffinplot@gmail.com. " +
                "PuffinPlot will try to write the details " +
                "to a file called PUFFIN-ERROR.txt in your home folder. "+
                "Please attach this file to your report. " +
                "If you have no unsaved data it is recommended that you " +
                "quit now and restart PuffinPlot. " +
                "If you have unsaved data, press Continue, then save your "+
                "data to a new file before quitting PuffinPlot." +
                "</p></body></html>");
        final Component parentComponent =
                app == null ? null : app.getMainWindow();
        final Object[] options = {"Continue", "Quit"};
        final int response = JOptionPane.showOptionDialog(parentComponent,
                message, "Unexpected error", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return (response==1);
    }
}
