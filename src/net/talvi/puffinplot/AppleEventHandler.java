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

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import java.util.List;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

/**
 * AppleEventHandler handles the user actions About, Open, Preferences, and Quit
 * on Mac OS X systems, passing them on to the appropriate methods of a
 * PuffinApp instance specified on initialization.
 *
 * @author pont
 */
public class AppleEventHandler implements
        PreferencesHandler, OpenFilesHandler, AboutHandler, QuitHandler {

    private static AppleEventHandler appleListener;
    private static com.apple.eawt.Application eawtApp;
    private final PuffinApp puffinApp;

    private AppleEventHandler(PuffinApp puffinApp) {
        this.puffinApp = puffinApp;
    }

    /**
     * Instantiates a new AppleEventHandler, which will send events to the
     * supplied PuffinApp instance. When the user initiates an About, Open,
     * Preferences, or Quit action, the AppleEventHandler will call the
     * corresponding method of the PuffinApp.
     *
     * @param puffinApp the PuffinApp instance to control using Apple events
     */
    public static void initialize(PuffinApp puffinApp) {
        if (eawtApp == null) {
            eawtApp = new com.apple.eawt.Application();
        }
        if (appleListener == null) {
            appleListener = new AppleEventHandler(puffinApp);
        }
        eawtApp.setPreferencesHandler(appleListener);
        eawtApp.setEnabledPreferencesMenu(true);
        eawtApp.setOpenFileHandler(appleListener);
        eawtApp.setAboutHandler(appleListener);
        eawtApp.setQuitHandler(appleListener);
    }

    /**
     * Handles an Apple About action by calling {@code PuffinApp.about()}
     *
     * @param event the event corresponding to the About action
     * @see PuffinApp#about()
     */
    @Override
    public void handleAbout(AboutEvent event) {
        if (puffinApp != null) {
            //event.setHandled(true);
            puffinApp.about();
        } else {
            throw new IllegalStateException(
                    "handleAbout can't find the PuffinApp.");
        }
    }

    /**
     * Handles an Apple Open File action by calling {@code PuffinApp.openFiles}
     *
     * @param event the event corresponding to the OpenFile action
     * @see PuffinApp#openFiles(List files, boolean createNewSuite)
     */
    @Override
    public void openFiles(OpenFilesEvent event) {
        if (puffinApp != null) {
            puffinApp.openFiles(event.getFiles(), true);
        } else {
            throw new IllegalStateException(
                    "handlePreferences can't find the PuffinApp.");
        }
    }

    /**
     * Handles an Apple Preferences action by calling
     * {@code PuffinApp.preferences()}
     *
     * @param event the event corresponding to the Preferences action
     * @see PuffinApp#showPreferences()
     */
    @Override
    public void handlePreferences(AppEvent.PreferencesEvent event) {
        if (puffinApp != null) {
            puffinApp.showPreferences();
        } else {
            throw new IllegalStateException(
                    "handlePreferences can't find the PuffinApp.");
        }
    }

    /**
     * Handles an Apple Quit action by calling {@code PuffinApp.quit()}
     *
     * @param event the event corresponding to the Quit action
     * @param response object to receive our response to the request
     * @see PuffinApp#quit()
     */
    @Override
    public void handleQuitRequestWith(QuitEvent event, QuitResponse response) {
        if (puffinApp != null) {
            puffinApp.quit();
            // If the quit is successful, PuffinApp will do it with a 
            // System.exit so we won't get past the call to quit().
            // If we do get here, it means the quit was aborted by the user
            // after a warning about unsaved data. In this case, we cancel
            // the quit.
            response.cancelQuit();
        } else {
            throw new IllegalStateException(
                    "handleQuit can't find the PuffinApp.");
        }
    }
}
