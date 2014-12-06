package org.mogware.system.dif;

import java.util.HashSet;
import java.util.Set;

public class Primitives {
    private final Set<Class> prims = new HashSet<>();

    public Primitives() {
        this.prims.add(Byte.class);
        this.prims.add(Integer.class);
        this.prims.add(Long.class);
        this.prims.add(Double.class);
        this.prims.add(Character.class);
        this.prims.add(Float.class);
        this.prims.add(Boolean.class);
        this.prims.add(Short.class);
    }

    public boolean isPrimitive(Class c) {
        return c.isPrimitive() || this.prims.contains(c);
    }
}
