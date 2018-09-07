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
package net.talvi.puffinplot.data;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author pont
 */
public class CsvWriterTest {
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyDelimiter() {
        new CsvWriter(new StringWriter(), "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMultiCharacterDelimiter() {
        new CsvWriter(new StringWriter(), "12");
    }

    @Test(expected = NullPointerException.class)
    public void testNullWriterTwoArgumentConstructor() {
        new CsvWriter(null, ",");
    }

    @Test(expected = NullPointerException.class)
    public void testNullWriterOneArgumentConstructor() {
        new CsvWriter(null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDelimiter() {
        new CsvWriter(new StringWriter(), null);
    }

    @Test
    public void testCustomSeparator() throws IOException {
        checkOutput("#", "\"Contains # separator\"#field 2#"
                + "\"Contains \"\" quotation mark\"",
                "Contains # separator", "field 2",
                "Contains \" quotation mark");
    }
    
    @Test
    public void testNullFields() throws IOException {
        checkOutput(",", "xxx,null,yyy,null,zzz",
                "xxx", null, "yyy", null, "zzz");
    }
    
    @Test
    public void testDefaultSeparator() throws IOException {
        checkOutput(null, "one,two", "one", "two");
    }
    
    @Test
    public void testListUnpacking() throws IOException {
        checkOutput(null, "aaa,bbb,ccc,ddd", "aaa",
                Arrays.asList("bbb", "ccc"), "ddd");
    }
    
    private void checkOutput(String separator, String expected,
            Object... fields) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        final CsvWriter csvWriter = separator == null ?
                new CsvWriter(stringWriter) :
                new CsvWriter(stringWriter, separator);
        csvWriter.writeCsv(fields);
        assertEquals(expected + "\n", stringWriter.getBuffer().toString());
        csvWriter.close();
    }
    
}
