package com.app.exceptions;

public class StoreException extends RuntimeException{
    private final ExceptionCode code;
    private final Throwable exception;
    private final String message;

    public StoreException(ExceptionCode code, Throwable exception, String message) {
        this.code = code;
        this.exception = exception;
        this.message = message;
    }

    public StoreException(ExceptionCode code, Throwable exception) {
        this.code = code;
        this.exception = exception;
        this.message = null;
    }


    public ExceptionCode getCode() {
        return code;
    }

    public Throwable getException() {
        return exception;
    }

    public String getMessage() {
        if(message == null || message.isBlank()) {
            return this.getException().getMessage();
        }
        return message;
    }
}
