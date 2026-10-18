package com.fleettrack.courier;

/** Projection returned by the PostGIS proximity query. */
public interface NearbyCourierView {

    Long getId();

    String getName();

    String getVehicleType();

    Double getLatitude();

    Double getLongitude();

    Double getDistanceMeters();
}
