package org.mogware.system.ioc;

public class Registration {
    private Object instance;
    private final Resolver<Object> resolve;
    private boolean instancePerCall = false;

    public Registration(Resolver<Object> resolve) {
        this.resolve = resolve;
        this.instance = null;
    }

    public Registration(Object instance) {
        this.instance = instance;
        this.resolve = null;
    }

    public Registration instancePerCall() {
        this.instancePerCall = true;
        return this;
    }

    public Object resolve(Container container) {
        if (this.instancePerCall)
            return this.resolve.apply(container);
        if (this.instance != null)
            return this.instance;
        return this.instance = this.resolve.apply(container);
    }
}
