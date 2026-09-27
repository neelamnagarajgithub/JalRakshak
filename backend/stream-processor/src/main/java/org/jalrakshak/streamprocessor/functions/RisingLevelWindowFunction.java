package org.jalrakshak.streamprocessor.functions;

import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.jalrakshak.streamprocessor.rules.AlertCandidate;
import org.jalrakshak.streamprocessor.rules.RisingLevelRuleEngine;

import java.time.Instant;
import java.util.UUID;

public class RisingLevelWindowFunction extends ProcessWindowFunction<MeasurementEvent, AlertCandidate, UUID, TimeWindow> {

    private final RuleConfig config;

    public RisingLevelWindowFunction(RuleConfig config) {
        this.config = config;
    }

    @Override
    public void process(UUID stationId, Context context, Iterable<MeasurementEvent> elements, Collector<AlertCandidate> out) {
        MeasurementEvent earliest = null;
        MeasurementEvent latest = null;

        for (MeasurementEvent event : elements) {
            if (event.getWaterLevelM() == null || event.getObservedAt() == null) {
                continue;
            }
            if (earliest == null || event.getObservedAt().isBefore(earliest.getObservedAt())) {
                earliest = event;
            }
            if (latest == null || event.getObservedAt().isAfter(latest.getObservedAt())) {
                latest = event;
            }
        }

        if (earliest == null || latest == null || earliest == latest) {
            return;
        }

        Instant windowStart = Instant.ofEpochMilli(context.window().getStart());
        Instant windowEnd = Instant.ofEpochMilli(context.window().getEnd());

        RisingLevelRuleEngine.evaluate(
                stationId,
                earliest.getWaterLevelM(), earliest.getObservedAt(),
                latest.getWaterLevelM(), latest.getObservedAt(),
                windowStart, windowEnd,
                config
        ).ifPresent(out::collect);
    }
}