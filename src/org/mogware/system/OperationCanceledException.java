package org.mogware.system;

public class OperationCanceledException extends RuntimeException {

    public OperationCanceledException() {
    }

    public OperationCanceledException(String message) {
        super(message);
    }

    public OperationCanceledException(String message, Exception innerException) {
        super(message, innerException);
    }
}    

