package org.mogware.system.dif;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class JsonWriter implements Writer {
    private final java.io.Writer out;

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
    private String indent = null;
    private String separator = ":";

    public JsonWriter(java.io.Writer out) {
        if (out == null)
            throw new NullPointerException("out is null");
        this.out = out;
    }

    public JsonWriter(OutputStream out) throws IOException {
        if (out == null)
            throw new NullPointerException("out is null");
        try {
            this.out = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IOException("UTF-8 unsupported encoding", ex);
        }
    }

    public final void setIndent(String indent) {
        if (indent.length() == 0) {
            this.indent = null;
            this.separator = ":";
        }
        else {
            this.indent = indent;
            this.separator = ": ";
        }
    }

    @Override
    public Writer beginObject(Class type) throws IOException {
        this.writeDeferredName();
        return this.open(Scope.EMPTY_OBJECT, "{")
                .propertyName("$type")
                .value(this.typeOf(type));
    }

    @Override
    public Writer endObject() throws IOException {
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}");
    }

    @Override
    public Writer beginArray() throws IOException {
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, "[");
    }

    @Override
    public Writer endArray() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
    }

    @Override
    public Writer beginList(Class type) throws IOException {
        writeDeferredName();
        this.open(Scope.EMPTY_OBJECT, "{")
                .propertyName("$type")
                .value(this.typeOf(type))
                .propertyName("$items");
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, "[");
    }

    @Override
    public Writer endList() throws IOException {
        this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}");
    }

    public Writer beginMap(Class type) throws IOException {
        this.writeDeferredName();
        return this.open(Scope.EMPTY_OBJECT, "{")
                .propertyName("$type")
                .value(this.typeOf(type));
    }

    @Override
    public Writer endMap() throws IOException {
        return this.close(Scope.EMPTY_OBJECT, Scope.NONEMPTY_OBJECT, "}");
    }

    @Override
    public Writer beginKeys() throws IOException {
        this.propertyName("$keys");
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, "[");
    }

    @Override
    public Writer endKeys() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
    }

    @Override
    public Writer beginItems() throws IOException {
        this.propertyName("$items");
        this.writeDeferredName();
        return this.open(Scope.EMPTY_ARRAY, "[");
    }

    @Override
    public Writer endItems() throws IOException {
        return this.close(Scope.EMPTY_ARRAY, Scope.NONEMPTY_ARRAY, "]");
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
        this.out.write("null");
        return this;
    }

    @Override
    public Writer value(boolean value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(value ? "true" : "false");
        return this;
    }

    @Override
    public Writer value(byte value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(Byte.toString(value));
        return this;
    }

    @Override
    public Writer value(char value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.stringOf(Character.toString(value));
        return this;
    }

    @Override
    public Writer value(double value) throws IOException {
        if (Double.isNaN(value) || Double.isInfinite(value))
            throw new IllegalArgumentException(
                    "Numeric values must be finite, but was " + value
            );
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(Double.toString(value));
        return this;
    }

    @Override
    public Writer value(float value) throws IOException {
        if (Float.isNaN(value) || Float.isInfinite(value))
            throw new IllegalArgumentException(
                    "Numeric values must be finite, but was " + value
            );
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(Float.toString(value));
        return this;
    }

    @Override
    public Writer value(int value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(Integer.toString(value));
        return this;
    }

    @Override
    public Writer value(long value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(Long.toString(value));
        return this;
    }

    @Override
    public Writer value(short value) throws IOException {
        this.writeDeferredName();
        this.beforeValue();
        this.out.write(Short.toString(value));
        return this;
    }

    @Override
    public Writer value(String value) throws IOException {
        if (value == null)
            return this.nullValue();
        this.writeDeferredName();
        this.beforeValue();
        this.stringOf(value);
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

    private JsonWriter open(Scope empty, String openBracket)
            throws IOException {
        this.beforeValue();
        this.stack.add(empty);
        this.out.write(openBracket);
        return this;
    }

    private JsonWriter close(Scope empty, Scope nonempty, String closeBracket)
            throws IOException {
        Scope context = this.peek();
        if (context != nonempty && context != empty)
            throw new IllegalStateException("Nesting problem: " + stack);
        if (this.deferredName != null)
            throw new IllegalStateException("Dangling name: " + deferredName);
        this.stack.remove(stack.size() - 1);
        if (context == nonempty)
            this.newline();
        out.write(closeBracket);
        if (this.stack.size() == 1)
            this.newline();
        return this;
    }

    private void newline() throws IOException {
        if (this.indent == null)
            return;
        this.out.write("\r\n");
        for (int i = 1; i < this.stack.size(); i++)
            this.out.write(indent);
    }

    private void writeDeferredName() throws IOException {
        if (this.deferredName != null) {
            this.beforeName();
            this.stringOf(deferredName);
            this.deferredName = null;
        }
    }

    private void beforeName() throws IOException {
        Scope context = this.peek();
        if (context == Scope.NONEMPTY_OBJECT)
            this.out.write(',');
        else if (context != Scope.EMPTY_OBJECT)
            throw new IllegalStateException("Nesting problem: " + stack);
        this.newline();
        this.replaceTop(Scope.DANGLING_NAME);
    }

    private void beforeValue() throws IOException {
        switch (this.peek()) {
        case EMPTY_DOCUMENT:
            this.replaceTop(Scope.NONEMPTY_DOCUMENT);
            break;
        case EMPTY_ARRAY:
            this.replaceTop(Scope.NONEMPTY_ARRAY);
            this.newline();
            break;
        case NONEMPTY_ARRAY:
            this.out.append(',');
            this.newline();
            break;
        case DANGLING_NAME: // value for name
            this.out.append(separator);
            this.replaceTop(Scope.NONEMPTY_OBJECT);
            break;
        case NONEMPTY_DOCUMENT:
            throw new IllegalStateException(
                    "JSON must have only one top-level value."
            );
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

    private void stringOf(String value) throws IOException {
        this.out.write("\"");
        for (int i = 0, length = value.length(); i < length; i++) {
            char c = value.charAt(i);
            switch (c) {
            case '"':
            case '\\':
                this.out.write('\\');
                this.out.write(c);
                break;
            case '\t':
                this.out.write("\\t");
                break;
            case '\b':
                this.out.write("\\b");
                break;
            case '\n':
                this.out.write("\\n");
                break;
            case '\r':
                this.out.write("\\r");
                break;
            case '\f':
                this.out.write("\\f");
                break;
            case '<':
            case '>':
            case '&':
            case '=':
            case '\'':
                this.out.write(c);
                break;
            default:
                if (c < 32)
                    this.out.write(String.format("\\u%04x", (int) c));
                else
                    this.out.write(c);
                break;
            }
        }
        this.out.write("\"");
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
}
