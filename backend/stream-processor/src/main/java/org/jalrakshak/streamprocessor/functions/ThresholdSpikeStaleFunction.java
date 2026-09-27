package org.jalrakshak.streamprocessor.functions;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.jalrakshak.streamprocessor.rules.AlertCandidate;
import org.jalrakshak.streamprocessor.rules.SpikeRuleEngine;
import org.jalrakshak.streamprocessor.rules.ThresholdRules;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-station (keyed by stationId) evaluation of:
 * <ul>
 *   <li>LEVEL_HIGH / RAIN_INTENSE / LEVEL_AND_RAIN -- stateless, per-event ({@link ThresholdRules})</li>
 *   <li>SENSOR_SPIKE -- a rolling buffer of prior readings within the baseline
 *       window (event-time pruned) feeding {@link SpikeRuleEngine}</li>
 *   <li>STATION_STALE -- a <b>processing-time</b> timer that is deleted and
 *       re-registered on every reading. If no reading arrives for
 *       {@code staleTimeout}, the timer fires and emits STATION_STALE. This
 *       deliberately does not use event time: if a station goes silent, no
 *       new events (and therefore no watermark advancement) will ever occur
 *       for that key, so an event-time window would never close and staleness
 *       would never be detected. See docs/architecture.md "Station stale
 *       detection" for the restart/recovery caveat: Flink's checkpointed
 *       timers survive a job restart-from-checkpoint/savepoint, but this has
 *       not been tested against a real failure in this environment, so it is
 *       not claimed as verified restart-safe.</li>
 * </ul>
 */
public class ThresholdSpikeStaleFunction extends KeyedProcessFunction<UUID, MeasurementEvent, AlertCandidate> {

    private final RuleConfig config;

    private transient ListState<TimestampedLevel> baselineBuffer;
    private transient ValueState<Long> activeStaleTimerTimestamp;
    private transient ValueState<Long> lastObservedAtEpochMilli;

    public ThresholdSpikeStaleFunction(RuleConfig config) {
        this.config = config;
    }

    @Override
    public void open(Configuration parameters) {
        baselineBuffer = getRuntimeContext().getListState(
                new ListStateDescriptor<>("spike-baseline-buffer", TimestampedLevel.class));
        activeStaleTimerTimestamp = getRuntimeContext().getState(
                new ValueStateDescriptor<>("active-stale-timer", Long.class));
        lastObservedAtEpochMilli = getRuntimeContext().getState(
                new ValueStateDescriptor<>("last-observed-at", Long.class));
    }

    @Override
    public void processElement(MeasurementEvent event, Context ctx, Collector<AlertCandidate> out) throws Exception {
        // 1) Simple per-event threshold rules
        for (AlertCandidate candidate : ThresholdRules.evaluate(event, config)) {
            out.collect(candidate);
        }

        // 2) SENSOR_SPIKE against the rolling baseline, then update the buffer
        if (event.getWaterLevelM() != null && event.getObservedAt() != null) {
            List<Double> baselineLevels = pruneAndCollectBaseline(event.getObservedAt());
            SpikeRuleEngine.evaluate(baselineLevels, event, config).ifPresent(out::collect);
            baselineBuffer.add(new TimestampedLevel(event.getObservedAt().toEpochMilli(), event.getWaterLevelM()));
        }

        // 3) STATION_STALE: reset the processing-time timer on every reading
        long fireAt = ctx.timerService().currentProcessingTime() + config.staleTimeout.toMillis();
        Long previous = activeStaleTimerTimestamp.value();
        if (previous != null) {
            ctx.timerService().deleteProcessingTimeTimer(previous);
        }
        ctx.timerService().registerProcessingTimeTimer(fireAt);
        activeStaleTimerTimestamp.update(fireAt);

        if (event.getObservedAt() != null) {
            lastObservedAtEpochMilli.update(event.getObservedAt().toEpochMilli());
        }
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<AlertCandidate> out) throws Exception {
        Long active = activeStaleTimerTimestamp.value();
        if (active == null || active != timestamp) {
            // A newer reading already rescheduled this timer; this firing is stale, ignore it.
            return;
        }

        UUID stationId = ctx.getCurrentKey();
        Long lastObserved = lastObservedAtEpochMilli.value();
        Instant lastObservedAt = lastObserved != null ? Instant.ofEpochMilli(lastObserved) : null;

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("stationId", stationId);
        evidence.put("lastObservedAt", lastObservedAt);
        evidence.put("staleTimeoutMinutes", config.staleTimeout.toMinutes());

        out.collect(AlertCandidate.builder()
                .stationId(stationId)
                .ruleCode("STATION_STALE")
                .severity("WARNING")
                .title("Station not reporting")
                .reason(String.format("No accepted reading for station in over %d minutes (last observed: %s)",
                        config.staleTimeout.toMinutes(), lastObservedAt))
                .evidence(evidence)
                .eventTime(Instant.ofEpochMilli(timestamp))
                .build());

        // Do not re-register here: the next actual reading (if any) will
        // re-arm the timer via processElement. This avoids firing repeatedly
        // every `staleTimeout` for a station that never comes back, which
        // would otherwise spam duplicate STATION_STALE candidates forever
        // (the suppression function would collapse those into one alert row
        // anyway, but there is no reason to keep emitting them).
        activeStaleTimerTimestamp.clear();
    }

    private List<Double> pruneAndCollectBaseline(Instant currentEventTime) throws Exception {
        long cutoff = currentEventTime.toEpochMilli() - config.spikeBaselineWindow.toMillis();
        List<TimestampedLevel> kept = new ArrayList<>();
        List<Double> levels = new ArrayList<>();
        for (TimestampedLevel sample : baselineBuffer.get()) {
            if (sample.getObservedAtEpochMilli() >= cutoff) {
                kept.add(sample);
                levels.add(sample.getWaterLevelM());
            }
        }
        baselineBuffer.update(kept);
        return levels;
    }
}