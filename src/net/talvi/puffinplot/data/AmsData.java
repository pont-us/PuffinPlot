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

import net.talvi.puffinplot.data.file.OrientationParameters;

/**
 * This class holds AMS (anisotropy of magnetic susceptibility) data for a named
 * sample. It is a simple container class, and does not process the data in any
 * way.
 *
 * @author pont
 */
public final class AmsData {

    private final String name;
    private final double[] tensor;
    private final double sampleAz, sampleDip;
    private final double formAz, formDip;
    private final double fTest;

    /**
     * Creates a new AMS data set with the data provided.
     * 
     * Note that the directional data is not copied directly into the newly
     * created object, but interpreted and converted from the orientation
     * system given by the {@code orientationParameters} argument to
     * PuffinPlot's own orientation system (P1=12, P2=90, P3=12, P4=0).
     * Thus the tensor, sample orientation, and formation orientation returned
     * by the class's getters might not match the values passed to its
     * constructor.
     * 
     * @param name the name of the sample
     * @param orientationParameters the orientation parameters defining the
     *   interpretation of the directional data
     * @param tensor the orientation tensor representing the sample's
     *   susceptibility anisotropy
     * @param sampleAz the sample azimuth in degrees
     * @param sampleDip the sample dip in degrees
     * @param formAz the formation azimuth in degrees
     * @param formDip the formation dip in degrees
     * @param fTest the F-test value as defined by Jelínek
     */
    public AmsData(String name, OrientationParameters orientationParameters,
            double[] tensor, double sampleAz, double sampleDip,
            double formAz, double formDip, double fTest) {
        this.name = name;
        this.tensor = orientationParameters.p1.rotateForP1(tensor);
        this.sampleAz =
                orientationParameters.p3.rotateSampleAzimuthForP3(sampleAz);
        this.sampleDip =
                orientationParameters.p2.correctSampleDipForP2(sampleDip);
        this.formAz = orientationParameters
                .p4.correctFormationDipDirectionForP4(formAz);
        this.formDip = formDip;
        this.fTest = fTest;
    }

    /**
     * Returns the sample's name.
     *
     * @return the sample's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the sample's susceptibility tensor.
     *
     * @return the sample's susceptibility tensor
     */
    public double[] getTensor() {
        return tensor;
    }

    /**
     * Returns the sample's dip azimuth in degrees.
     *
     * @return the sample's dip azimuth in degrees
     */
    public double getSampleAz() {
        return sampleAz;
    }

    /**
     * Returns the sample's dip in degrees.
     *
     * @return the sample's dip in degrees
     */
    public double getSampleDip() {
        return sampleDip;
    }

    /**
     * Returns the formation dip azimuth in degrees.
     *
     * @return the formation dip azimuth in degrees
     */
    public double getFormAz() {
        return formAz;
    }

    /**
     * Returns the formation dip in degrees.
     *
     * @return the formation dip in degrees
     */
    public double getFormDip() {
        return formDip;
    }

    /**
     * Returns the F-test value for the sample's anisotropy measurement.
     *
     * @return the F-test value for the sample's anisotropy measurement
     */
    public double getfTest() {
        return fTest;
    }
}
