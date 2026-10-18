package com.fleettrack.realtime;

import com.fleettrack.messaging.EventJson;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fans a live update out to EVERY application instance via Redis pub/sub.
 *
 * <p>Why not push to WebSockets straight from the Kafka consumer? A Kafka consumer group
 * delivers each record to exactly one instance, but the dispatchers watching that courier
 * may be connected to any instance. Redis pub/sub is a broadcast: all instances receive the
 * message and each pushes it to its own connected clients.
 */
@Component
public class LiveUpdatePublisher {

    public static final String CHANNEL = "fleettrack:live-updates";

    private final StringRedisTemplate redis;
    private final EventJson json;

    public LiveUpdatePublisher(StringRedisTemplate redis, EventJson json) {
        this.redis = redis;
        this.json = json;
    }

    public void publish(String destination, Object payload) {
        redis.convertAndSend(CHANNEL, json.write(new LiveUpdate(destination, json.write(payload))));
    }
}
