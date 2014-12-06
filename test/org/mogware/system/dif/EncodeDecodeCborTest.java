package org.mogware.system.dif;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import org.junit.Test;
import org.mogware.system.Guid;

public class EncodeDecodeCborTest extends TestCaseBinary {

    @Override
    protected byte[] encode(Object object) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CborWriter writer = new CborWriter(outputStream);
        new Encoder().encode(writer, object);
        writer.close();
        return outputStream.toByteArray();
    }

    @Override
    protected Object decode(byte[] value) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(value);
        CborReader reader = new CborReader(inputStream);
        return new Decoder().decode(reader);
    }

    @Override
    protected Object decode(byte[] value, boolean rootValueAsArray)
            throws Exception {
        return decode(value);
    }

    @Test
    public void basicTypes() throws Exception {
        System.out.println("EncodeDecodeCborTest: basicTypes");
        assertSimple("null", null, hexToBytes("F6"));
        assertSimple("false", Boolean.FALSE, hexToBytes(
            "BF65247479706567626F6F6C65616E6576616C7565F4FF"
        ));
        assertSimple("true", Boolean.TRUE, hexToBytes(
            "BF65247479706567626F6F6C65616E6576616C7565F5FF"
        ));
        assertSimple("simple string", "foo", hexToBytes(
            "BF652474797065706A6176612E6C616E672" +
            "E537472696E676576616C756563666F6FFF"
        ));
        assertSimpleByte((byte) 42, hexToBytes(
            "BF65247479706564627974656576616C7565182AFF"
        ));
        assertSimpleShort((short) 42, hexToBytes(
            "BF6524747970656573686F72746576616C7565182AFF"
        ));
        assertSimpleInt(42, hexToBytes(
            "BF65247479706563696E746576616C7565182AFF"
        ));
        assertSimpleLong(42L, hexToBytes(
            "BF652474797065646C6F6E676576616C7565182AFF"
        ));
        assertSimpleFloat(Float.valueOf("1.0625"), hexToBytes(
            "BF65247479706565666C6F61746576616C7565FA3F880000FF"
        ));
        assertSimpleDouble(Double.valueOf("1.0625"), hexToBytes(
            "BF65247479706566646F75626C656576616C7565FB3FF1000000000000FF"
        ));
    }

    @Test
    public void complexTypes() throws Exception {
        System.out.println("EncodeDecodeCborTest: complexTypes");
        assertArray("simple array", new Long[]{ 19L, 20L }, hexToBytes(
                "9F1314FF"
        ));
        assertArray("empty array", new Long[]{}, hexToBytes("9FFF"));
        assertArray("string array", new String[]{ "foo", "bar" }, hexToBytes(
                "9F63666F6F63626172FF"
        ));
        assertArray("object array",
            new Object[]{ "foo", 1234L, 5.0D }, hexToBytes(
                "9FBF652474797065706A6176612E6C616E672E537472696E67" +
                "6576616C756563666F6FFF1904D2FB4014000000000000FF"
            ));
        assertSimple("basic object",
            new TestObject(true, 1234L, 5.0D), hexToBytes(
                "BF65247479706578216F72672E6D6F67776172652E73797374" +
                "656D2E6469662E546573744F626A656374656669727374F566" +
                "7365636F6E641904D2657468697264FB4014000000000000FF"
            ));
        assertSimple("array list",
            new ArrayList(Arrays.<Long>asList(1L, 2L)), hexToBytes(
                "BF652474797065736A6176612E7574696C2E41727261794C69" +
                "737466246974656D739F0102FFFF"
            ));
        assertSimple("hash map",
            new HashMap(new HashMap<String, Long>() {{
                put("first", 1L); put("second", 2L);
            }}), hexToBytes(
                "BF652474797065716A6176612E7574696C2E486173684D6170" +
                "65666972737401667365636F6E6402FF"
            ));
    }

    @Test
    public void standardTypes() throws Exception {
        assertSimple("date", new Date(100000L), hexToBytes(
                "BF6524747970656E6A6176612E7574696C2E44617465657661" +
                "6C75651A000186A0FF"
        ));
        assertSimple("locale", Locale.FRANCE, hexToBytes(
                "BF652474797065706A6176612E7574696C2E4C6F63616C65686C616E67" +
                "7561676562667267636F756E7472796246526776617269616E7460FF"
        ));
        assertSimple("uri", new URI("http://localhost/"), hexToBytes(
                "BF6524747970656C6A6176612E6E65742E5552496576616C75" +
                "6571687474703A2F2F6C6F63616C686F73742FFF"
        ));
        assertSimple("guid", new Guid(100000L, 100000L), hexToBytes(
                "BF652474797065776F72672E6D6F67776172652E7379737465" +
                "6D2E477569646B6D6F7374536967426974731A000186A06C6C" +
                "65617374536967426974731A000186A0FF"
        ));
    }
}
