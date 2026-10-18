package com.fleettrack.common.exception;

/** Mapped to 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " " + id + " not found");
    }
}
