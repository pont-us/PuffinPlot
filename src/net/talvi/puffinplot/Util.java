/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2015 Pontus Lurcock.
 *
 * PuffinPlot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PuffinPlot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

/**
 * This class collects miscellaneous, general-purpose utility functions
 * which are useful to PuffinPlot.
 * 
 * @author pont
 */
public class Util {
    
    /**
     * Takes an integer, reduces it by one, and ensures it lies in the
     * range 0 <= i < upperLimit.
     */
    private static int constrainInt(int i, int upperLimit) {
        i -= 1;
        if (i<0) i = 0;
        if (i>= upperLimit) i = upperLimit-1;
        return i;
    }
    
    /**
     * <p>Converts a string specification of number ranges to a corresponding
     * {@link BitSet}. The specification is of the form commonly encountered
     * in Print dialog boxes: a sequence of comma-separated units. Each unit
     * can be either an integer (meaning that the corresponding item should
     * be selected) or two integers separated by a hyphen (-) character
     * (meaning that all items in the corresponding range should be 
     * selected). Example inputs and outputs are shown below.</p>
     * 
     * <table>
     * <caption>Example specifications</caption>
<tr><td> 1 </td><td> {@code 1} </td></tr>
<tr><td> 1,3 </td><td> {@code 101} </td></tr>
<tr><td> 4-6 </td><td> {@code 000111} </td></tr>
<tr><td> 4-6,8-10,10,11,15-16 </td><td> {@code 0001110111100011} </td></tr>
<tr><td> 1-4,3-5,10,12-14,17 </td><td> {@code 11111000010111001} </td></tr>
</table>
     * 
     * <p>Note that the range specifications are one-based (the first
     * item is specified by 1, not 0) but the {@link BitSet} output
     * is zero-based.</p>
     * 
     * <p>Since an error in the specification string might result in
     * an impractically large bitset, this method also takes a limit
     * argument; no bits will be set beyond the specified limit.</p>
     * 
     * @param input a specification string
     * @param limit upper limit for bits to set; {@code limit-1} will be the 
     * highest possible set bit
     * @return a bit-set representation of the specified range
     */
    public static BitSet numberRangeStringToBitSet(String input, int limit) {
        final Scanner sc = new Scanner(input);
        sc.useLocale(Locale.ENGLISH);
        sc.useDelimiter(", *");
        final BitSet result = new BitSet();
        final List<String> rangeExprs = new ArrayList<>();
        while (sc.hasNext()) {
            if (sc.hasNextInt()) {
                final int val = constrainInt(sc.nextInt(), limit);
                result.set(val);
            } else {
                rangeExprs.add(sc.next());
            }
        }
        for (String rangeExpr: rangeExprs) {
            int start = -1;
            int end = -1;
            final Scanner sc2 = new Scanner(rangeExpr);
            sc2.useLocale(Locale.ENGLISH);
            sc2.useDelimiter("-");
            if (sc2.hasNextInt()) start = constrainInt(sc2.nextInt(), limit);
            if (sc2.hasNextInt()) end = constrainInt(sc2.nextInt(), limit);
            if (start>-1 && end>-1) {
                result.set(start, end+1);
            }
        }
        return result;
    }
    
    /**
     * Clips a line to a supplied rectangle.
     * 
     * @param line a line (null if none)
     * @param r a clipping rectangle (null if none)
     * @return the line, as clipped to the supplied rectangle, if 
     * the two overlap; null if they do not overlap; null
     * if either of the input parameters were null
     */
    public static Line2D clipLineToRectangle(Line2D line, Rectangle2D r) {
        // Cohen-Sutherland algorithm, after the description in
        // Foley, van Dam, Feiner, Hughes, & Phillips
        if (line==null || r == null) return null;
        boolean accept = false, done = false;
        Outcode oc0 = new Outcode(line.getP1(), r),
                oc1 = new Outcode(line.getP2(), r),
                ocOut;
        double x = 0, y = 0,
                x0 = line.getX1(),
                y0 = line.getY1(),
                x1 = line.getX2(),
                y1 = line.getY2();
        do {
            if (oc0.inside() && oc1.inside()) {
                done = accept = true;
            } else if (oc0.sameSide(oc1)) {
                done = true;
            } else {
                ocOut = oc0.outside() ? oc0 : oc1;
                if (ocOut.top()) {
                    x = x0 + (x1 - x0) * (r.getMaxY() - y0) / (y1 - y0);
                    y = r.getMaxY();
                } else if (ocOut.bottom()) {
                    x = x0 + (x1 - x0) * (r.getMinY() - y0) / (y1 - y0);
                    y = r.getMinY();
                } else if (ocOut.right()) {
                    y = y0 + (y1 - y0) * (r.getMaxX() - x0) / (x1 - x0);
                    x = r.getMaxX();
                } else if (ocOut.left()) {
                    y = y0 + (y1 - y0) * (r.getMinX() - x0) / (x1 - x0);
                    x = r.getMinX();
                }
                if (ocOut.equals(oc0)) {
                    x0 = x;
                    y0 = y;
                    oc0 = new Outcode(x0, y0, r);
                } else {
                    x1 = x;
                    y1 = y;
                    oc1 = new Outcode(x1, y1, r);
                }
            }
        } while (!done);
        
        if (accept) return new Line2D.Double(x0, y0, x1, y1);
        else return null;
    }

    /**
     * A wrapper around {@code Double.parseDouble} which returns
     * a default value of {@code 0} if the supplied string cannot
     * be parsed. ({@code Double.parseDouble}, in contrast, throws
     * a {@code NumberFormatException}.)
     * 
     * @param s A string representation of a floating-point number.
     * @return A {@code double} with a value corresponding to the
     * supplied string, or {@code 0} if the string could not be 
     * interpreted as a floating-point number.
     */
    public static double parseDoubleSafely(String s) {
        double result = 0;
        try {
            result = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            // do nothing
        }
        return result;
    }

    /**
     * Return a directory where PuffinPlot can store files for "internal"
     * use. The directory is created if necessary. On *nix systems, a 
     * dot-prefixed directory in the user's home directory is used. On
     * Windows, a subdirectory of the LOCALAPPDATA directory is used.
     * 
     * @return Path to a directory for application data
     * @throws IOException if there was an error creating or finding the directory
     */
    public static Path getAppDataDirectory() throws IOException {
        final String parentDir;
        final String childDir;
        final String windowsAppDir = System.getenv("LOCALAPPDATA");
        /* Another possibility would be "APPDATA" or "AppData", but that
         * would return the directory for the roaming profile rather
         * than (as here) the local profile. The local profile seems most
         * appropriate, but either should work in practice.
         * http://stackoverflow.com/questions/11113974/ has more detail.
         */
        if (windowsAppDir != null) {
            // LOCALAPPDATA is non-null, so we're on a Windows system.
            parentDir = windowsAppDir;
            // This directory is shared, so we create our own subdirectory.
            childDir = "PuffinPlot";
        } else {
            // LOCALAPPDATA is null. Assume we're on a *nix system and
            // can create a dot-directory in the user's home directory.
            parentDir = System.getProperty("user.home");
            childDir = ".puffinplot";
        }
        final Path parentPath = Paths.get(parentDir);
        final Path childPath = Paths.get(childDir);
        final Path appDataPath = parentPath.resolve(childPath);
        if (!Files.isDirectory(appDataPath)) {
            Files.createDirectory(appDataPath);
        }
        return appDataPath;
    }


    private static class Outcode {
        private final int bitField;
        
        public Outcode(Point2D p, Rectangle2D r) {
            this(p.getX(), p.getY(), r);
        }
        
        public Outcode(double x, double y, Rectangle2D r) {
            bitField = (y > r.getMaxY() ? 8 : 0) |
               (y < r.getMinY() ? 4 : 0) |
               (x > r.getMaxX() ? 2 : 0) |
               (x < r.getMinX() ? 1 : 0);
        }
        
        public boolean top() { return (bitField & 8) != 0; }
        public boolean bottom() { return (bitField & 4) != 0; }
        public boolean right() { return (bitField & 2) != 0; }
        public boolean left() { return (bitField & 1) != 0; }
        public boolean inside() { return bitField == 0; }
        public boolean outside() { return bitField != 0; }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Outcode) {
                final Outcode oc = (Outcode) o;
                return (bitField == oc.bitField);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + this.bitField;
            return hash;
        }
        
        public boolean sameSide(Outcode oc) {
            return (bitField & oc.bitField) != 0;
        }
    }
    
    /**
     * Returns the rectangular envelope for the supplied points.
     * This is the smallest rectangle which contains all the points.
     * 
     * @param points a set of points
     * @return the smallest rectangle which contains all the points,
     * or null if no points were supplied
     */
    public static Rectangle2D envelope(Collection<Point2D> points) {
        if (points.isEmpty()) return null;
        double x0, y0, x1, y1;
        x0 = y0 = Double.POSITIVE_INFINITY;
        x1 = y1 = Double.NEGATIVE_INFINITY;
        for (Point2D p: points) {
            final double x = p.getX();
            final double y = p.getY();
            if (x < x0) x0 = x;
            if (x > x1) x1 = x;
            if (y < y0) y0 = y;
            if (y > y1) y1 = y;
        }
        return new Rectangle2D.Double(x0, y0, x1-x0, y1-y0);
    }
    
    /**
     * Scales a line around its midpoint.
     * 
     * Given a line with length L, returns a line with length scale*L,
     * with the same midpoint and direction as the provided line.
     * 
     * @param line the line to scale
     * @param scale the scale factor
     * @return the scaled line
     */
    public static Line2D scaleLine(Line2D line, double scale) {
        if (scale==1 || line==null) return line;
        final double x0 = line.getX1();
        final double y0 = line.getY1();
        final double x1 = line.getX2();
        final double y1 = line.getY2();
        final double xmid = (x0+x1)/2;
        final double ymid = (y0+y1)/2;
        final double xd = (x1-x0) * 0.5 * scale;
        final double yd = (y1-y0) * 0.5 * scale;
        return new Line2D.Double(xmid-xd, ymid-yd, xmid+xd, ymid+yd);
    }
    
    private static int numericalPermissions(Collection<PosixFilePermission> perms) {
        int result = 0;
        for (PosixFilePermission pfp: perms) {
            switch (pfp) {
                case OWNER_READ:
                    result |= 1<<8;
                    break;
                case OWNER_WRITE:
                    result |= 1<<7;
                    break;
                case OWNER_EXECUTE:
                    result |= 1<<6;
                    break;
                case GROUP_READ:
                    result |= 1<<5;
                    break;
                case GROUP_WRITE:
                    result |= 1<<4;
                    break;
                case GROUP_EXECUTE:
                    result |= 1<<3;
                    break;
                case OTHERS_READ:
                    result |= 1<<2;
                    break;
                case OTHERS_WRITE:
                    result |= 1<<1;
                    break;
                case OTHERS_EXECUTE:
                    result |= 1;
                    break;
            }
        }
        return result;
    }
    
    public static void zipDirectory(final Path dir, Path zipFile)
            throws IOException {
        final byte[] buffer = new byte[1024];
        
        // We use the Apache Commons compress library rather than the built-in
        // java.util zip library, because the latter doesn't support Posix
        // file permissions.
        
        try (final ZipArchiveOutputStream zipStream =
                new ZipArchiveOutputStream(zipFile.toFile())) {
            
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path,
                        BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile()) {
                        final Path relPath = dir.relativize(path);
                        final ZipArchiveEntry entry =
                                new ZipArchiveEntry(relPath.toString());
                        
                        entry.setUnixMode(numericalPermissions(Files.getPosixFilePermissions(path)));
                        zipStream.putArchiveEntry(entry);
                        final Path fullFilePath = dir.resolve(path);
                        try (final FileInputStream inStream =
                                new FileInputStream(fullFilePath.toFile())) {
                            int len;
                            while ((len = inStream.read(buffer)) > 0) {
                                zipStream.write(buffer, 0, len);
                            }
                        }
                        zipStream.closeArchiveEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    /**
     * Calculate the SHA-1 digest of a file and return it as an
     * upper-case hexadecimal string.
     * 
     * @param file the file for which to calculate the digest
     * @return the SHA-1 digest
     * @throws IOException if there is an error reading the file
     * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available
     */
    public static String calculateSHA1(File file)
            throws IOException, NoSuchAlgorithmException {
        final MessageDigest digest;
        try (InputStream inputStream = new FileInputStream(file)) {
            final byte[] buffer = new byte[1024];
            digest = MessageDigest.getInstance("SHA-1");
            int count;
            do {
                count = inputStream.read(buffer);
                if (count > 0) {
                    digest.update(buffer, 0, count);
                }
            } while (count != -1);
        }
        // printHexBinary seems to return upper case in practice, but I can't
        // find this officially guaranteed anywhere, so safest to ensure it
        // with a toUpperCase().
        return javax.xml.bind.DatatypeConverter.
                printHexBinary(digest.digest()).toUpperCase();
    }
    
    /**
     * 
     * @param url
     * @param outputPath 
     */
    public static void downloadUrlToFile(String url, Path outputPath) throws IOException {

        // NOTE: Files.copy can block indefinitely. Should this be in a new thread?
       
        
        // Use REPLACE_EXISTING?
        
               // if (!Files.exists(jythonPath)) {
            final URI uri = URI.create(url);
            try (InputStream in = uri.toURL().openStream()) {
                Files.copy(in, outputPath);
            }
        //}
    }

    static Object parseGitTimestamp(String gitTimestap) {
        final String[] parts = gitTimestap.split(" ");
        if (parts.length != 2) {
            throw new IllegalArgumentException(String.format(
                    "Malformed git timestamp \"%s\": should have 2 parts,"
                            + "not %d.", gitTimestap, parts.length));
        }
        final long epochSeconds = Long.parseLong(parts[0]);
        final String timeZone = parts[1];
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.of(timeZone));
    }


}
