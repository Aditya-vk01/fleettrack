package com.fleettrack.messaging;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

/**
 * Serializes events with the application's Jackson 3 mapper. Kafka and Redis carry plain
 * JSON strings, so producers and consumers only share a JSON contract, not Java classes
 * or type headers.
 */
@Component
public class EventJson {

    private final ObjectMapper objectMapper;

    public EventJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    public <T> T read(String json, Class<T> type) {
        return objectMapper.readValue(json, type);
    }
}
