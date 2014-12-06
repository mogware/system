package org.mogware.system.dif;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class TestCaseString {

    protected abstract String encode(Object object) throws Exception;
    protected abstract Object decode(String value) throws Exception;
    protected abstract Object decode(String value, boolean rootValueAsArray)
            throws Exception;

    protected void assertSimpleByte(byte n, String value) throws Exception {
        assertSimple("byte", new Byte(n), value);
    }

    protected void assertSimpleShort(short n, String value) throws Exception {
        assertSimple("short", new Short(n), value);
    }

    protected void assertSimpleInt(int n, String value) throws Exception {
        assertSimple("integer", new Integer(n), value);
    }

    protected void assertSimpleLong(long n, String value) throws Exception {
        assertSimple("long", new Long(n), value);
    }

    protected void assertSimpleFloat(float n, String value) throws Exception {
        assertSimple("float", new Float(n), value);
    }
 
    protected void assertSimpleDouble(double n, String value) throws Exception {
        assertSimple("double", new Double(n), value);
    }

    protected void assertSimple(String type, Object object, String value)
            throws Exception {
        assertSimple(type, object, value, object);
    }

    protected void assertSimple(String type, Object before, String value,
            Object after) throws Exception {
        assertEquals("encode " + type, value, encode(before));
        assertEquals("decode " + type, after, decode(value));
    }

    protected void assertArray(String type, Object[] object, String value)
            throws Exception {
        assertArray(type, object, value, object);
    }

    protected void assertArray(String type, Object[] before, String value,
            Object[] after) throws Exception {
        assertEquals("encode " + type, value, encode(before));
        assertArrayEquals(
                "decode " + type, after, (Object[]) decode(value, true)
        );
    }
}
