package net.talvi.puffinplot.data;

import static java.lang.Math.PI;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import Jama.Matrix;
import java.util.Collection;

public class Point {

    public final double x, y, z;
    public static final Point ORIGIN = new Point(0,0,0);
    
    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point(Point p) {
        this(p.x, p.y, p.z);
    }

    public Point correctSample(double az, double dip) {
        double[][] matrix =
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
    
    public Point correctForm(double az, double dip) {
        double cd = sin(dip);
        double sa = sin(az);
        double sd = cos(dip);
        double ca = cos(az);
        
        double[][] matrix =
            {{ca*sd*ca+sa*sa, sd*sa*ca-sa*ca, cd*ca},
            {sa*sd*ca-ca*sa, sd*sa*sa+ca*ca, cd*sa},
            {-ca*cd, -sa*cd, sd}};
        
        return transform(matrix);
    }
    
    public Point transform(double[][] matrix) {
        double[][] m = matrix;
        return new Point(
                x * m[0][0] + y * m[0][1] + z * m[0][2],
                x * m[1][0] + y * m[1][1] + z * m[1][2],
                x * m[2][0] + y * m[2][1] + z * m[2][2]);
    }

    public Point normalize() {
        double m = mag();
        return new Point(x / m, y / m, z / m);
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
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Point plus(Point p) {
        return new Point(x + p.x, y + p.y, z + p.z);
    }

    public Point minus(Point p) {
        return new Point(x - p.x, y - p.y, z - p.z);
    }

    public Point times(double a) {
        return new Point(x * a, y * a, z * a);
    }

    public double scalarProduct(Point p) {
        return (x * p.x + y * p.y + z * p.z);
    }

    public Point invert() {
        return new Point(-x, -y, -z);
    }

    public Matrix oTensor() {
        return new Matrix(new double[][]{{x * x, x * y, x * z},
            {y * x, y * y, y * z},
            {z * x, z * y, z * z}});
    }

    public static Point centreOfMass(Iterable<Point> points) {
        double xs = 0, ys = 0, zs = 0;
        int i = 0;
        for (Point p : points) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
            i++;
        }
        return new Point(xs / i, ys / i, zs / i);
    }

    public static Point fromPolarDegrees(double m, double inc, double dec) {
        double i = toRadians(inc);
        double d = toRadians(dec);
        return new Point(m * cos(i) * cos(d),
                m * cos(i) * sin(d),
                m * sin(i));
    }

    public double incRadians() {
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
    
    public double decRadians() {
        double theta = atan2(y, x);
        if (theta<0) theta += 2*PI;
        return theta;
    }

    public double incDegrees() {
        return toDegrees(incRadians());
    }

    public double decDegrees() {
        return toDegrees(decRadians());
    }
    
    public static double vectorSumLength(Collection<Point> points) {
        double xs=0, ys=0, zs=0;
        for (Point p: points) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
        }
        return sqrt(xs*xs + ys*ys + zs*zs);
    }
    
    public static Point meanDirection(Collection<Point> points) {
        double xs = 0, ys = 0, zs = 0;
        for (Point p: points) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
        }
        double R = sqrt(xs*xs + ys*ys + zs*zs);
        return new Point(xs/R, ys/R, zs/R);
    }
}
