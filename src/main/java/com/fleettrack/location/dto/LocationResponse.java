package com.fleettrack.location.dto;

import java.time.Instant;

import com.fleettrack.location.CourierLocation;

public record LocationResponse(
        double latitude,
        double longitude,
        Double speedKmh,
        Double headingDegrees,
        Instant recordedAt) {

    public static LocationResponse from(CourierLocation location) {
        return new LocationResponse(location.getLatitude(), location.getLongitude(),
                location.getSpeedKmh(), location.getHeadingDegrees(), location.getRecordedAt());
    }
}
