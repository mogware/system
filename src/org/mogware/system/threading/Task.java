package org.mogware.system.threading;

import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import org.mogware.system.delegates.Func0;

public class Task<T> extends FutureTask<T> {
    private final Executor executor;

    public Task(Func0<T> callable) {
        this(callable, TaskFactory.executorService);
    }

    public Task(Func0<T> callable, Executor executor) {
        super(callable);
        this.executor = executor;
        if (this.executor == null)
            throw new NullPointerException("executor must not be null.");
    }

    public void start() {
        this.executor.execute(this);
    }
}
