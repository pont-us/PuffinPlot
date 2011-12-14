package net.talvi.puffinplot;

import java.io.IOException;

/**
 * An exception representing a condition which is not fatal, but
 * which needs to be caught and reported to the user.
 * 
 * @author pont
 */
public class PuffinUserException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a PuffinPlot user exception with the specified message.
     * @param message the message to use for the new exception
     */
    public PuffinUserException(String message) {
        super(message);
    }
    
    /**
     * Creates a PuffinPlot user exception wrapping the supplied exception.
     * The detail message will be taken from the supplied exception.
     * @param exception the exception to wrap
     */
    public PuffinUserException(IOException exception) {
        super(exception);
    }
    
}
