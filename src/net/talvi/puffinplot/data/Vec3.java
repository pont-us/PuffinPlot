package net.talvi.puffinplot.data;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.signum;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.abs;
import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Vec3 {

    public final double x, y, z;
    public static final Vec3 ORIGIN = new Vec3(0,0,0);
    public static final Vec3 NORTH = new Vec3(1,0,0);
    public static final Vec3 EAST = new Vec3(0,1,0);
    public static final Vec3 DOWN = new Vec3(0,0,1);
    
    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(Vec3 p) {
        this(p.x, p.y, p.z);
    }
    /*
     * Returns a new vector equals to this vector with the specified
     * angle added to the declination.
     */
    public Vec3 addDecRad(double angle) {
        final double[][] matrix =
        {{ cos(angle), -sin(angle), 0},
         { sin(angle), cos(angle),  0},
         { 0,          0,           1}};
        return transform(matrix);
    }
    
    public Vec3 addIncRad(double angle) {
        final double sini = -sin(angle);
        final double sind = y/sqrt(y*y+x*x); // sine of -declination
        final double cosi = cos(angle);
        final double cosd = x/sqrt(y*y+x*x); // cosine of -declination
        
        double[][] matrix =
            {{cosd*cosi*cosd+sind*sind, cosi*sind*cosd-sind*cosd, sini*cosd},
            {sind*cosi*cosd-cosd*sind,  cosi*sind*sind+cosd*cosd, sini*sind},
            {-cosd*sini,               -sind*sini,                cosi}};
        
        return transform(matrix);
    }
    
    /*
     * Rotate 180 degrees about the X axis. Since X is up in magnetometer
     * co-ordinates, you can use this to correct a specimen that you put 
     * into the magnetometer back-to-front.
     * 
     */
    public Vec3 rotX180() {
        return new Vec3(x, -y, -z);
    }

    /* Return the unit vector on the intersection of the equator (z=0 line)
     * and the great circle between the supplied points.
     *
     */
    public static Vec3 equatorPoint(Vec3 v0, Vec3 v1) {
        return v0.times(signum(v0.z) / v0.z).
                plus(v1.times(signum(v1.z) / v1.z)).
                normalize();
    }

    /* Return true iff the supplied vector is in the same (upper/lower)
     * hemisphere as this one.
     */
    public boolean sameHemisphere(Vec3 v) {
        return signum(z) == signum(v.z);
    }

    /**
     * Given a list of points, return a a list of lists containing the same points
     * plus possible extras. Each sublist of the returned list is guaranteed
     * only to contain points in one hemisphere. Where the original list
     * crosses the equator, an extra point is interpolated exactly on the
     * equator.
     *
     * @param vs
     * @return
     */
    public static List<List<Vec3>> interpolateEquatorPoints(List<Vec3> vs) {
        List<List<Vec3>> result = new ArrayList<List<Vec3>>();
        List<Vec3> currentSegment = new ArrayList<Vec3>();
        Vec3 prev = null;
        for (Vec3 v: vs) {
            if (prev == null) {
                currentSegment.add(v);
            } else {
                if (prev.sameHemisphere(v)) {
                    currentSegment.add(v);
                } else {
                    Vec3 between = Vec3.equatorPoint(prev, v);
                    currentSegment.add(between);
                    result.add(currentSegment);
                    currentSegment = new ArrayList<Vec3>();
                    currentSegment.add(between);
                    currentSegment.add(v);
                }
            }
            prev = v;
        }
        result.add(currentSegment);
        return result;
    }

    /* Given two vectors, interpolates unit vectors along a great circle.
     * Uses Shoemake's Slerp algorithm.
     */
    public static List<Vec3> spherInterpolate(Vec3 v0, Vec3 v1, double stepSize) {
        Vec3 v0n = v0.normalize();
        Vec3 v1n = v1.normalize();
        double omega = Math.acos(v0n.dot(v1n));
        // TODO fix this for equator-crossing case?
        if (omega < stepSize) return Arrays.asList(new Vec3[] {v0n, v1n});
        int steps = (int) (omega / stepSize) + 1;
        final List<Vec3> result = new ArrayList<Vec3>(steps+1);
        Vec3 prevVec = null;

        for (int i=0; i<steps; i++) {
            final double t = (double) i / (double) (steps-1);
            double scale0 = (sin((1.0-t)*omega)) / sin(omega);
            double scale1 = sin(t*omega) / sin(omega);
            Vec3 thisVec = v0n.times(scale0).plus(v1n.times(scale1));
            //There's now a separate routine for interpolating equator points.
            //if (i>0 && !thisVec.sameHemisphere(prevVec)) {
            // result.add(equatorPoint(prevVec, thisVec));
            //}
            result.add(thisVec);
            prevVec = thisVec;
        }
                result.add(v1n);
        return result;
    }

    /**
     * TODO: interpolate spherically from start to end; onPath indicates
     * the direction. Of the two possible circular arcs, choose the one
     * that passes closest to onPath.
     */
    public static List<Vec3> spherInterpDir(Vec3 start, Vec3 end, Vec3 onPath,
            double stepSize) {
        final Vec3 avgDir = start.plus(end).normalize();
        if (avgDir.dot(onPath) > 0) {
            return spherInterpolate(start, end, stepSize);
        } else {
            List<Vec3> a = spherInterpolate(start, end.invert(), stepSize);
            List<Vec3> b = spherInterpolate(end.invert(), start.invert(), stepSize);
            List<Vec3> c = spherInterpolate(start.invert(), end, stepSize);
            List<Vec3> result = new ArrayList<Vec3>(a.size() + b.size() + c.size());
            result.addAll(a);
            result.addAll(b);
            result.addAll(c);
            return result;
        }
    }

    /*
     * Rotate about the Y axis
     */
    public Vec3 rotY(double angle) {
        final double[][] m =
         {{cos(angle), 0, sin(angle)},
          {0, 1, 0},
          { -sin(angle), 0, cos(angle)}};
        return transform(m);
    }
    
    /*
     * Rotate about the Z axis
     */
    public Vec3 rotZ(double angle) {
        final double[][] m =
         {{ cos(angle), -sin(angle), 0 },
         { sin(angle),  cos(angle), 0 },
         { 0      ,  0      , 1 }};
        return transform(m);
    }

    public static double[][] getSampleCorrectionMatrix(double az, double dip) {
        final double[][] matrix =
        {{ sin(dip)*cos(az) , -sin(az) , cos(dip)*cos(az)  },
         { sin(dip)*sin(az) ,  cos(az) , cos(dip)*sin(az)  },
         { -cos(dip)        , 0        , sin(dip)          }};
        return matrix;
    }

    public Vec3 correctSample(double az, double dip) {
        return transform(Vec3.getSampleCorrectionMatrix(az, dip));
    }
    
    /*
     * We can derive a matrix for the formation correction in three
     * steps:
     * 
     * 1. Rotate around Z (vertical) axis until strike is parallel
     *    to Y (east-west) axis.
     *    (That is, rotate anticlockwise by the dip azimuth angle.)
     * 2. Rotate around the Y axis by the dip angle.
     * 3. Rotate back around vertical axis by the dip azimuth angle.
     * 
     * The commented-out code below corresponds to this procedure. The
     * actual implementation uses a single matrix formed by the product of
     * the matrices for the individual steps; this is faster and more
     * accurate, but more difficult to understand as source code. 
     * 
        double[][] m1 =
        {{ cos(az), -sin(az), 0 },
         { sin(az),  cos(az), 0 },
         { 0      ,  0      , 1 }};
        
        double[][] m2 =
        {{ cos(dip), 0, sin(dip) },
         { 0, 1, 0},
         { -sin(dip), 0, cos(dip)}};
        
        double[][] m3 =
        {{ cos(-az), -sin(-az), 0 },
         { sin(-az),  cos(-az), 0 },
         { 0, 0, 1}};
        
        return transform(m3).transform(m2).transform(m1);
     */
    public Vec3 correctForm(double az, double dip) {
        return transform(Vec3.getFormationCorrectionMatrix(az, dip));
    }

    /**
     * Rotates a given vector by the same rotation which would
     * bring this vector vertical. In other words, performs a
     * tilt correction on the supplied vector, with this vector
     * defining the normal of the tilted plane. Assumes that this
     * is a unit vector.
     *
     * @param v vector to rotate
     * @return rotated vector
     */
    private Vec3 correctTilt(Vec3 v) {
        final double d = sqrt(x*x + y*y);
        return v.correctPlane(d, y/d, z, x/d);
    }

    public static double[][] getFormationCorrectionMatrix(double az, double dip) {
        return getPlaneCorrectionMatrix(sin(dip), sin(az), cos(dip), cos(az));
    }

    private static double[][] getPlaneCorrectionMatrix(double sd, double sa, double cd, double ca) {
        final double[][] matrix =
            {{ ca*cd*ca+sa*sa,  cd*sa*ca-sa*ca, sd*ca},
            {  sa*cd*ca-ca*sa,  cd*sa*sa+ca*ca, sd*sa},
            { -ca*sd,          -sa*sd,          cd}};
        return matrix;
    }

    private Vec3 correctPlane(double sd, double sa, double cd, double ca) {
        return transform(Vec3.getPlaneCorrectionMatrix(sd, sa, cd, ca));
    }
    
    /**
     * Returns a list of equally spaced points around a great circle
     * having this vector as its pole. Assumes that this is a unit
     * vector.
     * 
     * @param n number of points to return
     * @param closed if true, first point will also be appended to end of list,
     * giving n+1 points, but only n unique ones, creating a closed circle.
     * @return list of points on great circle
     */
    public List<Vec3> greatCirclePoints(int n, boolean closed) {
        final List<Vec3> points = new ArrayList<Vec3>(n+1);
        for (int i=0; i<n; i++) {
            points.add(correctTilt(Vec3.fromPolarRadians(1, 0, 2*PI*i/n)));
        }
        if (closed) points.add(points.get(0));
        return points;
    }

    public Vec3 transform(double[][] matrix) {
        final double[][] m = matrix;
        return new Vec3(
                x * m[0][0] + y * m[0][1] + z * m[0][2],
                x * m[1][0] + y * m[1][1] + z * m[1][2],
                x * m[2][0] + y * m[2][1] + z * m[2][2]);
    }

    /**
     * Using the enclosing vector to define the pole of a great circle G, this
     * method accepts another unit vector v and returns the nearest
     * unit vector to v which lies on G. Algorithm from McFadden and
     * McElhinny 1988, p. 165.
     *
     * @param v a unit vector
     * @return the nearest point to v which lies on this great circle
     */
    public Vec3 nearestOnCircle(Vec3 v) {
        final double tau = this.dot(v);
        final double rho = sqrt(1 - tau * tau);
        return new Vec3(v.x - tau * x, v.y - tau * y, v.z - tau * z).
                divideBy(rho);
    }

    public Vec3 normalize() {
        final double m = mag();
        return new Vec3(x / m, y / m, z / m);
    }

    public double getComponent(MeasurementAxis axis) {
        switch (axis) {
            case X: return x;
            case Y: return y;
            case Z: return z;
            case MINUSZ: return -z;
            case H: return sqrt(x*x+y*y);
            default: throw new IllegalArgumentException("invalid axis "+axis);
        }
    }

    public double mag() {
        return sqrt(x * x + y * y + z * z);
    }

    public Vec3 plus(Vec3 p) {
        return new Vec3(x + p.x, y + p.y, z + p.z);
    }

    public Vec3 minus(Vec3 p) {
        return new Vec3(x - p.x, y - p.y, z - p.z);
    }

    public Vec3 times(double a) {
        return new Vec3(x * a, y * a, z * a);
    }

    public Vec3 times(Vec3 p) {
        return new Vec3(x * p.x, y * p.y, z * p.z);
    }

    public Vec3 divideBy(double a) {
        return new Vec3(x / a, y / a, z / a);
    }

    public Vec3 divideBy(Vec3 p) {
        return new Vec3(x / p.x, y / p.y, z / p.z);
    }

    public double dot(Vec3 p) {
        return (x * p.x + y * p.y + z * p.z);
    }

    public Vec3 cross(Vec3 p) {
        return new Vec3(y*p.z - z*p.y, z*p.x - x*p.z, x*p.y - y*p.x);
    }

    public Vec3 invert() {
        return new Vec3(-x, -y, -z);
    }

    public Matrix oTensor() {
        return new Matrix(new double[][]{{x * x, x * y, x * z},
            {y * x, y * y, y * z},
            {z * x, z * y, z * z}});
    }

    public double angleTo(Vec3 v) {
        // Uses a cross product to get a signed angle.
        Vec3 cross = cross(v);
        double sign = cross.z;
        if (sign == 0) sign = cross.y;
        if (sign == 0) sign = cross.x;
        return asin(cross.mag()) * signum(sign);
    }

    public static Vec3 centreOfMass(List<Vec3> points) {
        double xs = 0, ys = 0, zs = 0;
        int i = 0;
        for (Vec3 p : points) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
            i++;
        }
        return new Vec3(xs / i, ys / i, zs / i);
    }

    public static Vec3 fromPolarDegrees(double m, double inc, double dec) {
        return Vec3.fromPolarRadians(m, toRadians(inc), toRadians(dec));
    }

    public static Vec3 fromPolarRadians(double m, double inc, double dec) {
        return new Vec3(m * cos(inc) * cos(dec),
                m * cos(inc) * sin(dec),
                m * sin(inc));
    }

    public double getIncRad() {
        return atan2(z, sqrt(x*x + y*y));
    }

    /**
     *  The 2G manual specifies the following conversion, more or less:
     * 
     *  if (x < 0) return atan(y / x) + PI;
     *  else if (x > 0 && y <= 0) return atan(y / x) + 2 * PI;
     *  else if (x == 0) {
     *      if (y > 0) return PI / 2;
     *      else if (y < 0) return -PI / 2;
     *      else return 0; // arbitrary
     *  } else {
     *      return atan(y / x);
     *  }
     *
     * However, the formulae given in the manual are known to be unreliable,
     * so we use the more straightforwardly correct atan2, and shift the 
     * range from [-pi, pi] to [0, 2pi].
     * 
     */
    
    public double getDecRad() {
        double theta = atan2(y, x);
        if (theta<0) theta += 2*PI;
        return theta;
    }

    public double getIncDeg() {
        return toDegrees(getIncRad());
    }

    public double getDecDeg() {
        return toDegrees(getDecRad());
    }

    public static Vec3 sum(Collection<Vec3> points) {
        double xs = 0, ys = 0, zs = 0;
        for (Vec3 p: points) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
        }
        return new Vec3(xs, ys, zs);
    }

    public static double vectorSumLength(Collection<Vec3> points) {
        return sum(points).mag();
    }

    /**
     * Note that this assumes unit vectors.
     *
     * @param points a collection of unit vectors
     * @return a vector representing their mean direction
     */
    public static Vec3 meanDirection(Collection<Vec3> points) {
        return sum(points).normalize();
    }

    public static Vec3 mean(Collection<Vec3> points) {
        return sum(points).divideBy(points.size());
    }

    public static Vec3 mean(Vec3... points) {
        return mean(Arrays.asList(points));
    }

    public Vec3 setX(double newX) {
        return new Vec3(newX, y, z);
    }

    public Vec3 setY(double newY) {
        return new Vec3(x, newY, z);
    }

    public Vec3 setZ(double newZ) {
        return new Vec3(x, y, newZ);
    }

    public static List<Vec3> ellipse(double dec, double inc,
            double eta, double etad, double etai,
            double zeta, double zetad, double zetai) {
        final int N = 64;
        List<Vec3> vs = new ArrayList<Vec3>(N);
        // we're going to draw the ellipse at the top of the unit
        // sphere, then rotate it down into position.

        // ed and ei tell us which way the ellipse is pointing.
        // (zd and zi are orthogonal so we'll ignore them.)
        // TODO
        Vec3 centre = Vec3.fromPolarDegrees(1., inc, dec);
        Vec3 etaDir = Vec3.fromPolarDegrees(1., etai, etad);
        Vec3 etaDirTop =
                etaDir.rotZ(-centre.getDecRad()).rotY(centre.getIncRad()-PI/2.).
                rotZ(centre.getDecRad());

        // draw the ellipse at the top
        double stepSize = 2.*PI / N;
        for (int i=0; i<N; i++) {
            double theta = i * stepSize;
            double a = (eta*sin(theta));
            double b = zeta*cos(theta);
            double r = eta*zeta/sqrt(a*a+b*b);
            Vec3 v1 = Vec3.fromPolarRadians(1., 0.5*PI-r, theta + etaDirTop.getDecRad());
            Vec3 v2 = v1.
                    rotZ(-centre.getDecRad()).rotY(PI/2.-centre.getIncRad()).
                    rotZ(centre.getDecRad());
            vs.add(v2);
        }
        vs.add(centre);

        return vs;
    }

    public static List<Vec3> ellipse(KentParams kp) {
        double eta = kp.getEtaMag();
        // if (eta>PI/2) eta = PI/2+0.1;
        final double zeta = kp.getZetaMag();
        // final int N = (int) (1000*sqrt(1+eta*eta+zeta*zeta));
        List<Vec3> vs = new ArrayList<Vec3>(1000);
        // we're going to draw the ellipse at the bottom of the unit
        // sphere, then rotate it down into position.

        // ed and ei tell us which way the ellipse is pointing.
        // (zd and zi are orthogonal so we'll ignore them.)
        Vec3 centre = kp.getMean();
        Vec3 etaDir = kp.getEtaDir();
        // calculate the direction of the eta axis
        Vec3 etaDirTop =
                etaDir.rotZ(-centre.getDecRad()).rotY(centre.getIncRad()-PI/2.).
                rotZ(centre.getDecRad());

        // draw the ellipse
        double stepSize = 1e-4;
        double stepLimit = 1e-2;
        double thetaLimit = 2*PI/50.;
        Vec3 vPrev = null;
        double thetaPrev = -100;
        int n=0;
        for (double theta=0; theta<2*PI; theta += stepSize) {
            // final double theta = i * stepSize;
            final double a = eta*sin(theta);
            final double b = zeta*cos(theta);
            final double r = eta*zeta/sqrt(a*a+b*b);
            // Calculate a point centred around zero
            Vec3 v1 = Vec3.fromPolarRadians(1., 0.5*PI-r, theta + etaDirTop.getDecRad());
            // rotate it into position

            //v2 = v1;
            if (vPrev==null || abs(vPrev.angleTo(v1)) > stepLimit ||
                        (theta - thetaPrev) > thetaLimit) {
                    Vec3 v2 = v1.rotZ(-centre.getDecRad()).
                            rotY(PI / 2. - centre.getIncRad()).
                        rotZ(centre.getDecRad());
                    //System.out.println(":"+v2.z);
                    vs.add(v2);
                    vPrev = v1;
                    thetaPrev = theta;
                    n++;
                }
            }
        vs.add(vs.get(0)); // close the path
        return vs;
    }

    String toCustomString(String pre, String post, String sep, int dp,
            boolean scale) {
        // TODO tidy up: this code's a mess.
        final String format = String.format("%s%%.%df%s%%.%df%s%%.%df%s",
                    pre, dp, sep, dp, sep, dp, post);
        final Vec3 vec;
        int oom = 0;
        if (scale) {
            oom = (int) (log10(mag())) - 1;
            double sf = pow(10, oom);
            Vec3 fixed = this.divideBy(sf);
            vec = fixed;
        } else {
            vec = this;
        }
        String result = String.format(format, vec.x, vec.y, vec.z);
        if (scale) {
            result += String.format("e%d", oom);
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("%.2f %.2f %.2f", x, y, z);
    }
}
