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

import java.util.Map;
import java.util.Objects;

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
    
    /**
     * Returns the value of this option in a supplied option map. This is
     * intended as a utility method for file loaders processing supplied
     * options. If the default value has the wrong class, an
     * {@link IllegalStateException} will be thrown (whether or not the default
     * value is used). If this option's identifier is not present as a key in
     * the supplied map, the default value will be returned. If the option map
     * is null, or if the option is present but has the wrong class, an
     * {@link IllegalArgumentException} will be thrown. If this method does
     * return a value rather than throwing an exception, that value is
     * guaranteed to be an instance of the class returned by {@link getType()},
     * and may therefore be safely cast to that class. Note that the returned
     * value may be {@code null}.
     *
     * @param optionMap a non-null map of option identifiers to values
     * @return the value of this option in the map, if it is present there;
     * otherwise, its default value
     * 
     */
    default Object getValue(Map<String, Object> optionMap) {
        Objects.requireNonNull(optionMap);
        final Object defaultValue = getDefaultValue();
        if (defaultValue != null && !getType().isInstance(defaultValue)) {
            throw new IllegalStateException(String.format(
                    "Default value %s for option %s has wrong class "
                    + "(should be %s)",
                    defaultValue.toString(), getIdentifier(),
                    getType().toString()
            ));
        }
        
        if (optionMap.containsKey(getIdentifier())) {
            final Object value = optionMap.get(getIdentifier());
            if (value == null || getType().isInstance(value)) {
                return value;
            } else {
                throw new IllegalArgumentException(String.format(
                        "Supplied value %s for option %s has wrong class "
                        + "(should be %s)",
                        value.toString(), getIdentifier(), getType().toString()
                ));
            }
        } else {
            return defaultValue;
        }
    }
    
}
