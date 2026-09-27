package org.jalrakshak.streamprocessor.rules;

import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers LEVEL_RISING_FAST: rise (latest - earliest reading in the window)
 * >= 0.5m (demo default) between the window's boundary readings.
 */
class RisingLevelRuleEngineTest {

    private final RuleConfig config = RuleConfig.fromEnvironment();
    private final UUID stationId = UUID.randomUUID();
    private final Instant windowStart = Instant.parse("2026-09-26T10:00:00Z");
    private final Instant windowEnd = Instant.parse("2026-09-26T10:15:00Z");

    @Test
    void firesWhenRiseMeetsThreshold() {
        Optional<AlertCandidate> result = RisingLevelRuleEngine.evaluate(
                stationId,
                2.0, windowStart,
                2.5, windowEnd.minusSeconds(1),
                windowStart, windowEnd, config);

        assertTrue(result.isPresent());
        assertEquals(RisingLevelRuleEngine.LEVEL_RISING_FAST, result.get().getRuleCode());
    }

    @Test
    void doesNotFireWhenRiseBelowThreshold() {
        Optional<AlertCandidate> result = RisingLevelRuleEngine.evaluate(
                stationId,
                2.0, windowStart,
                2.3, windowEnd.minusSeconds(1),
                windowStart, windowEnd, config);

        assertTrue(result.isEmpty());
    }

    @Test
    void fallingLevelNeverFires() {
        Optional<AlertCandidate> result = RisingLevelRuleEngine.evaluate(
                stationId,
                3.0, windowStart,
                1.0, windowEnd.minusSeconds(1),
                windowStart, windowEnd, config);

        assertTrue(result.isEmpty());
    }

    @Test
    void firesAtExactlyTheThreshold() {
        Optional<AlertCandidate> result = RisingLevelRuleEngine.evaluate(
                stationId,
                2.0, windowStart,
                2.5, windowEnd.minusSeconds(1),
                windowStart, windowEnd, config);
        assertTrue(result.isPresent(), "a rise of exactly 0.5m should meet the >= threshold");
    }
}
