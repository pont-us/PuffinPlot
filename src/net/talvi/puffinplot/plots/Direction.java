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

package net.talvi.puffinplot.plots;

import java.awt.geom.Point2D;
import static java.lang.Math.abs;
import java.util.Collection;
import java.util.EnumSet;

enum Direction {
    RIGHT("R", "E", 0), UP("U", "N", 1), LEFT("L", "W", 2), DOWN("D", "S", 3);
    private final String letter;
    private final String compassDir;
    private final int position;
    private static final Direction[] ordering = new Direction[4];
    static {
        for (Direction d : values())
            ordering[d.position] = d;
    }

    private Direction(String letter, String compassDir, int position) {
        this.letter = letter;
        this.compassDir = compassDir;
        this.position = position;
    }

    boolean isHorizontal() {
        return (position % 2) == 0;
    }

    Direction labelPos(boolean farSide) {
        final Direction d = this.isHorizontal() ? DOWN : LEFT;
        return farSide ? d.opposite() : d;
    }

    Direction rotAcw90() {
        return ordering[(position + 1) % 4];
    }

    public Direction opposite() {
        return ordering[(position + 2) % 4];
    }

    double labelRot() {
        return this.isHorizontal() ? 0 : -Math.PI / 2;
    }

    public String getCompassDir() {
        return compassDir;
    }

    public String getLetter() {
        return letter;
    }
    
    /**
     * Given a "central" point and a collection of "peripheral"
     * points, safeDirection attempts to find a quadrant containing
     * no peripheral points. It is intended for use with label placement:
     * if safeDirection is called with a point's two (joined) neighbours
     * as the "others", it will return a direction where a label can
     * be placed without overlapping the line. While this method can
     * be called with any number of "others", a good solution cannot be
     * guaranteed when more than two other points are provided.
     * 
     * @param centre a central point
     * @param others other points
     * @return the direction of a quadrant (as seen from the central point)
     * which, if possible, contains none of the other points
     */
    
    public static Direction safeDirection(Point2D centre,
            Collection<Point2D> others) {
        // Start with full sets of directions and pare them off when
        // we find points in their quadrants.
        // The directions in smallSet represent vacant hemiplanes, not
        // vacant quadrants. If others.size()>1, smallSet may end up empty,
        // but if it is non-empty it should contain an "optimal" vacant
        // direction.
        final EnumSet<Direction> smallSet = EnumSet.allOf(Direction.class);
        // bigSet is the fallback, used when smallSet is empty.
        // It represents vacant quadrants.
        final EnumSet<Direction> bigSet = EnumSet.allOf(Direction.class);
        
        for (Point2D p: others) {
            final double x = p.getX() - centre.getX();
            final double y = p.getY() - centre.getY();
            // hz records whether the h-offset is greater than the v-offset.
            // If it is, we know that p must lie in the L or R quadrant.
            // If not, it must lie in the T or B quadrant.
            boolean hz = (abs(x) > abs(y));
            if (x < 0) {
                smallSet.remove(LEFT);
                if (hz) bigSet.remove(LEFT);
            } else {
                smallSet.remove(RIGHT);
                if (hz) bigSet.remove(RIGHT);
            }
            if (y < 0) {
                smallSet.remove(UP);
                if (!hz) bigSet.remove(UP);
            } else {
                smallSet.remove(DOWN);
                if (!hz) bigSet.remove(DOWN);
            }
        }
        
        if (!smallSet.isEmpty()) {
            // Return direction of a vacant hemiplane if possible.
            return smallSet.iterator().next();
        } else if (!bigSet.isEmpty()) {
            // If not hemiplane, return a vacant quadrant
            return bigSet.iterator().next();
        } else {
            // If no good solution exists, just put the label to the
            // right of the point.
            return RIGHT;
        }
    }
    
}
