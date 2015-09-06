package org.mogware.system.ioc;

import java.util.HashMap;
import java.util.Map;
import org.mogware.system.delegates.Func1;

public class Container {
    private final Map<Class<?>, Registration> registrations = new HashMap<>();

    private boolean isValueType(Class<?> clazz) {
        if (clazz.isArray())
            return true;
        if (clazz.isEnum())
            return true;
        return clazz.isPrimitive();
    }

    public Registration register(Class<?> service,
            Func1<Container, Object> resolve) {
        if (service == null)
            throw new NullPointerException("service must not be null");
        Registration registration = new Registration(resolve);
        this.registrations.put(service, registration);
        return registration;
    }

    public Registration register(Class<?> service, Object instance) {
        if (service == null)
            throw new NullPointerException("service must not be null");
        if (instance == null)
            throw new NullPointerException("instance must not be null");
        if (!this.isValueType(service) && !service.isInterface())
            throw new IllegalArgumentException("instance must be an interface");
        if (! service.isAssignableFrom(instance.getClass()))
            throw new IllegalArgumentException(
                    "instance not implementing: " + service.getName()
            );
        Registration registration = new Registration(instance);
        this.registrations.put(service, registration);
        return registration;
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<?> service) {
        if (service == null)
            throw new NullPointerException("service must not be null");
        Registration registration = this.registrations.get(service);
        if (registration != null)
            return (T) registration.resolve(this);
        return null;
    }
}
