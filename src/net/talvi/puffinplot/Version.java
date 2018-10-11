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
package net.talvi.puffinplot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.UnaryOperator;

/**
 * A class containing information about PuffinPlot's version.
 */
public class Version {

    private final String versionString;
    private final String dateString;
    private final String yearRange;
    
    private Version(String versionString, String dateString, String yearRange) {
        this.versionString = versionString;
        this.dateString = dateString;
        this.yearRange = yearRange;
    }

    public static Version fromMercurialProperties(
            UnaryOperator<String> propertyFetcher) {
        final String hgRevWithDirtyFlag =
                propertyFetcher.apply("build.hg.revid");
        final String hgDate = propertyFetcher.apply("build.hg.date");
        final String hgTag = propertyFetcher.apply("build.hg.tag");
        final boolean modified = hgRevWithDirtyFlag.endsWith("+");
        final String hgRev = hgRevWithDirtyFlag.replace("+", "");
        final String versionString =
                hgTag.startsWith("version_") && !modified ?
                hgTag.substring(8) :
                hgRev + (modified ? " (modified)" : "");
        /*
         * The filtered hgdate format consists of an epoch time in UTC, a space,
         * and a timezone offset in seconds. We don't care about the timezone,
         * so we just take the first part.
         */
        final String hgEpochDate = hgDate.split(" ")[0];
        final String buildDate = propertyFetcher.apply("build.date");
        String dateString =  buildDate +
                " (date of build; revision date not available)";
        try {
            final Date date = new Date(Long.parseLong(hgEpochDate) * 1000);
            final DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm");
            dateString = df.format(date);
        } catch (NumberFormatException ex) {
            // Nothing to do -- we just fall back to the default string.
        }
        
        final String year = buildDate==null || "unknown".equals(buildDate) ?
                "2012" : buildDate.substring(0, 4);
        final String yearRange = "2012".equals(year) ? "2012" : "2012–"+year;
        return new Version(versionString, dateString, yearRange);
    }

    /**
     * @return a string representing this Version
     */
    public String getVersionString() {
        return versionString;
    }
    
    /**
     * @return a string representing the release date of this Version
     */
    public String getDateString() {
        return dateString;
    }
    
    /**
     * @return a string representing the copyright year range of this Version
     */
    public String getYearRange() {
        return yearRange;
    }
}
