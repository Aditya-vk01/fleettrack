package com.fleettrack.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.fleettrack.common.exception.InvalidStateTransitionException;
import com.fleettrack.courier.Courier;
import com.fleettrack.courier.CourierStatus;
import com.fleettrack.courier.VehicleType;

import org.junit.jupiter.api.Test;

class DeliveryTest {

    private final Instant t0 = Instant.parse("2026-01-01T10:00:00Z");

    static Delivery newDelivery() {
        return new Delivery("Ada", "+15550100", "Pickup St", 40.71, -74.00, "Dropoff Ave", 40.73, -73.99);
    }

    static Courier availableCourier() {
        Courier courier = new Courier("Bob", "+15550200", VehicleType.BICYCLE);
        courier.changeAvailability(CourierStatus.AVAILABLE);
        return courier;
    }

    @Test
    void walksTheHappyPathAndStampsTimestamps() {
        Delivery delivery = newDelivery();
        Courier courier = availableCourier();

        assertThat(delivery.assignTo(courier, t0)).isEqualTo(DeliveryStatus.CREATED);
        delivery.transitionTo(DeliveryStatus.PICKED_UP, t0.plusSeconds(60));
        delivery.transitionTo(DeliveryStatus.IN_TRANSIT, t0.plusSeconds(61));
        delivery.transitionTo(DeliveryStatus.DELIVERED, t0.plusSeconds(600));

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(delivery.getCourier()).isSameAs(courier);
        assertThat(delivery.getAssignedAt()).isEqualTo(t0);
        assertThat(delivery.getPickedUpAt()).isEqualTo(t0.plusSeconds(60));
        assertThat(delivery.getDeliveredAt()).isEqualTo(t0.plusSeconds(600));
    }

    @Test
    void rejectsSkippingStatesAndLeavesStatusUnchanged() {
        Delivery delivery = newDelivery();

        assertThatThrownBy(() -> delivery.transitionTo(DeliveryStatus.DELIVERED, t0))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("CREATED -> DELIVERED");
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CREATED);
        assertThat(delivery.getDeliveredAt()).isNull();
    }

    @Test
    void cannotAssignTwice() {
        Delivery delivery = newDelivery();
        delivery.assignTo(availableCourier(), t0);

        assertThatThrownBy(() -> delivery.assignTo(availableCourier(), t0))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
