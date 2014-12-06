package org.mogware.system.dif;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CborWriter implements Writer {
    final ByteOutputStream out;

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

    private enum Scope {
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        DANGLING_NAME,
        NONEMPTY_OBJECT,
        EMPTY_DOCUMENT,
        NONEMPTY_DOCUMENT
    }

    private final List<Scope> stack = new ArrayList<>();
    {
        stack.add(Scope.EMPTY_DOCUMENT);
    }

    private String deferredName = null;

    public CborWriter(OutputStream out) {
        if (out == null)
            throw new NullPointerException("out is null");
        this.out = new ByteOutputStream(out);
    }

    @Override
    public Writer beginObject(Class type) throws IOException {
        this.writeDeferredName();
        return this.open(Scope.EMPTY_OBJECT, TYPE_MAP)
                .propertyName("$type")
                .value(this.typeOf(type));
    }

    @Override
    public Writer endObject() throws IOException {
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT);
    }

    @Override
    public Writer beginArray() throws IOException {
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, TYPE_ARRAY);
    }

    @Override
    public Writer endArray() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
    }

    @Override
    public Writer beginList(Class type) throws IOException {
        writeDeferredName();
        this.open(Scope.EMPTY_OBJECT, TYPE_MAP)
                .propertyName("$type")
                .value(this.typeOf(type))
                .propertyName("$items");
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, TYPE_ARRAY);
    }

    @Override
    public Writer endList() throws IOException {
        this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT);
    }

    @Override
    public Writer beginMap(Class type) throws IOException {
        this.writeDeferredName();
        return this.open(Scope.EMPTY_OBJECT, TYPE_MAP)
                .propertyName("$type")
                .value(this.typeOf(type));
    }

    @Override
    public Writer endMap() throws IOException {
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT);
    }

    @Override
    public Writer beginKeys() throws IOException {
        this.propertyName("$keys");
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, TYPE_ARRAY);
    }

    @Override
    public Writer endKeys() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
    }

    @Override
    public Writer beginItems() throws IOException {
        this.propertyName("$items");
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, TYPE_ARRAY);
    }

    @Override
    public Writer endItems() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
    }

    @Override
    public Writer propertyName(String name) throws IOException {
        if (name == null)
            throw new NullPointerException("name is null");
        if (this.deferredName != null)
            throw new IllegalStateException(this.deferredName);
        this.deferredName = name;
        return this;
    }

    @Override
    public Writer nullValue() throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeSimpleType(TYPE_SIMPLE_VALUE, NULL);
        return this;
    }

    @Override
    public Writer value(boolean value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeSimpleType(TYPE_SIMPLE_VALUE, value ? TRUE : FALSE);
        return this;
    }

    @Override
    public Writer value(byte value) throws IOException {
        return this.value((int) value);
    }

    @Override
    public Writer value(char value) throws IOException {
        return this.value((int)value);
    }

    @Override
    public Writer value(double value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeUInt64(
            (TYPE_SIMPLE_VALUE << 5) | DOUBLE_PRECISION_FLOAT,
            Double.doubleToRawLongBits(value)
        );
        return this;
    }

    @Override
    public Writer value(float value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeUInt32(
            (TYPE_SIMPLE_VALUE << 5) | SINGLE_PRECISION_FLOAT,
            Float.floatToRawIntBits(value)
        );
        return this;
    }

    @Override
    public Writer value(int value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeInt(value);
        return this;
    }

    @Override
    public Writer value(long value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeLong(value);
        return this;
    }

    @Override
    public Writer value(short value) throws IOException {
        return this.value((int) value);
    }

    @Override
    public Writer value(String value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.writeString(value);
        return this;
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void close() throws IOException {
        this.out.close();
        if (this.peek() != Scope.NONEMPTY_DOCUMENT)
            throw new IOException("Incomplete document");
    }

    private CborWriter open(Scope empty, int type) throws IOException {
        this.beforeValue();
        this.stack.add(empty);
        this.out.writeSimpleType(type, BREAK);
        return this;
    }

    private CborWriter close(Scope empty, Scope nonempty)
            throws IOException {
        Scope scope = this.peek();
        if (scope != nonempty && scope != empty)
            throw new IllegalStateException("Nesting problem: " + stack);
        if (this.deferredName != null)
            throw new IllegalStateException("Dangling name: " + deferredName);
        this.stack.remove(stack.size() - 1);
        out.writeSimpleType(TYPE_SIMPLE_VALUE, BREAK);
        return this;
    }

    private void writeDeferredName() throws IOException {
        if (this.deferredName != null) {
            this.beforeName();
            this.out.writeString(deferredName);
            this.deferredName = null;
        }
    }

    private void beforeName() throws IOException {
        Scope scope = this.peek();
        if (scope != Scope.NONEMPTY_OBJECT && scope != Scope.EMPTY_OBJECT)
            throw new IllegalStateException("Nesting problem: " + stack);
        this.replaceTop(Scope.DANGLING_NAME);
    }

    private void beforeValue() throws IOException {
        switch (this.peek()) {
        case EMPTY_DOCUMENT:
            this.replaceTop(Scope.NONEMPTY_DOCUMENT);
            break;
        case EMPTY_ARRAY:
            this.replaceTop(Scope.NONEMPTY_ARRAY);
            break;
        case NONEMPTY_ARRAY:
            break;
        case DANGLING_NAME: // value for name
            this.replaceTop(Scope.NONEMPTY_OBJECT);
            break;
        case NONEMPTY_DOCUMENT:
            throw new IllegalStateException("JSON must have only one top-level value.");
        default:
            throw new IllegalStateException("Nesting problem: " + stack);
        }
    }

    private Scope peek() {
        return this.stack.get(stack.size() - 1);
    }

    private void replaceTop(Scope topOfStack) {
        this.stack.set(stack.size() - 1, topOfStack);
    }

    private String typeOf(Class type) {
        if (boolean.class == type || Boolean.class == type)
            return "boolean";
        if (byte.class == type || Byte.class == type)
            return "byte";
        if (short.class == type || Short.class == type)
            return "short";
        if (int.class == type || Integer.class == type)
            return "int";
        if (long.class == type || Long.class == type)
            return "long";
        if (double.class == type || Double.class == type)
            return "double";
        if (float.class == type || Float.class == type)
            return "float";
        if (char.class == type || Character.class == type)
            return "char";
        return type.getName();
    }

    private class ByteOutputStream {
        private static final int NEG_INT_MASK = TYPE_NEGATIVE_INTEGER << 5;
        private final OutputStream out;

        public ByteOutputStream(OutputStream out) {
            this.out = out;
        }

        public void flush() throws IOException {
            this.out.flush();
        }

        public void close() throws IOException {
            this.out.close();
        }

        public void writeType(int majorType, long value) throws IOException {
            this.writeUInt((majorType << 5), value);
        }

        public void writeSimpleType(int majorType, int value)
                throws IOException {
            this.out.write((majorType << 5) | (value & 0x1f));
        }

        public void writeString(String value) throws IOException {
            this.writeString(TYPE_TEXT_STRING,
                    value == null ? null : value.getBytes("UTF-8"));
        }

        public void writeInt(long value) throws IOException {
            long sign = value >> 63;
            int mt = (int) (sign & NEG_INT_MASK);
            this.writeUInt(mt, (int) ((sign ^ value) & 0xffffffffL));
        }

        public void writeLong(long value) throws IOException {
            long sign = value >> 63;
            int mt = (int) (sign & NEG_INT_MASK);
            this.writeUInt(mt, sign ^ value);
        }

        protected void writeString(int majorType, byte[] bytes)
                throws IOException {
            int len = (bytes == null) ? 0 : bytes.length;
            this.writeType(majorType, len);
            for (int i = 0; i < len; i++)
                this.out.write(bytes[i]);
        }

        protected void writeUInt(int mt, long value) throws IOException {
            if (value < 0x18L)
                this.out.write((int)(mt | value));
            else if (value < 0x100L)
                this.writeUInt8(mt, (int) value);
            else if (value < 0x10000L)
                this.writeUInt16(mt, (int) value);
            else if (value < 0x100000000L)
                this.writeUInt32(mt, (int) value);
            else
                this.writeUInt64(mt, value);
        }

        protected void writeUInt8(int mt, int value) throws IOException {
            this.out.write(mt | ONE_BYTE);
            this.out.write(value & 0xFF);
        }

        protected void writeUInt16(int mt, int value) throws IOException {
            this.out.write(mt | TWO_BYTES);
            this.out.write(value >> 8);
            this.out.write(value & 0xFF);
        }

        protected void writeUInt32(int mt, int value) throws IOException {
            this.out.write(mt | FOUR_BYTES);
            this.out.write(value >> 24);
            this.out.write(value >> 16);
            this.out.write(value >> 8);
            this.out.write(value & 0xFF);
        }

        protected void writeUInt64(int mt, long value) throws IOException {
            this.out.write(mt | EIGHT_BYTES);
            this.out.write((int) (value >> 56));
            this.out.write((int) (value >> 48));
            this.out.write((int) (value >> 40));
            this.out.write((int) (value >> 32));
            this.out.write((int) (value >> 24));
            this.out.write((int) (value >> 16));
            this.out.write((int) (value >> 8));
            this.out.write((int) (value & 0xFF));
        }
    }
}
