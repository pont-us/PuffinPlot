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
package net.talvi.puffinplot.data;

/**
 * This interface provides access to the standard parameters for 
 * Fisherian spherical statistics: mean direction, alpha-95, and
 * <i>k</i>.
 * <p>
 * Note that usage of these parameters does not necessarily imply
 * that the statistics were obtained by the original Fisher (1953) method:
 * the McFadden &amp; McElhinny (1988) technique produces the same
 * parameters by a very different method.
 * 
 * @author pont
 */
public interface FisherParams {

    /**
     * Returns the mean direction.
     *
     * @return the mean direction
     */
    Vec3 getMeanDirection();

    /**
     * Returns the alpha-95 value denoting the 95% confidence interval. The
     * returned value is in degrees.
     *
     * @return the alpha-95 value denoting the 95% confidence interval
     */
    double getA95();
    
    /**
     * Reports whether the alpha-95 value represents a real number.
     *
     * @return false if the alpha-95 is NaN or infinite, otherwise true
     */
    default boolean isA95Valid() {
         return Double.isFinite(getA95());
    }
    
    /**
     * Returns the <i>k</i>-value, an estimate of the precision parameter
     * <i>κ</i>.
     *
     * @return the <i>k</i>-value, an estimate of the precision parameter
     * <i>κ</i>
     */
    double getK();
    
    /**
     * Returns <i>R</i>, the total unit vector path length. For standard Fisher
     * (1953) statistics, <i>R</i> is the length of the sum of the unit vectors
     * corresponding to the directions being analysed. For McFadden &amp;
     * McElhinny (1988) statistics, <i>R</i> has a similar interpretation; see
     * the paper for details.
     *
     * @return <i>R</i>, the total unit vector path length.
     */
    double getR();
    
    /**
     * Returns <i>N</i>, the number of directions analysed. In the case of the
     * McFadden &amp; McElhinny (1988), this corresponds to the number of great
     * circles, and does <i>not</i> include any endpoints (direct observations)
     * used in the calculation. (See the paper for details.)
     *
     * @return <i>N</i>, the number of directions analysed
     */
    int getN();
}
