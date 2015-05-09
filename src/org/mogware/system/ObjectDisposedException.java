package org.mogware.system;

public class ObjectDisposedException extends RuntimeException {

    public ObjectDisposedException() {
    }

    public ObjectDisposedException(String message) {
        super(message);
    }

    public ObjectDisposedException(String message, Exception innerException) {
        super(message, innerException);
    }
}