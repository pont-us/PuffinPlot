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
package net.talvi.puffinplot.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import Jama.Matrix;
import net.talvi.puffinplot.TestUtils;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.lang.Math.toRadians;
import static org.junit.Assert.assertEquals;

public class KentParamsTest {

    private double delta = 1e-10;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testCalculateNonParametricBootstrap() throws Exception {
        Assume.assumeTrue("Linux".equals(System.getProperty("os.name")));
        final File script = makeScript();
        testCalculate(KentParams.calculateBootstrap(makeTensors(), false,
                script.getCanonicalPath()), script, new String[] {"-f", null});
    }

    @Test
    public void testCalculateParametricBootstrap() throws Exception {
        Assume.assumeTrue("Linux".equals(System.getProperty("os.name")));
        final File script = makeScript();
        testCalculate(KentParams.calculateBootstrap(makeTensors(), true,
                script.getCanonicalPath()), script,
                new String[] {"-f", null, "-par"});
    }

    @Test
    public void testCalculateHext() throws IOException {
        Assume.assumeTrue("Linux".equals(System.getProperty("os.name")));
        final File script = makeScript();
        testCalculate(KentParams.calculateHext(makeTensors(),
                script.getCanonicalPath()), script, new String[] {"-f", null});
    }
    
    private List<Tensor> makeTensors() {
        final Matrix identity = Matrix.identity(3, 3);
        return Arrays.asList(
                new Tensor(1, 2, 3, 4, 5, 6, identity, identity),
                new Tensor(6, 5, 4, 3, 2, 1, identity, identity));
    }
    
    private void testCalculate(List<KentParams> kps, File script,
            String[] expectedArguments)
            throws IOException {
        // Check that the correct arguments were provided to the script.
        
        final List<String> argLines = Files.readAllLines(script.toPath().
                getParent().resolve("arguments"));
        assertEquals(1, argLines.size());
        final String[] args = argLines.get(0).split(" ");
        assertEquals(expectedArguments.length, args.length);
        for (int i=0; i<expectedArguments.length; i++) {
            if (expectedArguments[i] != null) {
                assertEquals(expectedArguments[i], args[i]);
            }
        }
        // We can't directly check the temporary input file because it
        // may already have been deleted, but the script has made a copy.
        
        // Check that a correct input file was provided to the script.
        
        final List<String> inputLines = Files.readAllLines(script.toPath().
                getParent().resolve("inputfile"));
        assertEquals(Arrays.asList(
                "1.00000 2.00000 3.00000 4.00000 5.00000 6.00000",
                "6.00000 5.00000 4.00000 3.00000 2.00000 1.00000"),
                inputLines);
        
        // Check that the script output was read correctly.
        
        assertEquals(1, (int) kps.get(0).getTau());
    }
    
    private File makeScript() throws IOException {
        File script = TestUtils.writeStringToTemporaryFile("script.sh",
                "#!/bin/sh\n\n" +
                        // get shell script parent directory
                        "dir=`dirname \"$0\"`\n" +
                        // write script arguments to a file
                        "echo $@ > $dir/arguments\n" +
                        // assume second argument is a file and make a copy
                        "cp $2 $dir/inputfile\n" +
                        // print some canned output in the expected format
                        "for i in 1 2 3 4 5 6 7 8 9\n" +
                        "do echo 1 2 3 4 5 6 7 8 9 10\n" +
                        "done\n",
                temporaryFolder);
        script.setExecutable(true);
        return script;
    }

    @Test
    public void testConstructorsAndGetters() {
        final double tau = 1;
        final double tauSigma = 2;
        final double dec = 3;
        final double inc = 4;
        final double etaMag = 5;
        final double etaDec = 6;
        final double etaInc = 7;
        final double zetaMag = 8;
        final double zetaDec = 9;
        final double zetaInc = 10;
        final KentParams kpFromDoubles = new KentParams(tau, tauSigma, dec, inc,
                etaMag, etaDec, etaInc, zetaMag, zetaDec, zetaInc);
        final KentParams kpFromString =
                new KentParams(String.format(Locale.ENGLISH,
                        "%f %f %f %f %f %f %f %f %f %f",
                        tau, tauSigma, dec, inc, etaMag, etaDec, etaInc,
                        zetaMag, zetaDec, zetaInc));
        for (KentParams kp: new KentParams[] {kpFromDoubles, kpFromString}) {
            assertEquals(tau, kp.getTau(), delta);
            assertEquals(tauSigma, kp.getTauSigma(), delta);
            assertEquals(dec, kp.getMean().getDecDeg(), delta);
            assertEquals(inc, kp.getMean().getIncDeg(), delta);
            assertEquals(toRadians(etaMag), kp.getEtaMag(), delta);
            assertEquals(etaDec, kp.getEtaDir().getDecDeg(), delta);
            assertEquals(etaInc, kp.getEtaDir().getIncDeg(), delta);
            assertEquals(toRadians(zetaMag), kp.getZetaMag(), delta);
            assertEquals(zetaDec, kp.getZetaDir().getDecDeg(), delta);
            assertEquals(zetaInc, kp.getZetaDir().getIncDeg(), delta);
        }
    }    
}
