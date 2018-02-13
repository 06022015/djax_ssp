package com.ssp.api.exception;

public class InvalidTypeException extends SSPException {

    public InvalidTypeException(String message) {
        super(500, message);
    }


    public InvalidTypeException(int code, String message) {
        super(code, message);
    }

    public InvalidTypeException(String message, Throwable cause, int code) {
        super(message, cause, code);
    }

    public InvalidTypeException(Throwable cause, int code) {
        super(cause, code);
    }

    public InvalidTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, int code) {
        super(message, cause, enableSuppression, writableStackTrace, code);
    }

    public InvalidTypeException(Throwable cause, String message) {
        super(cause, message);
    }
}
