package org.jalrakshak.streamprocessor.rules;

import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SENSOR_SPIKE: flags a reading whose water level differs from the median
 * of the preceding baseline window (default 1h) by more than the configured
 * threshold.
 *
 * <p>Warm-up: with fewer than {@code spikeWarmupReadings} prior readings in
 * the baseline window, no verdict is produced (median over too few points is
 * unreliable / a single outlier could poison it).</p>
 *
 * <p>This class holds no Flink state itself -- the calling
 * {@code KeyedProcessFunction} owns a per-station buffer of recent (time,
 * waterLevel) readings and passes the relevant window slice in on every
 * call. Kept plain/pure so it can be unit tested without a Flink test
 * harness.</p>
 */
public final class SpikeRuleEngine {

    public static final String SENSOR_SPIKE = "SENSOR_SPIKE";

    private SpikeRuleEngine() {
    }

    /**
     * @param baselineReadings prior readings (excluding the current one) whose observedAt falls
     *                         within the baseline window, in any order
     * @param current          the new reading being evaluated
     */
    public static Optional<AlertCandidate> evaluate(List<Double> baselineReadings, MeasurementEvent current, RuleConfig config) {
        if (current.getWaterLevelM() == null) {
            return Optional.empty();
        }
        if (baselineReadings.size() < config.spikeWarmupReadings) {
            return Optional.empty();
        }

        double median = median(baselineReadings);
        double delta = current.getWaterLevelM() - median;

        if (Math.abs(delta) < config.spikeThresholdM) {
            return Optional.empty();
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("stationId", current.getStationId());
        evidence.put("eventTimestamp", current.getObservedAt());
        evidence.put("waterLevelM", current.getWaterLevelM());
        evidence.put("baselineMedianM", median);
        evidence.put("deltaM", delta);
        evidence.put("thresholdM", config.spikeThresholdM);
        evidence.put("baselineSampleCount", baselineReadings.size());

        return Optional.of(AlertCandidate.builder()
                .stationId(current.getStationId())
                .ruleCode(SENSOR_SPIKE)
                .severity("WARNING")
                .title("Sensor reading spike")
                .reason(String.format("Water level %.2fm differs from %dh baseline median %.2fm by %.2fm (>= %.2fm threshold)",
                        current.getWaterLevelM(), config.spikeBaselineWindow.toHours(), median, Math.abs(delta), config.spikeThresholdM))
                .evidence(evidence)
                .eventTime(current.getObservedAt())
                .build());
    }

    static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}