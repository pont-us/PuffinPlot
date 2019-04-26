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
 *
 * @author pont
 */
public class SimpleOptionDefinition implements OptionDefinition {

    private final String identifier;
    private final String description;
    private final Class type;
    private final Object defaultValue;
    
    public SimpleOptionDefinition(String identifier, String description,
            Class type, Object defaultValue) {
        this.identifier = identifier;
        this.description = description;
        this.type = type;
        this.defaultValue = defaultValue;
        if (type.isAssignableFrom(defaultValue.getClass())) {
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
    
}
