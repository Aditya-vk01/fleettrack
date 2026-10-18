package com.fleettrack.messaging;

import com.fleettrack.config.FleettrackProperties;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Topics are declared in code; Spring's KafkaAdmin creates them at startup if missing.
 * Partition count caps consumer parallelism per group. Events are keyed by courier/delivery
 * id, so ordering is guaranteed per courier/delivery (not globally, which we don't need).
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic courierLocationsTopic(FleettrackProperties properties) {
        return TopicBuilder.name(properties.kafka().topics().courierLocations())
                .partitions(properties.kafka().partitions())
                .replicas(properties.kafka().replicas())
                .build();
    }

    @Bean
    public NewTopic deliveryEventsTopic(FleettrackProperties properties) {
        return TopicBuilder.name(properties.kafka().topics().deliveryEvents())
                .partitions(properties.kafka().partitions())
                .replicas(properties.kafka().replicas())
                .build();
    }

    @Bean
    public NewTopic courierLocationsDeadLetterTopic(FleettrackProperties properties) {
        return TopicBuilder.name(properties.kafka().topics().courierLocations() + KafkaErrorHandlingConfig.DLT_SUFFIX)
                .partitions(1)
                .replicas(properties.kafka().replicas())
                .build();
    }

    @Bean
    public NewTopic deliveryEventsDeadLetterTopic(FleettrackProperties properties) {
        return TopicBuilder.name(properties.kafka().topics().deliveryEvents() + KafkaErrorHandlingConfig.DLT_SUFFIX)
                .partitions(1)
                .replicas(properties.kafka().replicas())
                .build();
    }
}
