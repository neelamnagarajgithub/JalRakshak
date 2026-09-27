package org.jalrakshak.shared.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory for a single, consistently-configured Jackson {@link ObjectMapper}
 * used to (de)serialize {@link org.jalrakshak.shared.MeasurementEvent} and
 * {@link org.jalrakshak.shared.AlertEvent} on Kafka.
 *
 * <p>The API service (Spring Boot) uses Spring Kafka's {@code JsonSerializer},
 * which is separately configured with Spring Boot's auto-configured
 * {@code ObjectMapper} (JavaTimeModule included by default). The
 * stream-processor module is plain Java (no Spring context), so it uses this
 * factory directly. Both sides agree on: ISO-8601 instants (not epoch
 * arrays), and tolerance of unknown JSON properties so the two modules can
 * evolve independently without hard failures.</p>
 */
public final class EventObjectMapper {

    private EventObjectMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
