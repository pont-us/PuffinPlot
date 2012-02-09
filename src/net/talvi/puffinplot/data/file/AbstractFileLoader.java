/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012 Pontus Lurcock.
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
package net.talvi.puffinplot.data.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.talvi.puffinplot.data.Datum;

/**
 * An abstract implementation of the {@link FileLoader} interface.
 * This class provides a few facilities for implementing classes.
 * 
 * @author pont
 */
abstract class AbstractFileLoader implements FileLoader {

    protected List<String> messages = new LinkedList<String>();
    protected List<Datum> data = new ArrayList<Datum>();;

    protected void addMessage(String message, Object... args) {
        messages.add(String.format(message, args));
    }

    protected void addDatum(Datum d) {
        data.add(d);
    }

    public List<Datum> getData() {
        return data;
    }

    public List<String> getExtraLines() {
        return Collections.EMPTY_LIST;
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
