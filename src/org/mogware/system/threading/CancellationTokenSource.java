package org.mogware.system.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.mogware.system.Disposable;
import org.mogware.system.delegates.Action0;

public final class CancellationTokenSource implements Disposable {
    public static final int ALLREADYCANCELED = 0;    
    public static final int NOTCANCELED = 1;
    public static final int NOTIFYING = 2;
    public static final int NOTIFYINGCOMPLETE = 3;

    private volatile AtomicInteger state = new AtomicInteger();

    final private List<Action0> registeredCallbacks = 
            Collections.synchronizedList(new ArrayList<Action0>());
    private volatile Action0 executingCallback = null;
    private volatile long threadIDExecutingCallbacks = -1;
    
    private CancellationTokenRegistration[] linkingRegistrations;

    public CancellationTokenSource() {
        this.state.set(NOTCANCELED);
    }

    public CancellationTokenSource(boolean set) {
        this.state.set(set ? NOTIFYINGCOMPLETE : ALLREADYCANCELED);
    }
    
    public boolean isCancellationRequested() {
        return this.state.get() >= NOTIFYING;
    }

    public boolean isCancellationCompleted() {
        return this.state.get() == NOTIFYINGCOMPLETE;
    }

    public void cancel() {
        this.notifyCancellation();
    }

    public CancellationToken getToken() {
        return new Token(this);
    }

    @Override
    public void dispose() {
        CancellationTokenRegistration[] linkingRegistrations =
                this.linkingRegistrations;
        if (linkingRegistrations != null) {
            this.linkingRegistrations = null;
            for (int i = 0; i < linkingRegistrations.length; i++)
                linkingRegistrations[i].dispose();
        }
        this.registeredCallbacks.clear();
    }

    public static CancellationTokenSource createLinkedTokenSource(
            CancellationToken token1, CancellationToken token2) {
        CancellationTokenSource linkedSource = new CancellationTokenSource();
        boolean token2CanBeCanceled = token2.canBeCanceled();
        if (token1.canBeCanceled()) {
            linkedSource.linkingRegistrations =
                    new CancellationTokenRegistration[token2CanBeCanceled?2:1];
            linkedSource.linkingRegistrations[0] =
                    token1.register(() -> linkedSource.cancel());
        }
        if (token2CanBeCanceled) {
            int index = 1;
            if (linkedSource.linkingRegistrations == null) {
                index = 0;
                linkedSource.linkingRegistrations =
                        new CancellationTokenRegistration[1];
            }
            linkedSource.linkingRegistrations[index] =
                    token2.register(() -> linkedSource.cancel());
        }
        return linkedSource;
    }

    public static CancellationTokenSource createLinkedTokenSource(
            CancellationToken[] tokens) {
        if (tokens == null)
            throw new NullPointerException("tokens must not be null.");
        if (tokens.length == 0)
            throw new IllegalArgumentException("Tokens array is empty.");
        CancellationTokenSource linkedSource = new CancellationTokenSource();
        linkedSource.linkingRegistrations =
                new CancellationTokenRegistration[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].canBeCanceled())
                linkedSource.linkingRegistrations[i] =
                        tokens[i].register(() -> linkedSource.cancel());
        }
        return linkedSource;
    }

    public static CancellationToken getTokenNone() {
        return new Token(null);
    }

    private CancellationTokenRegistration register(Action0 callback) {
        if (! this.isCancellationRequested()) {
            this.registeredCallbacks.add(callback);
            TokenRegistration registration = new TokenRegistration(
                    this, callback, this.registeredCallbacks);
            if (! this.isCancellationRequested())
                return registration;
            boolean deregisterOccurred = registration.tryDeregister();
            if (! deregisterOccurred)
                return registration;
        }
        callback.run();
        return new TokenRegistration(null, null, null);
    }

    private void notifyCancellation() {
        if (this.isCancellationRequested())
            return;
        if (this.state.compareAndSet(NOTCANCELED, NOTIFYING)) {
            this.threadIDExecutingCallbacks = Thread.currentThread().getId();
            this.executeCallbackHandlers();
        }
    }

    private void executeCallbackHandlers() {
        Action0[] callbacks = this.registeredCallbacks.toArray(
                new Action0[this.registeredCallbacks.size()]);        
        if (callbacks.length == 0) {
            this.state.set(NOTIFYINGCOMPLETE);
            return;
        }
        try {
            for (int i = callbacks.length - 1; i >= 0; i--) {
                this.executingCallback = callbacks[i];
                if (this.executingCallback != null)
                    this.executingCallback.run();
            }
        } finally {
            this.state.set(NOTIFYINGCOMPLETE);
            this.executingCallback = null;
        }
    }
        
    private boolean canBeCanceled() {
        return this.state.get() != ALLREADYCANCELED;        
    }

    private void waitForCallbackToComplete(final Action0 callback) {
        SpinWait spinner = new SpinWait();
        while (this.executingCallback == callback)
            spinner.spinOnce();
    }
    
    private final static class Token implements CancellationToken {
        private final CancellationTokenSource source;

        public Token(CancellationTokenSource source) {
            this.source = source;
        }

        @Override
        public boolean isCancellationRequested() {
            return this.source != null && this.source.isCancellationRequested();
        }

        @Override
        public boolean canBeCanceled() {
            return this.source != null && this.source.canBeCanceled();
        }
        
        @Override
        public CancellationTokenRegistration register(Action0 callback) {
            if (this.source == null)
                throw new IllegalStateException("source cannot be null.");
            return this.source.register(callback);
        }

        @Override
        public CancellationTokenRegistration registerInterrupt() {
            if (this.source == null)
                throw new IllegalStateException("source cannot be null.");
            final Thread self = Thread.currentThread();
            return this.source.register(() -> self.interrupt());
        }
    }

    private final static class TokenRegistration
                implements CancellationTokenRegistration {
        private final CancellationTokenSource source;        
        private final Action0 callback;
        private final List<Action0> registrationList;

        public TokenRegistration(CancellationTokenSource source,
                Action0 callback, List<Action0> registrationList) {
            this.source = source;
            this.callback = callback;
            this.registrationList = registrationList;
        }

        @Override
        public void dispose() {
            boolean deregisterOccured = tryDeregister();
            Action0 callback = this.callback;
            if (callback != null) {
                if (this.source.isCancellationRequested() &&
                        !this.source.isCancellationCompleted() &&
                        !deregisterOccured &&
                        this.source.threadIDExecutingCallbacks != 
                                Thread.currentThread().getId())
                this.source.waitForCallbackToComplete(this.callback);
            }
        }
        
        private boolean tryDeregister() {
            if (this.source == null)
                return false;
            return this.registrationList.remove(this.callback);            
        }
    }
}
