package org.mogware.system.dif;

public interface ArrayType extends Target {
    public int size();
    public boolean add(Object value);
    public Object get(int index);
    public void clear();
}
