package org.mogware.system.dif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import junit.framework.Assert;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class BsonWriterTest {

    @Test
    public void emptyDocument() throws Exception {
        System.out.println("BsonWriterTest: emptyDocument");
        BsonWriter bsonWriter = new BsonWriter(new ByteArrayOutputStream());
        IOException caught = null;
        try {
            bsonWriter.close();
        } catch (IOException e) {
            caught = e;
        } finally {
            Assert.assertNotNull(caught);
        }
    }

    @Test
    public void basicTypes() throws Exception {
        System.out.println("BsonWriterTest: basicTypes");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BsonWriter bsonWriter = new BsonWriter(outputStream);
        bsonWriter
            .beginArray()
                .value('@')
                .value(true)
                .value(10)
                .value(10.99F)
                .value("string")
                .value(null)
            .endArray();
        bsonWriter.close();
        assertArrayEquals(hexToBytes(
            "35000000023000020000004000083100011032000A00000001330000" +
            "000040E1FA254002340007000000737472696E67000A350000"),
            outputStream.toByteArray()
        );
    }

    @Test
    public void nestedObject() throws Exception {
        System.out.println("BsonWriterTest: nestedObject");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BsonWriter bsonWriter = new BsonWriter(outputStream);
        bsonWriter
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
        bsonWriter.close();
        assertArrayEquals(hexToBytes(
            "8400000002247479706500110000006A6176612E6C616E672E4F626A" +
            "6563740002737472696E670011000000746869732069732061207374" +
            "72696E670012696E746567657200881300000000000001646F75626C" +
            "65004AD8124DFB210940046172726179001E00000002300007000000" +
            "737472696E670012310070170000000000000000"),
            outputStream.toByteArray()
        );
    }

    @Test
    public void arrayList() throws Exception {
        System.out.println("BsonWriterTest: arrayList");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BsonWriter bsonWriter = new BsonWriter(outputStream);
        bsonWriter
            .beginList(ArrayList.class)
                .value("string")
                .value(1000L)
            .endList();
        bsonWriter.close();
        assertArrayEquals(hexToBytes(
            "4A00000002247479706500140000006A6176612E7574696C2E417272" +
            "61794C6973740004246974656D73001E000000023000070000007374" +
            "72696E6700123100E8030000000000000000"),
            outputStream.toByteArray()
        );
    }

    @Test
    public void hashMap() throws Exception {
        System.out.println("BsonWriterTest: hashMap");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BsonWriter bsonWriter = new BsonWriter(outputStream);
        bsonWriter
            .beginMap(HashMap.class)
                .beginKeys()
                    .value("key")
                .endKeys()
                .beginItems()
                    .value(1000L)
                .endItems()
            .endMap();
        bsonWriter.close();
        assertArrayEquals(hexToBytes(
            "5100000002247479706500120000006A6176612E7574696C2E486173" +
            "684D61700004246B6579730010000000023000040000006B65790000" +
            "04246974656D730010000000123000E8030000000000000000"),
            outputStream.toByteArray()
        );
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
}
