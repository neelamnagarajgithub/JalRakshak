package org.jalrakshak.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * A single, explicit policy for what happens when the alert-persistence
 * consumer fails to process a record (malformed JSON via
 * {@code ErrorHandlingDeserializer}, or an exception in
 * {@code AlertEventConsumer}): retry a small, bounded number of times with a
 * short delay, then log the failure and move on (skip the record) rather
 * than retrying forever and blocking the partition, or crashing the
 * container. This is a deliberately simple, documented default -- see
 * docs/architecture.md "Alert persistence and redelivery".
 */
@Configuration
public class KafkaConsumerErrorHandlingConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerErrorHandlingConfig.class);

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        // 3 retries, 1 second apart, then give up on that record and continue.
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> logger.error(
                        "Giving up processing Kafka record after retries: topic={} partition={} offset={} key={}",
                        record.topic(), record.partition(), record.offset(), record.key(), exception),
                new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class);
        return handler;
    }
}
