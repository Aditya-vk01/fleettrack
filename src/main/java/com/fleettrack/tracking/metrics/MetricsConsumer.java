package com.fleettrack.tracking.metrics;

import java.time.Clock;
import java.time.Duration;

import com.fleettrack.delivery.DeliveryStatus;
import com.fleettrack.events.DeliveryStatusChangedEvent;
import com.fleettrack.events.LocationUpdatedEvent;
import com.fleettrack.messaging.EventJson;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "fleettrack-metrics": turns the event stream into Prometheus metrics.
 *
 * <p>Tags are kept low-cardinality (status, not courier id or delivery id): every distinct
 * tag value is a separate time series in Prometheus.
 */
@Component
public class MetricsConsumer {

    private final MeterRegistry registry;
    private final EventJson json;
    private final Clock clock;
    private final Counter locationUpdates;
    private final Timer pipelineLag;
    private final Timer deliveryDuration;

    public MetricsConsumer(MeterRegistry registry, EventJson json, Clock clock) {
        this.registry = registry;
        this.json = json;
        this.clock = clock;
        this.locationUpdates = Counter.builder("fleettrack.location.updates")
                .description("Location updates that went through the event pipeline")
                .register(registry);
        this.pipelineLag = Timer.builder("fleettrack.location.pipeline.lag")
                .description("Time from API accepting a ping to the metrics consumer seeing it")
                .register(registry);
        this.deliveryDuration = Timer.builder("fleettrack.delivery.duration")
                .description("Time from delivery creation to DELIVERED")
                .register(registry);
    }

    @KafkaListener(topics = "${fleettrack.kafka.topics.courier-locations}", groupId = "fleettrack-metrics")
    public void onLocationUpdated(String payload) {
        LocationUpdatedEvent event = json.read(payload, LocationUpdatedEvent.class);
        locationUpdates.increment();
        Duration lag = Duration.between(event.receivedAt(), clock.instant());
        if (!lag.isNegative()) {
            pipelineLag.record(lag);
        }
    }

    @KafkaListener(topics = "${fleettrack.kafka.topics.delivery-events}", groupId = "fleettrack-metrics")
    public void onDeliveryStatusChanged(String payload) {
        DeliveryStatusChangedEvent event = json.read(payload, DeliveryStatusChangedEvent.class);
        registry.counter("fleettrack.delivery.transitions", "status", event.toStatus().name()).increment();
        if (event.toStatus() == DeliveryStatus.DELIVERED && event.deliveryCreatedAt() != null) {
            deliveryDuration.record(Duration.between(event.deliveryCreatedAt(), event.changedAt()));
        }
    }
}
