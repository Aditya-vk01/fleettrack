package com.fleettrack.tracking.eta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;

import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.courier.Courier;
import com.fleettrack.courier.CourierStatus;
import com.fleettrack.courier.VehicleType;
import com.fleettrack.delivery.Delivery;
import com.fleettrack.delivery.DeliveryStatus;
import com.fleettrack.events.LocationUpdatedEvent;

import org.junit.jupiter.api.Test;

class EtaCalculatorTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final EtaCalculator calculator = new EtaCalculator(new FleettrackProperties(
            null, null, new FleettrackProperties.Eta(30, 15), null));

    @Test
    void haversineMatchesKnownDistance() {
        // Times Square -> Empire State Building is roughly 1.1 km
        double meters = GeoMath.haversineMeters(40.7580, -73.9855, 40.7484, -73.9857);
        assertThat(meters).isCloseTo(1068, within(15.0));
    }

    @Test
    void travelTimeIsDistanceOverSpeed() {
        assertThat(EtaCalculator.travelTime(3000, 30)).isEqualTo(Duration.ofMinutes(6));
    }

    @Test
    void targetsPickupWhileAssignedAndDropoffAfterPickup() {
        Delivery delivery = new Delivery("Ada", "+15550100", null, 40.7484, -73.9857, null, 40.7580, -73.9855);
        Courier courier = new Courier("Bob", "+15550200", VehicleType.CAR);
        courier.changeAvailability(CourierStatus.AVAILABLE);
        delivery.assignTo(courier, now);

        LocationUpdatedEvent atTimesSquare = new LocationUpdatedEvent(1L, 40.7580, -73.9855, null, null, now, now);

        EtaEstimate toPickup = calculator.estimate(delivery, atTimesSquare, now);
        assertThat(toPickup.target()).isEqualTo(EtaEstimate.Target.PICKUP);
        assertThat(toPickup.distanceMeters()).isBetween(1050L, 1085L);
        // ~1068 m at the 30 km/h default -> about 2 minutes
        assertThat(Duration.between(now, toPickup.estimatedArrivalAt())).isBetween(Duration.ofSeconds(120), Duration.ofSeconds(135));

        delivery.transitionTo(DeliveryStatus.PICKED_UP, now);
        EtaEstimate toDropoff = calculator.estimate(delivery, atTimesSquare, now);
        assertThat(toDropoff.target()).isEqualTo(EtaEstimate.Target.DROPOFF);
        assertThat(toDropoff.distanceMeters()).isLessThan(5);
    }

    @Test
    void usesReportedSpeedWhenCourierIsActuallyMoving() {
        Delivery delivery = new Delivery("Ada", "+15550100", null, 40.0, -74.0, null, 40.0, -74.0);
        Courier courier = new Courier("Bob", "+15550200", VehicleType.CAR);
        courier.changeAvailability(CourierStatus.AVAILABLE);
        delivery.assignTo(courier, now);

        // ~11.1 km north of the pickup
        LocationUpdatedEvent fast = new LocationUpdatedEvent(1L, 40.1, -74.0, 60.0, 0.0, now, now);
        EtaEstimate estimate = calculator.estimate(delivery, fast, now);

        // 11.1 km at 60 km/h is about 11 minutes, not the ~22 the 30 km/h default would give
        assertThat(Duration.between(now, estimate.estimatedArrivalAt()).toMinutes()).isBetween(10L, 12L);
    }
}
