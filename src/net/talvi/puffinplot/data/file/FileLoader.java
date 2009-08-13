package net.talvi.puffinplot.data.file;

import java.util.List;
import net.talvi.puffinplot.data.Datum;

public interface FileLoader {
    public List<Datum> getData();
    public List<String> getMessages();
}
