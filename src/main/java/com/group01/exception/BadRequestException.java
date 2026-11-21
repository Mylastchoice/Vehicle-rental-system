package com.group01.exception;

/** The request is syntactically valid but a value is not acceptable (HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
