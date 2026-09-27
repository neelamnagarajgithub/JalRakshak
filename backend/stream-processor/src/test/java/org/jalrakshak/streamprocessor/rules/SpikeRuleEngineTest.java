package org.jalrakshak.streamprocessor.rules;

import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers SENSOR_SPIKE: |current - median(baseline)| >= 1.0m, requiring at
 * least {@code spikeWarmupReadings} (demo default: 3) prior baseline
 * readings before a verdict is produced at all.
 */
class SpikeRuleEngineTest {

    private final RuleConfig config = RuleConfig.fromEnvironment();

    private MeasurementEvent reading(double waterLevelM) {
        return MeasurementEvent.builder()
                .eventId("evt-test")
                .stationId(UUID.randomUUID())
                .observedAt(Instant.parse("2026-09-26T10:00:00Z"))
                .waterLevelM(waterLevelM)
                .schemaVersion(1)
                .build();
    }

    @Test
    void noVerdictDuringWarmUpEvenIfDeltaIsHuge() {
        // Only 2 baseline readings, warm-up requires 3.
        Optional<AlertCandidate> result = SpikeRuleEngine.evaluate(List.of(2.0, 2.1), reading(10.0), config);
        assertTrue(result.isEmpty());
    }

    @Test
    void noSpikeWhenWithinThreshold() {
        Optional<AlertCandidate> result = SpikeRuleEngine.evaluate(List.of(2.0, 2.1, 1.9, 2.05), reading(2.5), config);
        assertTrue(result.isEmpty(), "0.5m below the 1.0m threshold from median ~2.0 should not fire");
    }

    @Test
    void spikeFiresWhenDeltaMeetsThreshold() {
        // median of [2.0, 2.1, 1.9, 2.05] is 2.025; current 3.1 -> delta ~1.075 >= 1.0
        Optional<AlertCandidate> result = SpikeRuleEngine.evaluate(List.of(2.0, 2.1, 1.9, 2.05), reading(3.1), config);
        assertTrue(result.isPresent());
        assertEquals(SpikeRuleEngine.SENSOR_SPIKE, result.get().getRuleCode());
    }

    @Test
    void spikeFiresOnSuddenDropToo() {
        Optional<AlertCandidate> result = SpikeRuleEngine.evaluate(List.of(3.0, 3.1, 2.9), reading(1.5), config);
        assertTrue(result.isPresent(), "a sudden drop should trigger the spike rule same as a sudden rise");
    }

    @Test
    void nullWaterLevelNeverFires() {
        MeasurementEvent noLevel = MeasurementEvent.builder()
                .eventId("evt-null").stationId(UUID.randomUUID())
                .observedAt(Instant.now()).schemaVersion(1).build();
        assertTrue(SpikeRuleEngine.evaluate(List.of(2.0, 2.1, 2.2), noLevel, config).isEmpty());
    }

    @Test
    void medianHandlesEvenAndOddCounts() {
        assertEquals(2.0, SpikeRuleEngine.median(List.of(1.0, 2.0, 3.0)));
        assertEquals(2.5, SpikeRuleEngine.median(List.of(1.0, 2.0, 3.0, 4.0)));
    }
}
