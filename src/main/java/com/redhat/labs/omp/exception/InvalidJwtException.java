package com.redhat.labs.omp.exception;

public class InvalidJwtException extends RuntimeException {

    private static final long serialVersionUID = -242025978016304827L;

    public InvalidJwtException() {
        super();
    }

    public InvalidJwtException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidJwtException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidJwtException(String message) {
        super(message);
    }

    public InvalidJwtException(Throwable cause) {
        super(cause);
    }

}
