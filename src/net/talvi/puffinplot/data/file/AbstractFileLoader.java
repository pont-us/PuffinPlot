package net.talvi.puffinplot.data.file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.talvi.puffinplot.data.Datum;

public abstract class AbstractFileLoader implements FileLoader {

    protected List<String> messages = new LinkedList<String>();
    protected List<Datum> data = new ArrayList<Datum>();;

    protected void addMessage(String message, Object... args) {
        messages.add(String.format(message, args));
    }

    protected void addDatum(Datum d) {
        data.add(d);
    }

    public List<Datum> getData() {
        return data;
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

}
