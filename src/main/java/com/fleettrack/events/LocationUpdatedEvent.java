package com.fleettrack.events;

import java.time.Instant;

/**
 * Published to the courier-locations topic every time a location ping is accepted.
 * Keyed by courierId, so all pings for one courier land on the same partition in order.
 *
 * @param recordedAt when the device took the reading (device clock)
 * @param receivedAt when the API accepted it (server clock) - used for pipeline-lag metrics
 */
public record LocationUpdatedEvent(
        Long courierId,
        double latitude,
        double longitude,
        Double speedKmh,
        Double headingDegrees,
        Instant recordedAt,
        Instant receivedAt) {
}
