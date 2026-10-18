package com.fleettrack.events;

import java.time.Instant;

import com.fleettrack.delivery.DeliveryStatus;

/**
 * Published to the delivery-events topic on every delivery state change, keyed by deliveryId.
 *
 * @param fromStatus null when the delivery was just created
 */
public record DeliveryStatusChangedEvent(
        Long deliveryId,
        Long courierId,
        DeliveryStatus fromStatus,
        DeliveryStatus toStatus,
        Instant changedAt,
        Instant deliveryCreatedAt) {
}
