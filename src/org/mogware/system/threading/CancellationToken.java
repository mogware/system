package org.mogware.system.threading;

import org.mogware.system.delegates.Action0;

/**
* Propagates notification that operations should be canceled.
*/
public interface CancellationToken {
    /**
    * Gets whether cancellation has been requested for this token
    */
    boolean isCancellationRequested();

    /**
    * Gets whether this token is capable of being in the canceled state.
    */
    boolean canBeCanceled();

    /**
     * Registers a callback that will be called when this CancellationToken
     * is canceled.
     */
    CancellationTokenRegistration register(Action0 callback);

    /**
     * Interrupt the current thread when this CancellationToken is canceled.
     */
    CancellationTokenRegistration registerInterrupt();
}
