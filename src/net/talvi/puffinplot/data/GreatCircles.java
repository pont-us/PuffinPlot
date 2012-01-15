package net.talvi.puffinplot.data;

import java.util.Locale;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import static java.lang.Math.acos;

/**
 * This class represents a set of great circles and a set of directions.
 * It calculates a best-fitting mean direction from these data using
 * the method of McFadden and McElhinny (1988).
 * 
 * @author pont
 */
public class GreatCircles implements FisherParams {

    private static final Logger logger = Logger.getLogger("net.talvi.puffinplot");
    private final List<GreatCircle> circles;
    private final List<Vec3> endpoints;
    private Vec3 direction;
    private double a95;
    private double k;
    private double R;
    private int minPoints;
    private PythonInterpreter interp = new PythonInterpreter();
    private Preferences prefs = Preferences.userNodeForPackage(net.talvi.puffinplot.PuffinPrefs.class);

    private static final double MAX_ITERATIONS = 1000;
    private static final double STABLE_LIMIT = Math.PI / 1800; // 0.1 degree

    private static final List<String> HEADERS =
        Arrays.asList("GC valid","GC dec. (°)", "GC inc. (°)",
            "GC a95 (°)", "GC k", "GC N", "GC M", "GC min points");

    /**
     * Calculates a mean direction from the supplied great circle and
     * directions.
     * 
     * @param endpoints a set of directions (probably from linear PCA fits)
     * @param circles a set of great circles
     */
    public GreatCircles(List<Vec3> endpoints, List<GreatCircle> circles) {
        if (endpoints == null) this.endpoints = Collections.emptyList();
        else this.endpoints = endpoints;
        this.circles = Collections.
                unmodifiableList(new ArrayList<GreatCircle>(circles));
        findDirection();
    }
    
    private void findDirection() {
        minPoints = getCircles().isEmpty() ? 0 : Integer.MAX_VALUE;
        for (GreatCircle gc: getCircles()) {
            final int numPoints = gc.getPoints().size();
            if (numPoints < minPoints) {
                minPoints = numPoints; 
            }
        }
        
        boolean goodFirstGuess = (endpoints.size() > 0);
        List<Vec3> D;
        if (goodFirstGuess) D = endpoints;
        else {
            // can't use SingletonList since it's immutable,
            // and we want to remove this guess after the first 
            // iteration
            D = new ArrayList<Vec3>(1);
            // We need to provide a starting point for the iteration.
            // Let's use the resultant direction of the last moments
            // in the great circle paths.
            Vec3 guess = Vec3.ORIGIN;
            for (GreatCircle c: circles) guess = guess.plus(c.lastPoint().minus(c.getPoints().get(0)));
            D.add(guess.normalize()); // todo: better guess
        }

        List<Vec3> G = new ArrayList<Vec3>(circles.size());
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
    public int getN() {
        return circles.size();
    }
    
    private int getMinPoints() {
        return minPoints;
    }

    private double alpha(double p) {
        double NN = getM() + getN() / 2.0;
        double v = 1 - ((NN-1)/(k*R)) *
                ( Math.pow(1/p, 1/(NN-1)) - 1 );
        logger.log(Level.INFO, String.format("%d %d %f %f %f %f", getM(), getN(), NN, k, R, v));
        return Math.toDegrees(Math.acos(v));
    }

    /** Returns the great circles which were originally supplied to the constructor.
     * @return the great circles which were originally supplied to the constructor */
    public List<GreatCircle> getCircles() {
        return Collections.unmodifiableList(circles);
    }

    /** Returns the best-fit mean direction for the supplied circles and directions. 
     * @return the best-fit mean direction for the supplied circles and directions */
    public Vec3 getMeanDirection() {
        return direction;
    }

    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.1f", d);
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
                fmt(a95), fmt(k), intFmt(getN()), intFmt(getM()), intFmt(getMinPoints()));
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
                prefs.get("data.greatcircles.validityExpr", "N>=3 and a95<3.5 and k>3");
        interp.set("a95", a95);
        interp.set("k", k);
        interp.set("N", getN());
        interp.set("M", getM());
        try {
            PyObject result = interp.eval(validityCondition);
            return result.__nonzero__();
        } catch (PyException ex) {
            logger.log(Level.WARNING, ex.toString());
            return false;
        }
    }

    public double getA95() {
        return a95;
    }
    
    public double getK() {
        return k;
    }
}
