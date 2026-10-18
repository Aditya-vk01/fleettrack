package com.fleettrack.tracking.eta;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.fleettrack.delivery.Delivery;
import com.fleettrack.delivery.DeliveryRepository;
import com.fleettrack.delivery.DeliveryStatus;
import com.fleettrack.events.LocationUpdatedEvent;
import com.fleettrack.messaging.EventJson;
import com.fleettrack.realtime.LiveUpdatePublisher;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "fleettrack-eta": recomputes the ETA of every active delivery of the
 * courier who just moved, caches it, and pushes it to dispatchers.
 */
@Component
public class EtaConsumer {

    private final DeliveryRepository deliveryRepository;
    private final EtaCalculator calculator;
    private final EtaStore etaStore;
    private final LiveUpdatePublisher liveUpdates;
    private final EventJson json;
    private final Clock clock;

    public EtaConsumer(DeliveryRepository deliveryRepository, EtaCalculator calculator, EtaStore etaStore,
                       LiveUpdatePublisher liveUpdates, EventJson json, Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.calculator = calculator;
        this.etaStore = etaStore;
        this.liveUpdates = liveUpdates;
        this.json = json;
        this.clock = clock;
    }

    @KafkaListener(topics = "${fleettrack.kafka.topics.courier-locations}", groupId = "fleettrack-eta")
    public void onLocationUpdated(String payload) {
        LocationUpdatedEvent event = json.read(payload, LocationUpdatedEvent.class);
        List<Delivery> active = deliveryRepository.findByCourier_IdAndStatusIn(event.courierId(), DeliveryStatus.ACTIVE);
        Instant now = clock.instant();
        for (Delivery delivery : active) {
            EtaEstimate estimate = calculator.estimate(delivery, event, now);
            etaStore.save(estimate);
            liveUpdates.publish("/topic/deliveries/" + delivery.getId() + "/eta", estimate);
        }
    }
}
