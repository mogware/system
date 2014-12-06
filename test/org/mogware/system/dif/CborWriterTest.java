package org.mogware.system.dif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import junit.framework.Assert;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

public class CborWriterTest {

    @Test
    public void emptyDocument() throws Exception {
        System.out.println("CborWriterTest: emptyDocument");
        CborWriter cborWriter = new CborWriter(new ByteArrayOutputStream());
        IOException caught = null;
        try {
            cborWriter.close();
        } catch (IOException e) {
            caught = e;
        } finally {
            Assert.assertNotNull(caught);
        }
    }

    @Test
    public void basicTypes() throws Exception {
        System.out.println("CborWriterTest: basicTypes");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborWriter cborWriter = new CborWriter(outputStream);
        cborWriter
            .beginArray()
                .value('@')
                .value(true)
                .value(10)
                .value(10.99F)
                .value("string")
                .value(null)
            .endArray();
        cborWriter.close();
        assertArrayEquals(hexToBytes(
            "9F1840F50AFA412FD70A66737472696E6760FF"),
            outputStream.toByteArray()
        );
    }

    @Test
    public void nestedObject() throws Exception {
        System.out.println("CborWriterTest: nestedObject");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborWriter cborWriter = new CborWriter(outputStream);
        cborWriter
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
        cborWriter.close();
        assertArrayEquals(hexToBytes(
            "BF652474797065706A6176612E6C616E672E4F626A6563746673747269" +
            "6E677074686973206973206120737472696E6767696E74656765721913" +
            "8866646F75626C65FB400921FB4D12D84A6561727261799F6673747269" +
            "6E67191770FFFF"),
            outputStream.toByteArray()
        );
    }

    @Test
    public void arrayList() throws Exception {
        System.out.println("CborWriterTest: arrayList");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborWriter cborWriter = new CborWriter(outputStream);
        cborWriter
            .beginList(ArrayList.class)
                .value("string")
                .value(1000L)
            .endList();
        cborWriter.close();
        assertArrayEquals(hexToBytes(
            "BF652474797065736A6176612E7574696C2E41727261794C6973746624" +
            "6974656D739F66737472696E671903E8FFFF"),
            outputStream.toByteArray()
        );
    }

    @Test
    public void hashMap() throws Exception {
        System.out.println("CborWriterTest: hashMap");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborWriter cborWriter = new CborWriter(outputStream);
        cborWriter
            .beginMap(HashMap.class)
                .beginKeys()
                    .value("key")
                .endKeys()
                .beginItems()
                    .value(1000L)
                .endItems()
            .endMap();
        cborWriter.close();
        assertArrayEquals(hexToBytes(
            "BF652474797065716A6176612E7574696C2E486173684D617065246B65" +
            "79739F636B6579FF66246974656D739F1903E8FFFF"),
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
