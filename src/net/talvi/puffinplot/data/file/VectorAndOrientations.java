/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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
package net.talvi.puffinplot.data.file;

import net.talvi.puffinplot.data.Vec3;

/**
 * VectorAndOrientations encapsulates a vector (in practice, most probably
 * a magnetization vector) and orientation parameters for the sample and
 * the rock formation. It is intended for use with the <code>OrientationParameters</code>
 * class, which defines an interpretation for the values in the <code>VectorAndOrientations</code>
 * class and provides a method to convert an <code>VectorAndOrientations</code>
 * into an equivalent <code>VectorAndOrientations</code> using PuffinPlot's
 * orientation parameter standard.
 * 
 * @see OrientationParameters
 */

public class VectorAndOrientations {
    
    /**
     * A vector measured relative to a sample's own co-ordinate system;
     * in practice, most likely to be a magnetization vector. The
     * interpretation of this vector depends on the P1 parameter of an
     * associated <code>OrientationParameters</code> object.
     */
    public final Vec3 vector;
    
    /**
     * A sample azimuth value in degrees. The interpretation of this
     * value depends on the P3 parameter of an associated
     * <code>OrientationParameters</code> object.
     */
    public final double sampleAzimuth;
    
    /**
     * A sample dip value in degrees. The interpretation of this value depends
     * on the P2 parameter of an associated <code>OrientationParameters</code>
     * object.
     */
    public final double sampleDip;
    
    /**
     * A formation azimuth value in degrees.  The interpretation of this value 
     * depends on the P4 parameter of an associated
     * <code>OrientationParameters</code> object.
     */
    public final double formationAzimuth;
    
    /**
     * A formation dip value in degrees, measured downward from the horizontal
     * plane. For example, a horizontal foliation would have a dip of 0,
     * and a vertical foliation would have a dip of 90.
     */
    public final double formationDip;
    
    /**
     * Instantiate a <code>VectorAndOrientations</code> object. All the
     * specified parameters are stored directly in the corresponding fields
     * without modification.
     * 
     * @param vector
     * @param sampleAzimuth
     * @param sampleDip
     * @param formationAzimuth
     * @param formationDip
     */
    public VectorAndOrientations(Vec3 vector,
            double sampleAzimuth, double sampleDip,
            double formationAzimuth, double formationDip) {
        this.vector = vector;
        this.sampleAzimuth = sampleAzimuth;
        this.sampleDip = sampleDip;
        this.formationAzimuth = formationAzimuth;
        this.formationDip = formationDip;
    }
}
