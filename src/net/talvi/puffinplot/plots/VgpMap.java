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
import net.talvi.puffinplot.data.Sample;
import net.talvi.puffinplot.data.Site;
import net.talvi.puffinplot.data.Suite;
import net.talvi.puffinplot.data.VGP;

/**
 * A plot which shows VGP positions on a world map.
 */
public class VgpMap extends Plot {

    private final List<List<Vec3>> outlines;
    
    public VgpMap(PlotParams params) {
        super(params);
        try {
            outlines = readOutlines();
        } catch (IOException e) {
            throw new Error(e);
        }
    }
    
    private static List<List<Vec3>> readOutlines() throws IOException {
        final List<List<Vec3>> outlines = new ArrayList<>();
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
            final List<Vec3> outline = new ArrayList<>();
            outlines.add(outline);
            for (int i = 0; i < parts.length; i += 2) {
                final Vec3 v = Vec3.fromPolarDegrees(1,
                        Double.parseDouble(parts[i+1]),
                        Double.parseDouble(parts[i]));
                outline.add(v);
            }
            }
        }
        return outlines;
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
        final double xo = dims.getMinX();
        final double yo = dims.getMaxY();
        final double w = dims.getWidth();
        final double h = dims.getHeight();
        final double R = min(w / (4 * sqrt(2)), h / (2 * sqrt(2)));
        
        graphics.setColor(Color.GRAY);
        graphics.setStroke(getStroke());
        // graphics.draw(new Rectangle2D.Double(xo, yo-h, w, h));
        graphics.draw(new Ellipse2D.Double(xo + w / 2 - 2 * R * sqrt(2), 
                yo - h / 2 - R * sqrt(2), 4 * R * sqrt(2), 2 * R * sqrt(2)));
        
        for (List<Vec3> outline: outlines) {
            graphics.draw(project(outline));
        }

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
                points.add(ShapePoint.build(this,
                        mollweide(site.getVgp().getLocation().toVec3(),
                                  R, xo + w/2, yo - h/2)).circle().filled(true).build());
            }
        }
        drawPoints(graphics);
        

    }
    
    private Path2D project(List<Vec3> outline) {
        final Rectangle2D dims = getDimensions();
        final double w = dims.getWidth();
        final double h = dims.getHeight();
        final double xo = dims.getMinX() + w / 2;
        final double yo = dims.getMaxY() - h / 2;
        final Path2D.Double path = new Path2D.Double();
        final double R = min(w / (4 * sqrt(2)), h / (2 * sqrt(2)));
        //path.moveTo(xo + outline.get(0).getDecDeg(),
        //        yo - outline.get(0).getIncDeg());
        final Point2D startPoint = mollweide(outline.get(0), R, xo, yo);
        path.moveTo(startPoint.getX(), startPoint.getY());
        Point2D previous = startPoint;
        for (Vec3 v: outline.subList(1, outline.size())) {
            // path.lineTo(xo + v.getDecDeg(), yo - v.getIncDeg());
            final Point2D p = mollweide(v, R, xo, yo);
            if (previous.distance(p) < R / 10) {
                path.lineTo(p.getX(), p.getY());
            } else {
                path.moveTo(p.getX(), p.getY());
            }
            previous = p;
        }
        // path.closePath();
        return path;
    }
    
    private static Point2D mollweide(Vec3 v, double R, double x0, double y0) {
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
        return new Point2D.Double(x0 + x, y0 - y);
    }
    
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
