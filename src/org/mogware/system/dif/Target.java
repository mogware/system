package org.mogware.system.dif;

public interface Target {
    public Object getTarget();
    public void setTarget(Object target);
    public boolean isObjectType();
    public boolean isArrayType();
}
