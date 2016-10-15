/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2016 Pontus Lurcock.
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

import static java.lang.Math.abs;
import static java.lang.Math.sin;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.exp;
import static java.lang.Math.sqrt;

/**
 * Calculate Arason-Levi Maximum Likelihood Estimates (MLE) for Inclination-only
 * Data.
 *
 * Based on: http://brunnur.vedur.is/pub/arason/paleomag/pal.js (JavaScript
 * implementation by Ari Þórðarson)
 *
 * Results checked against:
 * http://brunnur.vedur.is/pub/arason/paleomag/aralev.txt (Fortran version by
 * Þórður Arason)
 *
 * Reference:
 *
 * Arason,. & Levi, S., 2010. Maximum likelihood solution for inclination-only
 * data in paleomagnetism. Geophysical Journal International, 182(2),
 * pp.753–771.
 *
 * @author pont
 */
public final class Aralev {

    private final double ainc, ak, t63, a95;
    private final int ierr;

    private static final double DR = 0.0174532925199433; // Degrees to radians (pi/180)
    
    private Aralev(int ierr, double ainc, double ak, double t63, double a95) {
        this.ierr = ierr;
        this.ainc = ainc;
        this.ak = ak;
        this.t63 = t63;
        this.a95 = a95;
    }

    /**
     * @return the ainc
     */
    public double getAinc() {
        return ainc;
    }

    /**
     * @return the ak
     */
    public double getAk() {
        return ak;
    }

    /**
     * @return the t63
     */
    public double getT63() {
        return t63;
    }

    /**
     * @return the a95
     */
    public double getA95() {
        return a95;
    }

    /**
     * @return the ierr
     */
    public int getIerr() {
        return ierr;
    }

    private static final class Bessel {

        public final double bi0e, bi1e, bi1i0;

        private Bessel(double bi0e, double bi1e, double bi1i0) {
            this.bi0e = bi0e;
            this.bi1e = bi1e;
            this.bi1i0 = bi1i0;
        }

        public static Bessel calculate(double x) {

            final double p1 = 1;
            final double p2 = 3.5156229;
            final double p3 = 3.0899424;
            final double p4 = 1.2067492;
            final double p5 = 0.2659732;
            final double p6 = 0.360768e-1;
            final double p7 = 0.45813e-2;

            final double q1 = 0.39894228;
            final double q2 = 0.1328592e-1;
            final double q3 = 0.225319e-2;
            final double q4 = -0.157565e-2;
            final double q5 = 0.916281e-2;
            final double q6 = -0.2057706e-1;
            final double q7 = 0.2635537e-1;
            final double q8 = -0.1647633e-1;
            final double q9 = 0.392377e-2;

            final double u1 = 0.5;
            final double u2 = 0.87890594;
            final double u3 = 0.51498869;
            final double u4 = 0.15084934;
            final double u5 = 0.2658733e-1;
            final double u6 = 0.301532e-2;
            final double u7 = 0.32411e-3;

            final double v1 = 0.39894228;
            final double v2 = -0.3988024e-1;
            final double v3 = -0.362018e-2;
            final double v4 = 0.163801e-2;
            final double v5 = -0.1031555e-1;
            final double v6 = 0.2282967e-1;
            final double v7 = -0.2895312e-1;
            final double v8 = 0.1787654e-1;
            final double v9 = -0.420059e-2;

            final double bi0e, bi1e, bi1i0;

            if (abs(x) < 3.75) {
                final double t = (x / 3.75) * (x / 3.75);
                final double b0 = p1 + t * (p2 + t * (p3 + t *
                        (p4 + t * (p5 + t * (p6 + t * p7)))));
                final double b1 = x * (u1 + t * (u2 + t * (u3 + t *
                        (u4 + t * (u5 + t * (u6 + t * u7))))));
                bi0e = b0 / exp(abs(x));
                bi1e = b1 / exp(abs(x));
                bi1i0 = b1 / b0;
            } else {
                final double t = 3.75 / abs(x);
                final double b0 = q1 + t * (q2 + t * (q3 + t * (q4 + t * 
                        (q5 + t * (q6 + t * (q7 + t * (q8 + t * q9)))))));
                double b1 = v1 + t * (v2 + t * (v3 + t * (v4 + t *
                        (v5 + t * (v6 + t * (v7 + t * (v8 + t * v9)))))));
                if (x < 0) {
                    b1 = -b1;
                }
                bi0e = b0 / sqrt(abs(x));
                bi1e = b1 / sqrt(abs(x));
                bi1i0 = b1 / b0;
            }

            return new Bessel(bi0e, bi1e, bi1i0);
        }
    }

    /**
     * Hyperbolic cotangent calculation using a Taylor expansion for very small
     * input values to avoid rounding errors.
     *
     * @param x a number
     * @return the hyperbolic tangent of x, or 0 if x is 0
     */
    private static double coth(double x) {
        double result;

        if (x == 0) {
            result = 0;
            return result;
        }

        final double t = abs(x);
        if (t < 0.001) {
            result = 1 / t + t / 3 - pow(t, 3) / 45 + pow(t, 5) * 2 / 945;
        } else if (t <= 15) {
            final double ep = exp(t);
            final double em = exp(-t);
            result = (ep + em) / (ep - em);
        } else {
            result = 1;
        }

        if (x < 0) {
            result = -result;
        }

        return result;
    }

    private static double AL1(double[] th, double the, double ak) {
        double s=0, c=0;
        for (int i = 0; i < th.length; i++) {
            final double x = ak * sin(the * DR) * sin(th[i] * DR);
            s += sin(th[i] * DR) * Bessel.calculate(x).bi1i0;
            c += cos(th[i] * DR);
        }

        double al1 = Math.atan2(s, c) / DR;

        if (al1 < 0.000001) {
            al1 = 0.000001;
        }
        if (al1 > 179.999999) {
            al1 = 179.999999;
        }

        return al1;
    }

    private static double AL2(double[] th, double the, double ak) {
        final int n = th.length;
        double s=0, c=0;
        for (int i = 0; i < n; i++) {
            final double x = ak * sin(the * DR) * sin(th[i] * DR);
            s += sin(th[i] * DR) * Bessel.calculate(x).bi1i0;
            c += cos(th[i] * DR);
        }

        final double x = n * coth(ak) - cos(the * DR) * c - sin(the * DR) * s;
        double al2 = 1e10;
        if (x / n > 1e-10) {
            al2 = n / x;
        }
        if (al2 < 1e-06) {
            al2 = 1e-06;
        }

        return al2;
    }

  private static double Xlik(double[] th, double the, double ak) {
        final int n = th.length;

        /* Illegal use */
        if (n < 1 || ak < 0) {
            return -1e10;
        }

        /* A1(k) = N ln(k) - N ln(sinh k) - N ln(2) */
        double a1;
        if (ak >= 0 && ak < 0.01) {
            double q = -ak * (1 - ak * (2 / 3 - ak * (1 / 3 - ak * (2 / 15 - ak * (8 / 45)))));
            a1 = n * (-log(2) - log(1 + q) - ak);
        } else if (ak >= 0.01 && ak <= 15) {
            a1 = n * (log(ak) - log(1 - exp(-2 * ak)) - ak);
        } else {
            a1 = n * (log(ak) - ak);
        }

        /* A2(k,t,ti) = Sum(k cos t cos ti) + Sum(ln(BessIo(k sin t sin ti))) */
        double a2 = 0;
        for (int i = 0; i < n; i++) {
            final double x = ak * sin(the * DR) * sin(th[i] * DR);
            final Bessel bessel = Bessel.calculate(x);
            a2 += ak * cos((th[i] - the) * DR) + log(bessel.bi0e);
        }

        /* A3(ti) = Sum( ln(sin(ti)) ) */
        double a3 = 0;
        for (int i = 0; i < n; i++) {
            double x = th[i];
            if (x < 0.000001) {
                x = 0.000001;
            }
            if (x > 179.999999) {
                x = 179.999999;
            }
            a3 += log(sin(x * DR));
        }

        /* The log-likelihood function */
        return a1 + a2 + a3;
    }

    private final static class Mean {

        public final double rt, rk;
    
        private Mean(double rt, double rk) {
            this.rt = rt;
            this.rk = rk;
        }

        public static Mean calculate(double[] th) {
            final int n = th.length;

            double s = 0;
            double s2 = 0;
            for (int i = 0; i < n; i++) {
                s = s + th[i];
                s2 = s2 + pow(th[i], 2);
            }
            Mean output = new Mean(s / n,
                    (n - 1) / ((s2 - s * s / n) * DR * DR));

            return output;
        }
    }

    public static Aralev calculate(double[] xinc) {
        /* Set constants */
        final double t63max = 105.070062145; /* 63 % of a sphere. */
        final double a95max = 154.158067237; /* 95 % of a sphere. */
        int ierr = 1;

        final int n = xinc.length;
        /* Check for illegal use */
        if (n == 1) {
            System.out.println("ERROR: Only one or none observed inclination in ARALEV");
            return new Aralev(ierr, xinc[0], -1, t63max, a95max);
        }

        /* Check if incl are out of range */
        for (int i = 0; i < n; i++) {
            if (!(xinc[i] >= -90 && xinc[i] <= 90)) {
                System.out.println("ERROR: Inclination data out of range [-90, +90] in ARALEV");
                return new Aralev(ierr, -98, -1, -1, -1);
            }
        }

        /* Check if all incl are identical */
        boolean same = true;
        for (int i = 1; i < n; i++) {
            if (xinc[i] != xinc[0]) {
                same = false;
            }
        }

        if (same) {
            System.out.println(" WARNING: All incl identical in ARALEV");
            return new Aralev(0, xinc[1], 1e10, 0, 0);
        }

        /* Inclinations to co-inclinations */
        double[] th = new double[n];
        for (int i = 0; i < n; i++) {
            th[i] = 90 - xinc[i];
        }

        /* Calculate arithmetic mean to use as first guess */
        double s=0, s2=0, c=0;
        for (int i = 0; i < n; i++) {
            s = s + th[i];
            s2 = s2 + th[i] * th[i];
            c = c + cos(th[i] * DR);
        }
        c = c / n;

        double rt = s / n;
        double x = (s2 - s * s / n) * DR * DR;
        double rk = 1e10;
        if (x / (n - 1) > 1e-10) {
            rk = (n - 1) / x;
        }
        double rt1 = rt;
        double rk1 = rk;

        /* Iterate in the interior to find solution to (theta, kappa) */
        /* Start iteration at arithmetic mean (theta, kappa) */
        rt = rt1;
        rk = rk1;
        int ie1 = 0;

        double the1 = rt;
        double akap1 = rk;

        boolean conv = false;
        for (int j = 0; j < 10000; j++) {
            rt = AL1(th, rt, rk);
            rk = AL2(th, rt, rk);
            final double dt = abs(rt - the1);
            final double dk = abs((rk - akap1) / rk);
            the1 = rt;
            akap1 = rk;
            if (j > 10 && dt < 1e-6 && dk < 1e-6) {
                conv = true;
                break;
            }
        }
        if (!conv) {
            ie1 = 1;
        }

        the1 = rt;
        akap1 = rk;
        double xl1 = Xlik(th, rt, rk);

        /* Find the maximum on the edge (theta = 0) */
        rt = 0;
        rk = rk1;
        x = 1 - c;
        if (x > 1e-10) {
            rk = 1 / x;
        }
        int ie2 = 0;

        double akap2 = rk;

        conv = false;
        for (int j = 0; j < 10000; j++) {
            x = coth(rk) - c;
            if (x > 1e-10) {
                rk = 1 / x;
            } else {
                rk = 1e10;
            }
            final double dk = abs((rk - akap2) / rk);
            akap2 = rk;
            if (j > 4 && dk < 1e-6) {
                conv = true;
                break;
            }
        }
        if (!conv) {
            ie2 = 1;
        }
        double the2 = 0;
        akap2 = rk;
        final double xl2 = Xlik(th, rt, rk);

        /* Find the maximum on the edge (theta = 180) */
        rt = 180;
        rk = rk1;
        x = 1 + c;
        if (x > 1e-10) {
            rk = 1 / x;
        }
        int ie3 = 0;

        double akap3 = rk;
        conv = false;
        for (int j = 0; j < 10000; j++) {
            x = coth(rk) + c;
            if (x > 1e-10) {
                rk = 1 / x;
            } else {
                rk = 1e10;
            }
            final double dk = abs((rk - akap3) / rk);
            akap3 = rk;
            if (j > 4 && dk < 1e-6) {
                conv = true;
                break;
            }
        }
        if (!conv) {
            ie3 = 1;
        }
        double the3 = 180;
        akap3 = rk;
        final double xl3 = Xlik(th, rt, rk);

        /* Find the maximum on the edge (kappa = 0) */
        rt = 90;
        rk = 0;
        double the4 = rt;
        double akap4 = rk;
        final double xl4 = Xlik(th, rt, rk);

        int ierr_tmp;

        /* Use the best solution of the four */
        int isol = 1;
        ierr_tmp = ie1;
        if (xl2 > xl1) {
            the1 = the2;
            akap1 = akap2;
            xl1 = xl2;
            isol = 2;
            ierr_tmp = 1;
            if (ie2 == 0) {
                ierr_tmp = 0;
            }
        }
        if (xl3 > xl1) {
            the1 = the3;
            akap1 = akap3;
            xl1 = xl3;
            isol = 3;
            ierr_tmp = 1;
            if (ie3 == 0) {
                ierr_tmp = 0;
            }
        }
        if (xl4 > xl1) {
            the1 = the4;
            akap1 = akap4;
            xl1 = xl4;
            isol = 4;
            ierr_tmp = 0;
        }
        double ainc_tmp = 90 - the1;
        double ak_tmp = akap1;
        if (ierr_tmp != 0) {
            System.out.println("WARNING: Convergence problems in ARALEV");
        }

        /* Test robustness of solution theta +/- 0.01° and kappa +/- 0.1% */
        for (x = 0; x < 16; x++) {
            rt = the1 + 0.01 * cos(22.5 * x * DR);
            rk = akap1 * (1 + 0.001 * sin(22.5 * x * DR));
            if (rt >= 0 && rt <= 180) {
                final double xl = Xlik(th, rt, rk);
                if (xl > xl1) {
                    ierr_tmp = ierr_tmp + 2;
                    System.out.println("WARNING: Robustness problem in ARALEV");
                }
            }
        }

        /* Estimation of angular standard deviation */
        /* Theta-63 calculated from (kappa), same method as Kono (1980) */

        double co = 0; // will be set below anyway but keeps the compiler happy
        if (akap1 >= 20) {
            co = 1 + log(1 - 0.63) / akap1;
        }
        if (akap1 > 0.1 && akap1 < 20) {
            co = 1 + log(1 - 0.63 * (1 - exp(-2 * akap1))) / akap1;
        }
        if (akap1 <= 0.1) {
            co = -0.26 + 0.4662 * akap1;
        }

        double t63_tmp = 0;
        if (co < 0) {
            t63_tmp = 180;
        }

        if (abs(co) < 1) {
            t63_tmp = 90 - Math.atan(co / sqrt(1 - co * co)) / DR;
        }
        if (t63_tmp > t63max) {
            t63_tmp = t63max;
        }

        /* Estimation of 95% (circular) symmetric confidence limit of the mean */
        /* Alpha-95 calculated from (N, kappa), same method as Kono (1980) */
        co = 1. - (n - 1.) * (pow(20., 1. / (n - 1.)) - 1.) / (n * (akap1 - 1.) + 1.);

        double a95_tmp = 0;
        if (co < 0) {
            a95_tmp = 180;
        }
        if (abs(co) < 1) {
            a95_tmp = 90 - Math.atan(co / sqrt(1. - co * co)) / DR;
        }
        if (a95_tmp > a95max) {
            a95_tmp = a95max;
        }

        return new Aralev(ierr_tmp, ainc_tmp, ak_tmp, t63_tmp, a95_tmp);
    }

    public static Aralev EvaluateInput(double[] inc) {

        final double[] th = new double[inc.length];

        for (int i = 0; i < inc.length; i++) {
            th[i] = 90 - inc[i];
        }

        Mean mean = Mean.calculate(th);

        //('result1').setAttribute('value', th.length);
        //('result2').setAttribute('value', Pal.FormatNumber(90 - mean.rt));
        //('result3').setAttribute('value', Pal.FormatNumber(mean.rk));
        Aralev aralevOutput = calculate(inc);

        //('result9').setAttribute('value', th.length);
        //('result4').setAttribute('value', Pal.FormatNumber(aralevOutput.ainc));
        //('result5').setAttribute('value', Pal.FormatNumber(aralevOutput.ak));
        //('result6').setAttribute('value', Pal.FormatNumber(aralevOutput.t63));
        //('result7').setAttribute('value', Pal.FormatNumber(aralevOutput.a95));
        //('result8').setAttribute('value', aralevOutput.ierr);
        return aralevOutput;
    }

}
