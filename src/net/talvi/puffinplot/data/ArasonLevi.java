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
import java.util.Collection;
import java.util.logging.Logger;
import org.apache.commons.math3.distribution.TDistribution;

/**
 * Calculate and store Arason-Levi Maximum Likelihood Estimates (MLE) for
 * inclination-only data.
 *
 * Based on: http://brunnur.vedur.is/pub/arason/paleomag/pal.js (JavaScript
 * implementation by Ari Þórðarson) and 
 * http://brunnur.vedur.is/pub/arason/paleomag/aralev.txt (Fortran version by
 * Þórður Arason). Results checked against the Fortran version.
 *
 * Reference:
 *
 * Arason, Þ. &amp; Levi, S., 2010. Maximum likelihood solution for
 * inclination-only data in paleomagnetism. Geophysical Journal
 * International, 182(2), pp.753–771.
 *
 * @author pont
 */
public final class ArasonLevi {

    private static final Logger LOGGER =
            Logger.getLogger("net.talvi.puffinplot");
    
    private final double meanInc, kappa, t63, a95;
    private final int errorCode, n;

    private static final double DR = 0.0174532925199433;
    // Degrees to radians (pi/180)
    
    private ArasonLevi(int ierr, int n,
            double ainc, double ak, double t63, double a95) {
        this.errorCode = ierr;
        this.meanInc = ainc;
        this.kappa = ak;
        this.t63 = t63;
        this.a95 = a95;
        this.n = n;
    }

    /**
     * @return the number of inclination samples
     */
    public int getN() {
        return n;
    }
    
    /**
     * @return a maximum-likelihood estimate for the mean
     * inclination
     */
    public double getMeanInc() {
        return meanInc;
    }

    /**
     * @return a maximum-likelihood estimate for the precision
     * parameter κ
     */
    public double getKappa() {
        return kappa;
    }

    /**
     * @return a maximum-likelihood estimate for the angular
     * standard deviation (θ63).
     */
    public double getT63() {
        return t63;
    }

    /**
     * @return a maximum-likelihood estimate for the 95%
     * confidence limit (α95).
     */
    public double getA95() {
        return a95;
    }

    /**
     * Error codes:
     * 
     * 0: no problem
     * 1: convergence problem of selected solution
     * 2: failed robustness check
     * 3: convergence and robustness problems
     * 
     * @return an error code
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Evaluation of the Hyperbolic Bessel functions
     * I0(x), I1(x) and their ratio I1(x)/I0(x).  These functions are
     * sometimes also called the modified Bessel functions of order zero
     * and one.  Since both the functions I0(x) and I1(x) increase
     * exponentially as x increases and become numerically unstable,
     * the output is given as I0(x)/exp(|x|), etc.
     * The ratio I1(x)/I0(x) increases smoothly from 0 to 1 as x
     * increases from zero.
     * 
     * x	input	                Range: -Inf  <  x     < +Inf
     * bi0e	output	I0(x)/exp(|x|)  Range:  0    <  bi0e  <= 1
     * bi1e	output	I1(x)/exp(|x|)  Range: -0.22 <  bi1e  <  0.22 
     * bi1i0	output	I1(x)/I0(x)     Range: -1    <  bi1i0 <  1
     *
     * Using the approximations of:
     * Press et al. (1989), Numerical recipes, The art of scientific 
     * computing (Fortran version), Cambridge Univ. Press, Cambridge, 702 pp.
     * Abramowitz and Stegun (1972, eq. 9.8.1-4), Handbook of Mathematical
     * Functions with Formulas, Graphs, and Mathematical Tables, Dover, 
     * New York.
     *
     * The accuracy of these approximations are:
     *   abs[ exp(x)*bi0e - I0(x) ]           < 1.6E-7   (for x<3.75)
     *   abs[ exp(x)*bi1e - I1(x) ]           < 0.3E-7   (for x<3.75)
     *   abs[ sqrt(x)*(bi0e - I0(x)/exp(x)) ] < 1.9E-7   (for x>=3.75)
     *   abs[ sqrt(x)*(bi1e - I1(x)/exp(x)) ] < 2.2E-7   (for x>=3.75)

     */
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
     * The function is similar to 1/x close to zero, and practically
     * identical to 1 for x>3.  For the value x=0 the function returns 
     * the value zero, although it should return +/-Inf.
     * We have the relation  coth(-x) = - coth(x)
     *
     * The accuracy of these calculations are:
     *   abs[ error ] < 1.E-13   (for 0.0001<x<0.01)
     *   Exact formula           (for 0.01 <= x <= 15)
     *   abs[ error ] < 2.E-13   (for 15<x)
     * 
     * @param x a number. Range: -Inf  <  x < +Inf, can not be 0
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

    /**
     * The Arason-Levi MLE Iteration Formula 1.
     * 
     * @param th vector of co-inclination data (in degrees)
     * @param the current estimate of co-inclination (Theta) (in degrees)
     * @param ak current estimate of precision parameter (Kappa)
     * @return new (better) estimate of Theta
     */
    private static double aralevIteration1(double[] th, double the, double ak) {
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

    /**
     * The Arason-Levi MLE Iteration Formula 2.
     * 
     * @param th Vector of co-inclination data (in degrees)
     * @param the Current estimate of co-inclination (Theta) (in degrees)
     * @param ak Current estimate of precision parameter (Kappa)
     * @return New (better) estimate of Kappa
     */
    private static double aralevIteration2(double[] th, double the, double ak) {
        final int n = th.length;
        double s = 0, c = 0;
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

    /**
     * Evaluation of the Log-Likelihood function for inclination-only data. The
     * evaluation of the function is split into three parts,
     * 
     * XLIK = A1 + A2 + A3
     * A1 = N ln(k) - N ln(sinh k) - N ln(2)
     * A2 = Sum[ k cos t cos ti + ln(BessIo(k sin t sin ti)) ]
     * A3 = Sum[ ln(sin(ti)) ]
     * where k = kappa, t = the, ti = th(i),
     * Sum[ ] is the sum over all data i from 1 to n, and
     * BessIo is the Hyperbolic Bessel function I0(x)
     *
     * @param th Vector of co-inclination data (in degrees). Range: 0-180
     * degrees
     * @param the Theta-value where the function is to be evaluated. Range:
     * 0-180 degrees
     * @param ak Kappa-value where the function is to be evaluated.
     * Range: 0 <= kappa
     * @return Value of the log-likelihood function
     */
  private static double Xlik(double[] th, double the, double ak) {
        final int n = th.length;

        /* Check for illegal use */
        if (n < 1 || ak < 0) {
            return -1e10;
        }

        /* A1(k) = N ln(k) - N ln(sinh k) - N ln(2) */
        double a1;
        if (ak >= 0 && ak < 0.01) {
            double q = -ak * (1 - ak * (2/3 - ak * (1/3 - ak * (2/15 - ak * (8/45)))));
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

    /**
     * Calculate Arason-Levi parameters for a collection of inclinations.
     * 
     * Inclinations must be in the range [-90, 90], and there must be
     * at least one inclination in the supplied collection.
     * 
     * @param inclinations in degrees
     * @return Arason-Levi parameters
     */
    public static ArasonLevi calculate(Collection<Double> inclinations) {
        final double[] xinc =
                    inclinations.stream().mapToDouble(x->x).toArray();
        /* Set constants */
        final double t63max = 105.070062145; /* 63 % of a sphere. */
        final double a95max = 154.158067237; /* 95 % of a sphere. */
        int ierr = 1;
        final int n = xinc.length;
        
        /* Special cases for illegal and unusual parameters */

        if (n == 0) {
            throw new IllegalArgumentException("No inclinations supplied.");
        }
        
        if (n == 1) {
            /* It's not entirely clear what the correct (per the original
             * code) error code is here. The Fortrtan returns 1 (error),
             * the JavaScript returns 0. Here I go with the Fortran, since
             * the JavaScript also creates an "ERROR" alert, suggesting
             * that its return code of 0 (no error) is wrong. 
             */
            LOGGER.warning("Only one inclination supplied.");
            return new ArasonLevi(ierr, n, xinc[0], -1, t63max, a95max);
        }

        /* Check if inclinations are out of range */
        for (int i = 0; i < n; i++) {
            if (!(xinc[i] >= -90 && xinc[i] <= 90)) {
                throw new IllegalArgumentException("Inclination data out of "
                        + "range [-90, +90]");
            }
        }

        /* Check if all inclinations are identical */
        boolean same = true;
        for (int i = 1; i < n; i++) {
            if (xinc[i] != xinc[0]) {
                same = false;
            }
        }
        if (same) {
            LOGGER.warning("All inclinations are identical.");
            return new ArasonLevi(0, n, xinc[1], 1e10, 0, 0);
        }

        /* Convert inclinations to co-inclinations */
        double[] th = new double[n];
        for (int i = 0; i < n; i++) {
            th[i] = 90 - xinc[i];
        }

        /* Calculate arithmetic mean to use as first guess */
        double s=0, s2=0, c=0;
        for (int i = 0; i < n; i++) {
            s += th[i];
            s2 += th[i] * th[i];
            c += cos(th[i] * DR);
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
            rt = aralevIteration1(th, rt, rk);
            rk = aralevIteration2(th, rt, rk);
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
        final double the3 = 180;
        akap3 = rk;
        final double xl3 = Xlik(th, rt, rk);

        /* Find the maximum on the edge (kappa = 0) */
        rt = 90;
        rk = 0;
        final double the4 = rt;
        final double akap4 = rk;
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
        final double ainc_tmp = 90 - the1;
        final double ak_tmp = akap1;
        if (ierr_tmp != 0) {
            LOGGER.warning("Convergence problems in ARALEV");
        }

        /* Test robustness of solution theta +/- 0.01° and kappa +/- 0.1% */
        for (x = 0; x < 16; x++) {
            rt = the1 + 0.01 * cos(22.5 * x * DR);
            rk = akap1 * (1 + 0.001 * sin(22.5 * x * DR));
            if (rt >= 0 && rt <= 180) {
                final double xl = Xlik(th, rt, rk);
                if (xl > xl1) {
                    ierr_tmp = ierr_tmp + 2;
                    LOGGER.warning("Robustness problem in ARALEV");
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

        return new ArasonLevi(ierr_tmp, n, ainc_tmp, ak_tmp, t63_tmp, a95_tmp);
    }

    /**
     * Calculate and store mean inclination estimates based on a simple
     * arithmetic mean. Implemented as a static nested class of
     * ArasonLevi because it is mainly intended for comparison with
     * Arason-Levi estimates.
     */
    public final static class ArithMean {

        public final double inc, kappa, t63, a95;
        private final int n;
    
        private ArithMean(int n, double inc, double kappa,
                double t63, double a95) {
            this.inc = inc;
            this.kappa = kappa;
            this.t63 = t63;
            this.a95 = a95;
            this.n = n;
        }
        
        
        /**
         * @return the number of inclination samples
         */
        public int getN() {
            return n;
        }
        
        /**
         * @return the arithmetic mean inclination
         */
        public double getMeanInc() {
            return inc;
        }
        
        /**
         * The inverse variance is returned as an estimate of κ. 
         * 
         * @return an estimate of κ, the precision parameter
         */
        public double getKappa() {
            return kappa;
        }
        
        /**
         * @return an angular standard deviation (θ63) estimate
         * based on Student's t-values
         */
        public double getT63() {
            return t63;
        }

        /**
         * @return an 95% confidence limit (α95) estimate
         * based on Student's t-values
         */
        public double getA95() {
            return a95;
        }
        
        public static ArithMean calculate(Collection<Double> inclinations) {
            final double[] inc =
                    inclinations.stream().mapToDouble(x->x).toArray();
            final int n = inc.length;

            if (n == 0) {
                throw new IllegalArgumentException("No inclinations supplied.");
            }
            
            // TODO: Check for out-of-range inclinations and identical
            // inclinations, as is done in ArasonLevi.calculate.
            
            final double t63max = 105.070062145; /* 63 % of a sphere. */
            final double a95max = 154.158067237; /* 95 % of a sphere. */
            if (n == 1) {
                LOGGER.warning("Only one inclination supplied.");
                return new ArithMean(n, inc[0], -1, t63max, a95max);
            }
            
            final double[] th = new double[n];
            
            for (int i = 0; i < inc.length; i++) {
                th[i] = 90 - inc[i];
            }

            double s = 0;
            double s2 = 0;
            for (int i = 0; i < n; i++) {
                s += th[i];
                s2 += pow(th[i], 2);
            }
            
            double sd = 0;
            double ak = -1;
            if (n > 1) {
                sd = Math.sqrt( (s2 -s*s/n)/(n - 1.) );
                ak = (n - 1.) / ((s2 -s*s/n)*DR*DR);
            }
            
            final TDistribution tdist = new TDistribution(n-1);
            
            return new ArithMean(n, 90 - s / n, ak,
                    tdist.inverseCumulativeProbability((1. + 0.63) / 2.) * sd,
                    tdist.inverseCumulativeProbability((1. + 0.95) / 2.) * sd /
                            Math.sqrt(n));
        }

    }
}

