package com.fleettrack.courier.dto;

import com.fleettrack.courier.CourierStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(@NotNull CourierStatus status) {
}
