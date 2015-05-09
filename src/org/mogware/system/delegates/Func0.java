package org.mogware.system.delegates;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface Func0<R> extends Function, Callable<R> {
    @Override
    R call();
}
