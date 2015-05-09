package org.mogware.system.delegates;

@FunctionalInterface
public interface Action2<T1, T2> extends Action {
    void run(T1 arg1, T2 arg2);
}

