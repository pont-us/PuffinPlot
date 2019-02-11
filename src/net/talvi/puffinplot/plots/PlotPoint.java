/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;

import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.TreatmentStep;

/**
 * PlotPoint represents a point on a PuffinPlot Plot. A "point" in this context
 * refers to any graphical element associated with a treatment step or sample;
 * for example, it may be a small shape used to indicate a precise position on a
 * plot, or a line of text giving information about a sample or step.
 *
 * @see Plot
 *
 * @author pont
 */
public interface PlotPoint {

    /**
     * Draws this point in a supplied graphics context.
     * 
     * @param graphics the graphics context in which to draw
     */
    void draw(Graphics2D graphics);

    /**
     * Draws this point in a specified graphics context, possibly drawing
     * a line from a supplied previous point to this one. Whether the line
     * is actually drawn is dependent on the implementing class and on the state
     * of the object implementing the method.
     * 
     * @param graphics the graphics context in which to draw
     * @param prev the previous point from which to draw the line to this one
     * @param annotate if {@code true}, annotate the point textually with
     *   implementation-dependent additional information
     */
    void drawWithPossibleLine(Graphics2D graphics, PlotPoint prev,
            boolean annotate);

    /**
     * @return the treatment step associated with this point, or {@code null}
     * if there is no associated treatment step
     */
    TreatmentStep getTreatmentStep();
    
    /**
     * @return the sample associated with this point, or {@code null}
     * if there is no associated sample
     */
    Sample getSample();

    /**
     * @return the shape of this point
     */
    Shape getShape();

    /**
     * @return the position of the centre of this point
     */
    Point2D getCentre();

    /**
     * Report whether this point is near to a supplied {@code Point2D}. The
     * exact behaviour is implementation-dependent. If this point represents a
     * precise position on a plot, {@code distance} indicates the maximum
     * distance which should be considered "near". If the point consists of a
     * larger area (e.g. a line of text) whose exact position does not represent
     * a property of the associated sample or step, the {@code distance}
     * argument may be ignored and the method may return {@code true} only if
     * the supplied {@code Point2D} lies within this {@code PlotPoint}'s extent.
     *
     * @param point the point whose nearness to this point should be determined
     * @param distance the maximum distance at which the two points should
     *        be considered "near"
     * @return {@code true} if and only if the supplied point is near to this
     *         point
     */
    boolean isNear(Point2D point, double distance);
}
