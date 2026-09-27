package org.jalrakshak.streamprocessor.functions;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.operators.KeyedProcessOperator;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.KeyedOneInputStreamOperatorTestHarness;
import org.jalrakshak.shared.AlertEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.jalrakshak.streamprocessor.rules.AlertCandidate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link AlertSuppressionFunction} through Flink's keyed-state test
 * harness (flink-test-utils / flink-streaming-java test-jar, both already
 * declared as test dependencies in this module's pom.xml), rather than
 * calling the class directly, since its behavior depends on
 * {@code getRuntimeContext().getState(...)} being backed by a real keyed
 * state backend.
 *
 * <p>Key under test: {@code stationId + "|" + ruleCode}, matching how
 * {@code StreamProcessorJob} keys the candidate stream before this function
 * (see docs/architecture.md "Repeated alert suppression").</p>
 */
class AlertSuppressionFunctionTest {

    private final RuleConfig config = RuleConfig.fromEnvironment(); // suppressionWindow default: 60 minutes
    private final UUID stationId = UUID.randomUUID();
    private KeyedOneInputStreamOperatorTestHarness<String, AlertCandidate, AlertEvent> harness;

    private static final KeySelector<AlertCandidate, String> KEY_SELECTOR =
            candidate -> candidate.getStationId() + "|" + candidate.getRuleCode();

    @BeforeEach
    void setUp() throws Exception {
        harness = new KeyedOneInputStreamOperatorTestHarness<>(
                new KeyedProcessOperator<>(new AlertSuppressionFunction(config)),
                KEY_SELECTOR,
                TypeInformation.of(String.class));
        harness.open();
    }

    @AfterEach
    void tearDown() throws Exception {
        harness.close();
    }

    private AlertCandidate candidate(String ruleCode, Instant eventTime) {
        return AlertCandidate.builder()
                .stationId(stationId)
                .ruleCode(ruleCode)
                .severity("HIGH")
                .title("Water level high")
                .reason("test")
                .eventTime(eventTime)
                .build();
    }

    private List<AlertEvent> collect() {
        return new java.util.ArrayList<>(harness.extractOutputValues());
    }

    @Test
    void firstOccurrenceGetsANewIdAndRepeatCountOne() throws Exception {
        Instant t0 = Instant.parse("2026-09-26T10:00:00Z");
        harness.processElement(new StreamRecord<>(candidate("LEVEL_HIGH", t0), t0.toEpochMilli()));

        List<AlertEvent> events = collect();
        assertEquals(1, events.size());
        assertEquals(1, events.get(0).getRepeatCount());
        assertNotNull(events.get(0).getId());
        assertEquals("ACTIVE", events.get(0).getStatus());
    }

    @Test
    void secondOccurrenceWithinSuppressionWindowReusesIdAndIncrementsCount() throws Exception {
        Instant t0 = Instant.parse("2026-09-26T10:00:00Z");
        Instant t1 = t0.plusSeconds(600); // 10 minutes later, well within the 60-minute default window

        harness.processElement(new StreamRecord<>(candidate("LEVEL_HIGH", t0), t0.toEpochMilli()));
        harness.processElement(new StreamRecord<>(candidate("LEVEL_HIGH", t1), t1.toEpochMilli()));

        List<AlertEvent> events = collect();
        assertEquals(2, events.size());
        assertEquals(events.get(0).getId(), events.get(1).getId(), "a repeat occurrence must reuse the same alert id");
        assertEquals(1, events.get(0).getRepeatCount());
        assertEquals(2, events.get(1).getRepeatCount());
        assertEquals(t1, events.get(1).getLastOccurredAt());
    }

    @Test
    void occurrenceAfterSuppressionWindowGetsANewIdAndResetsCount() throws Exception {
        Instant t0 = Instant.parse("2026-09-26T10:00:00Z");
        Instant t1 = t0.plus(java.time.Duration.ofMinutes(61)); // past the 60-minute default window

        harness.processElement(new StreamRecord<>(candidate("LEVEL_HIGH", t0), t0.toEpochMilli()));
        harness.processElement(new StreamRecord<>(candidate("LEVEL_HIGH", t1), t1.toEpochMilli()));

        List<AlertEvent> events = collect();
        assertEquals(2, events.size());
        assertNotEquals(events.get(0).getId(), events.get(1).getId(),
                "once the condition re-fires after the suppression window has elapsed, it should be treated as re-armed");
        assertEquals(1, events.get(1).getRepeatCount());
    }

    @Test
    void differentRuleCodesForSameStationAreIndependentSuppressionKeys() throws Exception {
        Instant t0 = Instant.parse("2026-09-26T10:00:00Z");

        harness.processElement(new StreamRecord<>(candidate("LEVEL_HIGH", t0), t0.toEpochMilli()));
        harness.processElement(new StreamRecord<>(candidate("RAIN_INTENSE", t0), t0.toEpochMilli()));

        List<AlertEvent> events = collect();
        assertEquals(2, events.size());
        assertNotEquals(events.get(0).getId(), events.get(1).getId());
        assertEquals(1, events.get(0).getRepeatCount());
        assertEquals(1, events.get(1).getRepeatCount());
    }
}
