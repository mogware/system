package org.mogware.system.delegates;

@FunctionalInterface
public interface Func2<T1, T2, R> extends Function {
    R call(T1 arg1, T2 arg2);
}
