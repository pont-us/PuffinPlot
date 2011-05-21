package net.talvi.puffinplot.data;

import java.util.Collections;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;
import static java.lang.Math.toRadians;

/**
 * A class representing the parameters of a Kent confidence ellipse.
 *
 * @author pont
 */
public class KentParams {

    private double tau;
    private double tauSigma;
    private Vec3 mean;
    private double etaMag;
    private Vec3 etaDir;
    private double zetaMag;
    private Vec3 zetaDir;

    public KentParams(String line) {
        Scanner sc = new Scanner(line);
        tau = sc.nextDouble();
        tauSigma = sc.nextDouble();
        double dec = sc.nextDouble();
        double inc = sc.nextDouble();
        mean = Vec3.fromPolarDegrees(1., inc, dec);
        etaMag = toRadians(sc.nextDouble());
        dec = sc.nextDouble();
        inc = sc.nextDouble();
        etaDir = Vec3.fromPolarDegrees(1., inc, dec);
        zetaMag = toRadians(sc.nextDouble());
        dec = sc.nextDouble();
        inc = sc.nextDouble();
        zetaDir = Vec3.fromPolarDegrees(1., inc, dec);
    }

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

    private static List<String> execute(String[] args) throws IOException {
        Process process = Runtime.getRuntime().exec(args);
        InputStream inputStream = process.getInputStream();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream));
        List<String> output = new ArrayList<String>(8);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            System.out.println("interrupted");
            // do nothing
        }
        do {
            String line = reader.readLine();
            if (line == null) break;
            output.add(line);
        } while (true);
        reader.close();
        process.destroy();
        return output;
    }

    public static List<KentParams> calculateBootstrap(List<Tensor> tensors,
            boolean parametric, String bootamsPath) {
        // final String cmdPath = "";
        try {
            File tempFile = File.createTempFile("puffin", "tensors");
            FileWriter writer = new FileWriter(tempFile);
            for (Tensor t: tensors) {
                writer.write(t.toTensorComponentString() + "\n");
            }
            writer.close();
            ArrayList<String> args = new ArrayList<String>(4);
            Collections.addAll(args, bootamsPath, "-f", tempFile.getAbsolutePath());
            if (parametric) args.add("-par");
            List<String> output = execute(args.toArray(new String[] {}));
            tempFile.delete();
            System.out.println("N ="+tensors.size());
            for (String s : output) {
                System.out.println(s);
            }
            List<KentParams> result = new ArrayList<KentParams>(3);
            for (int i=4; i<7; i++) {
                Scanner s = new Scanner(output.get(i));
                result.add(new KentParams(s.nextDouble(), s.nextDouble(),
                        s.nextDouble(), s.nextDouble(),
                        s.nextDouble(), s.nextDouble(), s.nextDouble(),
                        s.nextDouble(), s.nextDouble(), s.nextDouble()));
            }
            return result;
        } catch (IOException e) {
            return null;
        }
    }

   public static List<KentParams> calculateHext(List<Tensor> tensors) {
        final String cmdPath = "/home/pont/files/phd/software/tauxe/pmagpy-2.66/s_hext.py";
        try {
            File tempFile = File.createTempFile("puffin", "hext");
            FileWriter writer = new FileWriter(tempFile);
            for (Tensor t: tensors) {
                writer.write(t.toTensorComponentString() + "\n");
            }
            writer.close();
            String[] args = {cmdPath, "-f", tempFile.getAbsolutePath()};
            List<String> output = execute(args);
            tempFile.delete();
            List<KentParams> result = new ArrayList<KentParams>(3);
            for (int i=2; i<5; i++) {
                Scanner s = new Scanner(output.get(i));
                result.add(new KentParams(s.nextDouble(), 0.,
                        s.nextDouble(), s.nextDouble(),
                        s.nextDouble(), s.nextDouble(), s.nextDouble(),
                        s.nextDouble(), s.nextDouble(), s.nextDouble()));
            }
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    public double getTau() {
        return tau;
    }

    public double getTauSigma() {
        return tauSigma;
    }

    public Vec3 getMean() {
        return mean;
    }

    public double getEtaMag() {
        return etaMag;
    }

    public Vec3 getEtaDir() {
        return etaDir;
    }

    public double getZetaMag() {
        return zetaMag;
    }

    public Vec3 getZetaDir() {
        return zetaDir;
    }
}
