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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.talvi.puffinplot.data.Vec3;
import org.junit.rules.TemporaryFolder;

/**
 * Utility methods used by unit tests.
 * 
 * @author pont
 */
public class TestUtils {
    
    public static class ListHandler extends Handler {

        public final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override public void flush() {}

        @Override public void close() throws SecurityException {}
        
        public boolean wasOneWarningLogged() {
            return records.size() == 1 &&
                    records.get(0).getLevel() == Level.WARNING;
        }
        
        public static ListHandler createAndAdd() {
            final ListHandler handler = new ListHandler();
            Logger.getLogger("net.talvi.puffinplot").addHandler(handler);
            return handler;
        }
    }
    

    
    public static Vec3 randomVector(Random rnd, double max) {
        return new Vec3(rnd.nextDouble()*2*max-max,
                        rnd.nextDouble()*2*max-max,
                        rnd.nextDouble()*2*max-max);
    }

    public static boolean isPrintableAscii(String string) {
        return string.chars().allMatch((int c) -> c >= 20 && c < 127);
    }
    
    public static boolean equalOrOpposite(Vec3 v0, Vec3 v1, double delta) {
        return v0.equals(v1, delta) || v0.invert().equals(v1, delta);
    }
    
    public static List<Vec3> makeVectorList(double[][] components, boolean normalize) {
        final List<Vec3> result = new ArrayList<Vec3>(components.length);
        for (double[] triplet: components) {
            assert(triplet.length == 3);
            final Vec3 v = new Vec3(triplet[0], triplet[1], triplet[2]);
            result.add(normalize ? v.normalize() : v);
        }
        return result;
    }
    
    public static File writeStringToTemporaryFile(String fileName,
            String fileContents,
            TemporaryFolder temporaryFolder) throws IOException {
        final Path filePath = temporaryFolder.getRoot().toPath().
                resolve(fileName);
        Files.write(filePath, fileContents.getBytes(),
                StandardOpenOption.CREATE);
        return filePath.toFile();
    }
}
