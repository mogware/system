package org.mogware.system.ioc;

@FunctionalInterface
public interface Resolver<TService> {
    TService apply(Container context);
}
