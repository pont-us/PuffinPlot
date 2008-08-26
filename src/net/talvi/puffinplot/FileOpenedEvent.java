package net.talvi.puffinplot;

import java.util.EventObject;

public class FileOpenedEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	public FileOpenedEvent(Object source) {
		super(source);
	}

}
