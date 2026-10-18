package com.fleettrack.common.exception;

/**
 * Thrown when a caller asks for a state change the delivery state machine does not allow,
 * e.g. CREATED -> DELIVERED. It is a 409 rather than a 400: the same request would be
 * perfectly valid if the delivery were in a different state.
 */
public class InvalidStateTransitionException extends ConflictException {

    public InvalidStateTransitionException(Enum<?> from, Enum<?> to) {
        super("Illegal state transition " + from + " -> " + to);
    }
}
