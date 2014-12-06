package org.mogware.system.dif;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class BsonReader implements Reader {
    private final InputStream inp;
    private final BsonParser parser;
    private final boolean rootValueAsArray;
    private final Stack stack = new Stack();

    public static final byte EOO = 0;
    public static final byte NUMBER = 1;
    public static final byte STRING = 2;
    public static final byte OBJECT = 3;
    public static final byte ARRAY = 4;
    public static final byte BOOLEAN = 8;
    public static final byte NULL = 10;
    public static final byte NUMBER_INT = 16;
    public static final byte NUMBER_LONG = 18;

    public BsonReader(InputStream inp) throws IOException {
        this(inp, false);
    }

    public BsonReader(InputStream inp, boolean rootValueAsArray)
            throws IOException {
        if (inp == null)
            throw new NullPointerException("inp is null");
        this.inp = inp;
        this.rootValueAsArray = rootValueAsArray;
        this.parser = new BsonParser();
    }

    @Override
    public void parse(ContentHandler ch) throws IOException {
        this.pushHandler(ch);
        this.parser.parse(new InputBuffer(this.inp), rootValueAsArray);
    }

    @Override
    public void pushHandler(ContentHandler handler) {
        ContentHandler oldhandler = this.parser.getContentHandler();
        this.parser.setContentHandler(handler);
        this.stack.push(oldhandler);
    }

    @Override
    public void popHandler() {
        this.parser.setContentHandler((ContentHandler)this.stack.pop());
    }

    @Override
    public ObjectType newObjectType() {
        return new BasicObject();
    }

    @Override
    public ArrayType newArrayType() {
        return new BasicArray();
    }

    private static class BsonParser {
        private ContentHandler contentHandler = null;

        public void setContentHandler(ContentHandler handler) {
            this.contentHandler = handler;
        }

        public ContentHandler getContentHandler() {
            return this.contentHandler;
        }

        public void parse(InputBuffer in, boolean rootValueAsArray)
                throws IOException {
            final int len = in.readInt();
            in.setMax(len);
            if (this.contentHandler != null)
                this.contentHandler.begin();
            if (rootValueAsArray)
                this.parseArray(in, len);
            else
                this.parseObject(in, len);
            if (this.contentHandler != null)
                this.contentHandler.end();
            if (in.numRead() != len)
                throw new IllegalStateException("bad data. lengths don't match");
        }

        public void parseObject(InputBuffer in, int len) throws IOException {
            int num = in.numRead() - 4;
            if (this.contentHandler != null)
                this.contentHandler.beginObject();
            while (this.parseMember(in)) ;
            if (this.contentHandler != null)
                this.contentHandler.endObject();
            if (in.numRead() - num != len)
                throw new IllegalStateException("object length does not match");
        }

        public void parseArray(InputBuffer in, int len) throws IOException {
            int num = in.numRead() - 4;
            if (this.contentHandler != null) {
                this.contentHandler.beginArray();
            }
            for (int key = 0; this.parseElement(in, key); key++) ;
            if (this.contentHandler != null) {
                this.contentHandler.endArray();
            }
            if (in.numRead() - num != len)
                throw new IllegalStateException("array length does not match");
        }

        private boolean parseElement(InputBuffer in, int key)
                throws IOException {
            final byte type = in.read();
            if (type == EOO)
                return false;
            try {
                if (key != Integer.parseInt(in.readCStr()))
                    throw new IOException("expected array key to be: " + key);
            } catch (NumberFormatException ex) {
                throw new IOException("array keys must be numeric");
            }
            this.parseValue(in, type);
            return true;
        }

        private boolean parseMember(InputBuffer in) throws IOException {
            final byte type = in.read();
            if (type == EOO)
                return false;
            String key = in.readCStr();
            if (this.contentHandler != null)
                this.contentHandler.beginObjectEntry(key);
            this.parseValue(in, type);
            if (this.contentHandler != null)
                this.contentHandler.endObjectEntry();
            return true;
        }

        private void parseValue(InputBuffer in, byte type) throws IOException {
            switch (type) {
            case NULL:
                if (this.contentHandler != null)
                    this.contentHandler.primitive(null);
                break;
            case BOOLEAN:
                {
                    boolean value = in.read() > 0;
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case NUMBER:
                {
                    double value = in.readDouble();
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case NUMBER_INT:
                {
                    int value = in.readInt();
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case NUMBER_LONG:
                {
                    long value = in.readLong();
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case STRING:
                {
                    String value = in.readUTF8String();
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case ARRAY:
                parseArray(in, in.readInt());
                break;
            case OBJECT:
                parseObject(in, in.readInt());
                break;
            default:
                throw new IOException("do not understand type : " + type);
            }
        }
    }

    private class InputBuffer {
        private final InputStream in;
        private int read = 0;
        private int max = 4;

        public InputBuffer(final InputStream in) {
            this.in = in;
            len = 0;
            pos = 0;
        }

        private int need(final int num) throws IOException {
            if (len - pos >= num) {
                final int ret = pos;
                pos += num;
                this.read += num;
                return ret;
            }

            if (num >= inputBuffer.length)
                throw new IllegalArgumentException("buffer too small");

            final int remaining = len - pos;
            if (pos > 0) {
                System.arraycopy(inputBuffer, pos, inputBuffer, 0, remaining);
                pos = 0;
                len = remaining;
            }

            int toread = Math.min(this.max - this.read - remaining,
                    inputBuffer.length - len);
            while (toread > 0) {
                int n = in.read(inputBuffer, len, toread);
                if (n <= 0)
                    throw new IOException("unexpected EOF");
                toread -= n;
                len += n;
            }

            int ret = pos;
            pos += num;
            read += num;
            return ret;
        }

        public int numRead() {
            return this.read;
        }

        public int getMax() {
            return this.max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public byte read() throws IOException {
            return inputBuffer[this.need(1)];
        }

        public int readInt() throws IOException {
            return readInt(inputBuffer, this.need(4));
        }

        private int readInt(byte[] data, int offset) {
            int x = 0;
            x |= (0xFF & data[offset+0]) << 0;
            x |= (0xFF & data[offset+1]) << 8;
            x |= (0xFF & data[offset+2]) << 16;
            x |= (0xFF & data[offset+3]) << 24;
            return x;
        }

        public long readLong() throws IOException {
            return readLong(inputBuffer, this.need(8));
        }

        public long readLong(byte[] data , int offset) {
            long x = 0;
            x |= (0xFFL & data[offset+0]) << 0;
            x |= (0xFFL & data[offset+1]) << 8;
            x |= (0xFFL & data[offset+2]) << 16;
            x |= (0xFFL & data[offset+3]) << 24;
            x |= (0xFFL & data[offset+4]) << 32;
            x |= (0xFFL & data[offset+5]) << 40;
            x |= (0xFFL & data[offset+6]) << 48;
            x |= (0xFFL & data[offset+7]) << 56;
            return x;
        }

        public double readDouble() throws IOException {
            return Double.longBitsToDouble(readLong());
        }

        public String readUTF8String() throws IOException {
            final int size = readInt();
            if ( size <= 0 || size > MAX_STRING )
                throw new IOException("bad string size: " + size);

            if (size < inputBuffer.length / 2) {
                if (size == 1) {
                    read();
                    return "";
                }

                return new String(inputBuffer, need(size), size - 1, "UTF-8");
            }

            final byte [] buf = size < random.length ? random : new byte[size];
            fillbuf(buf, size);

            try {
                return new String(buf, 0, size - 1 , "UTF-8" );
            }
            catch (UnsupportedOperationException ex){
                throw new IOException("impossible" , ex);
            }
        }

        public String readCStr() throws IOException {
            boolean isAscii = true;

            random[0] = read();
            if (random[0] == 0)
                return "";
            random[1] = read();
            if (random[1] == 0) {
                final String out = ONE_BYTE_STRINGS[random[0]];
                return (out != null) ? out : new String(random, 0, 1, "UTF-8");
            }

            bb.reset();
            bb.write(random, 0, 2);
            isAscii = isAscii(random[0]) && isAscii(random[1]);

            byte b;
            while ((b = read()) != 0) {
                bb.write(b);
                isAscii = isAscii && isAscii(b);
            }

            String out = null;
            try {
                out = bb.asString(isAscii ? "US-ASCII" : "UTF-8");
            }
            catch (UnsupportedOperationException ex) {
                throw new IOException("impossible" , ex);
            }
            return out;
        }

        private void fillbuf(byte b[]) throws IOException {
            fillbuf(b , b.length);
        }

        public void fillbuf(byte b[], int len) throws IOException {
            final int have = len - pos;
            final int tocopy = Math.min(len, have);
            System.arraycopy(inputBuffer, pos, b, 0, tocopy);

            pos += tocopy;
            this.read += tocopy;
            len -= tocopy;

            int off = tocopy;
            while (len > 0){
                final int x = in.read(b, off, len);
                if (x <= 0)
                    throw new IOException("unexpected EOF");
                this.read += x;
                off += x;
                len -= x;
            }
        }
    }

    private final byte[] random = new byte[1024];
    private final byte[] inputBuffer = new byte[1024];

    private final ByteBuffer bb = new ByteBuffer();

    private int pos; // current offset into inputBuffer
    private int len; // length of valid data in inputBuffer

    private static final int MAX_STRING = ( 32 * 1024 * 1024 );

    static final String[] ONE_BYTE_STRINGS = new String[128];
    static {
        fillRange((byte)'0', (byte)'9');
        fillRange((byte)'a', (byte)'z');
        fillRange((byte)'A', (byte)'Z');
    }

    static void fillRange(byte min, byte max) {
        for (; min < max; min++){
            String s = "";
            s += (char)min;
            ONE_BYTE_STRINGS[(int)min] = s;
        }
    }

    private static boolean isAscii( final byte b ){
        return b >=0 && b <= 127;
    }
}
