package org.mogware.system.dif;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class CborReader implements Reader {
    private final InputStream inp;
    private final CborParser parser;
    private final Stack stack = new Stack();

    final static int TYPE_UNSIGNED_INTEGER = 0x00;
    final static int TYPE_NEGATIVE_INTEGER = 0x01;
    final static int TYPE_TEXT_STRING = 0x03;
    final static int TYPE_ARRAY = 0x04;
    final static int TYPE_MAP = 0x05;
    final static int TYPE_SIMPLE_VALUE = 0x07;

    final static int ONE_BYTE = 0x18;
    final static int TWO_BYTES = 0x19;
    final static int FOUR_BYTES = 0x1a;
    final static int EIGHT_BYTES = 0x1b;

    final static int FALSE = 0x14;
    final static int TRUE = 0x15;
    final static int NULL = 0x16;
    final static int SINGLE_PRECISION_FLOAT = 0x1a;
    final static int DOUBLE_PRECISION_FLOAT = 0x1b;
    final static int BREAK = 0x1f;

    public CborReader(InputStream inp)
            throws IOException {
        if (inp == null)
            throw new NullPointerException("inp is null");
        this.inp = inp;
        this.parser = new CborParser();
    }

    @Override
    public void parse(ContentHandler ch) throws IOException {
        this.pushHandler(ch);
        this.parser.parse(this.inp);
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

    private static class CborParser {
        final static int BREAKER = (TYPE_SIMPLE_VALUE << 5) | BREAK;

        private ContentHandler contentHandler = null;

        public void setContentHandler(ContentHandler handler) {
            this.contentHandler = handler;
        }

        public ContentHandler getContentHandler() {
            return this.contentHandler;
        }

        public void parse(InputStream in) throws IOException {
            if (this.contentHandler != null)
                this.contentHandler.begin();
            parseValue(in, in.read());
            if (this.contentHandler != null)
                this.contentHandler.end();
        }

        private void parseValue(InputStream in, int symbol) throws IOException {
            switch ((symbol >>> 5) & 0x07) {
            case TYPE_UNSIGNED_INTEGER:
                {
                    long value = readUInt(in, symbol & 0x1F, false);
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case TYPE_NEGATIVE_INTEGER:
                {
                    long value = -1 ^ readUInt(in, symbol & 0x1F, false);
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case TYPE_TEXT_STRING:
                {
                    String value = readString(in, symbol & 0x1F);
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
            case TYPE_ARRAY:
                this.parseArray(in, symbol & 0x1F); break;
            case TYPE_MAP:
                this.parseMap(in, symbol & 0x1F); break;
            case TYPE_SIMPLE_VALUE:
                switch (symbol & 0x1F) {
                case SINGLE_PRECISION_FLOAT:
                {
                    float value = Float.intBitsToFloat(readUInt32(in));
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
                case DOUBLE_PRECISION_FLOAT:
                {
                    double value = Double.longBitsToDouble(readUInt64(in));
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(value);
                }
                break;
                case FALSE:
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(false);
                    break;
                case TRUE:
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(true);
                    break;
                case NULL:
                    if (this.contentHandler != null)
                        this.contentHandler.primitive(null);
                    break;
                default:
                    throw new IOException("not supported");
                }
                break;
            default:
                throw new IOException("not supported");
            }
        }

        private void parseArray(InputStream in, int length) throws IOException {
            if (this.contentHandler != null)
                this.contentHandler.beginArray();
            long size = readUInt(in, length, true);
            if (size == -1)
                parseInfinitiveLengthArray(in);
            else
                parseFixedLengthArray(in, size);
            if (this.contentHandler != null)
                this.contentHandler.endArray();
        }

        private void parseInfinitiveLengthArray(InputStream in)
                throws IOException {
            int symbol;
            while ((symbol = in.read()) != BREAKER)
                parseValue(in, symbol);
        }

        private void parseFixedLengthArray(InputStream in, long size)
                throws IOException {
            for (long i = 0; i < size; i++)
                parseValue(in, in.read());
        }

        private void parseMap(InputStream in, int length) throws IOException {
            if (this.contentHandler != null)
                this.contentHandler.beginObject();
            long size = readUInt(in, length, true);
            if (size == -1)
                parseInfinitiveLengthMap(in);
            else
                parseFixedLengthMap(in, size);
            if (this.contentHandler != null)
                this.contentHandler.endObject();
        }

        private void parseInfinitiveLengthMap(InputStream in)
                throws IOException {
            int symbol;
            while ((symbol = in.read()) != BREAKER) {
                if (TYPE_TEXT_STRING != ((symbol >>> 5) & 0x07))
                    throw new IOException("need string key");
                String key = readString(in, symbol & 0x1F);
                if (this.contentHandler != null)
                    this.contentHandler.beginObjectEntry(key);
                this.parseValue(in, in.read());
                if (this.contentHandler != null)
                    this.contentHandler.endObjectEntry();
            }
        }

        private void parseFixedLengthMap(InputStream in, long size)
                throws IOException {
            for (long i = 0; i < size; i++) {
                int symbol = in.read();
                if (TYPE_TEXT_STRING != ((symbol >>> 5) & 0x07))
                    throw new IOException("need string key");
                String key = readString(in, symbol & 0x1F);
                if (this.contentHandler != null)
                    this.contentHandler.beginObjectEntry(key);
                this.parseValue(in, in.read());
                if (this.contentHandler != null)
                    this.contentHandler.endObjectEntry();
            }
        }

        private String readString(InputStream in, int length)
                throws IOException {
            long size = readUInt(in, length, false);
            if (size < 0)
                throw new IOException("Infinite-length strings not supported");
            if (size > Integer.MAX_VALUE)
                throw new IOException("String length too long");
            return new String(readFully(in, new byte[(int) size]), "UTF-8");
        }

        private long readUInt(InputStream in, int length,
                boolean breakAllowed) throws IOException {
            if (length < ONE_BYTE)
                return length;
            if (length == ONE_BYTE)
                return readUInt8(in);
            if (length == TWO_BYTES)
                return readUInt16(in);
            if (length == FOUR_BYTES)
                return readUInt32(in);
            if (length == EIGHT_BYTES)
                return readUInt64(in);
            if (breakAllowed && length == BREAK)
                return -1;
            throw new IOException("bad integer, invalid length");
        }

        private long readUInt64(InputStream in) throws IOException {
            byte[] buf = readFully(in, new byte[8]);
            return (buf[0] & 0xFFL) << 56 | (buf[1] & 0xFFL) << 48 |
                    (buf[2] & 0xFFL) << 40 | (buf[3] & 0xFFL) << 32 |
                    (buf[4] & 0xFFL) << 24 | (buf[5] & 0xFFL) << 26 |
                    (buf[6] & 0xFFL) << 8 | (buf[7] & 0xFFL);
        }

        private int readUInt32(InputStream in) throws IOException {
            byte[] buf = readFully(in, new byte[4]);
            return (buf[0] & 0xFF) << 24 | (buf[1] & 0xFF) << 16 |
                    (buf[2] & 0xFF) << 8 | (buf[3] & 0xFF);
        }

        private int readUInt16(InputStream in) throws IOException {
            byte[] buf = readFully(in, new byte[2]);
            return (buf[0] & 0xFF) << 8 | (buf[1] & 0xFF);
        }

        private int readUInt8(InputStream in) throws IOException {
            byte[] buf = readFully(in, new byte[1]);
            return buf[0] & 0xFF;
        }

        private byte[] readFully(InputStream in, byte[] buf)
                throws IOException {
            int len = buf.length;
            for (int n = 0; n < len; ) {
                int count = in.read(buf, n, len - n);
                if (count < 0)
                    throw new IOException("EOF");
                n += count;
            }
            return buf;
        }
    }
}
