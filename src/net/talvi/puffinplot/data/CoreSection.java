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
package net.talvi.puffinplot.data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A representation of a section of core, comprising a contiguous
 * sequence of {@code Sample}s. This can be useful, for instance, for
 * rotating an entire section to align its declination with a neighbouring
 * section.
 */

public class CoreSection implements SampleGroup {

    private final List<Sample> samples;

    /**
     * A representation of the end of a core section -- top or bottom.
     */
    public static enum End {

        /**
         * The top of a core section
         */
        TOP,

        /**
         * The bottom of a core section
         */
        BOTTOM;
    }
  
    private CoreSection(List<Sample> samples) {
        this.samples = samples;
    }

    /**
     * Returns a core section containing the specified samples in the
     * specified order. Note that the samples are not copied: the returned
     * {@code CoreSection} contains references to the original samples,
     * so operations upon it may modify them.
     * 
     * @param samples a list of samples
     * @return a core section containing the provided samples
     */
    public static CoreSection fromSamples(List<Sample> samples) {
        Objects.requireNonNull(samples);
        return new CoreSection(samples);
    }

    /**
     * @return the samples in this core section
     */
    @Override
    public List<Sample> getSamples() {
        return samples;
    }

    /**
     * Rotates the declinations of all the data points in all the samples
     * by the specified angle.
     * 
     * @param clockwiseDegrees rotation angle
     */
    public void rotateDeclinations(double clockwiseDegrees) {
        getSamples().forEach(s -> s.getData().forEach(
                d -> d.setMoment(d.getMoment().
                        rotZ(Math.toRadians(clockwiseDegrees)))));
        for (Sample s: getSamples()) {
            if (s.getImportedDirection() != null) {
                s.setImportedDirection(s.getImportedDirection().
                        rotZ(Math.toRadians(clockwiseDegrees)));
            }
        }
    }

    /**
     * Returns the topmost or bottommost samples in this core section.
     * 
     * @param end the end from which to return the samples
     * (top or bottom)
     * @param nSamples the number of samples to return (from 0 to
     * the number of samples in this section)
     * @return the topmost or bottommost samples
     */
    public List<Sample> getSamplesNearEnd(End end, int nSamples) {
        if (nSamples < 0) {
            throw new IllegalArgumentException(
                    "numberOfSamples must be non-negative");
        }
        if (nSamples > getSamples().size()) {
            throw new IllegalArgumentException(String.format(
                    "%d samples requested, but only %d in section",
                    nSamples, getSamples().size()));
        }
        switch (end) {
            case TOP:
                return getSamples().subList(0, nSamples);
            case BOTTOM:
                return getSamples().subList(getSamples().size() - nSamples,
                        getSamples().size());
            default:
                throw new IllegalArgumentException();
        }
    }
    
    /**
     * Calculates and returns the Fisherian mean direction of the
     * topmost or bottommost samples in this section.
     * 
     * @param end the section end (top or bottom)
     * @param nSamples the number of samples to average
     * @return the mean direction of the top or bottom samples
     * 
     * @throws IllegalStateException if any section end samples do not
     * have a direction
     */
    public Vec3 getDirectionNearEnd(End end, int nSamples) {
        final List<Sample> endSamples = getSamplesNearEnd(end, nSamples);
        if (endSamples.stream().anyMatch(s -> s.getDirection() == null)) {
            throw new IllegalStateException("Some end samples have no "
                    + "defined direction.");
        } 
        return FisherValues.calculate(endSamples.stream().
                map(Sample::getDirection).
                collect(Collectors.toList())).getMeanDirection();
    }

    /**
     * Report whether the direction is defined near the specified end
     * of this core. The direction is defined iff every sample near the
     * end has a direction.
     * 
     * @param end the section end
     * @param nSamples the number of samples to regard as "near the end"
     * @return true iff all the samples near the end have a direction
     */
    boolean isDirectionDefinedNearEnd(End end, int nSamples) {
        final List<Sample> endSamples = getSamplesNearEnd(end, nSamples);
        return endSamples.stream().allMatch(s -> s.getDirection() != null);
    }

}
