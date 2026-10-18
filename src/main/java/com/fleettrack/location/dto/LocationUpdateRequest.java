package com.fleettrack.location.dto;

import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @param recordedAt optional device timestamp; the server time is used when absent
 */
public record LocationUpdateRequest(
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @PositiveOrZero @DecimalMax("400") Double speedKmh,
        @DecimalMin("0") @DecimalMax(value = "360", inclusive = false) Double headingDegrees,
        Instant recordedAt) {
}
