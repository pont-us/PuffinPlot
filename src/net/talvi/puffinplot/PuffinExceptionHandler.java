/**
 * 
 */
package net.talvi.puffinplot;

public class PuffinExceptionHandler {

	public PuffinExceptionHandler() {}
	
	public void handle(Throwable t) {
		PuffinApp.getInstance().errorDialog("PuffinPlot error", t.toString());
		System.exit(1);
	}
}