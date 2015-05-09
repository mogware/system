package org.mogware.system.delegates;

@FunctionalInterface
public interface Action0 extends Action, Runnable {
    @Override
    void run();
}
