package com.fleettrack.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fleettrack.common.exception.BadRequestException;
import com.fleettrack.common.exception.ConflictException;
import com.fleettrack.common.exception.InvalidStateTransitionException;
import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.courier.Courier;
import com.fleettrack.courier.CourierRepository;
import com.fleettrack.courier.CourierStatus;
import com.fleettrack.delivery.dto.AssignDeliveryRequest;
import com.fleettrack.delivery.dto.DeliveryResponse;
import com.fleettrack.events.DeliveryStatusChangedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private DeliveryStatusChangeRepository changeRepository;
    @Mock
    private CourierRepository courierRepository;
    @Mock
    private ApplicationEventPublisher events;

    private DeliveryService service;
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        FleettrackProperties properties = new FleettrackProperties(
                new FleettrackProperties.Kafka(new FleettrackProperties.Topics("locations", "deliveries"), 1, 1),
                new FleettrackProperties.Assignment(5000),
                new FleettrackProperties.Eta(25, 15),
                new FleettrackProperties.Notifications(300));
        service = new DeliveryService(deliveryRepository, changeRepository, courierRepository, events,
                properties, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void autoAssignPicksNearestCourierWithinDefaultRadius() {
        Delivery delivery = DeliveryTest.newDelivery();
        Courier courier = DeliveryTest.availableCourier();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(courierRepository.lockNearestAvailable(40.71, -74.00, 5000)).thenReturn(Optional.of(courier));
        when(deliveryRepository.saveAndFlush(delivery)).thenReturn(delivery);

        DeliveryResponse response = service.assign(1L, null);

        assertThat(response.status()).isEqualTo(DeliveryStatus.ASSIGNED);
        assertThat(courier.getStatus()).isEqualTo(CourierStatus.BUSY);
        assertThat(delivery.getAssignedAt()).isEqualTo(now);

        ArgumentCaptor<DeliveryStatusChangedEvent> event = ArgumentCaptor.forClass(DeliveryStatusChangedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().fromStatus()).isEqualTo(DeliveryStatus.CREATED);
        assertThat(event.getValue().toStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        verify(changeRepository).save(any(DeliveryStatusChange.class));
    }

    @Test
    void autoAssignWithNoCourierInRangeIsConflictAndChangesNothing() {
        Delivery delivery = DeliveryTest.newDelivery();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(courierRepository.lockNearestAvailable(anyDouble(), anyDouble(), eq(800.0))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(1L, new AssignDeliveryRequest(null, 800.0)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("800 m");
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CREATED);
        verify(deliveryRepository, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void assigningAnAlreadyAssignedDeliveryFailsBeforeLockingAnyCourier() {
        Delivery delivery = DeliveryTest.newDelivery();
        delivery.assignTo(DeliveryTest.availableCourier(), now);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.assign(1L, null))
                .isInstanceOf(InvalidStateTransitionException.class);
        verifyNoInteractions(courierRepository);
    }

    @Test
    void busyCourierCannotBeAssignedExplicitly() {
        Delivery delivery = DeliveryTest.newDelivery();
        Courier courier = DeliveryTest.availableCourier();
        courier.markBusy();
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(courierRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(courier));

        assertThatThrownBy(() -> service.assign(1L, new AssignDeliveryRequest(7L, null)))
                .isInstanceOf(ConflictException.class);
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.CREATED);
    }

    @Test
    void deliveringReleasesTheCourier() {
        Delivery delivery = DeliveryTest.newDelivery();
        Courier courier = DeliveryTest.availableCourier();
        courier.markBusy();
        delivery.assignTo(courier, now);
        delivery.transitionTo(DeliveryStatus.PICKED_UP, now);
        delivery.transitionTo(DeliveryStatus.IN_TRANSIT, now);
        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.saveAndFlush(delivery)).thenReturn(delivery);

        service.transition(1L, DeliveryStatus.DELIVERED);

        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(courier.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
    }

    @Test
    void assignedCannotBeSetThroughTheGenericTransitionEndpoint() {
        assertThatThrownBy(() -> service.transition(1L, DeliveryStatus.ASSIGNED))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(deliveryRepository);
    }
}
