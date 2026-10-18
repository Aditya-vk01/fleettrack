package com.fleettrack.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDeliveryRequest(
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be 7-15 digits, optionally starting with +")
        String recipientPhone,
        @Size(max = 255) String pickupAddress,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double pickupLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double pickupLongitude,
        @Size(max = 255) String dropoffAddress,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double dropoffLatitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double dropoffLongitude) {
}
