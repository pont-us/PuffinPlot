/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2017 Pontus Lurcock.
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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.DatumField;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

/**
 *
 * @author pont
 */
public class UcDavisLoader extends AbstractFileLoader {
    
    private LineNumberReader reader;
    private final File file;
    private final Map<Object, Object> importOptions;

    private void readFile() throws IOException {
        
        final String headerLine = reader.readLine();
        if (headerLine == null) {
            addMessage("%s is empty", file.getName());
            return;
        }

        final SortedSet<Integer> levels = new TreeSet();
        final Map<ColumnDef, Integer> fieldMap = new HashMap<>();
        final String[] headers = headerLine.trim().split("\t");
        for (int i=1; i<headers.length; i++) {
            final String header = headers[i];
            final ColumnDef cd = ColumnDef.fromHeader(header);
            levels.add(cd.treatmentLevel);
            fieldMap.put(cd, i);
        }
        
        String line;
        while ((line = reader.readLine()) != null) {
            if ("".equals(line.trim())) continue;
            final String[] partStrings = line.split("\t");
            final List<Double> parts = Stream.of(partStrings).
                    map(Double::valueOf).
                    collect(Collectors.toList());
            for (Integer level: levels) {
                final Datum d = new Datum();
                d.setMoment(Vec3.fromPolarDegrees(
                        parts.get(fieldMap.get(new ColumnDef(level,
                                DatumField.VIRT_MAGNETIZATION))),
                        parts.get(fieldMap.get(new ColumnDef(level,
                                DatumField.VIRT_INCLINATION))),
                        parts.get(fieldMap.get(new ColumnDef(level,
                                DatumField.VIRT_DECLINATION)))));
                d.setTreatType(TreatType.DEGAUSS_XYZ);
                final double levelTesla = level / 1000.;
                d.setAfX(levelTesla);
                d.setAfY(levelTesla);
                d.setAfZ(levelTesla);
                d.setMeasType(MeasType.CONTINUOUS);
                d.setDepth(partStrings[0]);
                addDatum(d);
            }
        }
    }
    
    private static class ColumnDef {
        public final Integer treatmentLevel;
        public final DatumField parameter;
        private final static Pattern HEADER_PATTERN =
                Pattern.compile("([^(]+)[(](\\d+)[)]");
        
        private ColumnDef(Integer treatmentLevel, DatumField parameter) {
            this.treatmentLevel = treatmentLevel;  
            this.parameter = parameter;
        }
        
        private static DatumField stringToField(String s) {
            switch (s) {
                case "X": return DatumField.X_MOMENT;
                case "Y": return DatumField.Y_MOMENT;
                case "Z": return DatumField.Z_MOMENT;
                case "D": return DatumField.VIRT_DECLINATION;
                case "I": return DatumField.VIRT_INCLINATION;
                case "J": return DatumField.VIRT_MAGNETIZATION;
                default: return null;
            }
        }
        
        public static ColumnDef fromHeader(String header) {
            final Matcher matcher = HEADER_PATTERN.matcher(header);
            matcher.matches();
            final DatumField field = stringToField(matcher.group(1));
            return new ColumnDef(Integer.parseInt(matcher.group(2)),
                    field);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(treatmentLevel, parameter);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ColumnDef other = (ColumnDef) obj;
            if (!Objects.equals(this.treatmentLevel, other.treatmentLevel)) {
                return false;
            }
            if (this.parameter != other.parameter) {
                return false;
            }
            return true;
        }
    }
    
    public UcDavisLoader(File file, Map<Object,Object> importOptions) {
        
        data = new LinkedList<>();
        this.importOptions = importOptions;
        this.file = file;
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            messages.add("Error reading \"" + file.getName() + "\"");
            messages.add(e.getMessage());
        }
        

    }
}
