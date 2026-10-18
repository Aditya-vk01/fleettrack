package com.fleettrack.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Positive;

/**
 * Both fields optional. With a courierId, that courier is assigned (if available).
 * Without one, the nearest AVAILABLE courier within radiusMeters of the pickup is chosen.
 */
public record AssignDeliveryRequest(
        @Positive Long courierId,
        @Positive @DecimalMax("100000") Double radiusMeters) {
}
