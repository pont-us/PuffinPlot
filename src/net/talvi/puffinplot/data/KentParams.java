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
package net.talvi.puffinplot.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.toRadians;

/**
 * A class representing the parameters of a Kent confidence ellipse. It also
 * provides methods to calculate the parameters from sets of tensors using
 * programs from Lisa Tauxe's pmagpy software suite. For details of the pmagpy
 * programs and anisotropy statistics, see Lisa Tauxe, <i>Essentials of
 * Paleomagnetism</i> (University of California Press, 2010).
 *
 * @author pont
 */
public class KentParams {

    private final double tau;
    private final double tauSigma;
    private final Vec3 mean;
    private final double etaMag;
    private final Vec3 etaDir;
    private final double zetaMag;
    private final Vec3 zetaDir;

    /**
     * Creates an object representing a Kent confidence ellipse defined by the
     * supplied parameters.
     *
     * @param tau the tau value (eigenvalue of normalized mean susceptibility
     * tensor)
     * @param tauSigma (95% confidence region for tau)
     * @param dec the mean declination in degrees
     * @param inc the mean inclination in degrees
     * @param etaMag the semiangle of the confidence ellipse's first axis
     * @param etaDec the declination of the confidence ellipse's first axis
     * @param etaInc the inclination of the confidence ellipse's first axis
     * @param zetaMag the semiangle of the confidence ellipse's second axis
     * @param zetaDec the declination of the confidence ellipse's second axis
     * @param zetaInc the inclination of the confidence ellipse's second axis
     */
    public KentParams(double tau, double tauSigma, double dec, double inc,
            double etaMag, double etaDec, double etaInc,
            double zetaMag, double zetaDec, double zetaInc) {
        this.tau = tau;
        this.tauSigma = tauSigma;
        this.mean = Vec3.fromPolarDegrees(1., inc, dec);
        this.etaMag = toRadians(etaMag);
        this.etaDir = Vec3.fromPolarDegrees(1., etaInc, etaDec);
        this.zetaMag = toRadians(zetaMag);
        this.zetaDir = Vec3.fromPolarDegrees(1., zetaInc, zetaDec);
    }
    
    /**
     * Creates an object representing a Kent confidence ellipse defined
     * by the parameters listed in the supplied string. The parameters in the 
     * string should be separated by white space,
     * and occur in the following order:
     * <p>
     * tau tau_sigma mean_dec mean_inc eta_semiangle eta_dec eta_inc
     * zeta_semiangle zeta_dec zeta_inc
     * <p>
     * In the above, dec and inc refer to declination and inclination
     * respectively; all angles are given in degrees.
     * 
     * @param line a string representation of the Kent parameters
     */
    public KentParams(String line) {
        final Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);
        tau = scanner.nextDouble();
        tauSigma = scanner.nextDouble();
        double dec = scanner.nextDouble();
        double inc = scanner.nextDouble();
        mean = Vec3.fromPolarDegrees(1., inc, dec);
        etaMag = toRadians(scanner.nextDouble());
        dec = scanner.nextDouble();
        inc = scanner.nextDouble();
        etaDir = Vec3.fromPolarDegrees(1., inc, dec);
        zetaMag = toRadians(scanner.nextDouble());
        dec = scanner.nextDouble();
        inc = scanner.nextDouble();
        zetaDir = Vec3.fromPolarDegrees(1., inc, dec);
    }

    private static List<String> execute(String[] args) throws IOException {
        final Process process = Runtime.getRuntime().exec(args);
        final InputStream inputStream = process.getInputStream();
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream));
        final List<String> output = new ArrayList<>(8);
        try {
            process.waitFor();
        } catch (InterruptedException exception) {
            Logger.getLogger("net.talvi.puffinplot").log(Level.SEVERE,
                    "External process interrupted", exception);
            // Do nothing
        }
        do {
            final String line = reader.readLine();
            if (line == null) break;
            output.add(line);
        } while (true);
        reader.close();
        process.destroy();
        return output;
    }

    /**
     * Calculates 95% Kent confidence ellipses from the supplied tensors by
     * running the {@code bootams.py} script from Lisa Tauxe's pmagpy suite. The
     * ellipses are calculated by bootstrap statistics.
     *
     * @param tensors the tensors on which to calculate statistics
     * @param parametric {@code true} to use a parametric bootstrap;
     * {@code false} to use a ‘naïve’ bootstrap
     * @param scriptPath filesystem path to the {@code bootams.py} script
     * @return a list of three 95% Kent confidence ellipses for the supplied
     * data. The ellipses are for the maximum, intermediate, and minimum axes,
     * in that order
     * @throws IOException if an I/O error occurred
     */
    public static List<KentParams> calculateBootstrap(List<Tensor> tensors,
            boolean parametric, String scriptPath) throws IOException {
        File tempFile = null;
        FileWriter writer = null;
        final List<KentParams> result = new ArrayList<>(3);
        try {
            tempFile = File.createTempFile("puffin", "tensors");
            writer = new FileWriter(tempFile);
            for (Tensor t: tensors) {
                writer.write(t.toTensorComponentString() + "\n");
            }
            writer.close();
            ArrayList<String> args = new ArrayList<>(4);
            Collections.addAll(args, scriptPath, "-f",
                    tempFile.getAbsolutePath());
            if (parametric) args.add("-par");
            final List<String> output = execute(args.toArray(new String[] {}));
            tempFile.delete();

            for (int i=4; i<7; i++) {
                Scanner sc = new Scanner(output.get(i));
                sc.useLocale(Locale.ENGLISH);
                result.add(new KentParams(sc.nextDouble(), sc.nextDouble(),
                        sc.nextDouble(), sc.nextDouble(),
                        sc.nextDouble(), sc.nextDouble(), sc.nextDouble(),
                        sc.nextDouble(), sc.nextDouble(), sc.nextDouble()));
            }
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            if (writer != null) writer.close();
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Calculates 95% Kent confidence ellipses from the supplied tensors by
     * running the {@code s_hext.py} script from Lisa Tauxe's pmagpy suite. The
     * ellipses are calculated by Hext statistics. Note that no value is
     * calculated for tau_sigma, which is set to zero.
     *
     * @param tensors the tensors on which to calculate statistics
     * @param scriptPath filesystem path to the {@code s_hext.py} script
     * @return a list of three 95% Kent confidence ellipses for the supplied
     * data. The ellipses are for the maximum, intermediate, and minimum axes,
     * in that order
     * @throws IOException if an I/O error occurred
     */
   public static List<KentParams> calculateHext(List<Tensor> tensors,
           String scriptPath) throws IOException {
       final List<KentParams> result = new ArrayList<>(3);
       File tempFile = null;
       FileWriter writer = null;
       try {
           tempFile = File.createTempFile("puffin", "hext");
           writer = new FileWriter(tempFile);
           for (Tensor t: tensors) {
               writer.write(t.toTensorComponentString() + "\n");
           }
           writer.close();
           final String[] args = {scriptPath, "-f", tempFile.getAbsolutePath()};
           List<String> output = execute(args);
           tempFile.delete();
           for (int i=2; i<5; i++) {
               final Scanner sc = new Scanner(output.get(i));
               sc.useLocale(Locale.ENGLISH);
               result.add(new KentParams(sc.nextDouble(), 0.,
                       sc.nextDouble(), sc.nextDouble(),
                       sc.nextDouble(), sc.nextDouble(), sc.nextDouble(),
                       sc.nextDouble(), sc.nextDouble(), sc.nextDouble()));
           }
       } finally {
           if (tempFile != null && tempFile.exists()) tempFile.delete();
           if (writer != null) writer.close();
       }
       return Collections.unmodifiableList(result);
    }

    /**
     * Returns the tau value (eigenvalue of normalized mean susceptibility
     * tensor).
     *
     * @return the tau value (eigenvalue of normalized mean susceptibility
     * tensor).
     */
    public double getTau() {
        return tau;
    }

    /**
     * Returns (95% confidence region for tau)
     *
     * @return (95% confidence region for tau)
     */
    public double getTauSigma() {
        return tauSigma;
    }

    /**
     * Returns the mean direction
     *
     * @return the mean direction
     */
    public Vec3 getMean() {
        return mean;
    }

    /**
     * Returns the semiangle of the confidence ellipse's first axis
     *
     * @return the semiangle of the confidence ellipse's first axis
     */
    public double getEtaMag() {
        return etaMag;
    }

    /**
     * Returns the direction of the confidence ellipse's first axis
     *
     * @return the direction of the confidence ellipse's first axis
     */
    public Vec3 getEtaDir() {
        return etaDir;
    }

    /**
     * Returns the semiangle of the confidence ellipse's second axis
     *
     * @return the semiangle of the confidence ellipse's second axis
     */
    public double getZetaMag() {
        return zetaMag;
    }

    /**
     * Returns the direction of the confidence ellipse's second axis
     *
     * @return the direction of the confidence ellipse's second axis
     */
    public Vec3 getZetaDir() {
        return zetaDir;
    }
}
