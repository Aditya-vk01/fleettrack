package com.fleettrack.delivery;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.fleettrack.common.exception.BadRequestException;
import com.fleettrack.common.exception.ConflictException;
import com.fleettrack.common.exception.InvalidStateTransitionException;
import com.fleettrack.common.exception.ResourceNotFoundException;
import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.courier.Courier;
import com.fleettrack.courier.CourierRepository;
import com.fleettrack.delivery.dto.AssignDeliveryRequest;
import com.fleettrack.delivery.dto.CreateDeliveryRequest;
import com.fleettrack.delivery.dto.DeliveryResponse;
import com.fleettrack.delivery.dto.StatusChangeResponse;
import com.fleettrack.events.DeliveryStatusChangedEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryStatusChangeRepository changeRepository;
    private final CourierRepository courierRepository;
    private final ApplicationEventPublisher events;
    private final FleettrackProperties properties;
    private final Clock clock;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           DeliveryStatusChangeRepository changeRepository,
                           CourierRepository courierRepository,
                           ApplicationEventPublisher events,
                           FleettrackProperties properties,
                           Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.changeRepository = changeRepository;
        this.courierRepository = courierRepository;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public DeliveryResponse create(CreateDeliveryRequest request) {
        Delivery delivery = deliveryRepository.saveAndFlush(new Delivery(
                request.recipientName().trim(), request.recipientPhone(),
                request.pickupAddress(), request.pickupLatitude(), request.pickupLongitude(),
                request.dropoffAddress(), request.dropoffLatitude(), request.dropoffLongitude()));
        recordChange(delivery, null, clock.instant());
        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse get(Long id) {
        return DeliveryResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> list(DeliveryStatus status) {
        List<Delivery> deliveries = status == null
                ? deliveryRepository.findTop100ByOrderByIdDesc()
                : deliveryRepository.findByStatusOrderByIdDesc(status);
        return deliveries.stream().map(DeliveryResponse::from).toList();
    }

    /**
     * Assigns a courier: either the one requested, or the nearest available courier to the
     * pickup point. Concurrency is handled at three levels:
     * <ol>
     *   <li>the chosen courier row is locked (FOR UPDATE / FOR UPDATE SKIP LOCKED), so two
     *       assignments can't grab the same courier;</li>
     *   <li>the delivery's @Version rejects a concurrent change to the same delivery (409);</li>
     *   <li>a partial unique index in the database forbids two active deliveries per courier.</li>
     * </ol>
     */
    @Transactional
    public DeliveryResponse assign(Long deliveryId, AssignDeliveryRequest request) {
        Delivery delivery = findOrThrow(deliveryId);
        // Fail fast before taking any courier lock.
        if (!delivery.getStatus().canTransitionTo(DeliveryStatus.ASSIGNED)) {
            throw new InvalidStateTransitionException(delivery.getStatus(), DeliveryStatus.ASSIGNED);
        }

        Courier courier;
        if (request != null && request.courierId() != null) {
            courier = courierRepository.findByIdForUpdate(request.courierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Courier", request.courierId()));
        } else {
            double radius = request != null && request.radiusMeters() != null
                    ? request.radiusMeters()
                    : properties.assignment().defaultRadiusMeters();
            courier = courierRepository.lockNearestAvailable(
                            delivery.getPickupLatitude(), delivery.getPickupLongitude(), radius)
                    .orElseThrow(() -> new ConflictException(
                            "No available courier within " + Math.round(radius) + " m of the pickup point"));
        }

        courier.markBusy();
        Instant now = clock.instant();
        DeliveryStatus previous = delivery.assignTo(courier, now);
        deliveryRepository.saveAndFlush(delivery);
        recordChange(delivery, previous, now);
        return DeliveryResponse.from(delivery);
    }

    /** PICKED_UP, IN_TRANSIT, DELIVERED or CANCELLED. Assignment has its own endpoint. */
    @Transactional
    public DeliveryResponse transition(Long deliveryId, DeliveryStatus target) {
        if (target == DeliveryStatus.ASSIGNED) {
            throw new BadRequestException("Use POST /api/deliveries/{id}/assign to assign a courier");
        }
        if (target == DeliveryStatus.CREATED) {
            throw new BadRequestException("A delivery cannot be moved back to CREATED");
        }
        Delivery delivery = findOrThrow(deliveryId);
        Instant now = clock.instant();
        DeliveryStatus previous = delivery.transitionTo(target, now);

        if (target.isTerminal() && delivery.getCourier() != null) {
            delivery.getCourier().releaseFromDelivery();
        }
        deliveryRepository.saveAndFlush(delivery);
        recordChange(delivery, previous, now);
        return DeliveryResponse.from(delivery);
    }

    @Transactional(readOnly = true)
    public List<StatusChangeResponse> history(Long deliveryId) {
        if (!deliveryRepository.existsById(deliveryId)) {
            throw new ResourceNotFoundException("Delivery", deliveryId);
        }
        return changeRepository.findByDeliveryIdOrderByChangedAtAscIdAsc(deliveryId).stream()
                .map(StatusChangeResponse::from)
                .toList();
    }

    private void recordChange(Delivery delivery, DeliveryStatus previous, Instant at) {
        Long courierId = delivery.getCourier() == null ? null : delivery.getCourier().getId();
        changeRepository.save(new DeliveryStatusChange(delivery.getId(), previous, delivery.getStatus(), courierId, at));
        // Bridged to Kafka after commit by KafkaEventPublisher.
        events.publishEvent(new DeliveryStatusChangedEvent(delivery.getId(), courierId, previous,
                delivery.getStatus(), at, delivery.getCreatedAt()));
    }

    private Delivery findOrThrow(Long id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", id));
    }
}
