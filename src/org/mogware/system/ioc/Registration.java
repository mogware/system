package org.mogware.system.ioc;

import org.mogware.system.delegates.Func1;

public class Registration {
    private Object instance;
    private final Func1<Container, Object> resolve;
    private boolean instancePerCall = false;

    public Registration(Func1<Container, Object> resolve) {
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
            return this.resolve.call(container);
        if (this.instance != null)
            return this.instance;
        return this.instance = this.resolve.call(container);
    }
}
