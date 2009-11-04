package net.talvi.puffinplot.data;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static java.lang.Math.signum;
import Jama.Matrix;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Vec3 {

    public final double x, y, z;
    public static final Vec3 ORIGIN = new Vec3(0,0,0);
    
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
     *
     */
    public boolean sameHemisphere(Vec3 v) {
        return signum(z) == signum(v.z);
    }

    /* Given two vectors, interpolates unit vectors along a great circle.
     * Uses Shoemake's Slerp algorithm.
     */

    public static Vec3[] spherInterpolate(Vec3 v0, Vec3 v1, double stepSize) {
        Vec3 v0n = v0.normalize();
        Vec3 v1n = v1.normalize();
        double omega = Math.acos(v0n.scalarProduct(v1n));
        if (omega < stepSize) return new Vec3[] {v0n, v1n};
        int steps = (int) (omega / stepSize) + 1;
        final Vec3[] result = new Vec3[steps];
        for (int i=0; i<steps; i++) {
            final double t = (double) i / (double) (steps-1);
            double scale0 = (sin((1.0-t)*omega)) / sin(omega);
            double scale1 = sin(t*omega) / sin(omega);
            result[i] = v0n.times(scale0).plus(v1n.times(scale1));
        }
        return result;
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
    
    public Vec3 correctSample(double az, double dip) {
        final double[][] matrix =
        {{ sin(dip)*cos(az) , -sin(az) , cos(dip)*cos(az)  },
         { sin(dip)*sin(az) ,  cos(az) , cos(dip)*sin(az)  },
         { -cos(dip)        , 0        , sin(dip)          }};
        return transform(matrix);
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
        final double sd = sin(dip);
        final double sa = sin(az);
        final double cd = cos(dip);
        final double ca = cos(az);
        
        double[][] matrix =
            {{ca*cd*ca+sa*sa, cd*sa*ca-sa*ca, sd*ca},
            {sa*cd*ca-ca*sa, cd*sa*sa+ca*ca, sd*sa},
            {-ca*sd, -sa*sd, cd}};
        
        return transform(matrix);
    }
    
    public Vec3 transform(double[][] matrix) {
        double[][] m = matrix;
        return new Vec3(
                x * m[0][0] + y * m[0][1] + z * m[0][2],
                x * m[1][0] + y * m[1][1] + z * m[1][2],
                x * m[2][0] + y * m[2][1] + z * m[2][2]);
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

    public double scalarProduct(Vec3 p) {
        return (x * p.x + y * p.y + z * p.z);
    }

    public Vec3 invert() {
        return new Vec3(-x, -y, -z);
    }

    public Matrix oTensor() {
        return new Matrix(new double[][]{{x * x, x * y, x * z},
            {y * x, y * y, y * z},
            {z * x, z * y, z * z}});
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

    Vec3 setX(double newX) {
        return new Vec3(newX, y, z);
    }

    Vec3 setY(double newY) {
        return new Vec3(x, newY, z);
    }

    Vec3 setZ(double newZ) {
        return new Vec3(x, y, newZ);
    }
}
