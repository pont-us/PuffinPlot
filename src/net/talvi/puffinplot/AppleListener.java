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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PuffinPlot.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.talvi.puffinplot;

import java.io.File;
import java.util.List;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import java.util.Collections;

/**
 * AppleListener handles the user actions About, Open, Preferences, and
 * Quit on Mac OS X systems, passing them on to the appropriate methods
 * of a PuffinApp instance specified on initialization.
 * 
 * @author pont
 */

public class AppleListener extends ApplicationAdapter {

    private static AppleListener appleListener;
    private static com.apple.eawt.Application eawtApp;
    private final PuffinApp puffinApp;
	
    private AppleListener(PuffinApp puffinApp) {
        this.puffinApp = puffinApp;
    }
	
    /**
     * Instantiates a new AppleListener, which will send events to 
     * the supplied PuffinApp instance. When the user initiates
     * an About, Open, Preferences, or Quit action, the AppleListener
     * will call the corresponding method of the PuffinApp.
     * 
     * @param puffinApp the PuffinApp instance to control using
     * Apple events
     */
    public static void initialize(PuffinApp puffinApp) {
        if (eawtApp == null) eawtApp = new com.apple.eawt.Application();
        if (appleListener == null) appleListener = new AppleListener(puffinApp);
        eawtApp.addApplicationListener(appleListener);
        eawtApp.setEnabledPreferencesMenu(true);
    }
	
    /**
     * Handles an Apple About action by calling {@code PuffinApp.about()}
     * @param event the event corresponding to the About action
     * @see PuffinApp#about()
     */
    @Override
    public void handleAbout(ApplicationEvent event) {
        if (puffinApp != null) {
            event.setHandled(true);
            puffinApp.about();
        } else
            throw new IllegalStateException("handleAbout can't find the PuffinApp.");
    }
	
    // TODO: Implement this?
    // public void handleOpenApplication(ApplicationEvent event) {}
    
    /**
     * Handles an Apple Open File action by calling {@code PuffinApp.openFiles}
     * @param event the event corresponding to the OpenFile action
     * @see PuffinApp#openFiles(List files, boolean createNewSuite)
     */
    @Override
    public void handleOpenFile(ApplicationEvent event) {
        puffinApp.openFiles(Collections.singletonList(new File(event.getFilename())),
                true);
    }

    /**
     * Handles an Apple Preferences action by calling {@code PuffinApp.preferences()}
     * @param event the event corresponding to the Preferences action
     * @see PuffinApp#showPreferences()
     */
    @Override
    public void handlePreferences(ApplicationEvent event) {
        if (puffinApp != null) {
            puffinApp.showPreferences();
            event.setHandled(true);
        } else
            throw new IllegalStateException("handlePreferences can't find the PuffinApp.");
    }

    // TODO: Implement this?
    // public void handlePrintFile(ApplicationEvent event) {};
	
    /**
     * Handles an Apple Quit action by calling {@code PuffinApp.quit()}
     * @param event the event corresponding to the Quit action
     * @see PuffinApp#quit()
     */
    @Override
    public void handleQuit(ApplicationEvent event) {
        if (puffinApp != null) {
            event.setHandled(false); // "Don't do the quit, we will handle it our way."
            puffinApp.quit();
        } else
            throw new IllegalStateException("handleQuit can't find the PuffinApp.");
    }
}
