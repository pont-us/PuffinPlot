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

import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

/**
 * Represents a location on the Earth's surface (latitude/longitude).
 * 
 * @author pont
 */
public class Location {
    
    private final double latitude;
    private final double longitude;
    
    private static final List<String> HEADERS =
            Arrays.asList("Lat (deg)", "Long (deg)");
    
    /**
     * Create a new location.
     * 
     * @param latitude latitude in degrees
     * @param longitude longitude in degrees
     */
    private Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * @return the latitude in degrees
     */
    public double getLatDeg() {
        return latitude;
    }

    /**
     * @return the longitude in degrees
     */
    public double getLongDeg() {
        return longitude;
    }
    
        /**
     * @return the latitude in degrees
     */
    public double getLatRad() {
        return toRadians(latitude);
    }

    /**
     * @return the longitude in degrees
     */
    public double getLongRad() {
        return toRadians(longitude);
    }
    
    /**
     * Returns a Location representing the supplied degree co-ordinates.
     * 
     * @param latDeg latitude of the Location in degrees
     * @param longDeg longitude of the Location in degrees
     * @return a Location representing the supplied co-ordinates
     */
    public static Location fromDegrees(double latDeg, double longDeg) {
        return new Location(latDeg, longDeg);
    }
    
    /**
     * Returns a Location representing the supplied radian co-ordinates.
     * 
     * @param latRad latitude of the Location in radians
     * @param longRad longitude of the Location in radians
     * @return a Location representing the supplied co-ordinates
     */
    public static Location fromRadians(double latRad, double longRad) {
        return new Location(toDegrees(latRad), toDegrees(longRad));
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

    /**
     * Returns the VGP parameters as a list of strings. The order of the
     * parameters is the same as the order of the headers provided by
     * {@link #getHeaders()}.
     *
     * @return the VGP parameters as a list of strings
     */
    public List<String> toStrings() {
        return Arrays.asList(Double.toString(getLatDeg()),
                Double.toString(getLongDeg()));
    }
    
    /**
     * @return a three-dimensional unit vector representing this location
     */
    public Vec3 toVec3() {
        return Vec3.fromPolarDegrees(1, latitude, longitude);
    }
    
    /**
     * @param vector a non-zero, finite vector
     * @return a Location corresponding to the direction of the supplied
     * vector
     */
    public static Location fromVec3(Vec3 vector) {
        return new Location(vector.getIncDeg(), vector.getDecDeg());
    }
}
