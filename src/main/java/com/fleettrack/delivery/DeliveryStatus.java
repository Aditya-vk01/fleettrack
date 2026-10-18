package com.fleettrack.delivery;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The delivery state machine. The happy path is
 * CREATED -> ASSIGNED -> PICKED_UP -> IN_TRANSIT -> DELIVERED; a delivery can be
 * CANCELLED only before pickup. Every status change goes through
 * {@link Delivery#transitionTo}, which checks {@link #canTransitionTo} - there is no
 * setter that writes an arbitrary status.
 */
public enum DeliveryStatus {
    CREATED,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    private static final Map<DeliveryStatus, Set<DeliveryStatus>> ALLOWED = new EnumMap<>(DeliveryStatus.class);

    static {
        ALLOWED.put(CREATED, EnumSet.of(ASSIGNED, CANCELLED));
        ALLOWED.put(ASSIGNED, EnumSet.of(PICKED_UP, CANCELLED));
        ALLOWED.put(PICKED_UP, EnumSet.of(IN_TRANSIT));
        ALLOWED.put(IN_TRANSIT, EnumSet.of(DELIVERED));
        ALLOWED.put(DELIVERED, EnumSet.noneOf(DeliveryStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(DeliveryStatus.class));
    }

    /** Statuses in which a courier is attached and working on the delivery. */
    public static final Set<DeliveryStatus> ACTIVE = Collections.unmodifiableSet(EnumSet.of(ASSIGNED, PICKED_UP, IN_TRANSIT));

    public boolean canTransitionTo(DeliveryStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public Set<DeliveryStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED.get(this));
    }

    public boolean isTerminal() {
        return ALLOWED.get(this).isEmpty();
    }

    public boolean isActive() {
        return ACTIVE.contains(this);
    }
}
