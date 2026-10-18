package com.fleettrack.tracking.eta;

import java.time.Duration;
import java.time.Instant;

import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.delivery.Delivery;
import com.fleettrack.delivery.DeliveryStatus;
import com.fleettrack.events.LocationUpdatedEvent;

import org.springframework.stereotype.Component;

/**
 * Straight-line distance / speed. Deliberately simple and isolated behind one method so a
 * routing-engine-based estimate (OSRM, Google Routes) could replace it without touching
 * the consumer.
 */
@Component
public class EtaCalculator {

    /** Below this the courier is probably stopped at a light; fall back to the average speed. */
    private static final double MIN_TRUSTED_SPEED_KMH = 5.0;

    private final double averageSpeedKmh;

    public EtaCalculator(FleettrackProperties properties) {
        this.averageSpeedKmh = properties.eta().averageSpeedKmh();
    }

    public EtaEstimate estimate(Delivery delivery, LocationUpdatedEvent position, Instant now) {
        boolean headingToPickup = delivery.getStatus() == DeliveryStatus.ASSIGNED;
        double targetLat = headingToPickup ? delivery.getPickupLatitude() : delivery.getDropoffLatitude();
        double targetLng = headingToPickup ? delivery.getPickupLongitude() : delivery.getDropoffLongitude();

        double distance = GeoMath.haversineMeters(position.latitude(), position.longitude(), targetLat, targetLng);
        double speedKmh = position.speedKmh() != null && position.speedKmh() >= MIN_TRUSTED_SPEED_KMH
                ? position.speedKmh()
                : averageSpeedKmh;

        return new EtaEstimate(
                delivery.getId(),
                position.courierId(),
                headingToPickup ? EtaEstimate.Target.PICKUP : EtaEstimate.Target.DROPOFF,
                Math.round(distance),
                now.plus(travelTime(distance, speedKmh)),
                now);
    }

    static Duration travelTime(double distanceMeters, double speedKmh) {
        double metersPerSecond = speedKmh * 1000.0 / 3600.0;
        return Duration.ofSeconds(Math.round(distanceMeters / metersPerSecond));
    }
}
