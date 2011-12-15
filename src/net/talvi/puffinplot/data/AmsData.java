package net.talvi.puffinplot.data;

/**
 * This class holds AMS (anisotropy of magnetic susceptibility)
 * data for a named sample. It is a simple container
 * class, and does not process the data in any way.
 * 
 * @author pont
 */
public final class AmsData {

    private final String name;
    private final double[] tensor;
    private final double sampleAz, sampleDip;
    private final double fTest;

    /**
     * Creates a new AMS data set with the data provided.
     * 
     * @param name the name of the sample
     * @param tensor the orientation tensor representing the sample's
     * susceptibility anisotropy
     * @param sampleAz the azimuth of the sample's dip, in degrees
     * @param sampleDip the sample's dip, in degrees
     * @param fTest the F-test value as defined by Jelínek
     */
    public AmsData(String name, double[] tensor, double sampleAz, double sampleDip,
            double fTest) {
        this.name = name;
        this.tensor = tensor;
        this.sampleAz = sampleAz;
        this.sampleDip = sampleDip;
        this.fTest = fTest;
    }

    /** Returns the sample's name.
     * @return the sample's name */
    public String getName() {
        return name;
    }

    /** Returns the sample's susceptibility tensor.
     * @return the sample's susceptibility tensor */
    public double[] getTensor() {
        return tensor;
    }

    /** Returns the sample's dip azimuth in degrees.
     * @return the sample's dip azimuth in degrees */
    public double getSampleAz() {
        return sampleAz;
    }

    /** Returns the sample's dip in degrees.
     * @return the sample's dip in degrees */
    public double getSampleDip() {
        return sampleDip;
    }

    /** Returns the F-test value for the sample's anisotropy measurement.
     * @return the F-test value for the sample's anisotropy measurement */
    public double getfTest() {
        return fTest;
    }
}
