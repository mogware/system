package org.mogware.system.dif;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class BsonWriter implements Writer {
    private final OutputStream out;

    public static final byte EOO = 0;
    public static final byte NUMBER = 1;
    public static final byte STRING = 2;
    public static final byte OBJECT = 3;
    public static final byte ARRAY = 4;
    public static final byte BOOLEAN = 8;
    public static final byte NULL = 10;
    public static final byte NUMBER_INT = 16;
    public static final byte NUMBER_LONG = 18;

    private enum Scope {
        EMPTY_ARRAY,
        NONEMPTY_ARRAY,
        EMPTY_OBJECT,
        NONEMPTY_OBJECT,
        EMPTY_DOCUMENT,
        NONEMPTY_DOCUMENT
    }

    private static class Context {
        int pos;
        int index;
        Scope scope;

        public Context(Scope scope, int pos) {
            this.scope = scope;
            this.pos = pos;
            this.index = 0;
        }
    }

    private final List<Context> stack = new ArrayList<>();
    {
        stack.add(new Context(Scope.EMPTY_DOCUMENT, 0));
    }

    private String deferredName = null;
    private final OutputBuffer buffer = new OutputBuffer();

    public BsonWriter(OutputStream out) {
        if (out == null)
            throw new NullPointerException("out is null");
        this.out = out;
    }

    @Override
    public Writer beginObject(Class type) throws IOException {
        return this.open(Scope.EMPTY_OBJECT, OBJECT)
                .propertyName("$type")
                .value(this.typeOf(type));
    }

    @Override
    public Writer endObject() throws IOException {
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT);
    }

    @Override
    public Writer beginArray() throws IOException {
        return this.open(Scope.EMPTY_ARRAY, ARRAY);
    }

    @Override
    public Writer endArray() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
    }

    @Override
    public Writer beginList(Class type) throws IOException {
        this.open(Scope.EMPTY_OBJECT, OBJECT)
                .propertyName("$type")
                .value(this.typeOf(type))
                .propertyName("$items");
        return this.open(Scope.EMPTY_ARRAY, ARRAY);
    }

    @Override
    public Writer endList() throws IOException {
        this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
        return close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT);

    }

    @Override
    public Writer beginMap(Class type) throws IOException {
        return this.open(Scope.EMPTY_OBJECT, OBJECT)
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
        return this.open(Scope.EMPTY_ARRAY, ARRAY);
    }

    @Override
    public Writer endKeys() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
    }

    @Override
    public Writer beginItems() throws IOException {
        this.propertyName("$items");
        return this.open(Scope.EMPTY_ARRAY, ARRAY);

    }

    @Override
    public Writer endItems() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY);
    }

    @Override
    public Writer propertyName(String name) {
        if (name == null)
            throw new NullPointerException("name is null");
        if (this.deferredName != null)
            throw new IllegalStateException(this.deferredName);
        this.deferredName = name;
        return this;
    }

    @Override
    public Writer nullValue() throws IOException {
        this.buffer.write(NULL);
        this.writeDeferredName();
        return this;
    }

    @Override
    public Writer value(boolean value) throws IOException {
        this.buffer.write(BOOLEAN);
        this.writeDeferredName();
        this.buffer.write(value ? (byte) 0x1 : (byte) 0x0);
        return this;
    }

    @Override
    public Writer value(byte value) throws IOException {
        return this.value((int) value);
    }

    @Override
    public Writer value(char value) throws IOException {
        return this.value(String.valueOf(value));
    }

    @Override
    public Writer value(double value) throws IOException {
        this.buffer.write(NUMBER);
        this.writeDeferredName();
        this.buffer.writeDouble(value);
        return this;
    }

    @Override
    public Writer value(float value) throws IOException {
        return this.value((double) value);
    }

    @Override
    public Writer value(int value) throws IOException {
        this.buffer.write(NUMBER_INT);
        this.writeDeferredName();
        this.buffer.writeInt(value);
        return this;
    }

    @Override
    public Writer value(long value) throws IOException {
        this.buffer.write(NUMBER_LONG);
        this.writeDeferredName();
        this.buffer.writeLong(value);
        return this;
    }

    @Override
    public Writer value(short value) throws IOException {
        return this.value((int) value);
    }

    @Override
    public Writer value(String value) throws IOException {
        if (value == null)
            return this.nullValue();
        this.buffer.write(STRING);
        this.writeDeferredName();
        int pos = this.buffer.getPosition();
        this.buffer.writeInt(0);
        int len = this.buffer.writeString(value);
        this.buffer.writeInt(pos, len);
        return this;
    }

    public void flush() throws IOException {
        this.buffer.pipe(this.out);
        this.buffer.reset();
    }

    public void close() throws IOException {
        this.flush();
        this.out.close();
        if (this.peek().scope != Scope.NONEMPTY_DOCUMENT)
            throw new IOException("Incomplete document");
    }

    private BsonWriter open(Scope empty, byte type) throws IOException {
        Context context = this.peek();
        if (context.scope == Scope.EMPTY_DOCUMENT)
            context.scope = Scope.NONEMPTY_DOCUMENT;
        else {
            this.buffer.write(type);
            this.writeDeferredName();
        }
        final int pos = this.buffer.getPosition();
        this.buffer.writeInt(0);
        this.stack.add(new Context(empty, pos));
        return this;
    }

    private BsonWriter close(Scope empty, Scope nonempty) {
        Context context = this.peek();
        if (context.scope != nonempty && context.scope != empty)
            throw new IllegalStateException("Nesting problem: " + stack);
        if (this.deferredName != null)
            throw new IllegalStateException("Dangling name: " + deferredName);
        this.buffer.write(EOO);
        int len = this.buffer.getPosition() - context.pos;
        this.buffer.writeInt(context.pos, len);
        this.stack.remove(stack.size() - 1);
        return this;
    }

    private void writeDeferredName() throws IOException {
        Context context = this.peek();
        switch (context.scope) {
        case EMPTY_OBJECT:
            context.scope = Scope.NONEMPTY_OBJECT;
        case NONEMPTY_OBJECT:
            this.buffer.writeString(deferredName);
            break;
        case EMPTY_ARRAY:
            context.scope = Scope.NONEMPTY_ARRAY;
        case NONEMPTY_ARRAY:
            this.buffer.writeString(String.valueOf(context.index++));
            break;
        default:
            throw new IOException("Must start with an Object or Array");
        }
        this.deferredName = null;
    }

    private Context peek() {
        return this.stack.get(stack.size() - 1);
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

    private static class OutputBuffer extends ByteBuffer {

        public int pipe(OutputStream out) throws IOException {
            out.write(this.buffer, 0, this.size);
            return this.size;
        }

        public void writeInt(int x) {
            this.write(x >> 0);
            this.write(x >> 8);
            this.write(x >> 16);
            this.write(x >> 24);
        }

        public void writeInt(int pos, int x) {
            final int save = this.getPosition();
            this.setPosition(pos);
            this.writeInt(x);
            this.setPosition(save);
        }

        public void writeLong(long l) {
            this.write((byte) (0xFFL & (l >> 0)));
            this.write((byte) (0xFFL & (l >> 8)));
            this.write((byte) (0xFFL & (l >> 16)));
            this.write((byte) (0xFFL & (l >> 24)));
            this.write((byte) (0xFFL & (l >> 32)));
            this.write((byte) (0xFFL & (l >> 40)));
            this.write((byte) (0xFFL & (l >> 48)));
            this.write((byte) (0xFFL & (l >> 56)));
        }

        public void writeDouble(double d) {
            this.writeLong(Double.doubleToRawLongBits(d));
        }

        public int writeString(String str) {
            final int len = str.length();
            int total = 0;

            for (int i = 0; i < len;) {
                int c = Character.codePointAt(str, i);

                if (c < 0x80) {
                    this.write((byte) c);
                    total += 1;
                } else if (c < 0x800) {
                    this.write((byte) (0xc0 + (c >> 6)));
                    this.write((byte) (0x80 + (c & 0x3f)));
                    total += 2;
                } else if (c < 0x10000) {
                    this.write((byte) (0xe0 + (c >> 12)));
                    this.write((byte) (0x80 + ((c >> 6) & 0x3f)));
                    this.write((byte) (0x80 + (c & 0x3f)));
                    total += 3;
                } else {
                    this.write((byte) (0xf0 + (c >> 18)));
                    this.write((byte) (0x80 + ((c >> 12) & 0x3f)));
                    this.write((byte) (0x80 + ((c >> 6) & 0x3f)));
                    this.write((byte) (0x80 + (c & 0x3f)));
                    total += 4;
                }

                i += Character.charCount(c);
            }

            this.write((byte) 0);
            total++;
            return total;
        }
    }
}
