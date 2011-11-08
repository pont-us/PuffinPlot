package net.talvi.puffinplot;

import java.io.IOException;

/**
 * An exception representing a condition which is not fatal, but
 * which needs to be caught and reported to the user.
 * 
 * @author pont
 */
public class PuffinUserException extends Exception {

    public PuffinUserException(String string) {
        super(string);
    }

    public PuffinUserException(IOException ex) {
        super(ex);
    }
    
}
