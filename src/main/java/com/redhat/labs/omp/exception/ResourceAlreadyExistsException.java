package com.redhat.labs.omp.exception;

public class ResourceAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 4178292547051117620L;

    public ResourceAlreadyExistsException() {
        super();
    }

    public ResourceAlreadyExistsException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ResourceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(Throwable cause) {
        super(cause);
    }

}
