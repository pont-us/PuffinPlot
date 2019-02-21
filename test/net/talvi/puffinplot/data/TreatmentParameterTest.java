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
package net.talvi.puffinplot.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * @author pont
 */
public class TreatmentParameterTest {

    @Test
    public void testValues() {
        final TreatmentParameter[] result = TreatmentParameter.values();
        final List<TreatmentParameter> resultList = Arrays.asList(result);

        /*
         * There seems little point in exhaustively testing the values array --
         * it would just repeat the definition. Instead we do a quick sanity
         * check that a couple of expected values are present and that the array
         * is of a reasonable size.
         */
        assertTrue(result.length > 20);
        assertTrue(resultList.contains(TreatmentParameter.AF_X));
        assertTrue(resultList.contains(TreatmentParameter.VOLUME));
        assertTrue(resultList.contains(TreatmentParameter.VIRT_MAGNETIZATION));
    }

    @Test
    public void testValueOf() {
        final TreatmentParameter[] values = TreatmentParameter.values();
        for (TreatmentParameter value : values) {
            assertEquals(value, TreatmentParameter.valueOf(value.toString()));
        }
    }

    @Test
    public void testGetHeadingAndGetByHeading() {
        final TreatmentParameter[] values = TreatmentParameter.values();
        for (TreatmentParameter value : values) {
            assertEquals(value,
                    TreatmentParameter.getByHeading(value.getHeading()));
        }
    }

    @Test
    public void testGetNiceName() {
        /*
         * No point in testing these exhausively, but we can check that they're
         * non-null and non-empty.
         */

        final TreatmentParameter[] values = TreatmentParameter.values();
        for (TreatmentParameter value : values) {
            final String result = value.getNiceName();
            assertNotNull(result);
            assertNotEquals("", result);
        }
    }

    @Test
    public void testGetTypeAndGetDefaultValue() {
        /*
         * Here we test that the default value for each field type is a valid
         * string representation of that type.
         */
        final TreatmentParameter[] fields = TreatmentParameter.values();
        final Set<Class> exceptionThrowingTypes = Arrays.stream(new Class[]{
            double.class, int.class, ArmAxis.class, MeasurementType.class,
            TreatmentType.class
        }).collect(Collectors.toSet());
        for (TreatmentParameter field : fields) {
            final String defaultValue = field.getDefaultValue();
            final Class type = field.getType();
            if (exceptionThrowingTypes.contains(type)) {
                try {
                    if (type == double.class) {
                        Double.parseDouble(defaultValue);
                    } else if (type == int.class) {
                        Integer.parseInt(defaultValue);
                    } else if (type == ArmAxis.class) {
                        ArmAxis.valueOf(defaultValue);
                    } else if (type == MeasurementType.class) {
                        MeasurementType.valueOf(defaultValue);
                    } else {
                        TreatmentType.valueOf(defaultValue);
                    }
                } catch (IllegalArgumentException ex) {
                    fail(String.format("Invalid default value \"%s\" for %s",
                            defaultValue, field.toString()));
                }
            } else if (type == boolean.class) {
                /*
                 * Boolean.parseBoolean doesn't throw exceptions: it just
                 * returns true for an input of "true" (with any casing) and
                 * "false" otherwise. But there seems no reason to allow values
                 * other than "true" and "false" for the default string
                 * representations in TreatmentParameter.
                 */
                assertTrue("false".equals(defaultValue)
                        || "true".equals(defaultValue));
            } else if (type == String.class) {
                assertNotNull(defaultValue);
            } else {
                fail("Unknown field type " + type);
            }
        }
    }

    @Test
    public void testIsVirtual() {
        final TreatmentParameter[] fields = TreatmentParameter.values();
        for (TreatmentParameter field : fields) {
            assertEquals(field.isVirtual(),
                    field.toString().startsWith("VIRT_"));
        }
    }

    @Test
    public void testIsImportable() {
        /*
         * All fields are importable, except for PuffinPlot's own fields and the
         * MS jump temperature.
         */
        final TreatmentParameter[] fields = TreatmentParameter.values();
        for (TreatmentParameter field : fields) {
            assertEquals(field.isImportable(),
                    !(field.toString().startsWith("PP_")
                    || field == TreatmentParameter.VIRT_MSJUMP));
        }
    }

    @Test
    public void testGetRealFields() {
        /**
         * We check that the set of all fields is partitioned by the result of
         * getRealFields() and the set of all fields for which isVirtual() is
         * true. (This is unfortunately rather verbose in Java.)
         */
        final Set<TreatmentParameter> allFields = Arrays.stream(TreatmentParameter.values()).
                collect(Collectors.toSet());
        final Set<TreatmentParameter> virtualFields = allFields.stream().
                filter(f -> f.isVirtual()).collect(Collectors.toSet());
        final Set<TreatmentParameter> realFields = TreatmentParameter.getRealFields().stream().
                collect(Collectors.toSet());
        // check that virtual and real are disjoint
        assertTrue(realFields.stream().
                noneMatch(f -> virtualFields.contains(f)));
        assertTrue(virtualFields.stream().
                noneMatch(f -> realFields.contains(f)));
        // check that every element is either in virtual or real
        final Set<TreatmentParameter> virtualPlusReal = new HashSet<>();
        virtualPlusReal.addAll(virtualFields);
        virtualPlusReal.addAll(realFields);
        assertEquals(allFields, virtualPlusReal);
    }

    @Test
    public void testGetRealFieldStrings() {
        final Set<String> actual = TreatmentParameter.getRealFieldStrings().stream().
                collect(Collectors.toSet());
        final Set<String> expected = TreatmentParameter.getRealFields().stream().
                map(f -> f.toString()).collect(Collectors.toSet());
        assertEquals(expected, actual);
    }
}
