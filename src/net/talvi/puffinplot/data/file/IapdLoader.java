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
    
    public IapdLoader(File file) {
                this.file = file;
        data = new LinkedList<>();
        try {
            reader = new LineNumberReader(new FileReader(file));
            readFile();
        } catch (IOException e) {

        }
    }

    private void readFile() throws IOException {
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
        while ((line = reader.readLine()) != null) {
            final Datum d = new Datum();
            final String[] parts = line.trim().split(" +");
            d.setAfX(parseDouble(parts[0]));
            d.setAfY(parseDouble(parts[0]));
            d.setAfZ(parseDouble(parts[0]));
            d.setMoment(Vec3.fromPolarDegrees(parseDouble(parts[1]),
                    parseDouble(parts[6]),
                    parseDouble(parts[5])));
            d.setDiscreteId(sampleName);
            d.setSampAz(sampleAz);
            d.setSampDip(sampleDip);
            d.setFormAz(formAz);
            d.setFormDip(formDip);
            d.setVolume(volume);
            d.setTreatType(TreatType.DEGAUSS_XYZ);
            d.setMeasType(MeasType.DISCRETE);
            data.add(d);
        }
    }
}
