/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
     * Rotates the vector by180 degrees about the specified axis.
     * Since x is up in magnetometer co-ordinates, specifying the x axis 
     * corrects the data for a specimen placed in the magnetometer back-to-front.
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
     * and the great circle between the supplied points.
     * @param v0 a vector specifying a direction
     * @param v1 a vector specifying a direction
     * @return a unit vector {@code v} for which {@code v.z==0}, which lies
     * on the shortest great-circle path between the normalizations of
     * {@code v0} and {@code v1}
     */
    public static Vec3 equatorPoint(Vec3 v0, Vec3 v1) {
        return v0.times(signum(v0.z) / v0.z).
                plus(v1.times(signum(v1.z) / v1.z)).
                normalize();
    }

    /** Returns true if and only if the supplied vector is in the same (upper/lower)
     * hemisphere as this one.
     * @param v a vector
     * @return {@code true} if and only if the supplied vector is in the same (upper/lower)
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
     * @param vs a list of vectors specifying directions
     * @return a list of lists of vectors which in sequence define the same path as
     * {@code vs}; none of the sub-lists crosses the equator
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

    /** Given two vectors, interpolates unit vectors along a great circle.
     * Uses Shoemake's Slerp algorithm.
     * @param v0 a vector
     * @param v1 a vector
     * @param stepSize the step size for interpolation in radians
     * @return a set of vectors describing a great-circle path between
     * {@code v0} and {@code v1}
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
     * Interpolates a great-circle path in a chosen direction between two
     * specified vectors. Of the two possible arcs, the result will be the 
     * arc which passes closer to {@code onPath}.
     * @param v0 a vector
     * @param v1 a vector
     * @param onPath arc direction indicator
     * @param stepSize
     * @return a list of points defining a great-circle arc between {@code v0}
     * and {@code v1}, passing as close as possible to {@code onPath}
     * 
     */
    public static List<Vec3> spherInterpDir(Vec3 v0, Vec3 v1, Vec3 onPath,
            double stepSize) {
        final Vec3 avgDir = v0.plus(v1).normalize();
        if (avgDir.dot(onPath) > 0) {
            return spherInterpolate(v0, v1, stepSize);
        } else {
            List<Vec3> a = spherInterpolate(v0, v1.invert(), stepSize);
            List<Vec3> b = spherInterpolate(v1.invert(), v0.invert(), stepSize);
            List<Vec3> c = spherInterpolate(v0.invert(), v1, stepSize);
            List<Vec3> result = new ArrayList<Vec3>(a.size() + b.size() + c.size());
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
         {{cos(angle), 0, sin(angle)},
          {0, 1, 0},
          { -sin(angle), 0, cos(angle)}};
        return transform(m);
    }
    
    /** Rotates this vector about the z axis.
     * @param angle an angle in radians
     * @return a new vector equal to this vector rotated {@code angle} radians about the z axis
     */
    public Vec3 rotZ(double angle) {
        final double[][] m =
         {{ cos(angle), -sin(angle), 0 },
         { sin(angle),  cos(angle), 0 },
         { 0      ,  0      , 1 }};
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
        final List<Vec3> points = new ArrayList<Vec3>(n+1);
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
            case MINUSZ: return -z;
            case H: return sqrt(x*x+y*y);
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
     * Note that this is neither the dot nor cross product, and
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
        Vec3 cross = cross(v);
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
        List<Vec3> result = new ArrayList<Vec3>();
        for (double dec = 0; dec < 360; dec += 5) {
            final Vec3 v1 = Vec3.fromPolarDegrees(1, 90 - radiusDegrees, dec);
            final Vec3 v2 = v1.rotY(Math.PI / 2 - getIncRad());
            final Vec3 v3 = v2.rotZ(getDecRad());
            result.add(v3);
        }
        result.add(result.get(0));
        return result;
    }

    // TODO this has much in common with the makeEllipse(KentParams).
    // if it's useful to make this method public and keep them
    // both, the common code should be factored out.
    private static List<Vec3> makeEllipse(double dec, double inc,
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

    /** Returns a list of points outlining the confidence ellipse for
     * a supplied set of Kent statistical parameters.
     * @param kentParams a set of Kent parameters
     * @return the confidence ellipse of the supplied parameters
     */
    public static List<Vec3> makeEllipse(KentParams kentParams) {
        double eta = kentParams.getEtaMag();
        // if (eta>PI/2) eta = PI/2+0.1;
        final double zeta = kentParams.getZetaMag();
        // final int N = (int) (1000*sqrt(1+eta*eta+zeta*zeta));
        List<Vec3> vs = new ArrayList<Vec3>(1000);
        // we're going to draw the ellipse at the bottom of the unit
        // sphere, then rotate it down into position.

        // ed and ei tell us which way the ellipse is pointing.
        // (zd and zi are orthogonal so we'll ignore them.)
        Vec3 centre = kentParams.getMean();
        Vec3 etaDir = kentParams.getEtaDir();
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

    /** Returns a string representation of this vector. 
     * @return a string representation of this vector */
    @Override
    public String toString() {
        return String.format("%.2f %.2f %.2f", x, y, z);
    }
}
