package org.mogware.system.dif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;

public class CborReaderTest {
    @Test
    public void testParseArray() throws Exception {
        System.out.println("CborReaderTest: parseArray");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(hexToBytes(
            "9FF463666F6F182AFB3FF1000000000000FF"
        ));
        CborReader reader = new CborReader(inputStream);
        reader.parse(new DefaultHandler() {
            @Override
            public void beginArray() throws IOException {
                reader.pushHandler(new DefaultHandler() {
                    @Override
                    public void primitive(Object value) throws IOException {
                        if (value instanceof Boolean)
                            Assert.assertEquals((Boolean) false, value);
                        else if (value instanceof Long)
                            Assert.assertEquals((Long) 42L, value);
                        else if (value instanceof Double)
                            Assert.assertEquals((Double) 1.0625D, value);
                        else if (value instanceof String)
                            Assert.assertEquals("foo", value);
                    }
                    @Override
                    public void endArray() throws IOException {
                        reader.popHandler();
                    }
                });
            }
        });
    }

    @Test
    public void testParseObject() throws Exception {
        System.out.println("CborReaderTest: parseObject");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(hexToBytes(
            "BF67626F6F6C65616EF466737472696E6763666F6F67696E" +
            "7465676572182A66646F75626C65FB3FF1000000000000FF"
        ));
        CborReader reader = new CborReader(inputStream);
        reader.parse(new DefaultHandler(){
            @Override
            public void beginObject() throws IOException {
                reader.pushHandler(new DefaultHandler() {
                    @Override
                    public void beginObjectEntry(String key)
                            throws IOException {
                        parseObjectEntry(reader, key);
                    }
                    @Override
                    public void endObject() throws IOException {
                        reader.popHandler();
                    }
                });
            }
        });
    }

    private static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
            data[i / 2] = (byte) (
                    (Character.digit(s.charAt(i), 16) << 4) +
                    (Character.digit(s.charAt(i+1), 16))
        );
        return data;
    }

    private static void parseObjectEntry(final Reader reader, final String key)
            throws IOException {
        reader.pushHandler(new DefaultHandler() {
            @Override
            public void primitive(Object value) throws IOException {
                switch (key) {
                case "boolean":
                    Assert.assertEquals((Boolean) false, value);
                    break;
                case "string":
                    Assert.assertEquals("foo", value);
                    break;
                case "integer":
                    Assert.assertEquals((Long)42L, value);
                    break;
                case "double":
                    Assert.assertEquals((Double) 1.0625D, value);
                    break;
                }
            }
            @Override
            public void endObjectEntry() throws IOException {
                reader.popHandler();
            }
        });
    }
}
