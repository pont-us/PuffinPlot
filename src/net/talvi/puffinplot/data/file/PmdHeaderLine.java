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
            "([0-9 ][0-9 ][0-9 ][.][0-9])   ";
    private final static String REGEX = String.format(
            "(.........) [aα]=%s[bβ]=%ss=%sd=%s" +
            "v=(.......)m3   ?(\\d\\d)[-/](\\d\\d)[-/](\\d\\d\\d\\d) "+
            "(\\d\\d):(\\d\\d)",
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
    private final double azimuth; // azimuth of specimen x direction
    private final double hade; // hade of specimen x direction
    private final double strike; // strike
    private final double dip; // dip
    private final double v; // volume

    public PmdHeaderLine(String name, double a, double b, double s,
            double d, double v) {
        this.name = name;
        this.azimuth = a;
        this.hade = b;
        this.strike = s;
        this.dip = d;
        this.v = v;
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
                azimuth == otherLine.azimuth &&
                hade == otherLine.hade &&
                strike == otherLine.strike &&
                dip == otherLine.dip &&
                v == otherLine.v;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 47 * hash + Objects.hashCode(this.name);
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.azimuth) ^ (Double.doubleToLongBits(this.azimuth) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.hade) ^ (Double.doubleToLongBits(this.hade) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.strike) ^ (Double.doubleToLongBits(this.strike) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.dip) ^ (Double.doubleToLongBits(this.dip) >>> 32));
        hash = 47 * hash + (int) (Double.doubleToLongBits(this.v) ^ (Double.doubleToLongBits(this.v) >>> 32));
        return hash;
    }

}
