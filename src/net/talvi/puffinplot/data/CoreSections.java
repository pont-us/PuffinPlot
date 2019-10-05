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
package net.talvi.puffinplot.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.talvi.puffinplot.data.CoreSection.End;

/**
 * A collection of {@code CoreSection}s, stored as a linked hash map
 * indexed by a string identifier. The purpose of this class is to
 * implement useful operations on groups of {@code CoreSection}s.
 * 
 */
public class CoreSections {

    private final LinkedHashMap<String, CoreSection> sections;
    
    private CoreSections(LinkedHashMap<String, CoreSection> sections) {
        this.sections = sections;
    }
    
    /**
     * Split a sample list into core sections according to the discrete
     * IDs of the samples. It is assumed that the samples are continuous
     * and that the discrete ID corresponds to the core section. The
     * ordering of the core sections, and of the samples within them,
     * corresponds to the original order of the supplied samples.
     * 
     * @param sampleList samples to split
     * @return a hash of core sections indexed by discrete ID
     */
    public static CoreSections fromSampleListByDiscreteId(
            List<Sample> sampleList) {
        final LinkedHashMap<String,List<Sample>> sublists =
                new LinkedHashMap<>();
        String previousId = null;
        for (Sample sample: sampleList) {
            final String thisId = sample.getDiscreteId();
            if (thisId == null) {
                continue;
            }
            if (!thisId.equals(previousId)) {
                sublists.put(thisId, new ArrayList<>());
            }
            sublists.get(thisId).add(sample);
            previousId = thisId;
        }
        
        final LinkedHashMap<String,CoreSection> sectionsTmp =
                new LinkedHashMap<>();
        for (String discreteId: sublists.keySet()) {
            sectionsTmp.put(discreteId,
                    CoreSection.fromSamples(sublists.get(discreteId)));
        }

        return new CoreSections(sectionsTmp);
    }
    
    /**
     * Get a linked map of core sections. The map is indexed by
     * core section identifier (which corresponds to the discrete ID
     * of a sample with continuous measurement type). Entries in the
     * map are ordered; if this object was produced by {@code
     * fromSampleListByDiscreteId}, the order corresponds to the original
     * sample order.
     * 
     * @return a map of core sections indexed by core section identifier
     */
    public LinkedHashMap<String,CoreSection> getSections() {
        return sections;
    }

    /**
     * Aligns the declinations of these core sections from the top down. For
     * each core section, the "top declination" and "bottom declination" are
     * determined from the Fisherian mean of the directions of the topmost or
     * bottommost samples. The number of samples used for this calculation is
     * determined by the value of the {@code margin} argument. The declinations
     * of each core section are rotated as a block, in such a way that the top
     * declination of each section matches the bottom declination of the section
     * above it, producing a record without discontinuities between the
     * sections. Additionally, the declinations of the core as a whole are
     * rotated so that either the top declination of the topmost section or the
     * mean declination of the entire core match a specified target declination.
     *
     * @param margin number of samples to use in determining top and bottom
     *        declinations
     * @param targetDeclination the target declination to which to align the
     *        core, in degrees
     * @param alignWith core declination which should be aligned with
     *        the target declination
     */
    public void alignSections(int margin, double targetDeclination,
            TargetDeclinationType alignWith) {
        
        /*
         * If we're aligning to the mean, there will be an extra alignment
         * pass afterwards so the initial target alignment is arbitrary --
         * but it still feels cleaner to use 0 as the top target.
         */
        double alignTo =
                alignWith == TargetDeclinationType.TOP
                ? targetDeclination
                : 0;
        
        // Align the sections with each other, from the top down.
        
        for (CoreSection section: sections.values()) {
            section.getSamples().forEach(s -> s.doPca(Correction.NONE));
            final double topDeclination = section.
                    getDirectionNearEnd(CoreSection.End.TOP, margin).
                    getDecDeg();
            final double offset = alignTo - topDeclination;
            section.rotateDeclinations(offset);
            section.getSamples().forEach(s -> s.doPca(Correction.NONE));
            alignTo = section.
                    getDirectionNearEnd(CoreSection.End.BOTTOM, margin).
                    getDecDeg();
        }
        
        /*
         * If necessary, calculate the mean declination and rotate the whole
         * core.
         */
        
        if (alignWith == TargetDeclinationType.MEAN) {
            final double initialMean =
                    FisherValues.calculate(getSections().values().stream()
                            .flatMap(s -> s.getSamples().stream())
                            .map(s -> s.getDirection())
                            .collect(Collectors.toList()))
                            .getMeanDirection().getDecDeg();
            final double offset = targetDeclination - initialMean;
            for (CoreSection section: sections.values()) {
                section.rotateDeclinations(offset);
                section.getSamples().forEach(s -> s.doPca(Correction.NONE));
            }
        }
    }

    /**
     * Returns a set containing all the samples which are near the end
     * of any section within this group of sections. The topmost and
     * bottommost {@code margin} samples of each section are counted
     * as being "near the end".
     * 
     * @param margin the number of samples from each end of each section
     *   to include in the returned set of samples
     * @return all the samples which are within {@code margin} samples
     *   of the end of any section within this group
     */
    public Set<Sample> getEndSamples(int margin) {
        return getSections().values().stream().
                map(s -> Stream.concat(
                        s.getSamplesNearEnd(End.TOP, margin).stream(),
                        s.getSamplesNearEnd(End.BOTTOM, margin).stream())).
                        flatMap(Function.identity()).
                collect(Collectors.toSet());
    }

    /**
     * Reports whether all samples within section ends have a defined direction.
     * 
     * @param margin number of samples in a section end
     * @return {@code true} if and only if every sample in every section end
     *   has a defined direction
     */
    public boolean areSectionEndDirectionsDefined(int margin) {
        return getEndSamples(margin).stream().
                allMatch(s -> s.getDirection() != null);
    }
    
    /**
     * The declination which should be aligned with a reference alignment
     * passed to
     * {@link #alignSections(int, double, net.talvi.puffinplot.data.CoreSections.ReferenceAlignmentType)}.
     */
    public static enum TargetDeclinationType {

        /**
         * Align top declination to reference declination.
         */
        TOP("Top of core"),
        
        /**
         * Align mean declination to reference declination.
         */
        MEAN("Core mean declination");

        private final String niceName;
        
        private TargetDeclinationType(String niceName) {
            this.niceName = niceName;
        }
        
        /**
         * @return a human-readable name for this target declination type
         */
        public String getNiceName() {
            return niceName;
        }
    }
}
