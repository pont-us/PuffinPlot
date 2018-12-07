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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.FieldUnit;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.MomentUnit;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;


public class FileFormatTest {

    private static double delta = 1e-10;
    
    @Test
    public void testReadLinesWithVolume() {
        final Map<Integer, DatumField> columnMap = makeColMap(
                1, DatumField.AF_Z,
                2, DatumField.X_MOMENT,
                3, DatumField.Y_MOMENT,
                4, DatumField.Z_MOMENT,
                5, DatumField.DISCRETE_ID,
                6, DatumField.VOLUME
        );
        final FileFormat ff = new FileFormat(columnMap, 1, MeasType.DISCRETE,
                TreatType.DEGAUSS_XYZ, " ", false, null, MomentUnit.AM,
                FieldUnit.MILLITESLA);
        final List<String> lines = Arrays.asList(
                "\"Level\" \"X\" \"Y\" \"Z\" \"file\" \"volume\"\n",
                "\"1\" 0 -0.00107902189997942 0.00779124456547835 0.000430048512009197 \"ch005\" 7\n",
                "\"2\" 20 -0.00287221566495697 0.00511191141997228 0.000325813297524727 \"ch005\" 7\n",
                "\"3\" 40 -0.0025011710899289 0.00491686732397584 -0.000243530541458016 \"ch005\" 7"
                );
        final List<Datum> data = ff.readLines(lines);
        checkData(data, new double[][] {
            {0, -0.000154145985711346, 0.00111303493792548, 0.0000614355017155996, 7, 0, 90, 0, 0},
            {0.020, -0.000410316523565281, 0.00073027305999604, 0.0000465447567892467, 7, 0, 90, 0, 0},
            {0.040, -0.000357310155704129, 0.000702409617710834, -0.0000347900773511451, 7, 0, 90, 0, 0},
        });
    }
    
    private static void checkData(List<Datum> data, double[][] d) {
        for (int i=0; i<data.size(); i++) {
            double[] expected = d[i];
            final Datum datum = data.get(i);
            assertEquals(expected[0], datum.getTreatmentLevel(), delta);
            assertEquals(expected[1], datum.getMoment().x, delta);
            assertEquals(expected[2], datum.getMoment().y, delta);
            assertEquals(expected[3], datum.getMoment().z, delta);
            assertEquals(expected[4], datum.getVolume(), delta);
            assertEquals(expected[5], datum.getSampAz(), delta);
            assertEquals(expected[6], datum.getSampDip(), delta);
            assertEquals(expected[7], datum.getFormAz(), delta);
            assertEquals(expected[8], datum.getFormDip(), delta);
        }
    }
        
    @Test
    public void testReadLineIncompletePolarData() {
        final Map<Integer, DatumField> columnMap = makeColMap(
                0, DatumField.X_MOMENT,
                1, DatumField.Y_MOMENT,
                2, DatumField.Z_MOMENT,
                3, DatumField.VIRT_DECLINATION
        );
        final FileFormat ff = new FileFormat(columnMap, 1, MeasType.DISCRETE,
                TreatType.DEGAUSS_XYZ, ",", false, Collections.emptyList(),
                MomentUnit.AM, FieldUnit.MILLITESLA);
        final Datum datum = ff.readLine("2,4,6,12");
        assertTrue(new Vec3(2,4,6).equals(datum.getMoment()));
    }
    
    @Test
    public void testConvertStringToColumnWidths() {
        assertEquals(Arrays.asList(1, 12, 3, 3, 7),
                FileFormat.convertStringToColumnWidths("1,  12 , 3,3, 7"));
        assertEquals(Arrays.asList(2, 4, 30),
                FileFormat.convertStringToColumnWidths("  2,4,,30   "));
        assertEquals(Arrays.asList(7, 8, 9),
                FileFormat.convertStringToColumnWidths("7, 8, wibble, 9"));
    }
    
    @Test
    public void testConstructorAndGetters() {
        final Map<Integer, DatumField> columnMap = makeColMap(
                1, DatumField.AF_Z,
                2, DatumField.X_MOMENT,
                3, DatumField.Y_MOMENT,
                4, DatumField.Z_MOMENT
        );
        final List<Integer> columnWidths = Arrays.asList(10, 9, 8, 7);
        FileFormat ff = new FileFormat(columnMap, 3, MeasType.DISCRETE,
                TreatType.DEGAUSS_XYZ, " ", true, columnWidths, MomentUnit.AM,
                FieldUnit.MILLITESLA);
        assertEquals(ff.getColumnMap(), columnMap);
        assertEquals(3, ff.getHeaderLines());
        assertEquals(MeasType.DISCRETE, ff.getMeasurementType());
        assertEquals(TreatType.DEGAUSS_XYZ, ff.getTreatmentType());
        assertEquals(" ", ff.getSeparator());
        assertEquals(true, ff.useFixedWidthColumns());
        assertEquals(MomentUnit.AM, ff.getMomentUnit());
        assertEquals(FieldUnit.MILLITESLA, ff.getFieldUnit());
    }
    
    @Test
    public void testSpecifiesFullVector() {
        assertFalse(makeFormat(1, DatumField.X_MOMENT, 2, DatumField.Y_MOMENT,
                3, DatumField.VIRT_DECLINATION, 4, DatumField.VIRT_INCLINATION).
                specifiesFullVector());
        assertFalse(makeFormat(1, DatumField.X_MOMENT, 2, DatumField.Z_MOMENT).
                specifiesFullVector());
        assertTrue(makeFormat(1, DatumField.X_MOMENT, 2, DatumField.Y_MOMENT,
                3, DatumField.Z_MOMENT).
                specifiesFullVector());
        assertTrue(makeFormat(1, DatumField.VIRT_DECLINATION, 2, DatumField.VIRT_INCLINATION,
                3, DatumField.VIRT_MAGNETIZATION).
                specifiesFullVector());
    }
    
    @Test
    public void testSpecifiesDirection() {
        assertFalse(makeFormat(1, DatumField.X_MOMENT, 2, DatumField.Y_MOMENT,
                3, DatumField.VIRT_DECLINATION).
                specifiesDirection());
        assertFalse(makeFormat(1, DatumField.X_MOMENT, 2, DatumField.Y_MOMENT,
                3, DatumField.VIRT_INCLINATION).
                specifiesDirection());
        assertTrue(makeFormat(1, DatumField.X_MOMENT, 2, DatumField.Y_MOMENT,
                3, DatumField.Z_MOMENT).
                specifiesDirection());
        assertTrue(makeFormat(1, DatumField.VIRT_DECLINATION,
                2, DatumField.VIRT_INCLINATION).
                specifiesDirection());
    }
    
    @Test
    public void testReadFromPrefs() {
        final Map<String,String> stringMap = makeMap(
                "fileformat.columnMap", "1,AF_X\t2,X_MOMENT\t\t3,Y_MOMENT\t4,Z_MOMENT\t5,DISCRETE_ID",
                "fileformat.columnWidths", "10,10,7,7",
                "fileformat.fieldUnit", "MILLITESLA",
                "fileformat.measType", "DISCRETE",
                "fileformat.momentUnit", "AM",
                "fileformat.separator", ",",
                "fileformat.treatType", "DEGAUSS_XYZ"
        );
        final Map<String,Integer> intMap = makeMap(
                "fileformat.headerLines", 1
        );
        final Map<String,Boolean> boolMap = makeMap(
                "fileformat.useFixedWidth", false
        );
        final Preferences prefsMock = Mockito.mock(Preferences.class);
        Mockito.when(prefsMock.get(Mockito.anyString(), Mockito.anyString())).
                thenAnswer(invocation -> stringMap.getOrDefault(
                        invocation.getArgument(0), invocation.getArgument(1)));
        Mockito.when(prefsMock.getInt(Mockito.anyString(), Mockito.anyInt())).
                thenAnswer(invocation -> intMap.getOrDefault(
                        invocation.getArgument(0), invocation.getArgument(1)));
        Mockito.when(prefsMock.getBoolean(Mockito.anyString(), Mockito.anyBoolean())).
                thenAnswer(invocation -> boolMap.getOrDefault(
                        invocation.getArgument(0), invocation.getArgument(1)));
        final FileFormat ff = FileFormat.readFromPrefs(prefsMock);
        assertEquals("10,10,7,7", ff.getColumnWidthsAsString());
        assertEquals(FieldUnit.MILLITESLA, ff.getFieldUnit());
        assertEquals(MeasType.DISCRETE, ff.getMeasurementType());
        assertEquals(MomentUnit.AM, ff.getMomentUnit());
        assertEquals(",", ff.getSeparator());
        assertEquals(TreatType.DEGAUSS_XYZ, ff.getTreatmentType());
        assertEquals(makeColMap(1, DatumField.AF_X,
                2, DatumField.X_MOMENT,
                3, DatumField.Y_MOMENT,
                4, DatumField.Z_MOMENT,
                5, DatumField.DISCRETE_ID), ff.getColumnMap());
    }

    @Test
    public void testWriteToPrefs() {
        final FileFormat ff = makeFormat(5, DatumField.AF_X, 6, DatumField.VIRT_MAGNETIZATION);
        final Preferences prefsMock = Mockito.mock(Preferences.class);
        ff.writeToPrefs(prefsMock);
        Mockito.verify(prefsMock).put("fileformat.columnMap", "5,AF_X\t6,VIRT_MAGNETIZATION");
        Mockito.verify(prefsMock).put("fileformat.fieldUnit", "MILLITESLA");
        Mockito.verify(prefsMock).put("fileformat.treatType", "DEGAUSS_XYZ");
        Mockito.verify(prefsMock).put("fileformat.momentUnit", "AM");
        Mockito.verify(prefsMock).put("fileformat.measType", "DISCRETE");
        Mockito.verify(prefsMock).put("fileformat.separator", " ");
        Mockito.verify(prefsMock).put("fileformat.columnWidths", "6,7,8");
        Mockito.verify(prefsMock).putBoolean("fileformat.useFixedWidth", false);
        Mockito.verify(prefsMock).putInt("fileformat.headerLines", 2);
    }
    
    private static FileFormat makeFormat(Object... colDefs) {
        return new FileFormat(makeColMap(colDefs), 2,
                MeasType.DISCRETE,
                TreatType.DEGAUSS_XYZ, " ", false, Arrays.asList(6, 7, 8),
                MomentUnit.AM,
                FieldUnit.MILLITESLA);        
    }

    private static Map<Integer, DatumField> makeColMap(Object... colDefs) {
        final Map<Integer, DatumField> map = new HashMap<>();
        for (int i=0; i<colDefs.length; i += 2) {
            map.put((Integer) colDefs[i], (DatumField) colDefs[i+1]);
        }
        return map;
    }
    
    private static <T> Map<String, T> makeMap(Object... contents) {
        final Map<String, T> map = new HashMap<>();
        for (int i=0; i<contents.length; i += 2) {
            map.put((String) contents[i], (T) contents[i+1]);
        }
        return map;
    }


}
