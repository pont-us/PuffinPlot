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

/**
 * A definition of an option which is accepted by a file loader class.
 * If a class implements the {@code FileLoader} interface and accepts
 * any options for file loading, its {@code getOptionDefinitions}
 * method should return a list of {@code OptionDefinition} instances
 * giving details of the expected options.
 * 
 * @see FileLoader
 * 
 * @author pont
 */
public interface OptionDefinition {
    
    /**
     * @return a short, unique identifier for the option
     */
    String getIdentifier();
    
    /**
     * @return a user-friendly description of the option
     */
    String getDescription();
    
    /**
     * @return the class of the option's value
     */
    Class getType();
    
    /**
     * @return the default value of the option. The default value must
     * be an instance of the class returned by {@code getType()}.
     * The implementer should return either an immutable value or a defensive
     * copy.
     */
    Object getDefaultValue();
    
}
