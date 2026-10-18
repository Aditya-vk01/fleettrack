package com.fleettrack.tracking.eta;

import java.time.Instant;

/**
 * @param target PICKUP while the courier is heading to the pickup, DROPOFF afterwards
 */
public record EtaEstimate(
        Long deliveryId,
        Long courierId,
        Target target,
        long distanceMeters,
        Instant estimatedArrivalAt,
        Instant calculatedAt) {

    public enum Target {
        PICKUP,
        DROPOFF
    }
}
