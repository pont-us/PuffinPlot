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

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An interface implemented by PuffinPlot file loaders. It provides
 * methods for producing a {@link LoadedData} object from a file.
 *
 */
public interface FileLoader {
 
    /**
     * Read a file using a specified set of options. The supported options
     * should correspond to the option definitions returned by
     * {@link #getOptionDefinitions() }.
     * 
     * @param file the file to read
     * @param options file reading options
     * @return a representation of the data within the specified file
     */
    LoadedData readFile(File file, Map<String, Object> options);
    
    /**
     * Reads a file without any specified options. The implementer is
     * expected to use default options.
     * 
     * @param file the file to read
     * @return a representation of the data within the specified file
     */
    default LoadedData readFile(File file) {
        return readFile(file, Collections.emptyMap());
    }

    /**
     * @return a list defining the options supported by this loader
     */
    default List<OptionDefinition> getOptionDefinitions() {
        return Collections.emptyList();
    }
    
    /**
     * Check that a set of options is valid for this file loader. The options
     * map must be non-null. All required options must be present. No unknown
     * options may be present. Every value must be of the correct class. If any
     * of these checks fail, an {@link IllegalArgumentException} is thrown.
     * Additionally, the default values of the options are checked. If any of
     * them is of the wrong class, an {@link IllegalStateException} is thrown.
     *
     * @param options the options to check
     */
    default void checkOptions(Map<String, Object> options) {
        Objects.requireNonNull(options);

        for (OptionDefinition od : getOptionDefinitions()) {
            final Object defaultValue = od.getDefaultValue();
            if (defaultValue != null
                    && !od.getType().isInstance(defaultValue)) {
                throw new IllegalStateException(String.format(
                        "Default value %s for option %s has wrong class "
                        + "(should be %s)",
                        defaultValue.toString(), od.getIdentifier(),
                        od.getType().toString()
                ));
            }
        }
        
        final Set<String> validKeys = getOptionDefinitions().stream()
                .map(OptionDefinition::getIdentifier)
                .collect(Collectors.toSet());
        final Set<String> requiredKeys = getOptionDefinitions().stream()
                .filter(OptionDefinition::isRequired)
                .map(OptionDefinition::getIdentifier)
                .collect(Collectors.toSet());
        final Set<String> suppliedKeys = options.keySet();
        if (!validKeys.containsAll(suppliedKeys)) {
            throw new IllegalArgumentException("Unknown option(s) supplied: "
                    + suppliedKeys.stream()
                            .filter(key -> !validKeys.contains(key))
                            .collect(Collectors.joining(", ")));
        }
        if (!suppliedKeys.containsAll(requiredKeys)) {
            throw new IllegalArgumentException("Required option(s) missing: "
                    + requiredKeys.stream()
                            .filter(key -> !suppliedKeys.contains(key))
                            .collect(Collectors.joining(", ")));
        }
        
        final Map<String, Class> optionClasses = getOptionDefinitions().stream()
                .collect(Collectors.toMap(OptionDefinition::getIdentifier,
                        OptionDefinition::getType));
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            final Object value = entry.getValue();
            final String key = entry.getKey();
            final Class expectedClass = optionClasses.get(key);
            if (value != null && !expectedClass.isInstance(value)) {
                throw new IllegalStateException(String.format(
                        "Supplied value %s for option %s has wrong class "
                        + "(should be %s)",
                        value.toString(), key, expectedClass.toString()
                ));
            }
        }
    }
}
