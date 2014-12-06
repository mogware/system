package org.mogware.system.dif;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassMeta {
    private final Map<String, Meta> classMetaCache = new ConcurrentHashMap<>();

    public static class Meta extends LinkedHashMap<String, Field> {}

    public Meta getDeepDeclaredFields(Class c) {
        Meta classInfo = this.classMetaCache.get(c.getName());
        if (classInfo != null)
            return classInfo;
        classInfo = new Meta();
        for (Class curr = c; curr != null; curr = curr.getSuperclass()) {
            try {
                Field[] local = curr.getDeclaredFields();

                for (Field field : local) {
                    if ((field.getModifiers() & Modifier.STATIC) == 0) {
                        if (!field.isAccessible()) {
                            try {
                                field.setAccessible(true);
                            } catch (Exception ex) { }
                        }
                        if (classInfo.containsKey(field.getName()))
                            classInfo.put(curr.getName() + '.' +
                                    field.getName(), field);
                        else
                            classInfo.put(field.getName(), field);
                    }
                }
            } catch (ThreadDeath t) {
                throw t;
            } catch (Throwable ex) { }
        }
        classMetaCache.put(c.getName(), classInfo);
        return classInfo;
    }
}
