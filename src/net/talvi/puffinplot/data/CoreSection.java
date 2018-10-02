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
 * 
 * @author pont
 */

public class CoreSection implements SampleGroup {

    private final List<Sample> samples;

    public static enum End {
        TOP, BOTTOM;
    }
  
    private CoreSection(List<Sample> samples) {
        this.samples = samples;
    }

    public static CoreSection fromSamples(List<Sample> samples) {
        Objects.requireNonNull(samples);
        return new CoreSection(samples);
    }

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
    
    public Vec3 getDirectionNearEnd(End end, int nSamples) {
        final List<Sample> endSamples = getSamplesNearEnd(end, nSamples);
        return FisherValues.calculate(endSamples.stream().
                map(Sample::getDirection).
                collect(Collectors.toList())).getMeanDirection();

    }
}
