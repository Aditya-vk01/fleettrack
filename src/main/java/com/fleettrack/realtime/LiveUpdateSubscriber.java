package com.fleettrack.realtime;

import java.nio.charset.StandardCharsets;

import com.fleettrack.messaging.EventJson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Runs on every instance: Redis channel -> this instance's WebSocket subscribers. */
@Component
public class LiveUpdateSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdateSubscriber.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final EventJson json;

    public LiveUpdateSubscriber(SimpMessagingTemplate messagingTemplate, EventJson json) {
        this.messagingTemplate = messagingTemplate;
        this.json = json;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            LiveUpdate update = json.read(new String(message.getBody(), StandardCharsets.UTF_8), LiveUpdate.class);
            messagingTemplate.convertAndSend(update.destination(), update.payload());
        } catch (RuntimeException e) {
            log.warn("Dropping malformed live update: {}", e.getMessage());
        }
    }
}
