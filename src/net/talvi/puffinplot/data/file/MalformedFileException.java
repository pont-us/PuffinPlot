package net.talvi.puffinplot.data.file;

/**
 * This exception is thrown when a file format does not conform to
 * the expectations of a file loader.
 * 
 * @author pont
 */
public class MalformedFileException extends Exception {
    private static final long serialVersionUID = 1L;
    
    /** Creates a new malformed file exception with a specified message. 
     * @param message the message to include in the exception */
    public MalformedFileException(String message) {
        super(message);
    }
    
}
