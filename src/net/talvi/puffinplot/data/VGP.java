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
package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

/**
 * A class representing a virtual geomagnetic pole.
 * <p>
 * This class can calculate a VGP from a site direction and position, along with
 * the associated confidence region. For details on the calculation technique
 * and on palaeomagnetic poles in general, see Chapter 7 of Robert F. Butler
 * (1992), <i>Paleomagnetism: Magnetic Domains to Geologic Terranes</i>. Oxford:
 * Blackwell Scientific.
 *
 * @author pont
 */
public class VGP {

    private final double dp;
    private final double dm;
    private final Location location;
    
    private static final List<String> HEADERS =
        Arrays.asList("VGP lat (deg)", "VGP long (deg)",
                "VGP dp (deg)", "VGP dm (deg)");
    
    private VGP(Location location, double dp, double dm) {
        this.location = location;
        this.dp = dp;
        this.dm = dm;
    }
    
    static VGP calculate(Vec3 direction, double a95, Location site) {
        final double I_m = direction.getIncRad();
        final double D_m = direction.getDecRad();
        final double λ_s = site.getLatRad();
        final double φ_s = site.getLongRad();
        final double p = atan2(2, tan(I_m));
        final double λ_p = asin(sin(λ_s)*cos(p) + cos(λ_s)*sin(p)*cos(D_m));
        final double β = asin((sin(p)*sin(D_m)) / (cos(λ_p)));
        final double φ_p = (((cos(p) >= sin(λ_s)*sin(λ_p)) ?
                φ_s + β : φ_s + PI - β) + (2*PI)) % (2*PI);
        final double dp = a95 * ((1+3*cos(p)*cos(p)) / 2);
        final double dm = a95 * (sin(p) / cos(I_m));
        return new VGP(Location.fromRadians(λ_p, φ_p), dp, dm);
    }
    
    /**
     * Calculate a VGP for the given direction and location.
     * 
     * The 95% confidence ellipse will also be calculated, using the a95 value
     * from the Fisher parameters.
     * 
     * @param parameters direction and a95 at sample site
     * @param site location of sample site
     * @return the VGP for the given direction and location
     */
    public static VGP calculate(FisherParams parameters, Location site) {
        return calculate(parameters.getMeanDirection(),
                parameters.getA95(), site);
    }
    
    /**
     * Returns the headers describing the parameters as a list of strings.
     *
     * @return the headers describing the parameters
     */
    public static List<String> getHeaders() {
        return HEADERS;
    }

    /**
     * Returns a list of empty strings equal in length to the number of
     * parameters.
     *
     * @return a list of empty strings equal in length to the number of
     * parameters
     */
    public static List<String> getEmptyFields() {
        return Collections.nCopies(HEADERS.size(), "");
    }

    private String fmt(double d) {
        return String.format(Locale.ENGLISH, "%.4f", d);
    }

    /**
     * Returns the VGP parameters as a list of strings. The order of the
     * parameters is the same as the order of the headers provided by
     * {@link #getHeaders()}.
     *
     * @return the VGP parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(
                fmt(getLocation().getLatDeg()),
                fmt(getLocation().getLongDeg()),
                fmt(getDp()), fmt(getDm()));
    }

    /**
     * @return <i>dp</i>, the angular length of the semi-axis of the ellipse of
     * confidence along the site-to-pole great circle
     */
    public double getDp() {
        return dp;
    }

    /**
     * @return <i>dm</i>, the angular length of the semi-axis of the ellipse of
     * confidence perpendicular the site-to-pole great circle.
     */
    public double getDm() {
        return dm;
    }

    /**
     * @return the location of this VGP 
     */
    public Location getLocation() {
        return location;
    }
   
}
