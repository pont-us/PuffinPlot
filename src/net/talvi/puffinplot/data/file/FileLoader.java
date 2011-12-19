package net.talvi.puffinplot.data.file;

import java.util.List;
import net.talvi.puffinplot.data.Datum;

/**
 * An interface for classes which read data from files on disk into PuffinPlot's
 * data structures.
 * 
 * @author pont
 */
public interface FileLoader {
    
    /** Returns the data points read from the file.
     * @return the data points read from the file
     */
    public List<Datum> getData();
    
    /** Returns any messages produced during the loading process. 
     * @return any messages produced during the loading process
     */
    public List<String> getMessages();
    
    /** Returns any lines in the file which were not handled by the loader. 
     * @return any lines in the file which were not handled by the loader
     */
    public List<String> getExtraLines();
}
