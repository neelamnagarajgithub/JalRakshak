package org.jalrakshak.streamprocessor.rules;

import org.jalrakshak.streamprocessor.config.RuleConfig;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * LEVEL_RISING_FAST: fires when water level rose by at least
 * {@code risingRateThresholdM} between the earliest and latest readings in a
 * 15-minute (default) sliding event-time window.
 *
 * <p>Pure function of the window's boundary readings so it can be unit
 * tested without instantiating a Flink window operator; the Flink wiring
 * (SlidingEventTimeWindows + a ProcessWindowFunction that finds the min/max
 * observedAt reading in the window) lives in
 * {@code RisingLevelWindowFunction}.</p>
 */
public final class RisingLevelRuleEngine {

    public static final String LEVEL_RISING_FAST = "LEVEL_RISING_FAST";

    private RisingLevelRuleEngine() {
    }

    public static Optional<AlertCandidate> evaluate(UUID stationId,
                                                      double earliestLevelM, Instant earliestTime,
                                                      double latestLevelM, Instant latestTime,
                                                      Instant windowStart, Instant windowEnd,
                                                      RuleConfig config) {
        double rise = latestLevelM - earliestLevelM;
        if (rise < config.risingRateThresholdM) {
            return Optional.empty();
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("stationId", stationId);
        evidence.put("eventTimestamp", latestTime);
        evidence.put("windowStart", windowStart);
        evidence.put("windowEnd", windowEnd);
        evidence.put("earliestWaterLevelM", earliestLevelM);
        evidence.put("earliestObservedAt", earliestTime);
        evidence.put("latestWaterLevelM", latestLevelM);
        evidence.put("latestObservedAt", latestTime);
        evidence.put("riseM", rise);
        evidence.put("thresholdM", config.risingRateThresholdM);

        return Optional.of(AlertCandidate.builder()
                .stationId(stationId)
                .ruleCode(LEVEL_RISING_FAST)
                .severity("HIGH")
                .title("Water level rising fast")
                .reason(String.format("Water level rose %.2fm within window %s to %s (>= %.2fm threshold)",
                        rise, windowStart, windowEnd, config.risingRateThresholdM))
                .evidence(evidence)
                .eventTime(latestTime)
                .build());
    }
}