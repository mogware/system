package org.mogware.system.dif;

import java.util.Map;
import java.util.Set;

public interface ObjectType extends Target {
    public boolean isList();
    public boolean isMap();
    public String getType();
    public Object put(String key, Object value);
    public Object get(String key);
    public Set<Map.Entry<String, Object>> entrySet();
    public ArrayType getKeys();
    public ArrayType getItems();
    public void clear();
}
