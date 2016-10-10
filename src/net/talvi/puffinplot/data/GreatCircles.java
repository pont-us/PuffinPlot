/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
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

import static java.lang.Math.acos;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

/**
 * This class represents a set of great circles and a set of directions.
 * It calculates a best-fitting mean direction from these data using
 * the method of McFadden and McElhinny (1988).
 * 
 * @author pont
 */
public final class GreatCircles implements FisherParams {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private final List<GreatCircle> circles;
    private final List<Vec3> endpoints;
    private final Vec3 direction;
    private final double a95;
    private final double k;
    private final double R;
    private final int minPoints;
//    private final PythonInterpreter interp = new PythonInterpreter();
    private final Preferences prefs = Preferences.userNodeForPackage(net.talvi.puffinplot.PuffinPrefs.class);

    private static final double MAX_ITERATIONS = 1000;
    private static final double STABLE_LIMIT = Math.PI / 1800; // 0.1 degree

    private static final List<String> HEADERS =
        Arrays.asList("GC valid","GC dec. (deg)", "GC inc. (deg)",
            "GC a95 (deg)", "GC k", "GC N", "GC M", "GC R", "GC min points");
    
    private static final ScriptEngine SCRIPT_ENGINE;

    static {
        ScriptEngineManager sem = new ScriptEngineManager();
        SCRIPT_ENGINE = sem.getEngineByMimeType("application/javascript");
    }
    
    /**
     * Calculates a mean direction from the supplied great circle and
     * directions.
     * 
     * @param endpoints a set of directions (probably from linear PCA fits)
     * @param circles a set of great circles
     */
    public GreatCircles(List<Vec3> endpoints, List<GreatCircle> circles) {
        if (endpoints == null) endpoints = Collections.emptyList();
        this.endpoints = endpoints;
        if (circles == null) circles = Collections.emptyList();
        circles = Collections.unmodifiableList(new ArrayList<>(circles));
        this.circles = circles;
        assert(endpoints.size() > 0 || circles.size() > 1);
        int minPointsTmp = getCircles().isEmpty() ? 0 : Integer.MAX_VALUE;
        for (GreatCircle gc: getCircles()) {
            final int numPoints = gc.getPoints().size();
            if (numPoints < minPointsTmp) {
                minPointsTmp = numPoints; 
            }
        }
        minPoints = minPointsTmp;
        boolean goodFirstGuess = (endpoints.size() > 0);
        List<Vec3> D;
        if (goodFirstGuess) D = endpoints;
        else {
            // can't use SingletonList since it's immutable,
            // and we want to remove this guess after the first 
            // iteration
            D = new ArrayList<>(1);
            // We need to provide a starting point for the iteration.
            // Let's use the resultant direction of the last moments
            // in the great circle paths.
            Vec3 guess = Vec3.ORIGIN;
            for (GreatCircle c: circles) guess = guess.plus(c.lastPoint().minus(c.getPoints().get(0)));
            D.add(guess.normalize()); // todo: better guess
        }
        List<Vec3> G = new ArrayList<>(circles.size());
        G.addAll(Collections.nCopies(circles.size(), Vec3.ORIGIN));
        boolean converged = false;
        int iter;
        for (iter = 0; iter < MAX_ITERATIONS && !converged; iter++) {
            if (iter > 0) converged = true;
            for (int i = 0; i < G.size(); i++) {
                Vec3 gOld = G.get(i);
                G.set(i, Vec3.ORIGIN);
                Vec3 guess = Vec3.sum(D).plus(Vec3.sum(G)).normalize();
                Vec3 gNew = circles.get(i).nearestOnCircle(guess);
                G.set(i, gNew);
                if (iter > 0 && acos(gNew.dot(gOld)) > STABLE_LIMIT)
                    converged = false;
            }
            if (iter == 0 && !goodFirstGuess) {
                D.remove(0);
            }
        }
        logger.log(Level.INFO, "{0} iterations", iter);
        R = Vec3.sum(D).plus(Vec3.sum(G)).mag();
        k = (2*getM()+getN()-2)/(2*(getM()+getN()-R));
        direction = Vec3.sum(D).plus(Vec3.sum(G)).normalize();
        a95 = alpha(0.95);
        logger.log(Level.INFO, "a95 {0}", a95);
    }

    /** Returns the number of stable endpoints used in the calculation.
     * @return the number of stable endpoints used in the calculation */
    public int getM() {
        return endpoints.size();
    }

    /** Returns the number of great circles used in the calculation.
     * @return the number of great circles used in the calculation */
    @Override
    public int getN() {
        return circles.size();
    }
    
    /**
     * Returns the smallest number of treatment steps used to define 
     * any of the great circles in this collection.
     * 
     * @return the smallest number of treatment steps used to define any of the 
     * great circles in this collection
     */
    private int getMinPoints() {
        return minPoints;
    }

    private double alpha(double confidence) {
        final double p = 1 - confidence;
        final double NN = getM() + getN() / 2.0;
        final double v = 1 - ((NN-1)/(k*R)) *
                ( Math.pow(1/p, 1/(NN-1)) - 1 );
        logger.log(Level.INFO, String.format(Locale.ENGLISH,
                "%d %d %f %f %f %f",
                getM(), getN(), NN, k, R, v));
        return Math.toDegrees(Math.acos(v));
    }

    /** Returns the great circles which were originally supplied to the constructor.
     * @return the great circles which were originally supplied to the constructor */
    public final List<GreatCircle> getCircles() {
        return Collections.unmodifiableList(circles);
    }

    /** Returns the best-fit mean direction for the supplied circles and directions. 
     * @return the best-fit mean direction for the supplied circles and directions */
    @Override
    public Vec3 getMeanDirection() {
        return direction;
    }

    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.2f", d);
    }
    
    private String intFmt(int d) {
        return String.format(Locale.ENGLISH, "%d", d);
    }

    /** Returns the statistical parameters as a list of strings.
     * The order of the parameters is the same as the order of
     * the headers provided by {@link #getHeaders()}.
     * @return the statistical parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(isValid() ? "Y" : "N",
                fmt(direction.getDecDeg()), fmt(direction.getIncDeg()),
                fmt(a95), fmt(k), intFmt(getN()), intFmt(getM()), fmt(getR()),
                intFmt(getMinPoints()));
    }

    /** Returns a list of empty strings equal in length to the number of parameters.
     * @return  a list of empty strings equal in length to the number of parameters
     */
    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }

    /** Returns the headers describing the parameters as a list of strings.
     * @return the headers describing the parameters
     */
    public static List<String> getHeaders() {
        return HEADERS;
    }
    
    /** Returns {@code true} if this great-circle fit is valid. 
     * @return {@code true} if this great-circle fit is valid
     */
    public boolean isValid() {
        final String validityCondition =
                prefs.get("data.greatcircles.validityExpr", "true");
        // Avoid firing up an interpreter for the most common cases.
        switch(validityCondition) {
            case "true":
                return true;
            case "false":
                return false;
        }
        final Bindings bindings = new SimpleBindings();
        bindings.put("a95", a95);
        bindings.put("k", k);
        bindings.put("N", getN());
        bindings.put("M", getM());
        SCRIPT_ENGINE.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
        try {
            final Object result = SCRIPT_ENGINE.eval(validityCondition);
            if (result.getClass() == Boolean.class) {
                return (Boolean) result;
            } else {
                return false;
            }
        } catch (ScriptException ex) {
            logger.log(Level.WARNING, ex.toString());
            return false;
        }
    }

    @Override
    public double getA95() {
        return a95;
    }
    
    @Override
    public boolean isA95Valid() {
        return !(Double.isNaN(a95) || Double.isInfinite(a95));
    }
    
    @Override
    public double getK() {
        return k;
    }
    
    @Override
    public double getR() {
        return R;
    }
}
