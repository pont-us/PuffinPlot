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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;

public class MainTest {
    
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();
    
    @Test
    public void testScript() throws FileNotFoundException, IOException {
        final String scriptTemplate =
                "var PrintStreamClass = Java.type(\"java.io.PrintStream\");\n" +
                "var stream = new PrintStreamClass(\"%s\");\n" +
                "stream.print(\"%s\\n\");\n" +
                "stream.flush();\n" +
                "stream.close();\n";
        final String fileContents = "Hello world.";
        final Path outputPath =
                tempDir.getRoot().toPath().resolve("output.txt");
        final String script = String.format(
                scriptTemplate, outputPath.toString(), fileContents);
        final Path scriptPath =
                tempDir.getRoot().toPath().resolve("script.js");
        try (PrintStream stream = new PrintStream(scriptPath.toFile())) {
            stream.print(script);
            Main.main(new String[] {
                "-scriptlanguage", "javascript",
                "-script", scriptPath.toString()
            });
        
        }
        assertEquals(Arrays.asList(fileContents),
                Files.readAllLines(outputPath));
    }
    
    @Test
    public void testHelp() {
        final ByteArrayOutputStream newStdout = new ByteArrayOutputStream();
        System.setOut(new PrintStream(newStdout));
        Main.main(new String[] {"-help"});
        final String expectedHelpMessage =
                "usage: java -jar PuffinPlot.jar <options>\n" +
                " -help                        print this message\n" +
                " -installjython               download and install Jython\n" +
                " -process <file>              " +
                "process given ppl file and save results\n" +
                " -script <file>               run specified script\n" +
                " -scriptlanguage <language>   " +
                "language for script (javascript or python)\n" +
                " -withapp                     " +
                "create a Puffin application (script mode\n" +
                "                              only)\n";
        assertEquals(expectedHelpMessage, newStdout.toString());
    }
    
}
