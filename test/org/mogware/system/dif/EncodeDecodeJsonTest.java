package org.mogware.system.dif;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.junit.Test;
import org.mogware.system.Guid;

public class EncodeDecodeJsonTest extends TestCaseString {
    @Override
    protected String encode(Object object) throws Exception {
        StringWriter outputWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(outputWriter);
        new Encoder().encode(writer, object);
        writer.close();
        return outputWriter.toString();
    }

    @Override
    protected Object decode(String value) throws Exception {
        StringReader inputReader = new StringReader(value);
        JsonReader reader = new JsonReader(inputReader);
        return new Decoder().decode(reader);
    }

    @Override
    protected Object decode(String value, boolean rootValueAsArray)
            throws Exception {
        return decode(value);
    }

    @Test
    public void basicTypes() throws Exception {
        System.out.println("EncodeDecodeJsonTest: basicTypes");
        assertSimple("null", null, "null");
        assertSimple("false", Boolean.FALSE,
                "{\"$type\":\"boolean\",\"value\":false}");
        assertSimple("true", Boolean.TRUE,
                "{\"$type\":\"boolean\",\"value\":true}");
        assertSimple("simple string", "foo",
                "{\"$type\":\"java.lang.String\",\"value\":\"foo\"}");
        assertSimpleByte((byte) 42, "{\"$type\":\"byte\",\"value\":42}");
        assertSimpleShort((short) 42, "{\"$type\":\"short\",\"value\":42}");
        assertSimpleInt(42, "{\"$type\":\"int\",\"value\":42}");
        assertSimpleLong(42L, "{\"$type\":\"long\",\"value\":42}");
        assertSimpleFloat(Float.valueOf("1.0625"),
                "{\"$type\":\"float\",\"value\":1.0625}");
        assertSimpleDouble(Double.valueOf("1.0625"),
                "{\"$type\":\"double\",\"value\":1.0625}");
    }

    @Test
    public void complexTypes() throws Exception {
        System.out.println("EncodeDecodeJsonTest: complexTypes");
        assertArray("simple array", new Long[]{ 19L, 20L }, "[19,20]");
        assertArray("empty array", new Long[]{}, "[]");
        assertArray("string array",
            new String[]{ "foo", "bar" },"[\"foo\",\"bar\"]"
        );
        assertArray("object array",
            new Object[]{ "foo", 1234L, 5.678D },
            "[{\"$type\":\"java.lang.String\",\"value\":\"foo\"},1234,5.678]"
        );
        assertSimple("basic object",
            new TestObject(true, 1234L, 5.678D),
            "{" +
                "\"$type\":\"org.mogware.system.dif.TestObject\"," +
                "\"first\":true,\"second\":1234,\"third\":5.678" +
            "}"
        );
        assertSimple("array list",
            new ArrayList(Arrays.<Long>asList(1L, 2L)),
            "{\"$type\":\"java.util.ArrayList\",\"$items\":[1,2]}"
        );
        assertSimple("hash map",
            new HashMap(new HashMap<String, Long>() {{
                put("first", 1L); put("second", 2L);
            }}),
            "{\"$type\":\"java.util.HashMap\",\"first\":1,\"second\":2}"
        );
    }

    @Test
    public void standardTypes() throws Exception {
        System.out.println("EncodeDecodeJsonTest: standardTypes");
        assertSimple("date", new Date(100000L),
            "{\"$type\":\"java.util.Date\",\"value\":100000}");
        assertSimple("locale", Locale.FRANCE,
            "{\"$type\":\"java.util.Locale\",\"language\":\"fr\"," +
                "\"country\":\"FR\",\"variant\":\"\"}");
        assertSimple("uri", new URI("http://localhost/"),
            "{\"$type\":\"java.net.URI\",\"value\":\"http://localhost/\"}");
        assertSimple("guid", new Guid(100000L, 100000L),
            "{\"$type\":\"org.mogware.system.Guid\"," +
                    "\"mostSigBits\":100000,\"leastSigBits\":100000}");
    }
}
