package com.fleettrack.courier.dto;

import com.fleettrack.courier.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCourierRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "must be 7-15 digits, optionally starting with +")
        String phone,
        @NotNull VehicleType vehicleType) {
}
