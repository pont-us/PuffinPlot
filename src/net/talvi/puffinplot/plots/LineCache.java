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
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A container class to collect Path2D objects for cached plotting.
 * This class is designed to work with equal area plots, which frequently
 * need to draw paths that can be computationally expensive to project
 * on-the-fly. An equal-area plot can contain both solid and dashed lines,
 * representing points in the upper and lower hemispheres respectively.
 * This class holds two separate collections of Path2D objects,
 * one of which contains paths to be plotted with solid lines, and the 
 * other paths to be plotted with dashed lines. Collecting the paths in
 * this way makes it more convenient to cache them for re-plotting when
 * the data has not changed.
 * 
 * @author pont
 */
public class LineCache {
        
    private final List<Path2D> solidPaths;
    private final List<Path2D> dashedPaths;
    private final Stroke solidStroke;
    private final Stroke dashedStroke;
    
    /**
     * Creates a new LineCache.
     * 
     * @param solidStroke the Stroke to use for drawing solid lines
     * @param dashedStroke the Stroke to use for drawing dashed lines
     */
    public LineCache(Stroke solidStroke, Stroke dashedStroke) {
        this.solidStroke = solidStroke;
        this.dashedStroke = dashedStroke;
        solidPaths = new ArrayList<>();
        dashedPaths = new ArrayList<>();
    }
    
    /**
     * Adds a path to this cache.
     * 
     * @param path the path to add
     * @param solid {@code true} to add to the solid paths, {@code false} to add to the dashed paths
     */
    public void addPath(Path2D path, boolean solid) {
        (solid ? solidPaths : dashedPaths).add(path);
    }
    
    /**
     * Draws the paths in this cache.
     * 
     * @param g the graphics context in which to draw the paths
     */
    public void draw(Graphics2D g) {
        g.setStroke(solidStroke);
        for (Path2D path: solidPaths) {
            g.draw(path);
        }
        g.setStroke(dashedStroke);
        for (Path2D path: dashedPaths) {
            g.draw(path);
        }
    }
}
