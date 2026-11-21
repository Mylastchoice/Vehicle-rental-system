package com.group01.exception;

/** The request conflicts with the current state, e.g. vehicle already booked or duplicate email (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
