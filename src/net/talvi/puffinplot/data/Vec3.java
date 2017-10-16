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

import Jama.Matrix;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * An immutable three-dimensional Cartesian vector. The class contains
 * many methods for manipulating vectors and collections of vectors.
 * 
 * @author pont
 */
public class Vec3 {

    /** the x component of the vector */
    public final double x;
    /** the y component of the vector */
    public final double y;
    /** the z component of the vector */
    public final double z;
    /** the origin vector (zero along each axis) */
    public static final Vec3 ORIGIN = new Vec3(0,0,0);
    /** a unit vector pointing north */
    public static final Vec3 NORTH = new Vec3(1,0,0);
    /** a unit vector pointing east */
    public static final Vec3 EAST = new Vec3(0,1,0);
    /** a unit vector pointing down */
    public static final Vec3 DOWN = new Vec3(0,0,1);
    
    /** Creates a vector with the specified components. 
     * @param x the x component of the new vector
     * @param y the y component of the new vector
     * @param z the z component of the new vector */
    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /** Returns a new vector equal to this vector with the specified
     * angle added to the declination.
     * @param angle the angle in radians to add to the declination
     * @return a new vector equal to this vector with the specified
     * angle added to the declination
     */
    public Vec3 addDecRad(double angle) {
        final double[][] matrix =
        {{ cos(angle), -sin(angle), 0},
         { sin(angle), cos(angle),  0},
         { 0,          0,           1}};
        return transform(matrix);
    }
    
    /** Returns a new vector equal to this vector with the specified
     * angle added to the inclination.
     * @param angle the angle in radians to add to the inclination
     * @return a new vector equal to this vector with the specified
     * angle added to the inclination
     */
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
    
    /**
     * Rotates the vector by 180 degrees about the specified axis.
     * Since x is up in magnetometer co-ordinates, specifying the x axis 
     * corrects the data for a specimen placed in the magnetometer back-to-front.
     * If an axis other than X, Y, or Z is specified, the original vector
     * is returned.
     * 
     * @param axis the axis about which to rotate
     * @return a new vector equal to this vector rotated 180&deg; about the
     * specified axis
     */
    public Vec3 rot180(MeasurementAxis axis) {
        switch (axis) {
            case X: return new Vec3(x, -y, -z);
            case Y: return new Vec3(-x, y, -z);
            case Z: return new Vec3(-x, -y, z);
            default: return this;
        }
    }

    /**
     * Returns the unit vector on the intersection of the equator (z=0 line)
     * and the great circle between the supplied points. The supplied 
     * vectors must be well-formed and in opposite hemispheres.
     * 
     * @param v0 a vector specifying a direction
     * @param v1 a vector specifying a direction
     * @return a unit vector {@code v} for which {@code v.z==0}, which lies
     * on the shortest great-circle path between the normalizations of
     * {@code v0} and {@code v1}
     */
    public static Vec3 equatorPoint(Vec3 v0, Vec3 v1) {
        if (!v0.isWellFormed()) {
            throw new IllegalArgumentException("v0 is not well-formed.");
        }
        if (!v1.isWellFormed()) {
            throw new IllegalArgumentException("v1 is not well-formed.");
        }
        if ((v0.z>0 && v1.z>0) || (v0.z<0 && v1.z<0)) {
            throw new IllegalArgumentException("Vectors must be in opposite hemispheres.");
        }
        if (v0.z == 0) {
            return v0;
        }
        if (v1.z == 0) {
            return v1;
        }
        
        // We're now guaranteed that v0 and v1 are non-horizontal and
        // in opposite hemispheres.
        
        final Vec3 v = v0.times(signum(v0.z) / v0.z).
                plus(v1.times(signum(v1.z) / v1.z)).
                normalize();
        
        assert(v.isWellFormed());
        return v;
    }

    /** Returns true if and only if the supplied vector is in the same (upper/lower)
     * hemisphere as this one.
     * 
     * Currently, the dividing place between the hemispheres (i.e. the plane
     * defined by z=0) is treated as distinct from either hemisphere: that is,
     * v1.sameHemisphere(v2) is true for any two points in this plane,
     * but false if either point is anywhere outside this plane.
     * 
     * @param v a vector
     * @return {@code true} if and only if the supplied vector is in the same (upper/lower)
     * hemisphere as this one.
     */
    public boolean sameHemisphere(Vec3 v) {
        assert(v.isWellFormed());
        return signum(z) == signum(v.z);
    }

    /**
     * Given a list of points, return a list of lists containing the same points
     * plus possible extras. Each sublist of the returned list is guaranteed
     * only to contain points in one hemisphere. Where the original list
     * crosses the equator, an extra point is interpolated exactly on the
     * equator.
     *
     * @param vs a list of vectors specifying directions
     * @return a list of lists of vectors which in sequence define the same path as
     * {@code vs}; none of the sub-lists crosses the equator
     */
    public static List<List<Vec3>> interpolateEquatorPoints(List<Vec3> vs) {
        final List<List<Vec3>> result = new ArrayList<>();
        List<Vec3> currentSegment = new ArrayList<>();
        Vec3 prev = null;
        for (Vec3 v: vs) {
            assert(v.isWellFormed());
            if (prev == null) {
                currentSegment.add(v);
            } else {
                if (prev.sameHemisphere(v)) {
                    currentSegment.add(v);
                } else {
                    final Vec3 between = Vec3.equatorPoint(prev, v);
                    currentSegment.add(between);
                    result.add(currentSegment);
                    currentSegment = new ArrayList<>();
                    currentSegment.add(between);
                    currentSegment.add(v);
                }
            }
            prev = v;
        }
        result.add(currentSegment);
        return result;
    }

    /** Given two vectors, interpolates unit vectors along a great circle.
     * Uses Shoemake's Slerp algorithm.
     * @param v0 a non-zero, well-formed vector
     * @param v1 a non-zero, well-formed vector
     * @param stepSize the step size for interpolation in radians
     * @return a set of vectors describing a great-circle path between
     * {@code v0} and {@code v1}
     */
    public static List<Vec3> spherInterpolate(Vec3 v0, Vec3 v1, double stepSize) {
        // TODO change these asserts to checks throwing IllegalArgumentExceptions
        assert(v0.isWellFormed());
        assert(v1.isWellFormed());
        assert(!Vec3.ORIGIN.equals(v0));
        assert(!Vec3.ORIGIN.equals(v1));
        
        final Vec3 v0n = v0.normalize();
        final Vec3 v1n = v1.normalize();
        double dotProduct = v0n.dot(v1n);
        /* Floating-point rounding errors can produce "normalized" vectors
         * with a length slightly greater than 1, and in very unlucky cases
         * a dot product of two normalized vectors which is >1. This
         * then produces a NaN for the arc cosine, which in turn can
         * cause Graphics2D.draw(Path2D) to hang if it propagates into
         * a Path2D. (Perhaps related to
         * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4587651 ,
         * but that one reports a crash rather than a hang.) In any case we need
         * to do the check below to guard against this very rare case.
         */
        if (dotProduct > 1) dotProduct = 1;
        if (dotProduct < -1) dotProduct = -1;
        
        final double omega = Math.acos(dotProduct);
        // TODO fix this for equator-crossing case?
        if (omega < stepSize) return Arrays.asList(new Vec3[] {v0n, v1n});
        final int steps = (int) (omega / stepSize) + 1;
        final List<Vec3> result = new ArrayList<>(steps+1);
        Vec3 prevVec = null;

        for (int i=0; i<steps; i++) {
            final double t = (double) i / (double) (steps-1);
            final double scale0 = (sin((1.0-t)*omega)) / sin(omega);
            final double scale1 = sin(t*omega) / sin(omega);
            final Vec3 thisVec = v0n.times(scale0).plus(v1n.times(scale1));
            //There's now a separate routine for interpolating equator points.
            //if (i>0 && !thisVec.sameHemisphere(prevVec)) {
            // result.add(equatorPoint(prevVec, thisVec));
            //}
            assert(thisVec.isWellFormed());
            result.add(thisVec);
            prevVec = thisVec;
        }
        
        // I think that adding v1n is actually duplicating the last point
        // but don't have time right now to do the due diligence around removing it.
        // TODO: update unit test to catch duplicated last point,
        // remove "result.add(v1n)", and check that nothing broke.
        result.add(v1n);
        return result;
    }

    /**
     * Interpolates a great-circle path in a chosen direction between two
     * specified vectors. Of the two possible arcs, the result will be the 
     * arc which passes closer to {@code onPath}.
     * @param v0 a vector
     * @param v1 a vector
     * @param onPath arc direction indicator
     * @param stepSize size of interpolation step in radians
     * @return a list of points defining a great-circle arc between {@code v0}
     * and {@code v1}, passing as close as possible to {@code onPath}
     * 
     */
    public static List<Vec3> spherInterpDir(Vec3 v0, Vec3 v1, Vec3 onPath,
            double stepSize) {
        assert(v0.isWellFormed());
        assert(v1.isWellFormed());
        assert(onPath.isWellFormed());
        final Vec3 avgDir = v0.plus(v1).normalize();
        if (avgDir.dot(onPath) > 0) {
            return spherInterpolate(v0, v1, stepSize);
        } else {
            final List<Vec3> a = spherInterpolate(v0, v1.invert(), stepSize);
            final List<Vec3> b = spherInterpolate(v1.invert(), v0.invert(), stepSize);
            final List<Vec3> c = spherInterpolate(v0.invert(), v1, stepSize);
            final List<Vec3> result = new ArrayList<>(a.size() + b.size() + c.size());
            result.addAll(a);
            result.addAll(b);
            result.addAll(c);
            return result;
        }
    }

    /** Rotates this vector about the y axis.
     * @param angle an angle in radians
     * @return a new vector equal to this vector rotated {@code angle} radians about the y axis
     */
    public Vec3 rotY(double angle) {
        final double[][] m =
         {{  cos(angle),  0, sin(angle) },
          {           0,  1,          0 },
          { -sin(angle),  0, cos(angle) }};
        return transform(m);
    }
    
    /** Rotates this vector about the z axis.
     * @param angle an angle in radians
     * @return a new vector equal to this vector rotated {@code angle} radians about the z axis
     */
    public Vec3 rotZ(double angle) {
        final double[][] m =
         {{ cos(angle), -sin(angle),  0 },
         {  sin(angle),  cos(angle),  0 },
         {           0,           0,  1 }};
        return transform(m);
    }

    /** Returns a matrix to correct a vector for a given sample orientation. 
     * @param az the sample dip azimuth in radians
     * @param dip the sample dip angle in radians
     * @return a matrix to transform vectors from sample co-ordinates to
     * geographic co-ordinates
     */
    public static double[][] getSampleCorrectionMatrix(double az, double dip) {
        final double[][] matrix =
        {{ sin(dip)*cos(az) , -sin(az) , cos(dip)*cos(az)  },
         { sin(dip)*sin(az) ,  cos(az) , cos(dip)*sin(az)  },
         { -cos(dip)        , 0        , sin(dip)          }};
        return matrix;
    }

    /** Applies a sample correction to this vector.
     * This transforms the data from sample (laboratory) co-ordinates to 
     * geographic (field) co-ordinates.
     * @param az the sample dip azimuth in radians
     * @param dip the sample dip angle in radians
     * @return this vector, transformed from sample co-ordinates to geographic co-ordinates
     */
    public Vec3 correctSample(double az, double dip) {
        return transform(Vec3.getSampleCorrectionMatrix(az, dip));
    }
    
    /** Applies a sample correction to this vector.
     * This transforms the data from geographic (field) co-ordinates to 
     * tectonic (pre-tilting) co-ordinates.
     * @param az the formation dip azimuth in radians
     * @param dip the formation dip angle in radians
     * @return this vector, transformed from geographic co-ordinates to 
     * tectonic co-ordinates
     */
    public Vec3 correctForm(double az, double dip) {
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
        return transform(Vec3.getFormationCorrectionMatrix(az, dip));
    }

    /*
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

    /** Returns a matrix to correct a vector for a given formation orientation.
     * @param az the formation dip azimuth in radians
     * @param dip the formation dip angle in radians
     * @return a matrix to transform vectors from geographic co-ordinates to
     * tectonic co-ordinates
     */
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
        final List<Vec3> points = new ArrayList<>(n+1);
        for (int i=0; i<n; i++) {
            points.add(correctTilt(Vec3.fromPolarRadians(1, 0, 2*PI*i/n)));
        }
        if (closed) points.add(points.get(0));
        return points;
    }

    /** Multiplies this vector by a supplied matrix. 
     * @param matrix a three-by-three matrix
     * @return the product of this vector and the supplied matrix */
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

    /** Normalizes this vector. 
     * @return a unit vector with the same direction as this vector */
    public Vec3 normalize() {
        final double m = mag();
        return new Vec3(x / m, y / m, z / m);
    }

    /** Returns a specified component of this vector. 
     * @param component the component to return
     * @return the value of the specified component
     */
    public double getComponent(MeasurementAxis component) {
        switch (component) {
            case X: return x;
            case Y: return y;
            case Z: return z;
            case MINUSX: return -x;
            case MINUSY: return -y;
            case MINUSZ: return -z;
            case H: return sqrt(x*x+y*y);
            /* We don't expect that the default case will ever be reached,
             * but it's required to keep the compiler happy and may be
             * useful if MeasurementAxis is later expanded.
             */
            default: throw new IllegalArgumentException("invalid axis "+component);
        }
    }

    /** Returns the magnitude of this vector.
     * @return the magnitude of this vector */
    public double mag() {
        return sqrt(x * x + y * y + z * z);
    }

    /** Adds this vector and another vector.
     * @param v a vector
     * @return a vector equal to the sum of this vector and {@code v} */
    public Vec3 plus(Vec3 v) {
        return new Vec3(x + v.x, y + v.y, z + v.z);
    }

    /** Subtracts another vector from this vector.
     * @param v a vector
     * @return a vector equal to this vector minus {@code v} */
    public Vec3 minus(Vec3 v) {
        return new Vec3(x - v.x, y - v.y, z - v.z);
    }

    /** Multiplies this vector by a scalar value. 
     * @param a a number
     * @return a vector equal to this vector multiplied by {@code a} */
    public Vec3 times(double a) {
        return new Vec3(x * a, y * a, z * a);
    }

    /** Multiplies the components of this vector individually
     * by the corresponding components of another vector.
     * Note that this is neither the dot product nor the cross product, and
     * is seldom used.
     * @param v a vector
     * @return a vector with the elements {@code (this.x*v.x,
     * this.y*v.y, this.z*v.z)}
     */
    public Vec3 times(Vec3 v) {
        return new Vec3(x * v.x, y * v.y, z * v.z);
    }

    /** Divides this vector by a scalar value.
     * @param a a number
     * @return a vector equal to this vector divided by {@code a}
     */
    public Vec3 divideBy(double a) {
        return new Vec3(x / a, y / a, z / a);
    }

    /** Divides the components of this vector individually
     * by the corresponding components of another vector.
     * @param v a vector
     * @return a vector with the elements {@code (this.x/v.x,
     * this.y/v.y, this.z/v.z)}
     */
    public Vec3 divideBy(Vec3 v) {
        return new Vec3(x / v.x, y / v.y, z / v.z);
    }

    /** Returns the dot product of this vector and another vector. 
     * @param v a vector
     * @return the dot product of this vector and {@code v}
     */
    public double dot(Vec3 v) {
        return (x * v.x + y * v.y + z * v.z);
    }

    /** Returns the cross product of this vector and another vector. 
     * @param v a vector
     * @return the cross product of this vector and {@code v}
     */
    public Vec3 cross(Vec3 v) {
        return new Vec3(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x);
    }

    /** Returns the inverse of this vector. This is produced by
     * negating each component.
     * @return the inverse of this vector
     */
    public Vec3 invert() {
        return new Vec3(-x, -y, -z);
    }

    /** Returns the orientation tensor of this vector.
     * The orientation tensor is defined in 
     * Scheidegger (1965).
     * @return the orientation tensor of this vector
     */
    public Matrix oTensor() {
        return new Matrix(new double[][]{{x * x, x * y, x * z},
            {y * x, y * y, y * z},
            {z * x, z * y, z * z}});
    }

    /** Returns the angle between this vector and another vector. 
     * @param v a vector
     * @return the angle between this vector and {@code v}
     */
    public double angleTo(Vec3 v) {
        // Uses a cross product to get a signed angle.
        final Vec3 cross = cross(v);
        double sign = cross.z;
        if (sign == 0) sign = cross.y;
        if (sign == 0) sign = cross.x;
        return asin(cross.mag()) * signum(sign);
    }

    /** Creates a vector from a polar specification in degrees.
     * @param mag magnitude for the new vector
     * @param inc inclination for the new vector, in degrees
     * @param dec declination for the new vector, in degrees
     * @return the specified vector
     */
    public static Vec3 fromPolarDegrees(double mag, double inc, double dec) {
        return Vec3.fromPolarRadians(mag, toRadians(inc), toRadians(dec));
    }

    /** Creates a vector from a polar specification in radians.
     * @param mag magnitude for the new vector
     * @param inc inclination for the new vector, in radians
     * @param dec declination for the new vector, in radians
     * @return the specified vector
     */
    public static Vec3 fromPolarRadians(double mag, double inc, double dec) {
        return new Vec3(mag * cos(inc) * cos(dec),
                mag * cos(inc) * sin(dec),
                mag * sin(inc));
    }

    /** Returns this vector's inclination in radians.
     * @return this vector's inclination in radians */
    public double getIncRad() {
        return atan2(z, sqrt(x*x + y*y));
    }

    /**
     * <p>Returns this vector's declination in radians.</p>
     * 
     * @return this vector's declination in radians
     */
    public double getDecRad() {
    /* <p>Note:
     * The 2G manual specifies the following conversion, more or less:
     * 
     *  <pre>{@code 
     *  if (x < 0) return atan(y / x) + PI;
     *  else if (x > 0 && y <= 0) return atan(y / x) + 2 * PI;
     *  else if (x == 0) {
     *      if (y > 0) return PI / 2;
     *      else if (y < 0) return -PI / 2;
     *      else return 0; // arbitrary
     *  } else {
     *      return atan(y / x);
     *  }
     * }</pre>
     *
     * However, the formulae given in the manual are known to be unreliable.
     * We use the more straightforwardly correct {@code atan2}, and shift the 
     * range from [-pi, pi] to [0, 2pi].</p>
     */
        double theta = atan2(y, x);
        if (theta<0) theta += 2*PI;
        return theta;
    }

    /** Returns this vector's inclination in degrees.
     * @return this vector's inclination in degrees */
    public double getIncDeg() {
        return toDegrees(getIncRad());
    }

    /** Returns this vector's declination in degrees.
     * @return this vector's declination in degrees */
    public double getDecDeg() {
        return toDegrees(getDecRad());
    }
    
    /** Returns the strike of the plane normal this vector
     * @return the strike of the plane normal this vector, in degrees */
    public double getStrikeDeg() {
        double decDeg = getDecDeg();
        // Ensure we have the declination of the *upward* vector 
        if (getIncDeg() > 0) decDeg += 180;
        double strike = decDeg - 90;
        while (strike < 0) { strike += 360; }
        while (strike > 360) { strike -= 360; }
        return strike;
    }

    /** Returns the strike of the plane normal this vector
     * @return the strike of the plane normal this vector, in degrees */
    public double getDipDeg() {
        double incDeg = getIncDeg();
        // Ensure we have an upward (negative) inclination
        if (incDeg > 0) incDeg = -incDeg;
        return incDeg + 90;
    }

    /** Returns the sum of a specified collection of vectors. 
     * @param vectors a collections of vectors
     * @return the sum of the supplied vectors */
    public static Vec3 sum(Collection<Vec3> vectors) {
        double xs = 0, ys = 0, zs = 0;
        for (Vec3 p: vectors) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
        }
        return new Vec3(xs, ys, zs);
    }

    /** Returns the mean direction of a collection of unit vectors.
     * 
     * @param points a collection of unit vectors
     * @return a unit vector representing the mean direction of {@code points}
     */
    public static Vec3 meanDirection(Collection<Vec3> points) {
        return sum(points).normalize();
    }
    
    
    /** Returns the mean of a collection of vectors. 
     * @param vectors a collections of vectors
     * @return the mean vector of the supplied vectors
     */
    public static Vec3 mean(Collection<Vec3> vectors) {
        double xs = 0, ys = 0, zs = 0;
        int i = 0;
        for (Vec3 p : vectors) {
            xs += p.x;
            ys += p.y;
            zs += p.z;
            i++;
        }
        return new Vec3(xs / i, ys / i, zs / i);
    }

    /** Sets the x component of this vector.
     * @param newX the new value for the x component
     * @return a vector with the specified x component, and with 
     * the other components taken from this vector */
    public Vec3 setX(double newX) {
        return new Vec3(newX, y, z);
    }

    /** Sets the y component of this vector.
     * @param newY the new value for the y component
     * @return a vector with the specified y component, and with 
     * the other components taken from this vector */
    public Vec3 setY(double newY) {
        return new Vec3(x, newY, z);
    }
    
    /** Sets the z component of this vector.
     * @param newZ the new value for the z component
     * @return a vector with the specified z component, and with 
     * the other components taken from this vector */
    public Vec3 setZ(double newZ) {
        return new Vec3(x, y, newZ);
    }

    /** Returns a list of vectors defining a small circle around this vector's direction. 
     * @param radiusDegrees the radius of the desired circle, in degrees
     * @return a list of vectors defining a small circle around this vector's direction
     */
    public List<Vec3> makeSmallCircle(double radiusDegrees) {
        final List<Vec3> result = new ArrayList<>();
        for (double dec = 0; dec < 360; dec += 5) {
            final Vec3 v1 = Vec3.fromPolarDegrees(1, 90 - radiusDegrees, dec);
            final Vec3 v2 = v1.rotY(Math.PI / 2 - getIncRad());
            final Vec3 v3 = v2.rotZ(getDecRad());
            assert(v3.isWellFormed());
            result.add(v3);
        }
        result.add(result.get(0));
        return result;
    }

    /** Returns a list of points outlining the confidence ellipse for
     * a supplied set of Kent statistical parameters.
     * @param kentParams a set of Kent parameters
     * @return the confidence ellipse of the supplied parameters
     */
    public static List<Vec3> makeEllipse(KentParams kentParams) {
        final double eta = kentParams.getEtaMag();
        final double zeta = kentParams.getZetaMag();
        final List<Vec3> vs = new ArrayList<>(1000);
        // We create each ellipse point at the top (?) of the unit
        // sphere, then rotate it into position.

        // ed and ei tell us which way the ellipse is pointing.
        // (zd and zi are orthogonal so we'll ignore them.)
        final Vec3 centre = kentParams.getMean();
        final Vec3 etaDir = kentParams.getEtaDir();
        // calculate the direction of the eta axis
        final Vec3 etaDirTop =
                etaDir.rotZ(-centre.getDecRad()).rotY(centre.getIncRad()-PI/2.).
                rotZ(centre.getDecRad());

        final double stepSize = 1e-4; // unprojected minimum step size (radians)
        final double stepLimit = 1e-2; // skip projected steps smaller than this...
        final double thetaLimit = 2*PI/50.; // ... unless unprojected step larger than this
        Vec3 vPrev = null; // the last projected vector we created
        double thetaPrev = -100; // the last theta at which we created a vector

        for (double theta=0; theta<2*PI; theta += stepSize) {
            final double a = eta*sin(theta);
            final double b = zeta*cos(theta);
            final double r = eta*zeta/sqrt(a*a+b*b);
            // Calculate a point centred around zero.
            final Vec3 vUnrot = Vec3.fromPolarRadians(1., 0.5*PI-r,
                    theta + etaDirTop.getDecRad());

            // Check whether to skip this point.
            if (vPrev==null || abs(vPrev.angleTo(vUnrot)) > stepLimit ||
                        (theta - thetaPrev) > thetaLimit) {
                // Rotate the point into position.
                Vec3 vRotated = vUnrot.rotZ(-centre.getDecRad()).
                        rotY(PI / 2. - centre.getIncRad()).
                        rotZ(centre.getDecRad());
                vs.add(vRotated);
                vPrev = vUnrot;
                thetaPrev = theta;
            }
        }
        vs.add(vs.get(0)); // close the path
        return vs;
    }

    String toCustomString(String pre, String post, String sep, int dp,
            boolean scale) {
        // TODO tidy up: this code's a mess.
        final String format = String.format(Locale.ENGLISH,
                "%s%%.%df%s%%.%df%s%%.%df%s",
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
        String result = String.format(Locale.ENGLISH,
                format, vec.x, vec.y, vec.z);
        if (scale) {
            result += String.format(Locale.ENGLISH, "e%d", oom);
        }
        return result;
    }

    /** Returns a string representation of this vector. 
     * @return a string representation of this vector */
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%.2f %.2f %.2f", x, y, z);
    }
    
    /** Checks that this vector contains no NaN or infinite values.
     *
     * @return true iff this vector contains no NaN or infinite values
     */
    public boolean isWellFormed() {
        return (!Double.isNaN(x)) && (!Double.isNaN(y)) && (!Double.isNaN(z))
                && (!Double.isInfinite(x)) && (!Double.isInfinite(y))
                && (!Double.isInfinite(z));
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Vec3)) {
            return false;
        }
        final Vec3 v = (Vec3) o;
        return (x == v.x && y == v.y && z == v.z);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.x) ^ (Double.doubleToLongBits(this.x) >>> 32));
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.y) ^ (Double.doubleToLongBits(this.y) >>> 32));
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.z) ^ (Double.doubleToLongBits(this.z) >>> 32));
        return hash;
    }
    
    /**
     * Returns the distance between two vectors.
     * 
     * @param v the vector with which to compare this one
     * @return the distance between this vector and {@code v}
     */
    public double distance(Vec3 v) {
        return abs(this.minus(v).mag());
    }
    
    /**
     * Compares vectors for equality to a specified precision.
     * 
     * Compares this vector with another one, returning {@code true}
     * if they are sufficiently close. "Sufficiently close" is defined
     * by a supplied precision parameter: for the method to return
     * {@code true}, the distance between the vectors must be less
     * than {@code precision * max(m1, m2)} where {@code m1} and
     * {@code m2} are the lengths of the two vectors.
     * 
     * @param v the vector with which to compare this one
     * @param precision the precision parameter for the comparison
     * @return {@code true} iff the vectors are equal to within the
     * specified precision
     */
    public boolean equals(Vec3 v, double precision) {
        final double mag0 = mag();
        final double mag1 = v.mag();
        if (mag0==0 && mag1==0) {
            return true;
        }
        final double delta = Double.max(mag0, mag1) * precision;
        return (distance(v) < delta);
    }
}
