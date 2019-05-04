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

import java.util.Collections;
import java.util.List;
import net.talvi.puffinplot.data.TreatmentStep;

/**
 * A trivial implementation of the {@link LoadedData} interface, which
 * contains no data, only an optional single message.
 * <p>
 * The class is intended to provide a convenient "null object" which can
 * be returned when file loading fails entirely, hopefully containing an
 * explanation of <i>why</i> it failed entirely.
 * 
 */
public class EmptyLoadedData implements LoadedData {

    private final List<String> messages;

    /**
     * Instantiate an {@code EmptyLoadedData} object with a single message.
     * The message will be returned in a list by {@code getMessages()}.
     * 
     * @param message the message with which to populate this object
     */
    public EmptyLoadedData(String message) {
        this.messages = Collections.singletonList(message);
    }
    
    /**
     * Instantiate an {@code EmptyLoadedData} object with no message.
     */
    public EmptyLoadedData() {
        this.messages = Collections.emptyList();
    }
    
    @Override
    public List<TreatmentStep> getTreatmentSteps() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }

    @Override
    public List<String> getExtraLines() {
        return Collections.emptyList();
    }
    
}
