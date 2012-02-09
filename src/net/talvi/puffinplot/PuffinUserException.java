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
package net.talvi.puffinplot;

import java.io.IOException;

/**
 * An exception representing a condition which is not fatal, but
 * which needs to be caught and reported to the user.
 * 
 * @author pont
 */
public class PuffinUserException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a PuffinPlot user exception with the specified message.
     * @param message the message to use for the new exception
     */
    public PuffinUserException(String message) {
        super(message);
    }
    
    /**
     * Creates a PuffinPlot user exception wrapping the supplied exception.
     * The detail message will be taken from the supplied exception.
     * @param exception the exception to wrap
     */
    public PuffinUserException(IOException exception) {
        super(exception);
    }
    
}
