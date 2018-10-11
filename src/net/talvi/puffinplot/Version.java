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
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;
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

    /**
     * Provides a version object corresponding to some supplied Mercurial
     * build properties. This factory takes as its argument a property
     * fetcher function which turns a property name into its value. It
     * returns a version object with values initialized from the values
     * of properties named {@code build.hg.revid} (revision ID),
     * {@code build.hg.date} (date of revision), {@code build.hg.tag}
     * (version tag of revision, if any), and {@code build.date}
     * (date of build).
     * 
     * @param propertyFetcher a function which returns property values
     *          for supplied property names
     * @return a version object corresponding to the values returned by the
     *          property fetcher
     */
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
            final DateFormat df =
                    new SimpleDateFormat("yyyy.MM.dd HH:mm 'UTC'");
            df.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
            /*
             * It seems to me to make the most sense to display the commit
             * date in UTC. There's no obvious reason to display it either
             * in the committer's timezone (given in the second part of
             * the build.hg.date property) or in the JVM's current timezone
             * (as would be the case if I didn't explicitly set it to
             * GMT+0000).
             */
            dateString = df.format(date);
        } catch (NumberFormatException ex) {
            // Nothing to do -- we just fall back to the default string.
        }
        
        return new Version(versionString, dateString,
                makeYearRange(dateString));
    }

    /**
     * Provides a version object corresponding to some supplied git
     * build properties. This factory takes as its argument a property fetcher
     * function which turns a property name into its value. It returns a version
     * object with values initialized from the values of properties named
     * {@code build.git.hash} (revision ID), {@code build.git.committerdate}
     * (committer date of revision), {@code build.git.tag} (version tag of
     * revision, if any), and {@code build.date} (date of build).
     *
     * @param propertyFetcher a function which returns property values
     *          for supplied property names
     * @return a version object corresponding to the values returned by the
     *          property fetcher
     */
    public static Version fromGitProperties(
            UnaryOperator<String> propertyFetcher) {
        final String rawCommitterDate =
                propertyFetcher.apply("build.git.committerdate");
        final String rawTag =
                propertyFetcher.apply("build.git.tag");
        final String rawHash = propertyFetcher.apply("build.git.hash");
        final String rawDirtyFlag = propertyFetcher.apply("build.git.dirty");
        final String rawBuildDate = propertyFetcher.apply("build.date");
        final boolean modified = Boolean.parseBoolean(rawDirtyFlag);
        final String shortHash = rawHash.substring(0, 12);
        
        final String versionString =
                rawTag.startsWith("HEAD tags/version_") && !modified ?
                rawTag.substring(18) :
                shortHash + (modified ? " (modified)" : "");
        
        String dateString = rawBuildDate +
                " (date of build; revision date not available)";
        try {
            final ZonedDateTime date = Util.parseGitTimestamp(rawCommitterDate);
            dateString = date.format(DateTimeFormatter.
                    ofPattern("yyyy.MM.dd HH:mm 'UTC'").
                    withZone(ZoneId.of("Z")));
        } catch (IllegalArgumentException | DateTimeException e) {
            // Do nothing -- fallback date string will be retained.
        }
        
        return new Version(versionString, dateString,
                makeYearRange(dateString));
    }
    
    private static String makeYearRange(String date) {
        final String year =
                date.startsWith("unknown") ? "2012" : date.substring(0, 4);
        final String yearRange = "2012".equals(year) ? "2012" : "2012–"+year;
        return yearRange;
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
