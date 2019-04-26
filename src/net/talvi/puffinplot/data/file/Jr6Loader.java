/* This file is part of PuffinPlot, a program for palaeomagnetic
 * treatmentSteps plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.TreatmentType;

/**
 * A loader for the JR6 format developed by Agico and used by the REMA6W
 * program.
 * 
 * @author pont
 */
public class Jr6Loader extends AbstractFileLoader {

    private final TreatmentType defaultTreatmentType;
    
    /**
     * Create a new JR6 loader for a specified input stream.
     * 
     * @param inputStream a stream of JR6 file contents
     * @param fileIdentifier an identifier for the file
     *   (only used in load messages)
     * @param defaultTreatmentType the treatment type to use for lines which
     *   don't specify one explicitly
     */
    public Jr6Loader(InputStream inputStream, String fileIdentifier,
            TreatmentType defaultTreatmentType) {
        this.defaultTreatmentType = defaultTreatmentType;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, "ASCII"))) {
            /*
             * BufferedReader.lines() handles all three common line terminators
             * automatically, so we don't need to worry about CRLFs etc.
             */
            final List<String> lines =
                    reader.lines().collect(Collectors.toList());
            processLines(lines);
        } catch (IOException | UncheckedIOException ex) {
            addMessage("Error reading file %s", fileIdentifier);
        }
    }
    
    private Jr6Loader() {
        defaultTreatmentType = TreatmentType.THERMAL;
    }

    /**
     * Return a JR6 loader for the specified file.
     * <p>
     * Currently one load option can be supplied: if the option map
     * contains the key {@code TreatmentType.class} with an associated
     * value of type {@code TreatmentType}, that value will be used as the
     * default treatment type for the data in the file. Otherwise,
     * treatment type will default to thermal. The default treatment type
     * is only used for data lines which do not explicitly specify a treatment
     * type.
     * 
     * @param file a JR6 file to read
     * @param importOptions load options (see method description for details)
     * @return a JR6 loader for the specified file
     */
    public static Jr6Loader readFile(File file,
            Map<Object, Object> importOptions) {
        try {
            final Object specifiedTreatmentType =
                    importOptions.get(TreatmentType.class);
            final TreatmentType defaultTreatmentType = 
                    (specifiedTreatmentType instanceof TreatmentType)
                    ? ((TreatmentType) specifiedTreatmentType)
                    : TreatmentType.THERMAL;
            final FileInputStream fis = new FileInputStream(file);
            return new Jr6Loader(fis, file.getName(), defaultTreatmentType);
        } catch (IOException ex) {
            final Jr6Loader loader = new Jr6Loader();
            loader.messages.add("Error reading \"" + file.getName() + "\"");
            loader.messages.add(ex.getMessage());
            return loader;
        }
    }

    private void processLines(List<String> lines) {
        for (String line: lines) {
            final Jr6DataLine dataLine =
                    Jr6DataLine.read(line, defaultTreatmentType);
            addTreatmentStep(makeTreatmentStep(dataLine));
        }
    }

    private TreatmentStep makeTreatmentStep(Jr6DataLine dataLine) {

        final VectorAndOrientations vectorAndOrientations =
                dataLine.getOrientationParameters().
                        convertToPuffinPlotConvention(
                                dataLine.getVectorAndOrientations());
        final TreatmentStep step =
                new TreatmentStep(vectorAndOrientations.vector);
        step.setSampAz(vectorAndOrientations.sampleAzimuth);
        step.setSampDip(vectorAndOrientations.sampleDip);
        step.setFormAz(vectorAndOrientations.formationAzimuth);
        step.setFormDip(vectorAndOrientations.formationDip);
        step.setMeasurementType(MeasurementType.DISCRETE);
        step.setDiscreteId(dataLine.getName());
        step.setTreatmentType(dataLine.getTreatmentType());
        switch (dataLine.getTreatmentType()) {
            case THERMAL:
                step.setTemperature(dataLine.getTreatmentLevel());
                break;
            case DEGAUSS_XYZ:
            case ARM:
                step.setAfX(dataLine.getTreatmentLevel() / 1000.);
                step.setAfY(dataLine.getTreatmentLevel() / 1000.);
            case DEGAUSS_Z:
                step.setAfZ(dataLine.getTreatmentLevel() / 1000.);
                break;
        }
        return step;
    }

}
