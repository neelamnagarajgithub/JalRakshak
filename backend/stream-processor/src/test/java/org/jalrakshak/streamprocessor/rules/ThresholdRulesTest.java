package org.jalrakshak.streamprocessor.rules;

import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the three stateless threshold rules against the provisional demo
 * defaults (docs/architecture.md): LEVEL_HIGH >= 4.0m, RAIN_INTENSE >=
 * 20mm/h, LEVEL_AND_RAIN (level >= 3.0m AND rain >= 15mm/h).
 * {@link RuleConfig#fromEnvironment()} is used directly since its defaults
 * (when no env vars are set) are exactly these demo values, avoiding a
 * separate test-only config constructor.
 */
class ThresholdRulesTest {

    private final RuleConfig config = RuleConfig.fromEnvironment();

    private MeasurementEvent event(Double waterLevelM, Double rainfallMmPerHour) {
        return MeasurementEvent.builder()
                .eventId("evt-test")
                .stationId(UUID.randomUUID())
                .observedAt(Instant.parse("2026-09-26T10:00:00Z"))
                .waterLevelM(waterLevelM)
                .rainfallMmPerHour(rainfallMmPerHour)
                .schemaVersion(1)
                .build();
    }

    @Test
    void noRulesFireForNormalReading() {
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(1.5, 2.0), config);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void levelHighFiresAtExactlyTheThreshold() {
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(4.0, 0.0), config);
        assertEquals(1, candidates.size());
        assertEquals(ThresholdRules.LEVEL_HIGH, candidates.get(0).getRuleCode());
    }

    @Test
    void levelHighDoesNotFireJustBelowThreshold() {
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(3.99, 0.0), config);
        assertTrue(candidates.stream().noneMatch(c -> c.getRuleCode().equals(ThresholdRules.LEVEL_HIGH)));
    }

    @Test
    void rainIntenseFiresAtThreshold() {
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(1.0, 20.0), config);
        assertEquals(1, candidates.size());
        assertEquals(ThresholdRules.RAIN_INTENSE, candidates.get(0).getRuleCode());
    }

    @Test
    void levelAndRainRequiresBothConditions() {
        // level alone (>= 3.0) but rain below its own threshold (< 15.0): only... nothing, since
        // 3.0 is below LEVEL_HIGH's 4.0 and rain 5.0 is below RAIN_INTENSE's 20.0 and LEVEL_AND_RAIN's 15.0.
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(3.0, 5.0), config);
        assertTrue(candidates.isEmpty());
    }

    @Test
    void levelAndRainFiresWhenBothThresholdsMet() {
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(3.0, 15.0), config);
        assertTrue(candidates.stream().anyMatch(c -> c.getRuleCode().equals(ThresholdRules.LEVEL_AND_RAIN)));
    }

    @Test
    void multipleRulesCanFireForOneReading() {
        // 5.0m >= LEVEL_HIGH(4.0) and >= LEVEL_AND_RAIN level(3.0); 25.0mm/h >= RAIN_INTENSE(20.0) and >= LEVEL_AND_RAIN rain(15.0)
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(5.0, 25.0), config);
        assertEquals(3, candidates.size());
    }

    @Test
    void nullFieldsAreSkippedWithoutError() {
        List<AlertCandidate> candidates = ThresholdRules.evaluate(event(null, null), config);
        assertTrue(candidates.isEmpty());
    }
}
