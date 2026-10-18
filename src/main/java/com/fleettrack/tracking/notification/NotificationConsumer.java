package com.fleettrack.tracking.notification;

import java.time.Duration;
import java.util.List;

import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.delivery.Delivery;
import com.fleettrack.delivery.DeliveryRepository;
import com.fleettrack.delivery.DeliveryStatus;
import com.fleettrack.events.DeliveryStatusChangedEvent;
import com.fleettrack.events.LocationUpdatedEvent;
import com.fleettrack.messaging.EventJson;
import com.fleettrack.tracking.eta.GeoMath;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "fleettrack-notifications": tells recipients about status changes and
 * sends one "arriving soon" message when the courier gets close to the dropoff.
 */
@Component
public class NotificationConsumer {

    private static final String ARRIVING_SENT_PREFIX = "fleettrack:notified:arriving:";

    private final DeliveryRepository deliveryRepository;
    private final NotificationSender sender;
    private final StringRedisTemplate redis;
    private final EventJson json;
    private final double arrivingSoonMeters;

    public NotificationConsumer(DeliveryRepository deliveryRepository, NotificationSender sender,
                                StringRedisTemplate redis, EventJson json, FleettrackProperties properties) {
        this.deliveryRepository = deliveryRepository;
        this.sender = sender;
        this.redis = redis;
        this.json = json;
        this.arrivingSoonMeters = properties.notifications().arrivingSoonMeters();
    }

    @KafkaListener(topics = "${fleettrack.kafka.topics.delivery-events}", groupId = "fleettrack-notifications")
    public void onDeliveryStatusChanged(String payload) {
        DeliveryStatusChangedEvent event = json.read(payload, DeliveryStatusChangedEvent.class);
        String message = switch (event.toStatus()) {
            case ASSIGNED -> "A courier has been assigned to your delivery.";
            case PICKED_UP -> "Your order has been picked up.";
            case IN_TRANSIT -> "Your order is on its way.";
            case DELIVERED -> "Your order has been delivered.";
            case CANCELLED -> "Your delivery has been cancelled.";
            case CREATED -> null;
        };
        if (message == null) {
            return;
        }
        deliveryRepository.findById(event.deliveryId()).ifPresent(delivery ->
                sender.send(new Notification(delivery.getId(), delivery.getRecipientPhone(), message)));
    }

    @KafkaListener(topics = "${fleettrack.kafka.topics.courier-locations}", groupId = "fleettrack-notifications")
    public void onLocationUpdated(String payload) {
        LocationUpdatedEvent event = json.read(payload, LocationUpdatedEvent.class);
        List<Delivery> inTransit = deliveryRepository.findByCourier_IdAndStatusIn(
                event.courierId(), List.of(DeliveryStatus.IN_TRANSIT));
        for (Delivery delivery : inTransit) {
            double distance = GeoMath.haversineMeters(event.latitude(), event.longitude(),
                    delivery.getDropoffLatitude(), delivery.getDropoffLongitude());
            if (distance <= arrivingSoonMeters && firstTime(delivery.getId())) {
                sender.send(new Notification(delivery.getId(), delivery.getRecipientPhone(),
                        "Your courier is about " + Math.round(distance) + " m away."));
            }
        }
    }

    /**
     * Kafka gives at-least-once delivery, and the courier sends many pings inside the radius.
     * SET NX makes the notification idempotent: only the first one wins, across all instances.
     */
    private boolean firstTime(Long deliveryId) {
        Boolean set = redis.opsForValue().setIfAbsent(ARRIVING_SENT_PREFIX + deliveryId, "1", Duration.ofHours(12));
        return Boolean.TRUE.equals(set);
    }
}
