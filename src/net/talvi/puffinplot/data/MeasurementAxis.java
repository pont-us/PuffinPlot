package net.talvi.puffinplot.data;

/**
 * An axis along which a magnetic moment measurement was made.
 * 
 * @author pont
 */

public enum MeasurementAxis {
    /** the x axis */
    X,
    /** the y axis */
    Y,
    /** the z axis */
    Z,
    /** the inverted z axis */
    MINUSZ,
    /** a virtual axis used in the modified Zijderveld plot, corresponding
     to the direction of the horizontal component of a data point.
     @see net.talvi.puffinplot.plots.ZPlot */
    H;
}
