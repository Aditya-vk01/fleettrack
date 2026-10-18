package com.fleettrack.courier.dto;

import com.fleettrack.courier.NearbyCourierView;

public record NearbyCourierResponse(
        Long id,
        String name,
        String vehicleType,
        Double latitude,
        Double longitude,
        long distanceMeters) {

    public static NearbyCourierResponse from(NearbyCourierView view) {
        return new NearbyCourierResponse(
                view.getId(),
                view.getName(),
                view.getVehicleType(),
                view.getLatitude(),
                view.getLongitude(),
                Math.round(view.getDistanceMeters()));
    }
}
