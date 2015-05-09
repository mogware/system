package org.mogware.system.threading;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.mogware.system.delegates.Func0;

public class TaskFactory {
    public static ExecutorService executorService =
            Executors.newCachedThreadPool();

    public static <T> Task<T> startNew(Func0<T> callable) {
        Task<T> task = new Task<>(callable);
        task.start();
        return task;
    }

    public static <T> Task<T> startNew(Func0<T> callable, Executor executor) {
        Task<T> task = new Task<>(callable, executor);
        task.start();
        return task;
    }

    public static boolean shutdown(long timeout) {
        executorService.shutdown();
        try {
            executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            return false;
        }
        return true;
    }
}
