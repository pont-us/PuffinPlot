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
package net.talvi.puffinplot;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.Suite;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This class contains the initial entry point for starting PuffinPlot.
 * It parses any command-line arguments, then creates the PuffinPlot
 * GUI or runs in scripting mode as appropriate.
 * 
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger("net.talvi.puffinplot");
    
    /**
     * Instantiates and starts a new PuffinApp.
     * @param args command-line arguments for the application
     */
    public static void main(final String[] args) {
        LOGGER.setLevel(Level.ALL);
        LOGGER.fine("Entering main method.");
        
        final Preferences prefs =
                Preferences.userNodeForPackage(PuffinPrefs.class);
        try {
            final String lookAndFeel = prefs.get("lookandfeel", "Default");
            if (null != lookAndFeel) switch (lookAndFeel) {
                case "Native":
                    UIManager.setLookAndFeel(
                            UIManager.getSystemLookAndFeelClassName());
                    break;
                case "Metal":
                    UIManager.setLookAndFeel(
                            UIManager.getCrossPlatformLookAndFeelClassName());
                    break;
                case "Nimbus":
                    /*
                     * Nimbus isn't guaranteed to be available on all systems,
                     * so we make sure it's there before trying to set it. If
                     * it's not there, nothing will happen so the system default
                     * will be used.
                     */
                    for (UIManager.LookAndFeelInfo info:
                            UIManager.getInstalledLookAndFeels()) {
                        if ("Nimbus".equals(info.getName())) {
                            UIManager.setLookAndFeel(info.getClassName());
                            break;
                        }
                    }
                    break;
            }
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException ex) {
            LOGGER.log(Level.WARNING, "Error setting look-and-feel", ex);
        }

        parseCliArguments(args);
    }
    
    @SuppressWarnings("static-access")
    private static Options createOptions() {
        final Option helpOpt = new Option("help", "print this message");
        final Option scriptOpt = OptionBuilder.withArgName("file")
                .hasArg().withDescription("run specified script")
                .create("script");
        final Option scriptLangOpt = OptionBuilder.withArgName("language")
                .hasArg()
                .withDescription("language for script (javascript or python)")
                .create("scriptlanguage");
        final Option installJythonOpt = OptionBuilder
                .withDescription("download and install Jython")
                .create("installjython");
        final Option processOpt = OptionBuilder.withArgName("file")
                .hasArg()
                .withDescription("process given ppl file and save results")
                .create("process");
        final Option withAppOpt = new Option("withapp",
                "create a Puffin application (script mode only)");
        final Options options = new Options();
        options.addOption(helpOpt);
        options.addOption(scriptOpt);
        options.addOption(scriptLangOpt);
        options.addOption(installJythonOpt);
        options.addOption(withAppOpt);
        options.addOption(processOpt);
        return options;
    }
    
    private static void parseCliArguments(String[] args) {
        final Options options = createOptions();
        final CommandLineParser parser = new GnuParser();
        try {
            final CommandLine commandLine = parser.parse(options, args);
            processCliArguments(commandLine, options);
        } catch (ParseException ex) {
            System.err.println("Could not parse arguments.\n" +
                    ex.getMessage());
            System.exit(1);
        }
    }
    
    private static void processCliArguments(CommandLine commandLine,
            Options options) {
        if (commandLine.hasOption("help")) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar PuffinPlot.jar <options>", options);
        }
        
        else if (commandLine.hasOption("script")) {
            final String scriptPath = commandLine.getOptionValue("script");
            final String scriptLanguage =
                    commandLine.hasOption("scriptlanguage") ?
                    commandLine.getOptionValue("scriptlanguage") : "python";
            try {
                ScriptEngine engineTemp = null;
                switch (scriptLanguage) {
                    case "javascript":
                        final ScriptEngineManager sem =
                                new ScriptEngineManager();
                        engineTemp = sem.getEngineByMimeType(
                                "application/javascript");
                        break;
                    case "python":
                        if (!JythonJarManager.checkInstalled(true)) {
                            System.err.println("Cannot run python script: "
                                    + "Jython is not installed.\n"
                                    + "Please install using the "
                                    + "-installjython option.");
                            System.exit(1);
                        }
                        engineTemp = PuffinApp.createPythonScriptEngine();
                        break;
                    default:
                        System.out.println("Unknown scripting language "+
                                commandLine.getOptionValue("scriptlanguage"));
                        System.exit(1);
                }
                
                final ScriptEngine engine = engineTemp;
                if (commandLine.hasOption("withapp")) {
                    java.awt.EventQueue.invokeLater(() -> {
                        final PuffinApp scriptApp = PuffinApp.create();
                        engine.put("puffin_app", scriptApp);
                        try (Reader reader = new FileReader(scriptPath)) {
                            engine.eval(reader);
                        } catch (ScriptException | IOException ex) {
                            LOGGER.log(Level.SEVERE, null, ex);
                            // TODO make sure this makes it to stderr
                        }
                    });
                } else {
                    final Reader reader = new FileReader(scriptPath);
                    engine.eval(reader);
                }
            } catch (IOException | ScriptException | RuntimeException ex) {
                // PyException is a RuntimeException, so doesn't *have*
                // to be caught, but it makes sense to do so.
                System.err.println("Error running Python script "+scriptPath);
                ex.printStackTrace(System.err);
                System.exit(1);
            }
        }
        
        else if (commandLine.hasOption("process")) {
            final String inputFileString =
                    commandLine.getOptionValue("process");
            final Suite suite = new Suite("PuffinPlot (process mode)");
            try {
                suite.readFiles(Arrays.asList(new File(inputFileString)));
                suite.doAllCalculations(Correction.NONE,
                        PuffinApp.getGreatCirclesValidityCondition());
                suite.calculateSuiteMeans(suite.getSamples(), suite.getSites());
                
                final String bareFilename =
                        inputFileString.replaceFirst("[.]...$", "");
                
                suite.saveCalcsSample(new File(bareFilename + "-sample.csv"));
                suite.saveCalcsSite(new File(bareFilename + "-site.csv"));
                suite.saveCalcsSuite(new File(bareFilename + "-suite.csv"));
                
            } catch (IOException | PuffinUserException ex) {
                System.err.println("An error occurred during processing.");
                Logger.getLogger(PuffinApp.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
        
        else if (commandLine.hasOption("installjython")) {
            System.out.println("Downloading and installing Jython...");
            try {
                JythonJarManager.download();
                System.out.println("Installed.");
            } catch (IOException ex) {
                System.err.println("Error installing Jython:");
                ex.printStackTrace(System.err);
                System.exit(1);
            }
        }
        
        else {
            // Start PuffinPlot in interactive Swing GUI mode.
            java.awt.EventQueue.invokeLater(() -> {
                final ExceptionHandler handler = new ExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(handler);
                final PuffinApp app = PuffinApp.create();
                handler.setApp(app);
                app.show();
            });
        }
    }
}
