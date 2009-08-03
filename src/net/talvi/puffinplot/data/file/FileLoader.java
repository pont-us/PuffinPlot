package net.talvi.puffinplot.data.file;

import java.util.List;
import net.talvi.puffinplot.data.Datum;

public interface FileLoader {

    public Datum getNext();
    public LoadingStatus getStatus();
    public List<String> getMessages();

}
