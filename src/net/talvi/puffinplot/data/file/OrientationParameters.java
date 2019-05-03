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
package net.talvi.puffinplot.data.file;

import java.util.Objects;

import net.talvi.puffinplot.data.Vec3;

import static java.lang.Math.toRadians;

/**
 * A set of parameters defining conventions for orienting a sample.
 * These parameters control the interpretation of an associated vector
 * measurement and a set of orientation angles; the
 * <code>VectorAndOrientations</code> class encapsulates the values
 * affected by <code>OrientationParameters</code>.
 * 
 * The orientation parameters are defined in various documents published
 * by AGICO, Inc., Brno, Czech Republic, and used in various AGICO
 * instrument control and data processing programs. For more detail
 * on their definition and use, see e.g. Chadima and Hrouda (2007)
 * or AGICO (2017).
 * 
 * Puffinplot's own orientation convention corresponds to the values
 * P1=12, P2=90, P3=12, P4=0. The main purpose of this class is to aid
 * in the conversion of other orientation conventions to PuffinPlot's
 * convention when importing files.
 * 
 * References:
 * 
 * AGICO, 2017. REMA6W control software for JR-6/JR-6A spinner magnetometers,
 * version 6.2.4., Brno: AGICO.
 * https://www.agico.com/downloads/documents/manuals/rema6-man.pdf
 * 
 * Chadima, M. & Hrouda, F., 2007. Remasoft 3.0 paleomagnetic data browser
 * and analyzer user manual, Brno: AGICO.
 * https://www.agico.com/downloads/documents/manuals/remasoft-usermanual.pdf
 * 
 * 
 * @see VectorAndOrientations
 * @author pont
 */

public class OrientationParameters {

    /**
     * A parameter representing an orientation convention for an azimuthal
     * angular value (e.g. the azimuthal orientation of a sample core).
     * Four values are possible; their exact interpretation is
     * context-dependent, but they are associated with four right-angled
     * directions in a plane, and indexed with the integers 3, 6, 9, and 12
     * (corresponding to directions on a clock face).
     */
    public enum AzimuthParameter {

        /**
         * A parameter value associated with a 90-degree angle.
         */
        A3(3),

        /**
         * A parameter value associated with a 180-degree angle.
         */
        A6(6),

        /**
         * A parameter value associated with a 270-degree angle.
         */
        A9(9),
        
        /**
         * A parameter value associated with a 0- (or 360-) degree angle.
         */
        A12(12);
        
        /**
         * An index for an azimuthal parameter value
         * (3, 6, 9, or 12), corresponding to an angle on a
         * clock face.
         */
        public final int index;
        
        private AzimuthParameter(int index) {
            this.index = index;
        }
        
        /**
         * Parses a string as an integer and return an azimuth parameter
         * with the corresponding index.
         * 
         * @param index a string consisting of "3", "6", "9", or "12",
         *    with optional surrounding whitespace
         * @return an azimuth parameter with the specified index
         */
        public static AzimuthParameter read(String index) {
            return read(Integer.parseInt(index.trim()));
        }
        
        /**
         * Returns an azimuth parameter for a specified index.
         * 
         * @param index 3, 6, 9, or 12
         * @return an azimuth parameter with the specified index
         */
        public static AzimuthParameter read(int index) {
            for (AzimuthParameter ap: values()) {
                if (index == ap.index) {
                    return ap;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown azimuthal parameter index \"" + index + "\"");
        }

        private Vec3 rotateForP1(Vec3 vector) {
            switch (this) {
                case A3: return vector.rotZ(toRadians(90));
                case A6: return vector.rotZ(toRadians(180));
                case A9: return vector.rotZ(toRadians(270));
                default: return vector;
            }
        }
        
        private double rotateSampleAzimuthForP3(double azimuthDegrees) {
            switch (this) {
                case A3: return (azimuthDegrees + 270) % 360;
                case A6: return (azimuthDegrees + 180) % 360;
                case A9: return (azimuthDegrees + 90) % 360;
                default: return azimuthDegrees;
            }
        }
    }
    
    /**
     * A parameter representing an orientation convention for a vertical
     * angular measurement (e.g. the dip of a sample core). The exact
     * interpretation is context-dependent, but the two possible index
     * values (0 and 90) correspond to angles measured in degrees.
     */
    public enum DipParameter {

        /**
         * An orientation value associated with an angle of 0 degrees.
         */
        D0(0),
        
        /**
         * An orientation value associated with an angle of 90 degrees.
         */
        D90(90);
        
        /**
         * An index for a possible orientation parameter value
         * (valid values are 0 and 90).
         */
        public final int index;
        

        private DipParameter(int index) {
            this.index = index;
        }
        
        /**
         * Parses a string as an integer and return a dip parameter
         * with the corresponding index.
         * 
         * @param index a string consisting of "0" or "90",
         *    with optional surrounding whitespace
         * @return a dip parameter with the specified index
         */
        public static DipParameter read(String index) {
            return read(Integer.parseInt(index.trim()));
        }

        /**
         * Returns a dip parameter for a specified index.
         * 
         * @param index 0 or 90
         * @return a dip parameter with the specified index
         */
        public static DipParameter read(final int index) {
            for (DipParameter dp : values()) {
                if (index == dp.index) {
                    return dp;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown dip parameter index \"" + index + "\"");
        }
    }
    
    /**
     * P1 is a clock value orientation of the Arrow which represents the x-axis
     * of Specimen coordinate system (right hand rule, see Legend). The Arrow
     * may be drawn: P1=12 Upslope; P1=3 To the Right; P1=6 Downslope; P1=9 To
     * the Left. Note that the Azimuth of the Arrow may, or may not be measured
     * (see Parameter P3).
     */
    public final AzimuthParameter p1;
    
    /**
     * P2 indicates which angle is measured as the specimen inclination. P2=0
     * Dip of specimen frontal plane; P2=90 Plunge of specimen z-axis
     * (Complementary angle to Dip of specimen frontal plane)
     */
    public final DipParameter p2;
    
    /**
     * P3 is a clock value that indicates which Direction on the specimen
     * frontal plane (x-y plane) is measured using geological compass and
     * expressed as Azimuth. P3=12 Antipode of Dip direction; P3=3 Right-handed
     * Strike; P3=6 Dip direction; P3=9 Left-handed Strike. Note that the
     * measured Direction may, or may not, coincide with Arrow (see Parameter
     * P1).
     *
     */
    public final AzimuthParameter p3;
    
    /**
     * P4 indicates how mesoscopic foliation adjacent to the specimen is
     * measured. P4=0 Dip direction/Dip notation is used; P4=90 Strike/Dip
     * notation is used (right hand rule)
     */
    public final DipParameter p4;
    
    /**
     * Creates an orientation parameter set from the specified parameters
     * values.
     * 
     * @param p1 Value for the P1 parameter
     * @param p2 Value for the P2 parameter
     * @param p3 Value for the P3 parameter
     * @param p4 Value for the P4 parameter
     */
    public OrientationParameters(AzimuthParameter p1,
            DipParameter p2, AzimuthParameter p3, DipParameter p4) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }
    
    /**
     * Creates a orientation parameter set from the specified index
     * values for the required parameters.
     * 
     * @param p1 Index value for the P1 parameter (3, 6, 9, or 12)
     * @param p2 Index value for the P2 parameter (0 or 90)
     * @param p3 Index value for the P3 parameter (3, 6, 9, or 12)
     * @param p4 Index value for the P4 parameter (0 or 90)
     * @return a parameter set with the specified values
     */
    public static OrientationParameters read(
            int p1, int p2, int p3, int p4) {
        return new OrientationParameters(
                AzimuthParameter.read(p1), DipParameter.read(p2),
                AzimuthParameter.read(p3), DipParameter.read(p4));        
    }
    
    /**
     * Creates a orientation parameter set from the strings containing
     * index values for the required parameters. Each string should
     * consist of a single decimal integer from the set of allowed
     * values for the associated parameter, with optional preceding
     * and/or following whitespace.
     * 
     * @param p1 String containing index value for the P1 parameter
     *        (3, 6, 9, or 12)
     * @param p2 String containing index value for the P2 parameter (0 or 90)
     * @param p3 String containing index value for the P3 parameter
     *        (3, 6, 9, or 12)
     * @param p4 String containing index value for the P4 parameter (0 or 90)
     * @return a parameter set with the specified values
     */
    public static OrientationParameters read(
            String p1, String p2, String p3, String p4) {
        return new OrientationParameters(
                AzimuthParameter.read(p1), DipParameter.read(p2),
                AzimuthParameter.read(p3), DipParameter.read(p4));
    }

    /**
     * This method takes a <code>VectorAndOrientations</code> object
     * and interprets its contents according to the settings in this
     * <code>OrientationParameters</code> object. It returns a
     * <code>VectorAndOrientations</code> object which, when
     * interpreted according to PuffinPlot's own orientation parameters,
     * represents the same vector and orientations as the
     * <code>VectorAndOrientations</code> passed as input to the method.
     * 
     * More briefly: this method converts data from a the orientation
     * system represented by this object to PuffinPlot's own orientation
     * system.
     * 
     * PuffinPlot's own orientation parameter convention is: 
     * P1=12, P2=90, P3=12, P4=0.
     * 
     * @param vectorAndOrientations a vector and orientations, to be
     *   interpreted according to the orientation parameters represented
     *   by this object
     * @return a vector and orientations which, when interpeted
     *   according to PuffinPlot's orientation parameters, represent the
     *   same vector and orientations as those suppled to the method
     *   
     */
    public VectorAndOrientations convertToPuffinPlotConvention(
            VectorAndOrientations vectorAndOrientations) {
        final Vec3 vector = p1.rotateForP1(vectorAndOrientations.vector);
        final double sampleDip = p2 == DipParameter.D90
                ? vectorAndOrientations.sampleDip
                : 90 - vectorAndOrientations.sampleDip;
        final double sampleAzimuth = p3.rotateSampleAzimuthForP3(
                vectorAndOrientations.sampleAzimuth);
        final double formationAzimuth = p4 == DipParameter.D0
                ? vectorAndOrientations.formationAzimuth
                : (vectorAndOrientations.formationAzimuth + 90) % 360;
        return new VectorAndOrientations(vector, sampleAzimuth, sampleDip,
                formationAzimuth, vectorAndOrientations.formationDip);
    }
    
    /**
     * Compares equality of two orientation parameter sets.
     * They are equal iff every parameter in one set has the same
     * value as the corresponding parameter in the other set.
     * 
     * @param other the object with which to compare
     * @return true iff the values are equals
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof OrientationParameters) {
            final OrientationParameters o = (OrientationParameters) other;
            return p1 == o.p1 && p2 == o.p2 && p3 == o.p3 && p4 == o.p4;
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for this parameter set. The hash code is
     * generated from the values of all four parameters.
     * 
     * @return a hash code for this parameter set
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.p1);
        hash = 37 * hash + Objects.hashCode(this.p2);
        hash = 37 * hash + Objects.hashCode(this.p3);
        hash = 37 * hash + Objects.hashCode(this.p4);
        return hash;
    }
}
