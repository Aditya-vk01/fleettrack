package com.fleettrack.common.exception;

/** The request itself is invalid, independent of server state. Mapped to 400 Bad Request. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
