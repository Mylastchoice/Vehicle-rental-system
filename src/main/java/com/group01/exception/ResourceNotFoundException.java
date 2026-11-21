package com.group01.exception;

/** The requested record does not exist (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String what, Object id) {
        return new ResourceNotFoundException(what + " not found: " + id);
    }
}
