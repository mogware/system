package org.mogware.system.delegates;

@FunctionalInterface
public interface Action1<T> extends Action {
    void run(T arg);
}
