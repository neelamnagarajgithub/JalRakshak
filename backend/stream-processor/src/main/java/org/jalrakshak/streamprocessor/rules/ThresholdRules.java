package org.jalrakshak.streamprocessor.rules;

import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates the three rules that only need the current reading (no window,
 * no history): LEVEL_HIGH, RAIN_INTENSE, LEVEL_AND_RAIN.
 *
 * <p>Boundary convention: all threshold comparisons are {@code >=} (a
 * reading exactly at the threshold triggers), matching how the thresholds
 * are phrased in docs/architecture.md ("Water level >= 4.0 m", etc).</p>
 */
public final class ThresholdRules {

    public static final String LEVEL_HIGH = "LEVEL_HIGH";
    public static final String RAIN_INTENSE = "RAIN_INTENSE";
    public static final String LEVEL_AND_RAIN = "LEVEL_AND_RAIN";

    private ThresholdRules() {
    }

    /** Evaluates all three simple threshold rules for one reading. A reading may match more than one. */
    public static List<AlertCandidate> evaluate(MeasurementEvent event, RuleConfig config) {
        List<AlertCandidate> candidates = new ArrayList<>();

        if (event.getWaterLevelM() != null && event.getWaterLevelM() >= config.levelHighThresholdM) {
            candidates.add(AlertCandidate.builder()
                    .stationId(event.getStationId())
                    .ruleCode(LEVEL_HIGH)
                    .severity("HIGH")
                    .title("Water level high")
                    .reason(String.format("Water level %.2fm >= %.2fm threshold",
                            event.getWaterLevelM(), config.levelHighThresholdM))
                    .evidence(evidence(event, "waterLevelM", event.getWaterLevelM(), "thresholdM", config.levelHighThresholdM))
                    .eventTime(event.getObservedAt())
                    .build());
        }

        if (event.getRainfallMmPerHour() != null && event.getRainfallMmPerHour() >= config.rainIntenseThresholdMmPerHour) {
            candidates.add(AlertCandidate.builder()
                    .stationId(event.getStationId())
                    .ruleCode(RAIN_INTENSE)
                    .severity("WARNING")
                    .title("Intense rainfall")
                    .reason(String.format("Rainfall %.2fmm/h >= %.2fmm/h threshold",
                            event.getRainfallMmPerHour(), config.rainIntenseThresholdMmPerHour))
                    .evidence(evidence(event, "rainfallMmPerHour", event.getRainfallMmPerHour(),
                            "thresholdMmPerHour", config.rainIntenseThresholdMmPerHour))
                    .eventTime(event.getObservedAt())
                    .build());
        }

        if (event.getWaterLevelM() != null && event.getRainfallMmPerHour() != null
                && event.getWaterLevelM() >= config.levelAndRainLevelThresholdM
                && event.getRainfallMmPerHour() >= config.levelAndRainRainThresholdMmPerHour) {
            Map<String, Object> ev = evidence(event, "waterLevelM", event.getWaterLevelM(),
                    "levelThresholdM", config.levelAndRainLevelThresholdM);
            ev.put("rainfallMmPerHour", event.getRainfallMmPerHour());
            ev.put("rainThresholdMmPerHour", config.levelAndRainRainThresholdMmPerHour);
            candidates.add(AlertCandidate.builder()
                    .stationId(event.getStationId())
                    .ruleCode(LEVEL_AND_RAIN)
                    .severity("CRITICAL")
                    .title("High water level with intense rainfall")
                    .reason(String.format("Water level %.2fm >= %.2fm and rainfall %.2fmm/h >= %.2fmm/h",
                            event.getWaterLevelM(), config.levelAndRainLevelThresholdM,
                            event.getRainfallMmPerHour(), config.levelAndRainRainThresholdMmPerHour))
                    .evidence(ev)
                    .eventTime(event.getObservedAt())
                    .build());
        }

        return candidates;
    }

    private static Map<String, Object> evidence(MeasurementEvent event, String valueKey, Object value,
                                                  String thresholdKey, Object threshold) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("stationId", event.getStationId());
        evidence.put("eventTimestamp", event.getObservedAt());
        evidence.put(valueKey, value);
        evidence.put(thresholdKey, threshold);
        return evidence;
    }
}