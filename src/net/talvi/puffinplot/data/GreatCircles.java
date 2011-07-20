package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.lang.Math.acos;

public class GreatCircles {

    private static final Logger logger = Logger.getLogger("GreatCircles");

    private final List<GreatCircle> circles;
    private final List<Vec3> endpoints;
    private Vec3 direction;
    private double a95;
    private double k;
    private double R;

    private static final double MAX_ITERATIONS = 1000;
    private static final double STABLE_LIMIT = Math.PI / 1800; // 0.1 degree

    private static final List<String> HEADERS =
        Arrays.asList("GC valid","GC inc.", "GC dec.", "GC a95", "GC k", "GC N", "GC M");

    public GreatCircles(List<Vec3> endpoints,
            List<List<Vec3>> circlePoints) {
        if (endpoints == null) this.endpoints = Collections.emptyList();
        else this.endpoints = endpoints;
        circles = new ArrayList<GreatCircle>(circlePoints.size());
        for (List<Vec3> points: circlePoints)
            circles.add(new GreatCircle(points));
        findDirection();
    }
    
    private void findDirection() {
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
        k = (2*M()+N()-2)/(2*(M()+N()-R));
        direction = Vec3.sum(D).plus(Vec3.sum(G)).normalize();
        a95 = alpha(0.95);
        logger.log(Level.INFO, "a95 {0}", a95);
    }

    private int M() {
        return endpoints.size();
    }

    private int N() {
        return circles.size();
    }

    private double alpha(double p) {
        double NN = M() + N() / 2.0;
        double v = 1 - ((NN-1)/(k*R)) *
                ( Math.pow(1/p, 1/(NN-1)) - 1 );
        logger.log(Level.INFO, String.format("%d %d %f %f %f %f", M(), N(), NN, k, R, v));
        return Math.toDegrees(Math.acos(v));
    }

    public List<GreatCircle> getCircles() {
        return circles;
    }

    public Vec3 getDirection() {
        return direction;
    }

    private String fmt(double d) {
        return String.format("%.1f", d);
    }

    public List<String> toStrings() {
        return Arrays.asList(isValid() ? "Y" : "",
                fmt(direction.getIncDeg()), fmt(direction.getDecDeg()),
                fmt(a95), fmt(k), fmt(N()), fmt(M()));
    }

    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }

    public static List<String> getHeaders() {
        return HEADERS;
    }

    public boolean isValid() {
        return N()>=3 && a95<3.5 && k>3;
    }

    public double getA95() {
        return a95;
    }
}
