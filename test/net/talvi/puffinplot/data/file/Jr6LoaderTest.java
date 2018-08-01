/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2018 Pontus Lurcock.
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

import java.io.InputStream;
import java.util.List;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.file.testdata.TestFileLocator;
import org.junit.Test;
import static org.junit.Assert.*;

public class Jr6LoaderTest {
    
    @Test
    public void testConstructor() {
        
        final String filename = "BC-Mersin (TDemag-580).jr6";
        
        final InputStream stream =
                TestFileLocator.class.getResourceAsStream(filename);
        
        final Jr6Loader jr6loader = new Jr6Loader(stream, filename);
        final List<Datum> loadedData = jr6loader.getData();
        assertFalse(loadedData.isEmpty());
        
    }
    
}
