package com.fleettrack.courier.dto;

import java.time.Instant;

import com.fleettrack.courier.Courier;
import com.fleettrack.courier.CourierStatus;
import com.fleettrack.courier.VehicleType;

public record CourierResponse(
        Long id,
        String name,
        String phone,
        VehicleType vehicleType,
        CourierStatus status,
        Double lastLatitude,
        Double lastLongitude,
        Instant lastSeenAt,
        Instant createdAt) {

    public static CourierResponse from(Courier courier) {
        return new CourierResponse(
                courier.getId(),
                courier.getName(),
                courier.getPhone(),
                courier.getVehicleType(),
                courier.getStatus(),
                courier.getLastLatitude(),
                courier.getLastLongitude(),
                courier.getLastSeenAt(),
                courier.getCreatedAt());
    }
}
