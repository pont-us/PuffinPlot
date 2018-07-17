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

import static java.lang.Math.toRadians;
import java.util.Objects;
import net.talvi.puffinplot.data.Vec3;

/**
 * Puffinplot's representation corresponds to P=12, P2=90, P3=12, P4=0.
 * 
 * @author pont
 */

class OrientationParameters {

    public enum AzimuthParameter {
        A3(3), A6(6), A9(9), A12(12);
        
        public final int index;
        
        private AzimuthParameter(int index) {
            this.index = index;
        }
        
        public static AzimuthParameter read(String index) {
            return read(Integer.parseInt(index.trim()));
        }
        
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
    
    public enum DipParameter {
        D0(0), D90(90);
        
        public final int index;
        
        private DipParameter(int index) {
            this.index = index;
        }
        
        public static DipParameter read(String index) {
            return read(Integer.parseInt(index.trim()));
        }

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
    
    public OrientationParameters(AzimuthParameter p1,
            DipParameter p2, AzimuthParameter p3, DipParameter p4) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
    }
    
    public static OrientationParameters read(
            int p1, int p2, int p3, int p4) {
        return new OrientationParameters(
                AzimuthParameter.read(p1), DipParameter.read(p2),
                AzimuthParameter.read(p3), DipParameter.read(p4));        
    }
    
    public static OrientationParameters read(
            String p1, String p2, String p3, String p4) {
        return new OrientationParameters(
                AzimuthParameter.read(p1), DipParameter.read(p2),
                AzimuthParameter.read(p3), DipParameter.read(p4));
    }

    public VectorAndOrientations standardize(
            VectorAndOrientations vectorAndOrientations) {
        final Vec3 vector = p1.rotateForP1(vectorAndOrientations.vector);
        final double sampleDip = p2 == DipParameter.D90 ?
                vectorAndOrientations.sampleDip :
                90 - vectorAndOrientations.sampleDip;
        final double sampleAzimuth = p3.rotateSampleAzimuthForP3(
                vectorAndOrientations.sampleAzimuth);
        final double formationAzimuth = p4 == DipParameter.D0 ?
                vectorAndOrientations.formationAzimuth :
                (vectorAndOrientations.formationAzimuth + 90) % 360;
        return new VectorAndOrientations(vector, sampleAzimuth, sampleDip,
                formationAzimuth, vectorAndOrientations.formationDip);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof OrientationParameters) {
            final OrientationParameters o = (OrientationParameters) other;
            return p1 == o.p1 && p2 == o.p2 && p3 == o.p3 && p4 == o.p4;
        } else {
            return false;
        }
    }

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
