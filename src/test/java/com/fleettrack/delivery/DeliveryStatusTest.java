package com.fleettrack.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeliveryStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed")
    @CsvSource({
            "CREATED, ASSIGNED",
            "CREATED, CANCELLED",
            "ASSIGNED, PICKED_UP",
            "ASSIGNED, CANCELLED",
            "PICKED_UP, IN_TRANSIT",
            "IN_TRANSIT, DELIVERED"
    })
    void allowsHappyPathAndEarlyCancel(DeliveryStatus from, DeliveryStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} rejected")
    @CsvSource({
            "CREATED, DELIVERED",     // skipping steps
            "CREATED, PICKED_UP",
            "ASSIGNED, DELIVERED",
            "PICKED_UP, ASSIGNED",    // going backwards
            "IN_TRANSIT, PICKED_UP",
            "PICKED_UP, CANCELLED",   // too late to cancel
            "IN_TRANSIT, CANCELLED",
            "DELIVERED, CANCELLED",   // terminal states
            "CANCELLED, ASSIGNED",
            "ASSIGNED, ASSIGNED"      // self-transition
    })
    void rejectsIllegalTransitions(DeliveryStatus from, DeliveryStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void terminalAndActiveStates() {
        assertThat(DeliveryStatus.DELIVERED.isTerminal()).isTrue();
        assertThat(DeliveryStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(DeliveryStatus.IN_TRANSIT.isTerminal()).isFalse();
        assertThat(DeliveryStatus.ACTIVE).containsExactlyInAnyOrder(
                DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT);
    }
}
