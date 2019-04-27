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

import java.util.ArrayList;
import java.util.List;
import net.talvi.puffinplot.data.TreatmentStep;

/**
 *
 * @author pont
 */
public class SimpleLoadedData implements LoadedData {

    private final List<TreatmentStep> treatmentSteps = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();
    private final List<String> extraLines = new ArrayList<>();
    
    @Override
    public List<TreatmentStep> getTreatmentSteps() {
        return treatmentSteps;
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }

    @Override
    public List<String> getExtraLines() {
        return extraLines;
    }
    
    public void addTreatmentStep(TreatmentStep step) {
        treatmentSteps.add(step);
    }
    
    public void addMessage(String message, Object... args) {
        messages.add(String.format(message, args));
    }
    
    public void addExtraLine(String extraLine) {
        extraLines.add(extraLine);
    }
    
    
}
