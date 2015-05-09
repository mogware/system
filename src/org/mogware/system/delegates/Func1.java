package org.mogware.system.delegates;

@FunctionalInterface
public interface Func1<T, R> extends Function {
    R call(T arg);
}
