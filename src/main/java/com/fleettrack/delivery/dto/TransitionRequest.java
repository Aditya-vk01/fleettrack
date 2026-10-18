package com.fleettrack.delivery.dto;

import com.fleettrack.delivery.DeliveryStatus;

import jakarta.validation.constraints.NotNull;

public record TransitionRequest(@NotNull DeliveryStatus status) {
}
