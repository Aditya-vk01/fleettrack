package com.fleettrack.tracking.live;

import com.fleettrack.events.LocationUpdatedEvent;
import com.fleettrack.messaging.EventJson;
import com.fleettrack.realtime.LiveUpdatePublisher;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer group "fleettrack-live": forwards each position to Redis pub/sub, from where
 * every instance pushes it to its WebSocket clients. Replaces dispatchers polling the API.
 */
@Component
public class LivePositionConsumer {

    private final LiveUpdatePublisher liveUpdates;
    private final EventJson json;

    public LivePositionConsumer(LiveUpdatePublisher liveUpdates, EventJson json) {
        this.liveUpdates = liveUpdates;
        this.json = json;
    }

    @KafkaListener(topics = "${fleettrack.kafka.topics.courier-locations}", groupId = "fleettrack-live")
    public void onLocationUpdated(String payload) {
        LocationUpdatedEvent event = json.read(payload, LocationUpdatedEvent.class);
        liveUpdates.publish("/topic/couriers", event);
        liveUpdates.publish("/topic/couriers/" + event.courierId(), event);
    }
}
