/** A list of unit vectors approximating a great-circle path.
 */

package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class GreatCircle {

    private final static Logger logger =
            Logger.getLogger(GreatCircle.class.getName());

    private final List<Vec3> points;
    private final Vec3 pole;
    private final double direction;
    // need a way to represent sector constraints here...

    public GreatCircle(List<Vec3> points) {
        List<Vec3> pointsUnscaled = points;
        points = this.points = new ArrayList(points.size());
        for (Vec3 p: pointsUnscaled) {
            this.points.add(p.normalize());
        }
        this.pole = new Eigens(points, true).vectors.get(2).normalize();
        // Calculate the direction of the point trend along the circle
        // (clockwise / anticlockwise). Can't be sure that all points
        // will be strictly in the right direction, so we'll add up
        // the offsets of all the point pairs and take the sign of the sum.
        // (Can't just take angle between first and last point, since
        // the path might over more than 180 degrees.)
        double total = 0;
        for (int i=1; i<points.size(); i++) {
            total += nearestOnCircle(points.get(i-1)).
                    angleTo(nearestOnCircle(points.get(i)));
        }
        direction = Math.signum(total);
        for (Vec3 p: points) {
            logger.info(p.toString() + "   " + angleFromLast(nearestOnCircle(p)));
        }
    }

    public List<Vec3> getPoints() {
        return points;
    }

    public Vec3 getPole() {
        return pole;
    }

    public Vec3 nearestOnCircle(Vec3 point) {
        return pole.nearestOnCircle(point);
    }

    public Vec3 lastPoint() {
        return points.get(points.size()-1);
    }

    /*  Positive is beyond last point; negative is before it.
     */
    public double angleFromLast(Vec3 v) {
        return nearestOnCircle(lastPoint()).angleTo(v)
                * direction;
    }
}
