package org.mogware.system.dif;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;

public class BsonReaderTest {
    @Test
    public void testParseArray() throws Exception {
        System.out.println("BsonReaderTest: parseArray");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(hexToBytes(
            "2A0000000830000002310004000000666F6F001232" +
            "002A00000000000000013300000000000000F13F00"
        ));
        BsonReader reader = new BsonReader(inputStream, true);
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
        System.out.println("BsonReaderTest: testParseObject");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(hexToBytes(
            "4000000008626F6F6C65616E000002737472696E670004000000666F6F001269" +
            "6E7465676572002A0000000000000001646F75626C6500000000000000F13F00"
        ));
        BsonReader reader = new BsonReader(inputStream);
        reader.parse(new DefaultHandler(){
            @Override
            public void beginObject() throws IOException {
                reader.pushHandler(new DefaultHandler() {
                    @Override
                    public void beginObjectEntry(String key) throws IOException {
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
