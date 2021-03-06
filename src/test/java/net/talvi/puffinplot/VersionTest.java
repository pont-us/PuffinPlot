/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2019 Pontus Lurcock.
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.talvi.puffinplot.testdata.TestFileLocator;

public class VersionTest {
    
    @Test
    public void testFromMercurialProperties() {
        final UnaryOperator<String> fetcher = makeFetcher(
                "build.hg.revid", "38547480edf2+",
                "build.hg.date", "1538903357 -7200",
                "build.hg.tag", "tip"
                );
        final Version version = Version.fromMercurialProperties(fetcher);
        assertEquals("2018.10.07 09:09 UTC", version.getDateString());
        assertEquals("38547480edf2 (modified)", version.getVersionString());
        assertEquals("2012–2018", version.getYearRange());
    }

    @Test
    public void testFromGitPropertiesNoTagClean() {
        final UnaryOperator<String> fetcher = makeFetcher(
                "build.git.committerdate", "1538903357 +0200",
                "build.git.tag", "undefined",
                "build.git.hash", "6e856a173a601453ab5736f359bcdf8484029bce",
                "build.git.dirty", "false"
                );
        final Version version = Version.fromGitProperties(fetcher);
        assertEquals("2018.10.07 09:09 UTC", version.getDateString());
        assertEquals("6e856a173a60", version.getVersionString());
        assertEquals("2012–2018", version.getYearRange());
    }

    @Test
    public void testFromGitPropertiesNoTagDirty() {
        final UnaryOperator<String> fetcher = makeFetcher(
                "build.git.committerdate", "1538903357",
                "build.git.tag", "undefined",
                "build.git.hash", "6e856a173a601453ab5736f359bcdf8484029bce",
                "build.git.dirty", "true"
                );
        final Version version = Version.fromGitProperties(fetcher);
        assertEquals("2018.10.07 09:09 UTC", version.getDateString());
        assertEquals("6e856a173a60 (modified)", version.getVersionString());
        assertEquals("2012–2018", version.getYearRange());
    }

    @Test
    public void testFromGitPropertiesWithTagClean() {
        final UnaryOperator<String> fetcher = makeFetcher(
                "build.git.committerdate", "1429795203",
                "build.git.tag", "version_1.03",
                "build.git.hash", "cda470416770ba19896dc9a15b34cd99258dbc20",
                "build.git.dirty", "false"
                );
        final Version version = Version.fromGitProperties(fetcher);
        assertEquals("2015.04.23 13:20 UTC", version.getDateString());
        assertEquals("1.03", version.getVersionString());
        assertEquals("2012–2015", version.getYearRange());
    }

    @Test
    public void testFromGitPropertiesWithTagDirty() {
        final UnaryOperator<String> fetcher = makeFetcher(
                "build.git.committerdate", "1429795203 +0200",
                "build.git.tag", "version_1.03",
                "build.git.hash", "cda470416770ba19896dc9a15b34cd99258dbc20",
                "build.git.dirty", "true"
                );
        final Version version = Version.fromGitProperties(fetcher);
        assertEquals("2015.04.23 13:20 UTC", version.getDateString());
        assertEquals("cda470416770 (modified)", version.getVersionString());
        assertEquals("2012–2015", version.getYearRange());
    }

    private static UnaryOperator<String> makeFetcher(String... values) {
        final Map<String,String> map = new HashMap<>();
        for (int i=0; i<values.length; i += 2) {
            map.put(values[i], values[i+1]);
        }
        return s -> map.get(s);
    }
    
    @Test
    public void testFromGitOutput1() {
        final String commit =
                "tree cd3a763feb7cc288327c93ed024b832d3307cc99\n"
                + "parent df9e490523f1ea329a8249c5906dc28542dfdcb8\n"
                + "author Pontus Lurcock <pont@talvi.net> 1572778467 +0100\n"
                + "committer Pontus Lurcock <pont@talvi.net> 1572778467 +0100\n"
                + "\n"
                + "First line of commit message\n"
                + "\n"
                + "More lines of commit message. Let's include the string \"\n"
                + "committer \" at the start of a line to make sure it's\n"
                + "handled correctly.\n";
        final String tag = "HEAD undefined";
        final String hash = "fe12703f2d2e16345ba3177096e457cf055f5bce";
        final String status = "?? wibble";
        final String buildDate = "2019-11-05T13:19:14Z";
        final Version version =
                Version.fromGitOutput(commit, tag, hash, status, buildDate);
        assertNotNull(version);
        assertEquals("2019.11.03 10:54 UTC", version.getDateString());
        assertEquals("fe12703f2d2e (modified)", version.getVersionString());
        assertEquals("2012–2019", version.getYearRange());
    }

    @Test
    public void testFromGitOutput2() {
        final String commit = "deliberately malformed commit data";
        final String tag = "HEAD tags/version_1.4.1^0";
        final String hash = "f2aafaaa1b26d9bdccbfb1615334f4c3cdeaf033";
        final String status = "";
        final String buildDate = "2019-11-05T13:19:14Z";
        final Version version =
                Version.fromGitOutput(commit, tag, hash, status, buildDate);
        assertNotNull(version);
        assertEquals(buildDate +
                " (date of build; revision date not available)",
                version.getDateString());
        assertEquals("1.4.1", version.getVersionString());
        assertEquals("2012–2019", version.getYearRange());
    }
    
    @Test
    public void testFromGitFiles() throws IOException {
        final Version version = Version.fromGitFiles(TestFileLocator.class,
                "git-output-commit", "git-output-tag", "git-output-hash",
                "git-output-status", "2019-11-05T13:19:14Z");
        assertNotNull(version);
        assertEquals("2019.11.06 08:45 UTC", version.getDateString());
        assertEquals("c3bef92ca522 (modified)", version.getVersionString());
        assertEquals("2012–2019", version.getYearRange());
    }

}
