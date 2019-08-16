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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot. If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot.data.file;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 *
 * @author pont
 */
public class FileLoaderTest {
    
    @Test(expected = NullPointerException.class)
    public void testCheckOptionsNull() {
        new FileLoaderImpl().checkOptions(null);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testCheckOptionsInvalidDefault() {
        final FileLoader fl = new FileLoader() {
            
            @Override
            public List<OptionDefinition> getOptionDefinitions() {
                return Collections.singletonList(new OptionDefinition() {
                    @Override
                    public String getIdentifier() {
                        return "test";
                    }

                    @Override
                    public String getDescription() {
                        return "test";
                    }

                    @Override
                    public Class getType() {
                        return String.class;
                    }
                    
                    @Override
                    public Object getDefaultValue() {
                        return 42;
                    }
                });
            }
            
            @Override
            public LoadedData readFile(File f, Map<String, Object> options) {
                return null;
            }
        };
        fl.checkOptions(Collections.emptyMap());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCheckOptionsUnknownOption() {
        final FileLoaderImpl fli = new FileLoaderImpl("option1");
        final Map<String, Object> options = new HashMap<String, Object>() {{
            put("option2", null);
        }};
        fli.checkOptions(options);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCheckOptionsMissingOption() {
        final FileLoaderImpl fli =
                new FileLoaderImpl("option1_required", "option2");
        final Map<String, Object> options = new HashMap<String, Object>() {
            {
                put("option2", null);
            }
        };
        fli.checkOptions(options);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckOptionsWrongClass() {
        final FileLoaderImpl fli =
                new FileLoaderImpl("option1_required", "option2");
        final Map<String, Object> options = new HashMap<String, Object>() {
            {
                put("option1", 42);
            }
        };
        fli.checkOptions(options);
    }
    
    public class FileLoaderImpl implements FileLoader {

        public List<OptionDefinition> optionDefinitions;
        
        public FileLoaderImpl(String... optionNames) {
            optionDefinitions = Arrays.asList(optionNames).stream()
                    .map(name -> new SimpleOptionDefinition(name, "test",
                            String.class, null, name.endsWith("_required")))
                    .collect(Collectors.toList());
        }
        
        public LoadedData readFile(File file, Map<String, Object> options) {
            return null;
        }
    }

}
