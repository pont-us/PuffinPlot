package net.talvi.puffinplot.data;

/**
 * <p>This interface provides access to the standard parameters for 
 * Fisherian spherical statistics: mean direction, alpha-95, and
 * <i>k</i></p>
 * 
 * <p>Note that usage of these parameters does not necessarily imply
 * that the statistics were obtained by the original Fisher (1953) method:
 * the McFadden &amp; McElhinny (1988) technique produces the same
 * parameters by a very different method.</p>
 * 
 * @author pont
 */
public interface FisherParams {

    /** Returns the mean direction.
     * @return the mean direction */
    Vec3 getMeanDirection();

    /** Returns the alpha-95 value denoting the 95% confidence interval.  
     * @return the alpha-95 value denoting the 95% confidence interval
     */
    double getA95();

    /** Returns the <i>k</i>-value, an estimate of the precision
     * parameter <i>κ</i>.
     * @return the <i>k</i>-value, an estimate of the precision
     * parameter <i>κ</i>
     */
    double getK();
}
