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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.plots;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.talvi.puffinplot.data.Vec3;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import java.util.stream.Collectors;
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.VGP;

/**
 * A plot which shows VGP positions on a world map.
 */
public class VgpMap extends Plot {

    /**
     * A collection of pre-projected, unscaled coastline shapes.
     * The Mollweide projection is relatively computationally expensive,
     * so it makes sense to cache the results for the background map.
     */
    private final List<List<Point2D>> outlines;
    private final List<List<Point2D>> linesofLongitude;
    
    /**
     * Instantiates a new VGP map.
     * 
     * @param params the plot parameters controlling this plot's content
     */
    public VgpMap(PlotParams params) {
        super(params);
        try {
            outlines = readAndProjectOutlines();
            linesofLongitude = createLinesOfLongitude();
        } catch (IOException e) {
            throw new Error(e);
        }
    }
    
    /**
     * Reads map outlines and return an unscaled, projected version of them.
     * The projection has a radius of 1 and origin of (0, 0).
     * 
     * @return the projected outlines
     * @throws IOException if there was an error reading the map data
     */
    private static List<List<Point2D>> readAndProjectOutlines()
            throws IOException {
        final List<List<Point2D>> outlines = new ArrayList<>();
        try (InputStream stream =
                VgpMap.class.getResourceAsStream("map-data.csv");
                Reader isr = new InputStreamReader(stream);
                BufferedReader reader = new BufferedReader(isr)) {
            while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            final String[] parts = line.split(", ");
            final List<Point2D> outline = new ArrayList<>();
            outlines.add(outline);
            for (int i = 0; i < parts.length; i += 2) {
                final Vec3 v = Vec3.fromPolarDegrees(1,
                        Double.parseDouble(parts[i + 1]),
                        Double.parseDouble(parts[i]));
                final Point2D v2 = project(v, 1, 0, 0);
                outline.add(v2);
            }
            }
        }
        
        return outlines;
    }
    
    /**
     * Precalculate and project lines of longitude.
     * 
     * @return projected, unscaled lines of longitude
     */
    private static List<List<Point2D>> createLinesOfLongitude() {
        final List<List<Point2D>> grid = new ArrayList<>();
        
        grid.add(project(Vec3.DOWN.greatCirclePoints(60, true)));

        for (int longitude = 30; longitude <= 330; longitude += 30) {
            Vec3 v = Vec3.fromPolarDegrees(1, 0, longitude);
            grid.add(project(v.greatCirclePoints(180, true)));
        }
        return grid;
    }

    
    @Override
    public String getName() {
        return "vgpmap";
    }

    @Override
    public String getNiceName() {
        return "VGP map";
    }

    @Override
    public void draw(Graphics2D graphics) {
        clearPoints();
        final Rectangle2D dims = getDimensions();
        final double w = dims.getWidth();
        final double h = dims.getHeight();
        final double xo = dims.getMinX() + w / 2;
        final double yo = dims.getMaxY() - h / 2;
        final double R = min(w / (4 * sqrt(2)), h / (2 * sqrt(2)));
        
        // Draw coastlines.
        
        graphics.setColor(Color.GRAY);
        graphics.setStroke(getStroke());
        final double scale = R * sqrt(2);
        graphics.draw(new Ellipse2D.Double(xo - 2 * scale, 
                yo - scale, 4 * scale, 2 * scale));
        
        outlines.forEach(outline -> graphics.draw(
                outlineToPath(outline, xo, yo, R, false)));

        // Draw lines of longitude.
        
        graphics.setColor(Color.LIGHT_GRAY);
        linesofLongitude.forEach(outline -> graphics.draw(
                outlineToPath(outline, xo, yo, R, true)));

        // Draw lines of latitude.
        
        for (int lat = -60; lat <= 60; lat += 30) {
            graphics.draw(new Line2D.Double(
                    project(Vec3.fromPolarDegrees(1, lat, -179.99), R, xo, yo),
                    project(Vec3.fromPolarDegrees(1, lat, 179.99), R, xo, yo)
            ));
        }
        
        // Draw VGPs (if any).
        
        final Sample sample = params.getSample();
        if (sample == null) {
            return;
        }
        final Suite suite = sample.getSuite();
        if (suite == null) {
            return;
        }
        final List<Site> sites = suite.getSites();

        graphics.setColor(Color.BLACK);
        for (Site site: sites) {
            final VGP vgp = site.getVgp();
            if (vgp != null) {
                points.add(ShapePoint
                        .build(this, project(vgp.getLocation().toVec3(),
                                  R, xo, yo))
                        .circle().filled(true).build());
            }
        }
        drawPoints(graphics);
    }
    
    /**
     * Creates a translated, scaled path from an unscaled list of points.
     * 
     * @param outline points to scale
     * @param xo x origin
     * @param yo y origin
     * @param R radius of plot
     * @param includeLongSegments plot segments even when they have length >0.1
     *        (otherwise they are omitted)
     * @return 
     */
    private static Path2D outlineToPath(List<Point2D> outline,
            double xo, double yo, double R, boolean includeLongSegments) {
        final Path2D.Double path =
                new Path2D.Double(Path2D.WIND_NON_ZERO, outline.size());
        final Point2D startPoint = outline.get(0);
        path.moveTo(startPoint.getX() * R + xo, startPoint.getY() * R + yo);
        Point2D previous = startPoint;
        for (Point2D v: outline.subList(1, outline.size())) {
            if (previous.distance(v) < 0.1 || includeLongSegments) {
                path.lineTo(v.getX() * R + xo, v.getY() * R + yo);
            } else {
                path.moveTo(v.getX() * R + xo, v.getY() * R + yo);
            }
            previous = v;
        }
        return path;
    }
    
    /**
     * Projects a vector using the Mollweide projection.
     * 
     * @param v the vector to project
     * @param R the radius of the projection
     * @param xo the x origin of the projection
     * @param yo the y origin of the projection
     * @return the co-ordinates of the projected vector
     */
    private static Point2D project(Vec3 v, double R, double xo, double yo) {
        double lambda = v.getDecRad();
        if (lambda > PI) {
            lambda -= 2 * PI;
        }
        final double phi = v.getIncRad();
        final double theta = theta(phi);
        final double lambda0 = 0; // central meridian
        final double x =
                R * (2 * sqrt(2) / PI) * (lambda - lambda0) * cos(theta);
        final double y = R * sqrt(2) * sin(theta);
        return new Point2D.Double(xo + x, yo - y);
    }
    
    /**
     * Projects a list of vectors using the Mollweide projection.
     * The projection uses a radius of 1 and origin of (0, 0).
     * 
     * @param vs vectors to project
     * @return projected vectors
     */
    private static List<Point2D> project(List<Vec3> vs) {
        return vs.stream()
                .map(v -> project(v, 1, 0, 0))
                .collect(Collectors.toList());
    }

    /**
     * Calculates a Mollweide θ value for a given latitude.
     * 
     * Given a latitude φ, calculate θ such that
     * 2θ + sin 2θ = π sin φ
     * 
     * @param phi latitude φ in radians
     * @return θ such that 2θ + sin 2θ = π sin φ
     */
    private static double theta(double phi) {
        final double delta = 1e-6;
        if (phi > PI / 2 - delta) {
            return PI / 2;
        }
        if (phi < -PI / 2 + delta) {
            return -PI / 2;
        }
        double theta = phi;
        final double max_iterations = 100;
        for (int i = 0; i < max_iterations; i++) {
            final double nextTheta = theta -
                    (2 * theta + sin(2 * theta) - PI * sin(phi)) /
                    (2 + 2 * cos(2 * theta));
            if (abs(nextTheta - theta) < delta) {
                return nextTheta;
            }
            theta = nextTheta;
        }
        return theta;
    }

}
