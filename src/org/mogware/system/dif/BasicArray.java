package org.mogware.system.dif;

import java.util.ArrayList;

public class BasicArray extends ArrayList<Object> implements ArrayType {
    Object target;

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
        return false;
    }

    @Override
    public boolean isArrayType() {
        return !this.isObjectType();
    }
}
    