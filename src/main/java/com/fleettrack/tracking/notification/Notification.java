package com.fleettrack.tracking.notification;

public record Notification(Long deliveryId, String recipientPhone, String message) {
}
