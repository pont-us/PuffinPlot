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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * A SwingWorker which downloads a file from a specified URL and
 * saves it to a specified path.
 */
public class DownloadWorker extends SwingWorker<Void, Void> {

    private final URL url;
    private final Path outputPath;
    private final static Logger logger =
            Logger.getLogger(IdToFileMap.class.getName());
    private IOException exception = null;

    /**
     * Create a worker which will download a a file from a given URL
     * to a given path.
     * 
     * @param url the URL from which to download the file
     * @param outputPath the path to which to save the file
     */
    public DownloadWorker(URL url, Path outputPath) {
        this.url = url;
        this.outputPath = outputPath;
    }

    private int getFileSize() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    protected Void doInBackground() {
        setProgress(0);
        final int totalSize = getFileSize();
        final File outputFile = outputPath.toFile();
        try (FileOutputStream outStream = new FileOutputStream(outputFile);
                ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
           
            long count;
            long totalTransferred = 0;
            logger.log(Level.INFO, "Starting download. Size {0}", totalSize);
            while (true) {
                if (isCancelled()) {
                    logger.log(Level.INFO, "Download cancelled, aborting.");
                    // The try-with-resources construct will close the
                    // stream and byte channel automatically when we return.
                    return null;
                }
                logger.fine(String.format("Download: %d", totalTransferred));
                count = outStream.getChannel().
                        transferFrom(rbc, totalTransferred, 256 * 1024);
                totalTransferred += count;
                setProgress(Math.min(100,
                        (int) (100 * totalTransferred / totalSize)));
                if (totalTransferred == totalSize) {
                    logger.info(String.format("Download: %d == %d",
                            totalTransferred, totalSize));
                    break;
                }
            }
            logger.info(String.format("Download complete: %d / %d",
                    totalTransferred, totalSize));
        } catch (IOException ex) {
            exception = ex;
            if (isCancelled()) {
                logger.log(Level.INFO,
                        "Download was cancelled and exception was thrown.");
            }
            logger.log(Level.SEVERE, "I/O Exception during download", ex);
            
            // TODO Do something about RuntimeExceptions in this method --
            // by default they are swallowed silently in a SwingWorker!
            // See https://baptiste-wicht.com/posts/2010/09/a-better-swingworker.html
        }
        setProgress(100);
        return null;
    }

    /**
     * If an exception was thrown during the download process, this method will
     * return it.
     * 
     * @return the exception which was thrown, or null if none was thrown
     */
    public IOException getException() {
        return exception;
    }
}
