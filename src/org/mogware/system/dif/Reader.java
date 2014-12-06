package org.mogware.system.dif;

import java.io.IOException;

public interface Reader {
    public interface ContentHandler {
        public void begin() throws IOException;
        public void end() throws IOException;
        public void beginObject() throws IOException;
        public void endObject() throws IOException;
        public void beginObjectEntry(String key) throws IOException;
        public void endObjectEntry() throws IOException;
        public void beginArray() throws IOException;
        public void endArray() throws IOException;
        public void primitive(Object value) throws IOException;
    }

    public ObjectType newObjectType();
    public ArrayType newArrayType();

    public void parse(ContentHandler ch) throws IOException;

    public void pushHandler(ContentHandler handler);
    public void popHandler();
}
