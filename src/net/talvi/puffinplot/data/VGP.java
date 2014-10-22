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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

/**
 * A class representing a virtual geomagnetic pole.
 * 
 * This class can calculate a VGP from a site direction and position,
 * along with the associated confidence region.
 * 
 * @author pont
 */
public class VGP {

    private final double dp;
    private final double dm;
    private final double longitude;
    private final double latitude;
    
    private VGP(double longitude, double latitude, double dp, double dm) {
        this.longitude = toDegrees(longitude);
        this.latitude = toDegrees(latitude);
        this.dp = dp;
        this.dm = dm;
    }
    
    public static VGP calculate(Vec3 direction, double a95, double longitude,
            double latitude) {
        final double I_m = direction.getIncRad();
        final double D_m = direction.getDecRad();
        final double λ_s = toRadians(longitude);
        final double φ_s = toRadians(latitude);
        final double p = atan2(2, tan(I_m));
        final double λ_p = asin(sin(λ_s)*cos(p) + cos(λ_s)*sin(p)*cos(D_m));
        final double β = asin((sin(p)*sin(D_m)) / (cos(λ_p)));
        final double φ_p = (cos(p) >= sin(λ_s)*sin(λ_p)) ?
                φ_s + β : φ_s + PI - β;
        final double dp = a95 * ((1+3*cos(p)*cos(p)) / 2);
        final double dm = a95 * (sin(p) / cos(I_m));
        return new VGP(φ_p, λ_p, dp, dm);
    }
    
    public static VGP calculate(FisherParams parameters, double longitude,
            double latitude) {
        return calculate(parameters.getMeanDirection(),
                parameters.getA95(), longitude, latitude);
    }

    /**
     * @return the dp
     */
    public double getDp() {
        return dp;
    }

    /**
     * @return the dm
     */
    public double getDm() {
        return dm;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return latitude;
    }
    
}
