package org.mogware.system.dif;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

public class Decoder {
    private final ClassReaders readers = new ClassReaders();

    // Save memory by re-using common Characters (Characters are immutable)
    private static final Character[] charCache = new Character[128];
    static {
        for (int i = 0; i < Decoder.charCache.length; i++)
            Decoder.charCache[i] = (char) i;
    }

    // Save memory by re-using all byte instances (Bytes are immutable)
    private static final Byte[] byteCache = new Byte[256];
    static {
        for (int i = 0; i < Decoder.byteCache.length; i++)
            Decoder.byteCache[i] = (byte) (i - 128);
    }

    private static final Map<String, Class> nameToClass = new HashMap<>();
    static {
        Decoder.nameToClass.put("string", String.class);
        Decoder.nameToClass.put("boolean", boolean.class);
        Decoder.nameToClass.put("char", char.class);
        Decoder.nameToClass.put("byte", byte.class);
        Decoder.nameToClass.put("short", short.class);
        Decoder.nameToClass.put("int", int.class);
        Decoder.nameToClass.put("long", long.class);
        Decoder.nameToClass.put("float", float.class);
        Decoder.nameToClass.put("double", double.class);
        Decoder.nameToClass.put("date", Date.class);
        Decoder.nameToClass.put("class", Class.class);
    }

    private static final Map<Class, Object[]> constructors = new HashMap<>();
    private static final Primitives prims = new Primitives();
    private static final ClassMeta meta = new ClassMeta();
    private static final Class[] emptyClassArray = new Class[]{};

    public Decoder() {
    }

    public Object decode(Reader reader) throws IOException {
        final List<Target> result = new ArrayList<>();
        reader.parse(new DefaultHandler() {
            @Override
            public void beginObject() throws IOException {
                ObjectType obj = reader.newObjectType();
                parseObject(reader, obj);
                result.add(obj);
            }
            @Override
            public void beginArray() throws IOException {
                ArrayType ary = reader.newArrayType();
                parseArray(reader, ary);
                result.add(ary);
            }
            @Override
            public void primitive(Object value) throws IOException {
                if (value == null) return;  // can only be "null"
                throw new IllegalStateException("Primitive unexpected");
            }
        });
        if (result.isEmpty())
            return null;
        if (result.size() != 1)
            throw new IOException("Single root needed");
        return convertParsedToJava(result.get(0));
    }

    private static void parseObject(final Reader reader, final ObjectType obj)
            throws IOException {
        reader.pushHandler(new DefaultHandler() {
            @Override
            public void beginObjectEntry(String key) throws IOException {
                parseObjectEntry(reader, obj, key);
            }
            @Override
            public void endObject() throws IOException {
                reader.popHandler();
            }
        });
    }

    private static void parseObjectEntry(final Reader reader,
            final ObjectType obj, final String key) throws IOException {
        reader.pushHandler(new DefaultHandler() {
            @Override
            public void beginObject() throws IOException {
                ObjectType val = reader.newObjectType();
                parseObject(reader, val);
                obj.put(key, val);
            }
            @Override
            public void beginArray() throws IOException {
                ArrayType val = reader.newArrayType();
                parseArray(reader, val);
                obj.put(key, val);
            }
            @Override
            public void primitive(Object val) throws IOException {
                obj.put(key, val);
            }
            @Override
            public void endObjectEntry() throws IOException {
                reader.popHandler();
            }
        });
    }

    private static void parseArray(final Reader reader, final ArrayType ary)
            throws IOException {
        reader.pushHandler(new DefaultHandler() {
            @Override
            public void beginObject() throws IOException {
                ObjectType val = reader.newObjectType();
                parseObject(reader, val);
                ary.add(val);
            }
            @Override
            public void beginArray() throws IOException {
                ArrayType val = reader.newArrayType();
                parseArray(reader, val);
                ary.add(val);
            }
            @Override
            public void primitive(Object val) throws IOException {
                ary.add(val);
            }
            @Override
            public void endArray() throws IOException {
                reader.popHandler();
            }
        });
    }

    protected Object convertParsedToJava(Target root) throws IOException {
        Class clazz = root.isArrayType() ? Object[].class : Object.class;
        Object target = this.getJavaInstance(clazz, root);
        root.setTarget(target);
        return this.convertTargetToJava(root);
    }

    protected Object convertTargetToJava(Target root) throws IOException {
        LinkedList<Target> stack = new LinkedList<>();
        stack.addFirst(root);

        while (!stack.isEmpty()) {
            Target obj = stack.removeFirst();
            if (obj.isObjectType()) {
                ObjectType objType = (ObjectType) obj;
                if (objType.isList())
                    traverseList(stack, objType);
                else if (objType.isMap())
                    traverseMap(stack, objType);
                else
                    traverseFields(stack, objType);
                objType.clear();
            }
            else if (obj.isArrayType()) {
                ArrayType aryType = (ArrayType) obj;
                traverseArray(stack, aryType);
                aryType.clear();
            }
        }

        return root.getTarget();
    }

    protected void traverseArray(LinkedList<Target> stack, ArrayType obj)
            throws IOException {
        int size = obj.size();
        if (size == 0)
            return;

        Object target = obj.getTarget();
        Class type = target.getClass().getComponentType();
        boolean isPrimitive = Decoder.prims.isPrimitive(type);

        for (int i = 0; i < size; i++) {
            Object custom, elem = obj.get(i);
            if (elem == null)
                Array.set(target, i, null);
            else if (isPrimitive)
                Array.set(target, i, Decoder.newPrimitiveWrapper(type, elem));
            else if ((custom = readIfMatching(elem, type, stack)) != null)
                Array.set(target, i, custom);
            else if (elem instanceof Target) {
                Object inst = this.getJavaInstance(type, (Target) elem);
                ((Target)elem).setTarget(inst);
                Array.set(target, i, inst);
                if (!Decoder.prims.isPrimitive(inst.getClass()))
                    stack.addFirst((Target) elem);
            }
            else if (elem instanceof String && type != String.class &&
                    type != Object.class && "".equals(((String) elem).trim()))
                Array.set(target, i, null);
            else
                Array.set(target, i, elem);
        }
        obj.clear();
    }

    protected void traverseList(LinkedList<Target> stack, ObjectType obj)
            throws IOException {
        ArrayType items = obj.getItems();
        if (items == null)
            return;

        int size = items.size();
        if (size == 0)
            return;

        Collection target = (Collection)obj.getTarget();
        for (int i = 0; i < size; i++) {
            Object custom, elem = items.get(i);
            if (elem == null)
                target.add(null);
            else if (Decoder.prims.isPrimitive(elem.getClass()))
                target.add(Decoder.newPrimitiveWrapper(elem.getClass(), elem));
            else if ((custom = readIfMatching(elem, null, stack)) != null)
                target.add(custom);
            else if (elem instanceof Target) {
                Object inst = this.getJavaInstance(Object.class, (Target)elem);
                ((Target)elem).setTarget(inst);
                target.add(inst);
                if (!Decoder.prims.isPrimitive(inst.getClass()))
                    stack.addFirst((Target) elem);
            }
            else
                target.add(elem);
        }
        obj.clear();
    }

    protected void traverseMap(LinkedList<Target> stack, ObjectType obj)
            throws IOException {
        ArrayType keys = obj.getKeys();
        ArrayType items = obj.getItems();
        Object[] props, values;

        if (keys == null && items == null) {
            props = new Object[obj.entrySet().size()];
            values = new Object[obj.entrySet().size()];
            Iterator<Map.Entry<String, Object>> i = obj.entrySet().iterator();
            for (int n = 0; i.hasNext(); n++) {
                Map.Entry<String, Object> e = i.next();
                props[n] = e.getKey();
                values[n] = e.getValue();
            }
        }
        else if (keys == null || items == null)
            throw new IOException("Map where eiter keys or items are empty");
        else if (keys.size() != items.size())
            throw new IOException("Map with keys and items of different size");
        else {
            props = new Object[keys.size()];
            values = new Object[keys.size()];
            for (int n = 0; n < props.length; n++) {
                props[n] = keys.get(n);
                values[n] = items.get(n);
            }
        }
        Map target = (Map)obj.getTarget();
        for (int n = 0; n < props.length; n++) {
            Object custom, prop = props[n];
            if (prop == null)
                props[n] = null;
            else if (Decoder.prims.isPrimitive(prop.getClass()))
                props[n] = Decoder.newPrimitiveWrapper(prop.getClass(), prop);
            else if ((custom = readIfMatching(prop, null, stack)) != null)
                props[n] = custom;
            else if (prop instanceof Target) {
                Object inst = this.getJavaInstance(Object.class, (Target)prop);
                ((Target)prop).setTarget(inst);
                props[n] = inst;
                if (!Decoder.prims.isPrimitive(inst.getClass()))
                    stack.addFirst((Target) prop);
            }
            else
                props[n] = prop;
            Object elem = values[n];
            if (elem == null)
                target.put(props[n], null);
            else if (Decoder.prims.isPrimitive(elem.getClass()))
                target.put(props[n],
                        Decoder.newPrimitiveWrapper(elem.getClass(), elem));
            else if ((custom = readIfMatching(elem, null, stack)) != null)
                target.put(props[n], custom);
            else if (elem instanceof Target) {
                Object inst = this.getJavaInstance(Object.class, (Target)elem);
                ((Target)elem).setTarget(inst);
                target.put(props[n], inst);
                if (!Decoder.prims.isPrimitive(inst.getClass()))
                    stack.addFirst((Target) elem);
            }
            else
                target.put(props[n], elem);
        }
        obj.clear();
    }

    protected void traverseFields(LinkedList<Target> stack, ObjectType obj)
            throws IOException {
        Object custom = readIfMatching(obj, null, stack);
        if (custom != null)
            obj.setTarget(custom);
        else {
            ClassMeta.Meta classInfo = Decoder.meta.getDeepDeclaredFields(
                    obj.getTarget().getClass()
            );
            Iterator<Map.Entry<String, Object>> i = obj.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, Object> e = i.next();
                Field field = classInfo.get(e.getKey());
                if (field != null)
                    assignField(stack, obj, field, e.getValue());
            }
        }
        obj.clear();
    }

    protected void assignField(LinkedList<Target> stack, ObjectType obj,
            Field field, Object rhs) throws IOException {
        Object custom, target = obj.getTarget();
        try {
            Class type = field.getType();
            if (rhs == null)
                field.set(target, null);
            else if (Decoder.prims.isPrimitive(type))
                field.set(target, Decoder.newPrimitiveWrapper(type, rhs));
            else if ((custom = readIfMatching(rhs, type, stack)) != null)
                field.set(target, custom);
            else if (rhs instanceof Target) {
                Object inst = this.getJavaInstance(Object.class, (Target) rhs);
                ((Target)rhs).setTarget(inst);
                field.set(target, inst);
                if (!Decoder.prims.isPrimitive(rhs.getClass()))
                    stack.addFirst((Target) rhs);
            }
            else if (rhs instanceof String && type != String.class &&
                    type != Object.class && "".equals(((String) rhs).trim()))
                field.set(target, null);
            else
                field.set(target, rhs);
        } catch (Exception ex) {
            throw new IOException(
                ex.getClass().getSimpleName() + " setting field '" +
                field.getName() + "' on target: " + target + " with value: " +
                rhs, ex
            );
        }
    }

    protected Object readIfMatching(Object obj, Class type,
            LinkedList<Target> stack) throws IOException {
        if (! (obj instanceof Target))
            return null;
        if (! ((Target)obj).isObjectType())
            return null;
        Class componentClass;
        if (((ObjectType) obj).getTarget() != null)
            componentClass = ((ObjectType) obj).getTarget().getClass();
        else {
            String typeStr = ((ObjectType) obj).getType();
            if (typeStr != null)
                componentClass = Decoder.classForName(typeStr);
            else if (type != null)
                componentClass = type;
            else
                return null;
        }
        ClassReaders.ClassReader closestReader =
                this.readers.getClosestReader(componentClass);
        if (closestReader == null)
            return null;
        return closestReader.read(obj, stack);
    }

    protected Object getJavaInstance(Class clazz, Target obj)
            throws IOException {
        if (obj.isObjectType()) {
            Class c = Decoder.classForName(((ObjectType)obj).getType());
            if (Decoder.prims.isPrimitive(c))
                return newPrimitiveWrapper(c, ((ObjectType)obj).get("value"));
            if (c.isEnum())
                return Decoder.getEnum(c, (ObjectType)obj);
            if (Enum.class.isAssignableFrom(c))
                return Decoder.getEnum(c.getSuperclass(), (ObjectType)obj);
            return this.newInstance(c);
        }
        return Array.newInstance(
                clazz.isArray() ? clazz.getComponentType() : Object.class,
                ((ArrayType)obj).size()
        );
    }

    private static Object getEnum(Class c, ObjectType obj) {
        try {
            return Enum.valueOf(c, (String) obj.get("name"));
        } catch (Exception e) {
            return Enum.valueOf(c, (String) obj.get("java.lang.Enum.name"));
        }
    }

    public static Class classForName(String name) throws IOException
    {
        if (name == null || name.isEmpty())
            throw new IOException("Invalid class name specified");
        try {
            Class c = Decoder.nameToClass.get(name);
            return c == null ? Decoder.loadClass(name) : c;
        } catch (ClassNotFoundException ex) {
            throw new IOException(
                "Class instance '" + name + "' could not be created", ex
            );
        }
    }

    public static Object newInstance(Class c) throws IOException {
        Object[] constructorInfo = Decoder.constructors.get(c);
        if (constructorInfo != null) {
            Constructor ctor = (Constructor) constructorInfo[0];
            Boolean useNull = (Boolean) constructorInfo[1];
            Class[] paramTypes = ctor.getParameterTypes();
            if (paramTypes == null || paramTypes.length == 0) {
                try {
                    return ctor.newInstance();
                } catch (Exception ex) {
                    throw new IOException(
                        "Could not instantiate " + c.getName(), ex
                    );
                }
            }
            Object[] values = Decoder.fillArgs(paramTypes, useNull);
            try {
                return ctor.newInstance(values);
            } catch (Exception ex) {
                throw new IOException(
                    "Could not instantiate " + c.getName(), ex
                );
            }
        }
        Object[] ret = Decoder.newInstanceEx(c);
        Decoder.constructors.put(c, new Object[] {ret[1], ret[2]});
        return ret[0];
    }

    private static Object[] newInstanceEx(Class c) throws IOException
    {
        try {
            Constructor ctor = c.getConstructor(Decoder.emptyClassArray);
            if (ctor != null)
                return new Object[] { ctor.newInstance(), ctor, true };
            return Decoder.tryOtherConstructors(c);
        } catch (Exception ex) {
            return Decoder.tryOtherConstructors(c);
        }
    }

    private static Object[] tryOtherConstructors(Class c) throws IOException {
        Constructor[] ctors = c.getDeclaredConstructors();
        if (ctors.length == 0)
            throw new IOException("Cannot instantiate '" + c.getName() +
                    "' - Primitive, interface, array[] or void");

        for (Constructor ctor : ctors) {
            ctor.setAccessible(true);
            Class[] argTypes = ctor.getParameterTypes();
            Object[] values = Decoder.fillArgs(argTypes, true);
            try {
                return new Object[] { ctor.newInstance(values), ctor, true };
            } catch (Exception ex) {
            }
        }

        for (Constructor ctor : ctors) {
            ctor.setAccessible(true);
            Class[] argTypes = ctor.getParameterTypes();
            Object[] values = Decoder.fillArgs(argTypes, false);
            try {
                return new Object[] {ctor.newInstance(values), ctor, false};
            } catch (Exception ex) {
            }
        }

        throw new IOException(
            "Could not instantiate " + c.getName() + " using any constructor"
        );
    }

    private static Object[] fillArgs(Class[] argTypes, boolean useNull)
            throws IOException {
        Object[] values = new Object[argTypes.length];
        for (int i = 0; i < argTypes.length; i++) {
            final Class argType = argTypes[i];
            if (Decoder.prims.isPrimitive(argType))
                values[i] = newPrimitiveWrapper(argType, null);
            else if (useNull)
                values[i] = null;
            else if (argType == String.class)
                values[i] = "";
            else if (argType == Date.class)
                values[i] = new Date();
            else if (List.class.isAssignableFrom(argType))
                values[i] = new ArrayList();
            else if (SortedSet.class.isAssignableFrom(argType))
                values[i] = new TreeSet();
            else if (Set.class.isAssignableFrom(argType))
                values[i] = new LinkedHashSet();
            else if (SortedMap.class.isAssignableFrom(argType))
                values[i] = new TreeMap();
            else if (Map.class.isAssignableFrom(argType))
                values[i] = new LinkedHashMap();
            else if (Collection.class.isAssignableFrom(argType))
                values[i] = new ArrayList();
            else if (Calendar.class.isAssignableFrom(argType))
                values[i] = Calendar.getInstance();
            else if (TimeZone.class.isAssignableFrom(argType))
                values[i] = TimeZone.getDefault();
            else if (argType == BigInteger.class)
                values[i] = BigInteger.TEN;
            else if (argType == BigDecimal.class)
                values[i] = BigDecimal.TEN;
            else if (argType == StringBuilder.class)
                values[i] = new StringBuilder();
            else if (argType == StringBuffer.class)
                values[i] = new StringBuffer();
            else if (argType == Locale.class)
                values[i] = Locale.FRANCE;  // overwritten
            else if (argType == Class.class)
                values[i] = String.class;
            else if (argType == java.sql.Timestamp.class)
                values[i] = new Timestamp(System.currentTimeMillis());
            else if (argType == java.sql.Date.class)
                values[i] = new java.sql.Date(System.currentTimeMillis());
            else if (argType == java.net.URL.class)
                values[i] = new URL("http://localhost"); // overwritten
            else if (argType == Object.class)
                values[i] = new Object();
            else
                values[i] = null;
        }
        return values;
    }

    private static Object newPrimitiveWrapper(Class c, Object rhs)
            throws IOException {
        if (c == Byte.class || c == byte.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "0";
                return Byte.parseByte((String)rhs);
            }
            return rhs != null ?
                Decoder.byteCache[((Number) rhs).byteValue() + 128] : (byte) 0;
        }
        if (c == Boolean.class || c == boolean.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "false";
                return Boolean.parseBoolean((String)rhs);
            }
            return rhs != null ? rhs : Boolean.FALSE;
        }
        if (c == Integer.class || c == int.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "0";
                return Integer.parseInt((String)rhs);
            }
            return rhs != null ? ((Number) rhs).intValue() : 0;
        }
        if (c == Long.class || c == long.class || c == Number.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "0";
                return Long.parseLong((String)rhs);
            }
            return rhs != null ? rhs : 0L;
        }
        if (c == Short.class || c == short.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "0";
                return Short.parseShort((String)rhs);
            }
            return rhs != null ? ((Number) rhs).shortValue() : (short) 0;
        }
        if (c == Double.class || c == double.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "0.0";
                return Double.parseDouble((String)rhs);
            }
            return rhs != null ? rhs : 0.0d;
        }
        if (c == Float.class || c == float.class) {
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "0.0f";
                return Float.parseFloat((String)rhs);
            }
            return rhs != null ? ((Number) rhs).floatValue() : 0.0f;
        }
        if (c == Character.class || c == char.class) {
            if (rhs == null)
                return '\u0000';
            if (rhs instanceof String) {
                if ("".equals(rhs))
                    rhs = "\u0000";
                return valueOf(((String) rhs).charAt(0));
            }
            if (rhs instanceof Character)
                return rhs;
        }

        throw new IOException(
            "Class '" + c.getName() + "' does not match newPrimitiveWrapper()"
        );
    }

    private static Class loadClass(String name) throws ClassNotFoundException {
        return Thread.currentThread().getContextClassLoader().loadClass(name);
    }

    private static Character valueOf(char c) {
        return c <= 127 ? Decoder.charCache[(int) c] : c;
    }

    private static class DefaultHandler implements Reader.ContentHandler {
        @Override
        public void begin() throws IOException {
        }
        @Override
        public void end() throws IOException {
        }
        @Override
        public void beginObject() throws IOException {
            throw new IllegalStateException("Start of object unexpected");
        }
        @Override
        public void endObject() throws IOException {
        }
        @Override
        public void beginObjectEntry(String key) throws IOException {
            throw new IllegalStateException("Entry " + key + " unexpected");
        }
        @Override
        public void endObjectEntry() throws IOException {
        }
        @Override
        public void beginArray() throws IOException {
            throw new IllegalStateException("Start of array unexpected");
        }
        @Override
        public void endArray() throws IOException {
        }
        @Override
        public void primitive(Object value) throws IOException {
            throw new IllegalStateException("Primitive unexpected");
        }
    }
}
