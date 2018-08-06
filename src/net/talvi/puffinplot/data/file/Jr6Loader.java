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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;
import net.talvi.puffinplot.data.Datum;
import net.talvi.puffinplot.data.MeasType;

class Jr6Loader extends AbstractFileLoader {

    public Jr6Loader(InputStream inputStream, String fileIdentifier) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, "ASCII"))) {
            /* BufferedReader.lines() handles all three common line terminators
             * automatically, so we don't need to worry about CRLFs etc. */
            final List<String> lines =
                    reader.lines().collect(Collectors.toList());
            processLines(lines);
        } catch (IOException | UncheckedIOException ex) {
            addMessage("Error reading file %s", fileIdentifier);
        }
    }

    private void processLines(List<String> lines) {
        for (String line: lines) {
            final Jr6DataLine dataLine = Jr6DataLine.read(line);
            addDatum(makeDatum(dataLine));
        }
    }

    private Datum makeDatum(Jr6DataLine dataLine) {

        final VectorAndOrientations vectorAndOrientations = dataLine.
                getOrientationParameters().convertToPuffinPlotConvention(dataLine.getVectorAndOrientations());
        final Datum d = new Datum(vectorAndOrientations.vector);
        d.setSampAz(vectorAndOrientations.sampleAzimuth);
        d.setSampDip(vectorAndOrientations.sampleDip);
        d.setFormAz(vectorAndOrientations.formationAzimuth);
        d.setFormDip(vectorAndOrientations.formationDip);
        d.setMeasType(MeasType.DISCRETE);
        d.setDiscreteId(dataLine.getName());
        d.setTreatType(dataLine.getTreatmentType());
        switch (dataLine.getTreatmentType()) {
            case THERMAL:
                d.setTemp(dataLine.getTreatmentLevel());
                break;
            case DEGAUSS_XYZ:
            case ARM:
                d.setAfX(dataLine.getTreatmentLevel());
                d.setAfY(dataLine.getTreatmentLevel());
            case DEGAUSS_Z:
                d.setAfZ(dataLine.getTreatmentLevel());
                break;
        }
        return d;
    }

}
