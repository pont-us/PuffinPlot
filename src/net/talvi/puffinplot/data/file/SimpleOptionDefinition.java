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
 * A simple implementation of the {@link OptionDefinition}
 * interface.
 */
public class SimpleOptionDefinition implements OptionDefinition {

    private final String identifier;
    private final String description;
    private final Class type;
    private final Object defaultValue;
    private final boolean required;
    
    /**
     * @param identifier short option identifier
     * @param description user-friendly option description
     * @param type the class of the values taken by this option
     * @param defaultValue the default value. It must be an instance
     *   of {@code type}
     * @param required whether this option is required
     */
    public SimpleOptionDefinition(String identifier, String description,
            Class type, Object defaultValue, boolean required) {
        this.identifier = identifier;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
        if (defaultValue != null && !type.isInstance(defaultValue)) {
            throw new IllegalArgumentException(
                    "Default value doesn't match type.");
        }
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Class getType() {
        return type;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    @Override
    public boolean isRequired() {
        return required;
    }
}
