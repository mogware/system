package org.mogware.system.dif;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class Encoder {
    private final boolean publicEnumsOnly;

    private final ClassWriters writers = new ClassWriters();

    private static final Primitives prims = new Primitives();
    private static final ClassMeta meta = new ClassMeta();

    public Encoder() {
        this.publicEnumsOnly = false;
    }

    public void encode(Writer writer, Object value) throws IOException {
        this.writeImpl(writer, value);
    }

    private void writeImpl(Writer out, Object obj)
            throws IOException {
        if (obj == null)
            out.nullValue();
        else if (obj.getClass().isArray())
            this.writeArray(out, obj);
        else if (obj instanceof Collection)
            this.writeCollection(out, (Collection) obj);
        else if (obj instanceof Map) {
            if(! this.writeMapWithStringKeys(out, (Map) obj))
                this.writeMap(out, (Map) obj);
        }
        else if (! writeIfMatching(out, true, obj))
            this.writeObject(out, obj);
    }

    private void writeArray(Writer out, Object array)
            throws IOException {
        Class arrayType = array.getClass();
        int len = Array.getLength(array);
        out.beginArray();
        if (byte[].class == arrayType) {
            byte[] bytes = (byte[]) array;
            for (int i = 0; i < len; i++)
                out.value(bytes[i]);
        }
        else if (char[].class == arrayType) {
            char[] chars = (char[]) array;
            for (int i = 0; i < len; i++)
                out.value(chars[i]);
        }
        else if (short[].class == arrayType) {
            short[] shorts = (short[]) array;
            for (int i = 0; i < len; i++)
                out.value(shorts[i]);
        }
        else if (int[].class == arrayType) {
            int[] ints = (int[]) array;
            for (int i = 0; i < len; i++)
                out.value(ints[i]);
        }
        else if (long[].class == arrayType) {
            long[] longs = (long[]) array;
            for (int i = 0; i < len; i++)
                out.value(longs[i]);
        }
        else if (float[].class == arrayType) {
            float[] floats = (float[]) array;
            for (int i = 0; i < len; i++)
                out.value(floats[i]);
        }
        else if (double[].class == arrayType) {
            double[] doubles = (double[]) array;
            for (int i = 0; i < len; i++)
                out.value(doubles[i]);
        }
        else if (boolean[].class == arrayType) {
            boolean[] bools = (boolean[]) array;
            for (int i = 0; i < len; i++)
                out.value(bools[i]);
        }
        else {
            final Class componentClass = array.getClass().getComponentType();
            final boolean isPrimitiveArray =
                    Encoder.prims.isPrimitive(componentClass);
            final boolean isObjectArray = Object[].class == arrayType;

            for (int i = 0; i < len; i++) {
                final Object value = Array.get(array, i);
                if (value == null)
                    out.nullValue();
                else if (isPrimitiveArray || value instanceof Boolean ||
                        value instanceof Long || value instanceof Double)
                    this.writePrimitive(out, value);
                else if (! this.writeArrayElementIfMatching(out,
                        componentClass, false, value)) {
                    if (isObjectArray) {
                        if (! writeIfMatching(out, true, value))
                            this.writeImpl(out, value);
                    }
                    else
                        this.writeImpl(out, value);
                }
           }
        }
        out.endArray();
    }

    private void writeCollection(Writer out, Collection col)
            throws IOException {
        out.beginList(col.getClass());
        Iterator i = col.iterator();
        while (i.hasNext()) {
            this.writeElement(out, i.next());
        }
        out.endList();
    }

    private boolean writeMapWithStringKeys(Writer out, Map map)
            throws IOException {
        if (! Encoder.ensureStringKeys(map))
            return false;
        out.beginMap(map.getClass());
        Iterator i = map.entrySet().iterator();
        while (i.hasNext()) {
            final Map.Entry entry = (Map.Entry) i.next();
            out.propertyName((String) entry.getKey());
            this.writeElement(out, entry.getValue());
        }
        out.endMap();
        return true;
    }

    private static boolean ensureStringKeys(Map map) {
        for (Object o : map.keySet()) {
            if (!(o instanceof String))
                return false;
        }
        return true;
    }

    private void writeMap(Writer out, Map map) throws IOException {
        out.beginMap(map.getClass());

        out.beginKeys();
        Iterator i = map.keySet().iterator();
        while (i.hasNext()) {
            this.writeElement(out, i.next());
        }
        out.endKeys();

        out.beginItems();
        i = map.values().iterator();
        while (i.hasNext()) {
            this.writeElement(out, i.next());
        }
        out.endItems();

        out.endMap();
    }

    private void writeObject(Writer out, Object obj)
            throws IOException {
        out.beginObject(obj.getClass());
        ClassMeta.Meta classInfo =
                Encoder.meta.getDeepDeclaredFields(obj.getClass());
        for (Map.Entry<String, Field> entry : classInfo.entrySet()) {
            String fieldName = entry.getKey();
            Field field = entry.getValue();
            this.writeField(out, obj, fieldName, field);
        }
        out.endObject();
    }

    private void writeField(Writer out, Object obj, String fieldName,
            Field field) throws IOException {
        int modifiers = field.getModifiers();
        if ((modifiers & Modifier.TRANSIENT) != 0)
            return;
        if (field.getDeclaringClass().isEnum() &&
                !Modifier.isPublic(modifiers) && this.publicEnumsOnly)
            return;

        out.propertyName(fieldName);

        Object o;
        try {
            o = field.get(obj);
        } catch (Exception ex) {
            o = null;
        }
        if (o == null)
            out.nullValue();
        else {
            Class type = field.getType();
            boolean forceType = o.getClass() != type;
            if (Encoder.prims.isPrimitive(type))
                this.writePrimitive(out, o);
            else if (! this.writeIfMatching(out, forceType, o))
                this.writeImpl(out, o);
        }
    }

    private void writePrimitive(Writer out, Object obj) throws IOException {
        if (obj instanceof Boolean)
            out.value((boolean) obj);
        else if (obj instanceof Character)
            out.value((char) obj);
        else if (obj instanceof Byte)
            out.value((byte) obj);
        else if (obj instanceof Double)
            out.value((double) obj);
        else if (obj instanceof Float)
            out.value((float) obj);
        else if (obj instanceof Integer)
            out.value((int) obj);
        else if (obj instanceof Long)
            out.value((long) obj);
        else if (obj instanceof Short)
            out.value((short) obj);
        else
            throw new IOException("Unknown primitive: " + obj.getClass());
    }

    private boolean writeIfMatching(Writer out, boolean showType, Object obj)
            throws IOException {
        return this.writeCustom(out, obj.getClass(), showType, obj);
    }

    private boolean writeArrayElementIfMatching(Writer out,
        Class componentClass, boolean showType, Object obj) throws IOException {
        if (!obj.getClass().isAssignableFrom(componentClass))
            return false;
        return this.writeCustom(out, componentClass, showType, obj);
    }

    private boolean writeCustom(Writer out, Class componentClass,
            boolean showType, Object obj) throws IOException {
        ClassWriters.ClassWriter closestWriter =
                this.writers.getClosestWriter(componentClass);
        if (closestWriter == null)
            return false;

        if (!showType && closestWriter.hasPrimitiveForm())
            closestWriter.writePrimitiveForm(out, obj);
        else {
            out.beginObject(obj.getClass());
            closestWriter.write(out, obj);
            out.endObject();
        }

        return true;
    }

    private void writeElement(Writer out, Object obj)
            throws IOException {
        if (obj == null)
            out.nullValue();
        else if (Encoder.prims.isPrimitive(obj.getClass()))
            this.writePrimitive(out, obj);
        else if (! this.writeIfMatching(out, true, obj))
            this.writeImpl(out, obj);
    }
}
