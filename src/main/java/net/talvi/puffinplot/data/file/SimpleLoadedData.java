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
import java.util.Locale;
import net.talvi.puffinplot.data.TreatmentStep;

/**
 * A simple implementation of the {@link LoadedData} interface. It caches
 * treatment steps, messages, and extra lines, and returns those cached
 * lists via the getters in the interface.
 */
public class SimpleLoadedData implements LoadedData {

    private List<TreatmentStep> treatmentSteps = new ArrayList<>();
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
    
    /**
     * Add a treatment step to the internal cache.
     * 
     * @param step the treatment step to add.
     */
    public void addTreatmentStep(TreatmentStep step) {
        treatmentSteps.add(step);
    }
    
    /**
     * Add a formatted message to the internal message cache. The message is
     * formatted using the {@link Locale#ENGLISH} locale. Formatting
     * is done with {@code String.format}.
     * 
     * @see String#format(java.util.Locale, java.lang.String, java.lang.Object...) 
     *
     * @param message the message to add, as a format string
     * @param args arguments referenced by the format specifiers in the format
     * string
     */
    public void addMessage(String message, Object... args) {
        messages.add(String.format(Locale.ENGLISH, message, args));
    }
    
    /**
     * Add a line to the internal extra line cache
     * 
     * @param extraLine the line to add
     */
    public void addExtraLine(String extraLine) {
        extraLines.add(extraLine);
    }

    /**
     * Replace the current treatment step cache with a supplied list.
     * The existing cached list is discarded.
     * 
     * @param steps the treatment steps with which to replace the current cache
     */
    public void setTreatmentSteps(List<TreatmentStep> steps) {
        this.treatmentSteps = steps;
    }
    
}
