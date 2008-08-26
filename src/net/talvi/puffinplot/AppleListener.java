package net.talvi.puffinplot;

import java.io.File;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class AppleListener extends ApplicationAdapter {

	private static AppleListener appleListener;
	private static com.apple.eawt.Application eawtApp;
	private PuffinApp puffinApp;
	
	private AppleListener (PuffinApp puffinApp) {
		this.puffinApp = puffinApp;
	}
	
	public static void initialize(PuffinApp puffinApp) {
		if (eawtApp == null) eawtApp = new com.apple.eawt.Application();
		if (appleListener == null) appleListener = new AppleListener(puffinApp);
		eawtApp.addApplicationListener(appleListener);
		eawtApp.setEnabledPreferencesMenu(true);
	}
	
//	public void handleAbout(ApplicationEvent event) {
//		if (puffinApp != null) {
//			event.setHandled(true);
//			puffinApp.about();
//		} else throw new IllegalStateException("handleAbout can't find the PuffinApp.");
//	}
	
	// public void handleOpenApplication(ApplicationEvent event) {}
    
	public void handleOpenFile(ApplicationEvent event) {
		puffinApp.openFiles(new File(event.getFilename()));
	}

	public void handlePreferences(ApplicationEvent event) {
		if (puffinApp != null) {
			puffinApp.preferences();
			event.setHandled(true);
		} else throw new IllegalStateException("handlePreferences can't find the PuffinApp.");
	}

	// public void handlePrintFile(ApplicationEvent event) {};
	
	public void handleQuit(ApplicationEvent event) {
		if (puffinApp != null) {
			event.setHandled(false); // "Don't do the quit, we will handle it our way."
			puffinApp.quit();
		} else throw new IllegalStateException("handleQuit can't find the PuffinApp.");
	}
}
