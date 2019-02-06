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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.talvi.puffinplot.data.MeasurementType;
import net.talvi.puffinplot.data.TreatmentStep;
import net.talvi.puffinplot.data.Vec3;

import static java.lang.Math.toRadians;

public class PmdLoader extends AbstractFileLoader {
    
    private static final List<String> VALID_HEADERS =
            Arrays.asList(new String[] {
                " PAL  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   Dg    Ig    Ds    Is   a95 ",
                "STEP  Xc [Am2]  Yc [Am2]  Zc [Am2]  MAG[A/m]   Dg    Ig    Ds    Is  a95 ",
                "STEP  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   GDEC  GINC  SDEC  SINC a95 ",
                "STEP  Xc [Am²]  Yc [Am²]  Zc [Am²]  MAG[A/m]   Dg    Ig    Ds    Is  a95 ",
                "STEP  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   Dg    Ig    Ds    Is   a95 "
            });

    private final String fileIdentifier;
    private String firstLineComment = "";
    
    public static PmdLoader readFile(File file, Map<Object, Object> importOptions) {
        try {
            final FileInputStream fis = new FileInputStream(file);
            return new PmdLoader(fis, importOptions, file.getName());
        } catch (IOException ex) {
            final PmdLoader pl = new PmdLoader();
            pl.messages.add("Error reading \"" + file.getName() + "\"");
            pl.messages.add(ex.getMessage());
            return pl;
        }
    }
    
    /**
     * Create a new PmdLoader containing the treatmentSteps from the supplied input
 stream.
     * 
     * @param inputStream the input stream from which to read the PMD file
     * @param importOptions import options (currently not used)
     * @param fileIdentifier an optional identifier for the file being
     * read. It is only used in constructing warning and error messages.
     */
    public PmdLoader(InputStream inputStream, Map<Object, Object> importOptions,
            String fileIdentifier) {
        this.fileIdentifier = (fileIdentifier == null) ?
                "UNKNOWN" : fileIdentifier;
        /*
         * Some PMD files are in pure ASCII. Most of the non-ASCII ones I've
         * encountered use codepage 437, which is a superset of ASCII). For
         * both these cases, CP437 is a suitable encoding. Unfortunately,
         * PMD files exported from Remasoft 3 can contain a degree symbol °
         * (0xB0) in ISO-8859-1 encoding, which maps to the "light shade"
         * block character ░ (Unicode 2591) in CP437 encoding. Here I
         * hardcode CP437 nevertheless, and deal with the possibility
         * of having ° mapped to ░ in the PmdDataLine class. An alternative
         * approach would be to use heuristics to guess the character set,
         * but the current method seems more straightforward and robust.
         */
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream, "Cp437"))) {
            /* BufferedReader.lines() handles all three common line terminators
             * automatically, so we don't need to worry about CRLFs etc.
             */
            final List<String> lines =
                    reader.lines().collect(Collectors.toList());
            processLines(lines);
        } catch (IOException | UncheckedIOException ex) {
            addMessage("Error reading file %s", fileIdentifier);
            //Logger.getLogger(PmdLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private PmdLoader() {
        fileIdentifier = "";
    }

    private void processLines(List<String> lines) {
        // Line 0 contains an optional comment.
        firstLineComment = lines.get(0);
        // Line 1 is the first of two header lines.
        final PmdHeaderLine headerLine = PmdHeaderLine.read(lines.get(1));
        if (headerLine == null) {
            addMessage("Line 2: unknown format for first header line");
            return;
        }
        // Line 2 is the second of two header lines, and consists purely of
        // column headings. We check it against a list of known formats.
        if (!VALID_HEADERS.contains(lines.get(2))) {
            addMessage("Line 3: unknown format for second header line");
            return;
        }
        for (int lineIndex=3; lineIndex < lines.size(); lineIndex++) {
            final String line = lines.get(lineIndex);
            if (line.length() == 1 && line.codePointAt(0) == 26) {
                /*
                 * Some PMD files have a final line consisting only
                 * of a 0x1A (^Z) character.
                 */
                break;
            }
            final PmdDataLine dataLine = PmdDataLine.read(line);
            final TreatmentStep step = new TreatmentStep(
                    dataLine.moment.divideBy(headerLine.volume));
            step.setMeasurementType(MeasurementType.DISCRETE);
            step.setDiscreteId(headerLine.name);
            step.setSampAz(headerLine.sampleAzimuth);
            step.setSampDip(90 - headerLine.sampleHade);
            step.setFormAz((headerLine.formationStrike + 90) % 360);
            step.setFormDip(headerLine.formationDip);
            step.setTreatmentType(dataLine.treatmentType);
            switch (step.getTreatmentType()) {
                case DEGAUSS_XYZ:
                    step.setAfX(dataLine.treatmentLevel/1000.);
                    step.setAfY(dataLine.treatmentLevel/1000.);
                    step.setAfZ(dataLine.treatmentLevel/1000.);
                    break;
                case THERMAL:
                    step.setTemp(dataLine.treatmentLevel);
                    break;
            }
            final Vec3 sampleCorrected = dataLine.moment.correctSample(
                    toRadians(headerLine.sampleAzimuth),
                    toRadians(90 - headerLine.sampleHade));
            final Vec3 formationCorrected = sampleCorrected.correctForm(
                    toRadians(headerLine.formationStrike + 90),
                    toRadians(headerLine.formationDip));
            final String location = String.format("file %s, line %d", fileIdentifier, lineIndex+1);
            checkConsistency(location, sampleCorrected.getDecDeg(),
                    dataLine.sampleCorrectedDeclination, 0.3);
            checkConsistency(location, sampleCorrected.getIncDeg(),
                    dataLine.sampleCorrectedInclination, 0.3);
            checkConsistency(location, formationCorrected.getDecDeg(),
                    dataLine.formationCorrectedDeclination, 0.3);
            checkConsistency(location, formationCorrected.getIncDeg(),
                    dataLine.formationCorrectedInclination, 0.3);
            checkConsistency(location, step.getIntensity(), dataLine.magnetization, 
                    Math.max(step.getIntensity(), dataLine.magnetization)/100);
            addTreatmentStep(step);
        }
    }
    
    private void checkConsistency(String location, double expected,
            double actual, double tolerance) {
        /* PMD files converted from JR6 files by Remasoft 3 don't always
         * include all the polar treatmentSteps, so identify these by their
         * first-line comment and skip the consistency checks for them.
         */
        if (!"JR6 file".equals(firstLineComment) &&
                Math.abs(expected - actual) > tolerance) {
            addMessage("Inconsistent data (%s): calculated %g, found %g",
                    location, expected, actual);
        }
    }
}
