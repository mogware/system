package org.mogware.system.dif;

import java.io.IOException;
import org.junit.Test;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import junit.framework.Assert;

public class JsonWriterTest {
    
    @Test
    public void emptyDocument() throws Exception {
        System.out.println("JsonWriterTest: emptyDocument");
        JsonWriter jsonWriter = new JsonWriter(new StringWriter());
        IOException caught = null;
        try {
            jsonWriter.close();
        } catch (IOException e) {
            caught = e;
        } finally {
            Assert.assertNotNull(caught);
        }
    }

    @Test
    public void newline() throws Exception {
        System.out.println("JsonWriterTest: newline");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter.setIndent("  ");
        jsonWriter
            .beginObject(Object.class)
                .propertyName("prop")
                .value(true)
            .endObject();
        jsonWriter.close();
        Assert.assertEquals(
            "{" + "\r\n" + 
                "  \"$type\": \"java.lang.Object\"" + "," + "\r\n" +
                "  \"prop\": true" + "\r\n" +
            "}" + "\r\n",
            outputWriter.toString()
        );
    }

    @Test
    public void string() throws Exception {
        System.out.println("JsonWriterTest: string");
        StringBuilder wanted = new StringBuilder();
        StringBuilder json = new StringBuilder().append('"');
        for (int i = 0; i < 5120; i++) {
            wanted.append((char) i);
            if (i == '\b')
                json.append("\\b");
            else if (i == '\f')
                json.append("\\f");
            else if (i == '\t')
                json.append("\\t");
            else if (i == '\n')
                json.append("\\n");
            else if (i == '\r')
                json.append("\\r");
            else if (i == '"')
                json.append("\\\"");
            else if (i == '\\')
                json.append("\\\\");
            else if (i < 32)
                json.append(String.format("\\u%04x", (int) i));
            else
                json.append((char) i);
        }
        json.append('"');
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter.value(wanted.toString());
        jsonWriter.close();
        Assert.assertEquals(json.toString(), outputWriter.toString());
    }

    @Test
    public void valueFormatting() throws Exception {
        System.out.println("JsonWriterTest: valueFormatting");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter
            .beginArray()
                .value('@')
                .value("\r\n\t\f\b?{\\r\\n\"\'")
                .value(true)
                .value(10)
                .value(10.99)
                .value(0.99)
                .value(0.000000000000000001D)
                .value((String)null)
                .value("This is a string.")
                .nullValue()
            .endArray();
        jsonWriter.close();
        Assert.assertEquals(
            "[\"@\",\"\\r\\n\\t\\f\\b?{\\\\r\\\\n\\\"'\",true,10,10.99,0.99," +
            "1.0E-18,null,\"This is a string.\",null]",
            outputWriter.toString()
        );
    }

    @Test
    public void stringEscaping() throws Exception {
        System.out.println("JsonWriterTest: stringEscaping");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter
            .beginArray()
                .value("\"These pretzels are making me thirsty!\"")
                .value("Jeff's house was burninated.")
                .value("1. You don't talk about fight club.\r\n" +
                       "2. You don't talk about fight club."
                )
                .value("35% of\t statistics\n are made\r up.")
            .endArray();
        jsonWriter.close();
        Assert.assertEquals(
            "[\"\\\"These pretzels are making me thirsty!\\\"\"," +
            "\"Jeff's house was burninated.\",\"1. You don't talk about " +
            "fight club.\\r\\n2. You don't talk about fight club.\"," +
            "\"35% of\\t statistics\\n are made\\r up.\"]",
            outputWriter.toString()
        );
    }

    @Test
    public void basicTypes() throws Exception {
        System.out.println("JsonWriterTest: basicTypes");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter
            .beginArray()
                .value('c')
                .value(false)
                .value(null)
                .value("foo")
                .value(42)
                .value(1.0625)
            .endArray();
        jsonWriter.close();
        Assert.assertEquals(
            "[\"c\",false,null,\"foo\",42,1.0625]", outputWriter.toString()
        );
    }

    @Test
    public void nestedObject() throws Exception {
        System.out.println("JsonWriterTest: nestedObject");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter
            .beginObject(Object.class)
                .propertyName("string")
                .value("this is a string")
                .propertyName("integer")
                .value(5000L)
                .propertyName("double")
                .value(3.1415926D)
                .propertyName("array")
                .beginArray()
                    .value("string")
                    .value(6000L)
                .endArray()
            .endObject();
        jsonWriter.close();
        Assert.assertEquals(
            "{\"$type\":\"java.lang.Object\"," +
            "\"string\":\"this is a string\",\"integer\":5000," +
            "\"double\":3.1415926,\"array\":[\"string\",6000]}",
            outputWriter.toString()
        );
    }

    @Test
    public void arrayList() throws Exception {
        System.out.println("JsonWriterTest: arrayList");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter
            .beginList(ArrayList.class)
                .value("string")
                .value(1000L)
            .endList();
        jsonWriter.close();
        Assert.assertEquals(
            "{\"$type\":\"java.util.ArrayList\",\"$items\":[\"string\",1000]}",
            outputWriter.toString()
        );
    }

    @Test
    public void hashMap() throws Exception {
        System.out.println("JsonWriterTest: hashMap");
        StringWriter outputWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(outputWriter);
        jsonWriter
            .beginMap(HashMap.class)
                .beginKeys()
                    .value("key")
                .endKeys()
                .beginItems()
                    .value(1000L)
                .endItems()
            .endMap();
        jsonWriter.close();
        Assert.assertEquals(
            "{\"$type\":\"java.util.HashMap\"," +
            "\"$keys\":[\"key\"],\"$items\":[1000]}",
            outputWriter.toString()
        );
    }
}
