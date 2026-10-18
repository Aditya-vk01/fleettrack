package com.fleettrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the {@code fleettrack.*} block in application.yaml.
 */
@ConfigurationProperties(prefix = "fleettrack")
public record FleettrackProperties(
        Kafka kafka,
        Assignment assignment,
        Eta eta,
        Notifications notifications) {

    public record Kafka(Topics topics, int partitions, int replicas) {
    }

    public record Topics(String courierLocations, String deliveryEvents) {
    }

    public record Assignment(double defaultRadiusMeters) {
    }

    public record Eta(double averageSpeedKmh, long ttlMinutes) {
    }

    public record Notifications(double arrivingSoonMeters) {
    }
}
