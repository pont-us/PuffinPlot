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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.talvi.puffinplot.data.Datum;
import static java.lang.Math.toRadians;
import net.talvi.puffinplot.data.Correction;
import net.talvi.puffinplot.data.TreatType;
import net.talvi.puffinplot.data.Vec3;

/**
 *
 * @author pont
 */
class PmdLoader extends AbstractFileLoader {
    
    private PmdHeaderLine headerLine;
    private static final List<String> VALID_HEADERS =
            Arrays.asList(new String[] {
                " PAL  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   Dg    Ig    Ds    Is   a95 ",
                "STEP  Xc [Am2]  Yc [Am2]  Zc [Am2]  MAG[A/m]   Dg    Ig    Ds    Is  a95 ",
                "STEP  Xc (Am2)  Yc (Am2)  Zc (Am2)  MAG(A/m)   GDEC  GINC  SDEC  SINC a95 ",
                "STEP  Xc [Am²]  Yc [Am²]  Zc [Am²]  MAG[A/m]   Dg    Ig    Ds    Is  a95 "
            });
    
    public PmdLoader(InputStream inputStream,
            Map<Object, Object> importOptions) {
        /*
         * Some PMD files are in pure ASCII. All the non-ASCII ones I've
         * encountered use codepage 437 (a superset of ASCII), so we fortunately
         * don't need to deduce the encoding here: we can just use Cp437 to
         * cover both possibilities.
         */
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream, "Cp437"))) {
            /* BufferedReader.lines() handles all three common line terminators
             * automatically, so we don't need to worry about CRLFs etc.
             */
            final List<String> lines = reader.lines().collect(Collectors.toList());
            processLines(lines);
        } catch (IOException ex) {
            Logger.getLogger(PmdLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void processLines(List<String> lines) {
        // Line 0 contains an optional comment, which we ignore.
        // Line 1 is the first of two header lines.
        headerLine = PmdHeaderLine.read(lines.get(1));
        // Line 2 is the second of two header lines, and consists purely of
        // column headings. We check it against a list of known formats.
        if (!VALID_HEADERS.contains(lines.get(2))) {
            addMessage("Line 3: unknown header format");
            return;
        }
        for (String line: lines.subList(3, lines.size())) {
            if (line.length() == 1 && line.codePointAt(0) == 26) {
                /*
                 * Some PMD files have a final line consisting only
                 * of a 0x1A (^Z) character.
                 */
                break;
            }
            final PmdDataLine dataLine = PmdDataLine.read(line);
            final Datum d = new Datum(dataLine.moment.divideBy(headerLine.volume));
            d.setSampAz(headerLine.sampleAzimuth);
            d.setSampDip(90 - headerLine.sampleHade);
            d.setFormAz((headerLine.formationStrike + 90) % 360);
            d.setFormDip(headerLine.formationDip);
            d.setTreatType(dataLine.treatmentType);
            switch (d.getTreatType()) {
                case DEGAUSS_XYZ:
                    d.setAfX(dataLine.treatmentLevel/1000.);
                    d.setAfY(dataLine.treatmentLevel/1000.);
                    d.setAfZ(dataLine.treatmentLevel/1000.);
                    break;
                case THERMAL:
                    d.setTemp(dataLine.treatmentLevel);
                    break;
            }
            final Vec3 sampleCorrected = dataLine.moment.correctSample(
                    toRadians(headerLine.sampleAzimuth),
                    toRadians(90 - headerLine.sampleHade));
            final Vec3 formationCorrected = sampleCorrected.correctForm(
                    toRadians(headerLine.formationStrike + 90),
                    toRadians(headerLine.formationDip));
            checkConsistency(sampleCorrected.getDecDeg(),
                    dataLine.sampleCorrectedDeclination, 0.3);
            checkConsistency(sampleCorrected.getIncDeg(),
                    dataLine.sampleCorrectedInclination, 0.3);
            checkConsistency(formationCorrected.getDecDeg(),
                    dataLine.formationCorrectedDeclination, 0.3);
            checkConsistency(formationCorrected.getIncDeg(),
                    dataLine.formationCorrectedInclination, 0.3);
            checkConsistency(d.getIntensity(), dataLine.magnetization, 
                    Math.max(d.getIntensity(), dataLine.magnetization)/100);
            addDatum(d);
        }
    }
    
    private void checkConsistency(double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new IllegalArgumentException(String.format("Inconsistent data (calculated %g, found %g)", expected, actual));
        }
    }
    
}
