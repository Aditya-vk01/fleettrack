package com.fleettrack.common.exception;

/** The request is valid but clashes with the current state of the resource. Mapped to 409 Conflict. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
