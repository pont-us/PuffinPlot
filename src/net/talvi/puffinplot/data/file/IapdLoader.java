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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;
import net.talvi.puffinplot.data.Datum;
import static java.lang.Double.parseDouble;
import java.util.Map;
import net.talvi.puffinplot.data.MeasType;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

/**
 * Loader for IAPD files.
 * 
 * @author pont
 */
public class IapdLoader extends AbstractFileLoader {
    
    private LineNumberReader reader;
    private final File file;
    private final Map<Object, Object> importOptions;
    
    public IapdLoader(File file, Map<Object,Object> importOptions) {
                this.file = file;
        data = new LinkedList<>();
        this.importOptions = importOptions;
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {
            messages.add("Error reading " + file.getName());
            messages.add(e.getMessage());
        }
    }

    private void readFile() throws IOException {
        
        TreatType treatType = TreatType.DEGAUSS_XYZ;
        MeasType measType = MeasType.DISCRETE;
        if (importOptions.containsKey(TreatType.class)) {
            treatType = (TreatType) importOptions.get(TreatType.class);
        }
        if (importOptions.containsKey(MeasType.class)) {
            measType = (MeasType) importOptions.get(MeasType.class);
        }
        
        final String headerLine = reader.readLine();
        if (headerLine == null) {
            addMessage("%s is empty", file.getName());
            return;
        }
        String[] header = headerLine.trim().split(" +");
        final String sampleName = header[0];
        final double sampleAz = parseDouble(header[1]);
        final double sampleDip = parseDouble(header[2]);
        final double formAz = parseDouble(header[3]);
        final double formDip = parseDouble(header[4]);
        final double volume = parseDouble(header[5]);
        
        String line;
        double a95max = 0;
        while ((line = reader.readLine()) != null) {
            final Datum d = new Datum();
            final String[] parts = line.trim().split(" +");
            final double treatmentLevel = parseDouble(parts[0]);
            
            switch (treatType) {
                case THERMAL:
                    d.setTemp(treatmentLevel);
                    break;
                case ARM:
                case DEGAUSS_XYZ:
                    d.setAfX(treatmentLevel / 1000);
                    d.setAfY(treatmentLevel / 1000);
                    d.setAfZ(treatmentLevel / 1000);
                    break;
                case DEGAUSS_Z:
                    d.setAfZ(treatmentLevel / 1000);
                    break;
                case IRM:
                    d.setIrmField(treatmentLevel / 1000);
                    break;
            }
            
            d.setMoment(Vec3.fromPolarDegrees(parseDouble(parts[1]) / 1000,
                    parseDouble(parts[6]),
                    parseDouble(parts[5])));
            d.setDiscreteId(sampleName);
            d.setSampAz(sampleAz);
            d.setSampDip(sampleDip);
            d.setFormAz(formAz);
            d.setFormDip(formDip);
            d.setVolume(volume);
            d.setTreatType(treatType);
            d.setMeasType(measType);
            final double a95 = parseDouble(parts[4]);
            if (a95 > a95max) {
                a95max = a95;
            }
            data.add(d);
        }
        if (a95max >= 5) {
            addMessage("File \"%s\" has high α95 values (max. %.1f).",
                    file.getName(), a95max);
        }
    }
}
