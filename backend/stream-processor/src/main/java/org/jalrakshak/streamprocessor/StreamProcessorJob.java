package org.jalrakshak.streamprocessor;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.jalrakshak.shared.AlertEvent;
import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.config.RuleConfig;
import org.jalrakshak.streamprocessor.functions.AlertSuppressionFunction;
import org.jalrakshak.streamprocessor.functions.IngestSplitFunction;
import org.jalrakshak.streamprocessor.functions.RisingLevelWindowFunction;
import org.jalrakshak.streamprocessor.functions.ThresholdSpikeStaleFunction;
import org.jalrakshak.streamprocessor.rules.AlertCandidate;
import org.jalrakshak.streamprocessor.serde.AlertEventSerializer;
import org.jalrakshak.streamprocessor.serde.MeasurementEventDeserializer;
import org.jalrakshak.streamprocessor.serde.MeasurementIngestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * JalRakshak stream-processing job.
 *
 * <pre>
 * river.measurements.v1
 *   -> KafkaSource (deserialize, never throws -- malformed records become
 *                    MeasurementIngestResult.malformed(...))
 *   -> IngestSplitFunction (main: valid MeasurementEvent, side output: malformed, logged)
 *   -> assignTimestampsAndWatermarks (observedAt, 5min bounded-out-of-orderness)
 *   -> keyBy(stationId)
 *        +-> ThresholdSpikeStaleFunction   (LEVEL_HIGH, RAIN_INTENSE, LEVEL_AND_RAIN,
 *        |                                  SENSOR_SPIKE, STATION_STALE)
 *        +-> SlidingEventTimeWindows(15min, 5min) + RisingLevelWindowFunction
 *                                          (LEVEL_RISING_FAST; late events routed
 *                                           to a side output, never trigger this rule)
 *   -> union(candidates)
 *   -> keyBy(stationId + "|" + ruleCode)
 *   -> AlertSuppressionFunction (assigns id / repeatCount, 1h suppression window)
 *   -> KafkaSink -> river.alerts.v1
 * </pre>
 */
public final class StreamProcessorJob {

    private static final Logger logger = LoggerFactory.getLogger(StreamProcessorJob.class);

    public static void main(String[] args) throws Exception {
        RuleConfig config = RuleConfig.fromEnvironment();
        logger.info("Starting JalRakshak stream processor with config: bootstrapServers={}, measurementsTopic={}, "
                        + "alertsTopic={}, staleTimeout={}, suppressionWindow={}",
                config.bootstrapServers, config.measurementsTopic, config.alertsTopic,
                config.staleTimeout, config.suppressionWindow);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(30_000);
        DataStream<AlertEvent> alerts = build(env, config);

        KafkaSink<AlertEvent> sink = KafkaSink.<AlertEvent>builder()
                .setBootstrapServers(config.bootstrapServers)
                .setRecordSerializer(new AlertEventSerializer(config.alertsTopic))
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
        alerts.sinkTo(sink).name("alerts-sink").uid("alerts-sink");

        env.execute("jalrakshak-stream-processor");
    }

    /** Package-visible so tests can build the pipeline against a different (e.g. collection-based) source/sink. */
    static DataStream<AlertEvent> build(StreamExecutionEnvironment env, RuleConfig config) {
        KafkaSource<MeasurementIngestResult> source = KafkaSource.<MeasurementIngestResult>builder()
                .setBootstrapServers(config.bootstrapServers)
                .setTopics(config.measurementsTopic)
                .setGroupId(config.consumerGroupId)
                // Resume from this consumer group's last COMMITTED offset on every restart
                // (so a routine restart during dev/testing does not replay the whole topic
                // and re-mint alert ids for measurements already processed); only fall back
                // to the very start of the topic the first time this group id has ever run,
                // when there is no committed offset yet. Requires checkpointing enabled
                // (see env.enableCheckpointing above) so offsets actually get committed back
                // to Kafka -- Flink's Kafka source does this automatically once checkpointing
                // is on and a group id is set (commit.offsets.on.checkpoint defaults to true).
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setDeserializer(new MeasurementEventDeserializer())
                .setProperty("commit.offsets.on.checkpoint", "true")
                .build();

        DataStream<MeasurementIngestResult> raw = env.fromSource(
                source, WatermarkStrategy.noWatermarks(), "measurements-source");

        SingleOutputStreamOperator<MeasurementEvent> split = raw
                .process(new IngestSplitFunction())
                .name("split-malformed").uid("split-malformed");
        split.getSideOutput(IngestSplitFunction.MALFORMED_TAG)
                .map(err -> "malformed measurement: " + err)
                .name("log-malformed").uid("log-malformed")
                .print("MALFORMED");

        DataStream<MeasurementEvent> validEvents = split.assignTimestampsAndWatermarks(
                WatermarkStrategy.<MeasurementEvent>forBoundedOutOfOrderness(config.watermarkOutOfOrderness)
                        .withTimestampAssigner((event, ts) -> event.getObservedAt().toEpochMilli()));

        DataStream<AlertCandidate> thresholdSpikeStaleCandidates = validEvents
                .keyBy(MeasurementEvent::getStationId)
                .process(new ThresholdSpikeStaleFunction(config))
                .name("threshold-spike-stale").uid("threshold-spike-stale");

        org.apache.flink.util.OutputTag<MeasurementEvent> lateTag =
                new org.apache.flink.util.OutputTag<MeasurementEvent>("late-rising-level-events") {};

        SingleOutputStreamOperator<AlertCandidate> risingLevelCandidates = validEvents
                .keyBy(MeasurementEvent::getStationId)
                .window(SlidingEventTimeWindows.of(
                        Time.milliseconds(config.risingWindowSize.toMillis()),
                        Time.milliseconds(config.risingWindowSlide.toMillis())))
                .sideOutputLateData(lateTag)
                .process(new RisingLevelWindowFunction(config))
                .name("rising-level-window").uid("rising-level-window");
        risingLevelCandidates.getSideOutput(lateTag)
                .map(e -> "late event for rising-level window, station=" + e.getStationId() + " observedAt=" + e.getObservedAt())
                .name("log-late-events").uid("log-late-events")
                .print("LATE");

        DataStream<AlertCandidate> allCandidates = thresholdSpikeStaleCandidates.union(risingLevelCandidates);

        return allCandidates
                .keyBy(new org.apache.flink.api.java.functions.KeySelector<AlertCandidate, String>() {
                    @Override
                    public String getKey(AlertCandidate c) {
                        return c.getStationId() + "|" + c.getRuleCode();
                    }
                })
                .process(new AlertSuppressionFunction(config))
                .name("alert-suppression").uid("alert-suppression");
    }

    private StreamProcessorJob() {
    }
}