package com.fleettrack.delivery.dto;

import java.time.Instant;
import java.util.Set;

import com.fleettrack.delivery.Delivery;
import com.fleettrack.delivery.DeliveryStatus;

/**
 * @param allowedTransitions what the client may do next - handy for a dispatcher UI
 */
public record DeliveryResponse(
        Long id,
        DeliveryStatus status,
        Set<DeliveryStatus> allowedTransitions,
        Long courierId,
        String recipientName,
        String recipientPhone,
        String pickupAddress,
        double pickupLatitude,
        double pickupLongitude,
        String dropoffAddress,
        double dropoffLatitude,
        double dropoffLongitude,
        Instant createdAt,
        Instant assignedAt,
        Instant pickedUpAt,
        Instant deliveredAt,
        Instant cancelledAt) {

    /** Must be called inside a transaction (courier is a lazy association). */
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
                d.getId(),
                d.getStatus(),
                d.getStatus().allowedTransitions(),
                d.getCourier() == null ? null : d.getCourier().getId(),
                d.getRecipientName(),
                d.getRecipientPhone(),
                d.getPickupAddress(),
                d.getPickupLatitude(),
                d.getPickupLongitude(),
                d.getDropoffAddress(),
                d.getDropoffLatitude(),
                d.getDropoffLongitude(),
                d.getCreatedAt(),
                d.getAssignedAt(),
                d.getPickedUpAt(),
                d.getDeliveredAt(),
                d.getCancelledAt());
    }
}
