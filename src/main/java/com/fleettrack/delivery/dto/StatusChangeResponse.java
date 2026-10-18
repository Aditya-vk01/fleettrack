package com.fleettrack.delivery.dto;

import java.time.Instant;

import com.fleettrack.delivery.DeliveryStatus;
import com.fleettrack.delivery.DeliveryStatusChange;

public record StatusChangeResponse(DeliveryStatus fromStatus, DeliveryStatus toStatus, Long courierId, Instant changedAt) {

    public static StatusChangeResponse from(DeliveryStatusChange change) {
        return new StatusChangeResponse(change.getFromStatus(), change.getToStatus(),
                change.getCourierId(), change.getChangedAt());
    }
}
