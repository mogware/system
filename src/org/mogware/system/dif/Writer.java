package org.mogware.system.dif;

import java.io.IOException;

public interface Writer {
    public Writer beginObject(Class type) throws IOException;
    public Writer endObject() throws IOException;

    public Writer beginArray() throws IOException;
    public Writer endArray() throws IOException;

    public Writer beginList(Class type) throws IOException;
    public Writer endList() throws IOException;

    public Writer beginMap(Class type) throws IOException;
    public Writer endMap() throws IOException;

    public Writer beginKeys() throws IOException;
    public Writer endKeys() throws IOException;

    public Writer beginItems() throws IOException;
    public Writer endItems() throws IOException;

    public Writer propertyName(String name) throws IOException;

    public Writer nullValue() throws IOException;

    public Writer value(boolean value) throws IOException;
    public Writer value(byte value) throws IOException;
    public Writer value(char value) throws IOException;
    public Writer value(double value) throws IOException;
    public Writer value(float value) throws IOException;
    public Writer value(int value) throws IOException;
    public Writer value(long value) throws IOException;
    public Writer value(short value) throws IOException;

    public Writer value(String value) throws IOException;
}
