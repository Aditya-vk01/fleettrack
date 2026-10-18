package com.fleettrack.messaging;

import com.fleettrack.config.FleettrackProperties;
import com.fleettrack.events.DeliveryStatusChangedEvent;
import com.fleettrack.events.LocationUpdatedEvent;

import io.micrometer.core.instrument.MeterRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges in-process domain events to Kafka, AFTER the database transaction commits.
 *
 * <p>Publishing after commit means consumers never act on data that was rolled back. The
 * trade-off: if the process dies between commit and send, that event is lost. For location
 * pings that is acceptable (another ping follows in seconds). The production-grade fix for
 * delivery events would be a transactional outbox table - see README "Next steps".
 *
 * <p>Sends are asynchronous: a slow or unavailable broker does not hold the HTTP request
 * (bounded by max.block.ms) and never fails the already-committed write.
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    private static final String FAILURES_METRIC = "fleettrack.kafka.publish.failures";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventJson json;
    private final FleettrackProperties properties;
    private final MeterRegistry meterRegistry;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, EventJson json,
                               FleettrackProperties properties, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.json = json;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        // Register the failure counters at zero, so dashboards show "0 failures" instead of
        // "No data" until the first failure happens.
        meterRegistry.counter(FAILURES_METRIC, "topic", properties.kafka().topics().courierLocations());
        meterRegistry.counter(FAILURES_METRIC, "topic", properties.kafka().topics().deliveryEvents());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onLocationUpdated(LocationUpdatedEvent event) {
        send(properties.kafka().topics().courierLocations(), String.valueOf(event.courierId()), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        send(properties.kafka().topics().deliveryEvents(), String.valueOf(event.deliveryId()), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, json.write(event)).whenComplete((result, error) -> {
                if (error != null) {
                    recordFailure(topic, error);
                }
            });
        } catch (RuntimeException e) {
            // e.g. metadata timeout when the broker is down: the DB write already succeeded.
            recordFailure(topic, e);
        }
    }

    private void recordFailure(String topic, Throwable error) {
        meterRegistry.counter(FAILURES_METRIC, "topic", topic).increment();
        log.warn("Failed to publish to {}: {}", topic, error.getMessage());
    }
}
