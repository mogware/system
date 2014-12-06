package org.mogware.system.dif;

import java.io.IOException;
import static org.junit.Assert.fail;

public class DefaultHandler implements Reader.ContentHandler {
    @Override
    public void begin() throws IOException {
    }

    @Override
    public void end() throws IOException {
    }

    @Override
    public void beginObject() throws IOException {
        fail("Start of object unexpected");
    }

    @Override
    public void endObject() throws IOException {
    }

    @Override
    public void beginObjectEntry(String key) throws IOException {
        fail("Entry " + key + " unexpected");
    }

    @Override
    public void endObjectEntry() throws IOException {
    }

    @Override
    public void beginArray() throws IOException {
        fail("Start of array unexpected");
    }

    @Override
    public void endArray() throws IOException {
    }

    @Override
    public void primitive(Object value) throws IOException {
        fail("Primitive unexpected");
    }
}
