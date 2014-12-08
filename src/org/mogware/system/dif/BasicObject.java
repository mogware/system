package org.mogware.system.dif;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class BasicObject extends LinkedHashMap<String, Object>
        implements ObjectType {
    Object target;
    String type;

    @Override
    public Object getTarget() {
        return this.target;
    }

    @Override
    public void setTarget(Object target) {
        this.target = target;
    }

    @Override
    public boolean isObjectType() {
        return true;
    }

    @Override
    public boolean isArrayType() {
        return !this.isObjectType();
    }

    @Override
    public boolean isList() {
        if (containsKey("$items") && !containsKey("$keys"))
            return (this.target instanceof Collection);
        if (this.type == null)
            return false;
        try {
            Class c = Decoder.classForName(this.type);
            if (Collection.class.isAssignableFrom(c))
                return true;
        } catch (IOException ex) { }
        return false;
    }

    @Override
    public boolean isMap() {
        if (containsKey("$items") && containsKey("$keys"))
            return (this.target instanceof Map);
        if (this.type == null)
            return false;
        try {
            Class c = Decoder.classForName(this.type);
            if (Map.class.isAssignableFrom(c))
                return true;
        } catch (IOException ex) { }
        return false;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null)
            return super.put(key, value);
        if ("$type".equals(key)) {
            String oldType = this.type;
            this.type = (String) value;
            return oldType;
        }
        return super.put(key, value);
    }

    @Override
    public Object get(String key) {
        return super.get(key);
    }

    @Override
    public ArrayType getKeys() {
        return (ArrayType) get("$items");
    }

    @Override
    public ArrayType getItems() {
        return (ArrayType) get("$items");
    }
}
