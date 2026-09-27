package org.jalrakshak.streamprocessor.functions;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.jalrakshak.shared.AlertEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.jalrakshak.streamprocessor.rules.AlertCandidate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Suppression policy (see docs/architecture.md "Repeated alert suppression"):
 * for a given stationId+ruleCode key, the first candidate in a suppression
 * window (default 1h) becomes a brand-new {@link AlertEvent} with a freshly
 * generated id and repeatCount=1. Any further candidate for the same key
 * whose event time falls inside that window is treated as a <b>repeat
 * occurrence</b>: it reuses the same alert id, increments repeatCount, and
 * refreshes lastOccurredAt/evidence -- it is never silently dropped, and it
 * never creates a second row for what is operationally the same ongoing
 * condition. Once a candidate's event time is past the suppression window,
 * the condition is treated as re-armed: a new id is generated and the window
 * resets.
 *
 * <p>This keeps the persistence-side idempotency story simple: the API's
 * alert-persistence consumer just upserts by {@code AlertEvent.id} (see
 * {@code AlertService#persistAlertEvent} in api-service), so "repeat
 * occurrence with the same id" and "this Kafka record got redelivered" are
 * handled by the exact same code path.</p>
 */
public class AlertSuppressionFunction extends KeyedProcessFunction<String, AlertCandidate, AlertEvent> {

    private final RuleConfig config;
    private transient ValueState<SuppressionState> state;

    public AlertSuppressionFunction(RuleConfig config) {
        this.config = config;
    }

    @Override
    public void open(Configuration parameters) {
        state = getRuntimeContext().getState(new ValueStateDescriptor<>("suppression-state", SuppressionState.class));
    }

    @Override
    public void processElement(AlertCandidate candidate, Context ctx, Collector<AlertEvent> out) throws Exception {
        SuppressionState existing = state.value();
        long eventTimeMillis = candidate.getEventTime() != null
                ? candidate.getEventTime().toEpochMilli()
                : ctx.timerService().currentProcessingTime();
        long suppressionWindowMillis = config.suppressionWindow.toMillis();

        boolean withinSuppressionWindow = existing != null
                && (eventTimeMillis - existing.getLastOccurredAtEpochMilli()) < suppressionWindowMillis
                && existing.getAlertId() != null;

        AlertEvent event;
        if (withinSuppressionWindow) {
            int repeatCount = existing.getRepeatCount() + 1;
            existing.setRepeatCount(repeatCount);
            existing.setLastOccurredAtEpochMilli(eventTimeMillis);
            state.update(existing);

            event = buildEvent(candidate, UUID.fromString(existing.getAlertId()),
                    Instant.ofEpochMilli(existing.getFirstTriggeredAtEpochMilli()),
                    Instant.ofEpochMilli(eventTimeMillis), repeatCount);
        } else {
            UUID newId = UUID.randomUUID();
            SuppressionState fresh = new SuppressionState();
            fresh.setAlertId(newId.toString());
            fresh.setFirstTriggeredAtEpochMilli(eventTimeMillis);
            fresh.setLastOccurredAtEpochMilli(eventTimeMillis);
            fresh.setRepeatCount(1);
            state.update(fresh);

            event = buildEvent(candidate, newId, Instant.ofEpochMilli(eventTimeMillis),
                    Instant.ofEpochMilli(eventTimeMillis), 1);
        }

        out.collect(event);
    }

    private AlertEvent buildEvent(AlertCandidate candidate, UUID id, Instant triggeredAt, Instant lastOccurredAt, int repeatCount) {
        Map<String, Object> evidence = candidate.getEvidence() != null
                ? new HashMap<>(candidate.getEvidence())
                : new HashMap<>();
        evidence.put("repeatCount", repeatCount);

        return AlertEvent.builder()
                .id(id)
                .stationId(candidate.getStationId())
                .ruleCode(candidate.getRuleCode())
                .severity(candidate.getSeverity())
                .status("ACTIVE")
                .title(candidate.getTitle())
                .reason(candidate.getReason())
                .evidence(evidence)
                .triggeredAt(triggeredAt)
                .lastOccurredAt(lastOccurredAt)
                .repeatCount(repeatCount)
                .build();
    }
}