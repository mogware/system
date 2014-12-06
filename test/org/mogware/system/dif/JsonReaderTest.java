package org.mogware.system.dif;

import java.io.IOException;
import java.io.StringReader;
import junit.framework.Assert;
import org.junit.Test;

public class JsonReaderTest {
    @Test
    public void testParseArray() throws Exception {
        System.out.println("JsonReaderTest: parseArray");
        StringReader inputReader = new StringReader(
            "[false,\"foo\",42,1.0625]"
        );
        JsonReader reader = new JsonReader(inputReader);
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
        System.out.println("JsonReaderTest: testParseObject");
        StringReader inputReader = new StringReader(
            "{\"boolean\":false,\"string\":\"foo\"," +
                "\"integer\":42,\"double\":1.0625}"
        );
        JsonReader reader = new JsonReader(inputReader);
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
