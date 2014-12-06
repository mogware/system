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

public class EncodeDecodeBsonTest extends TestCaseBinary {

    @Override
    protected byte[] encode(Object object) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BsonWriter writer = new BsonWriter(outputStream);        
        new Encoder().encode(writer, object);
        writer.close();
        return outputStream.toByteArray();
    }

    @Override
    protected Object decode(byte[] value) throws Exception {
        return decode(value, false);
    }

    @Override
    protected Object decode(byte[] value, boolean rootValueAsArray)
            throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(value);
        BsonReader reader = new BsonReader(inputStream, rootValueAsArray);        
        return new Decoder().decode(reader);
    }

    @Test
    public void basicTypes() throws Exception {
        System.out.println("EncodeDecodeBsonTest: basicTypes");
        assertSimple("false", Boolean.FALSE, hexToBytes(
            "200000000224747970650008000000626F6F6C65616E000876616C7565000000"
        ));
        assertSimple("true", Boolean.TRUE, hexToBytes(
            "200000000224747970650008000000626F6F6C65616E000876616C7565000100"
        ));
        assertSimple("simple string", "foo", hexToBytes(
            "3000000002247479706500110000006A6176612E6C616E672" +
            "E537472696E67000276616C75650004000000666F6F0000"
        ));
        assertSimpleByte((byte) 42, hexToBytes(
            "20000000022474797065000500000062797465001076616C7565002A00000000"
        ));
        assertSimpleShort((short) 42, hexToBytes(
            "21000000022474797065000600000073686F7274001076616C7565002A00000000"
        ));
        assertSimpleInt(42, hexToBytes(
            "1F0000000224747970650004000000696E74001076616C7565002A00000000"
        ));
        assertSimpleLong(42L, hexToBytes(
            "2400000002247479706500050000006C6F6E" +
            "67001276616C7565002A0000000000000000"
        ));
        assertSimpleFloat(Float.valueOf("1.0625"), hexToBytes(
            "250000000224747970650006000000666C6F61" +
            "74000176616C756500000000000000F13F00"
        ));
        assertSimpleDouble(Double.valueOf("1.0625"), hexToBytes(
            "260000000224747970650007000000646F7562" +
            "6C65000176616C756500000000000000F13F00"
        ));
    }

    @Test
    public void complexTypes() throws Exception {
        System.out.println("EncodeDecodeBsonTest: complexTypes");        
        assertArray("simple array", new Long[]{ 19L, 20L }, hexToBytes(
            "1B0000001230001300000000000000123100140000000000000000"
        ));
        assertArray("empty array", new Long[]{}, hexToBytes("0500000000"));
        assertArray("string array", new String[]{ "foo", "bar" }, hexToBytes(
                "1B00000002300004000000666F6F00023100040000006261720000"
        ));
        assertArray("object array", 
            new Object[]{ "foo", 1234L, 5.678D }, hexToBytes(
                "4E0000000330003000000002247479706500110000006A617661" +
                "2E6C616E672E537472696E67000276616C75650004000000666F" +
                "6F0000123100D20400000000000001320083C0CAA145B6164000"
            ));
        assertSimple("basic object",
            new TestObject(true, 1234L, 5.678D), hexToBytes(
                "5900000002247479706500220000006F72672E6D6F6777617265" +
                "2E73797374656D2E6469662E546573744F626A65637400086669" +
                "7273740001127365636F6E6400D2040000000000000174686972" +
                "640083C0CAA145B6164000"
            ));
        assertSimple("array list",
            new ArrayList(Arrays.<Long>asList(1L, 2L)), hexToBytes(
                    "4700000002247479706500140000006A6176612E7574696C" + 
                    "2E41727261794C6973740004246974656D73001B00000012" + 
                    "3000010000000000000012310002000000000000000000"
            ));
        assertSimple("hash map",
            new HashMap(new HashMap<String, Long>() {{
                put("first", 1L); put("second", 2L);
            }}), hexToBytes(
                    "4100000002247479706500120000006A6176612E7574696C" + 
                    "2E486173684D617000126669727374000100000000000000" +
                    "127365636F6E6400020000000000000000"
            ));
    }

    @Test
    public void standardTypes() throws Exception {
        assertSimple("date", new Date(100000L), hexToBytes(
                "2E000000022474797065000F0000006A6176612E7574696C2E44" + 
                "617465001276616C756500A08601000000000000"
        ));
        assertSimple("locale", Locale.FRANCE, hexToBytes(
                "5000000002247479706500110000006A6176612E7574696C2E4C6F" + 
                "63616C6500026C616E6775616765000300000066720002636F756E" + 
                "74727900030000004652000276617269616E7400010000000000"
        ));
        assertSimple("uri", new URI("http://localhost/"), hexToBytes(
                "3A000000022474797065000D0000006A6176612E6E65742E5552490002" + 
                "76616C75650012000000687474703A2F2F6C6F63616C686F73742F0000"
        ));
        assertSimple("guid", new Guid(100000L, 100000L), hexToBytes(
                "5300000002247479706500180000006F72672E6D6F67776172652E73" +
                "797374656D2E4775696400126D6F73745369674269747300A0860100" +
                "00000000126C656173745369674269747300A08601000000000000"
        ));
    }
}
