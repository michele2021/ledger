package com.accounting.core.exceptions;

public class JobProcessingException extends Exception {

    public JobProcessingException(String message) {
        super(message);
    }

    public JobProcessingException(String message, Throwable cause) {
        super(message + cause.getMessage(), cause);
    }
}
