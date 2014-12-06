package org.mogware.system.dif;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public abstract class TestCaseBinary {

    protected abstract byte[] encode(Object object) throws Exception;
    protected abstract Object decode(byte[] value) throws Exception;
    protected abstract Object decode(byte[] value, boolean rootValueAsArray)
            throws Exception;

    protected void assertSimpleByte(byte n, byte[] value) throws Exception {
        assertSimple("byte", new Byte(n), value);
    }

    protected void assertSimpleShort(short n, byte[] value) throws Exception {
        assertSimple("short", new Short(n), value);
    }

    protected void assertSimpleInt(int n, byte[] value) throws Exception {
        assertSimple("integer", new Integer(n), value);
    }

    protected void assertSimpleLong(long n, byte[] value) throws Exception {
        assertSimple("long", new Long(n), value);
    }

    protected void assertSimpleFloat(float n, byte[] value) throws Exception {
        assertSimple("float", new Float(n), value);
    }
 
    protected void assertSimpleDouble(double n, byte[] value) throws Exception {
        assertSimple("double", new Double(n), value);
    }

    protected void assertSimple(String type, Object object, byte[] value)
            throws Exception {
        assertSimple(type, object, value, object);
    }

    protected void assertSimple(String type, Object before, byte[] value,
            Object after) throws Exception {
        assertArrayEquals("encode " + type, value, encode(before));
        assertEquals("decode " + type, after, decode(value));
    }

    protected void assertArray(String type, Object[] object, byte[] value)
            throws Exception {
        assertArray(type, object, value, object);
    }

    protected void assertArray(String type, Object[] before, byte[] value,
            Object[] after) throws Exception {
        assertArrayEquals("encode " + type, value, encode(before));
        assertArrayEquals(
                "decode " + type, after, (Object[]) decode(value, true)
        );
    }

    protected static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) (
                    (Character.digit(s.charAt(i), 16) << 4) +
                    (Character.digit(s.charAt(i+1), 16))
        );
        return data;
    }
}
