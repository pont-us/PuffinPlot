package net.talvi.puffinplot.data.file;

import net.talvi.puffinplot.data.Datum;

public interface FileLoader {

    public Datum getNext();
    public LoadingStatus getStatus();
    public Iterable<String> getMessages();

}
