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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
class PmdHeaderLine {

    private final static String NUMBER_REGEX =
            "([0-9 -][0-9 -][0-9 ][.][0-9])   ";
    private final static String REGEX = String.format(
            "(.........) [aα]=%s[bß]=%ss=%sd=%s" +
            "v=(.......)m3(   ?(\\d\\d)[-/](\\d\\d)[-/](\\d\\d\\d\\d) "+
            "(\\d\\d):(\\d\\d))?",
            NUMBER_REGEX, NUMBER_REGEX, NUMBER_REGEX, NUMBER_REGEX);
    
    static PmdHeaderLine read(String input) {
        final Pattern pattern = Pattern.compile(REGEX);
        final Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            return null;
        }
        return new PmdHeaderLine(matcher.group(1).trim(),
                Double.parseDouble(matcher.group(2)),
                Double.parseDouble(matcher.group(3)),
                Double.parseDouble(matcher.group(4)),
                Double.parseDouble(matcher.group(5)),
                Double.parseDouble(matcher.group(6)));
    }

    public final String name;
    public final double sampleAzimuth; // sampleAzimuth of specimen x direction
    public final double sampleHade; // sampleHade of specimen x direction
    public final double formationStrike; // formationStrike
    public final double formationDip; // formationDip
    public final double volume; // volume

    public PmdHeaderLine(String name, double a, double b, double s,
            double d, double v) {
        this.name = name;
        this.sampleAzimuth = a;
        this.sampleHade = b;
        this.formationStrike = s;
        this.formationDip = d;
        this.volume = v;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof PmdHeaderLine)) {
            return false;
        }
        final PmdHeaderLine otherLine = (PmdHeaderLine) other;
        return name.equals(otherLine.name) &&
                sampleAzimuth == otherLine.sampleAzimuth &&
                sampleHade == otherLine.sampleHade &&
                formationStrike == otherLine.formationStrike &&
                formationDip == otherLine.formationDip &&
                volume == otherLine.volume;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.sampleAzimuth) ^ (Double.doubleToLongBits(this.sampleAzimuth) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.sampleHade) ^ (Double.doubleToLongBits(this.sampleHade) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.formationStrike) ^ (Double.doubleToLongBits(this.formationStrike) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.formationDip) ^ (Double.doubleToLongBits(this.formationDip) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.volume) ^ (Double.doubleToLongBits(this.volume) >>> 32));
        return hash;
    }

}
