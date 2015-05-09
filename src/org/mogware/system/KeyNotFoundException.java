package org.mogware.system;

public class KeyNotFoundException extends RuntimeException {

    public KeyNotFoundException() {
    }

    public KeyNotFoundException(String message) {
        super(message);
    }

    public KeyNotFoundException(String message, Exception innerException) {
        super(message, innerException);
    }
}
