/* This file is part of PuffinPlot, a program for palaeomagnetic
 * data plotting and analysis. Copyright 2012-2017 Pontus Lurcock.
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

import java.io.BufferedInputStream;
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
 * A worker to download a file in the background. Written to
 * download the Jython jar.
 */
class DownloadWorker extends SwingWorker<Void, Void> {

    private final URL url;
    private final Path outputPath;
    private final static Logger logger =
            Logger.getLogger(IdToFileMap.class.getName());

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
        BufferedInputStream inStream = null;
        FileOutputStream outStream = null;
        final int totalSize = getFileSize();
        try {
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            outStream = new FileOutputStream(outputPath.toFile());
            final byte[] data = new byte[1024];
            long count;
            long totalTransferred = 0;
            logger.log(Level.INFO, "Starting Jython download. Size {0}", totalSize);
            while (true) {
                logger.fine(String.format("Jython download: %d", totalTransferred));
                count = outStream.getChannel().transferFrom(rbc, totalTransferred, 256 * 1024);
                totalTransferred += count;
                setProgress(Math.min(100, (int) (100 * totalTransferred / totalSize)));
                if (totalTransferred == totalSize) {
                    logger.info(String.format("%d == %d", totalTransferred, totalSize));
                    break;
                }
            }
            logger.info(String.format("Jython download complete: %d / %d", totalTransferred, totalSize));
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            // TODO Do seomthing about RuntimeExceptions in this method --
            // by default they are swallowed silently in a SwingWorker!
            // See https://baptiste-wicht.com/posts/2010/09/a-better-swingworker.html
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
        setProgress(100);
        return null;
    }
}
