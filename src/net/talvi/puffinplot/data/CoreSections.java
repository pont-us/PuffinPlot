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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

class CoreSections {

    private final LinkedHashMap<String,CoreSection> sections;
    
    private CoreSections(LinkedHashMap<String,CoreSection> sections) {
        this.sections = sections;
    }
    
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
    
    public LinkedHashMap<String,CoreSection> getSections() {
        return sections;
    }

    void alignSections(double topAlignment) {
        final int margin = 1;
        double alignTo = topAlignment;
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
    }

}
