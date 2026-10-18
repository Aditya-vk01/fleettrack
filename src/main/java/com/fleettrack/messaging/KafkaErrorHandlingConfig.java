package com.fleettrack.messaging;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import tools.jackson.core.JacksonException;

/**
 * A consumer that throws gets 3 retries (1s apart); after that the record goes to a
 * dead-letter topic ("&lt;topic&gt;.DLT") instead of blocking its partition forever.
 * Malformed JSON will never succeed, so it skips the retries.
 *
 * <p>Spring Boot plugs this bean into every @KafkaListener container automatically.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    public static final String DLT_SUFFIX = ".DLT";

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                // partition -1: let Kafka pick, so the DLT needn't have as many partitions as the source
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1));
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}
