package org.jalrakshak.streamprocessor.config;

import java.io.Serializable;
import java.time.Duration;

/**
 * All configurable thresholds, windows and timeouts for the stream
 * processor, loaded from environment variables / -D system properties with
 * documented defaults, and validated at job startup (fail fast on obviously
 * invalid values rather than silently running with nonsense config).
 *
 * <p><b>These defaults are provisional engineering/demo values, not
 * validated hydrological standards.</b> They come from the brief's
 * provisional demo defaults table and are only a starting point for a real
 * deployment, which would need thresholds set per-station by domain
 * experts.</p>
 */
public final class RuleConfig implements Serializable {

    // --- Kafka ---
    public final String bootstrapServers;
    public final String measurementsTopic;
    public final String alertsTopic;
    public final String consumerGroupId;

    // --- Simple threshold rules ---
    public final double levelHighThresholdM;
    public final double rainIntenseThresholdMmPerHour;
    public final double levelAndRainLevelThresholdM;
    public final double levelAndRainRainThresholdMmPerHour;

    // --- LEVEL_RISING_FAST: sliding event-time trend window ---
    public final double risingRateThresholdM;
    public final Duration risingWindowSize;
    public final Duration risingWindowSlide;

    // --- SENSOR_SPIKE: rolling median baseline ---
    public final double spikeThresholdM;
    public final int spikeWarmupReadings;
    public final Duration spikeBaselineWindow;

    // --- STATION_STALE: processing-time timeout ---
    public final Duration staleTimeout;

    // --- Event-time / watermarking ---
    public final Duration watermarkOutOfOrderness;

    // --- Repeated-alert suppression ---
    public final Duration suppressionWindow;

    private RuleConfig(Builder b) {
        this.bootstrapServers = b.bootstrapServers;
        this.measurementsTopic = b.measurementsTopic;
        this.alertsTopic = b.alertsTopic;
        this.consumerGroupId = b.consumerGroupId;
        this.levelHighThresholdM = b.levelHighThresholdM;
        this.rainIntenseThresholdMmPerHour = b.rainIntenseThresholdMmPerHour;
        this.levelAndRainLevelThresholdM = b.levelAndRainLevelThresholdM;
        this.levelAndRainRainThresholdMmPerHour = b.levelAndRainRainThresholdMmPerHour;
        this.risingRateThresholdM = b.risingRateThresholdM;
        this.risingWindowSize = b.risingWindowSize;
        this.risingWindowSlide = b.risingWindowSlide;
        this.spikeThresholdM = b.spikeThresholdM;
        this.spikeWarmupReadings = b.spikeWarmupReadings;
        this.spikeBaselineWindow = b.spikeBaselineWindow;
        this.staleTimeout = b.staleTimeout;
        this.watermarkOutOfOrderness = b.watermarkOutOfOrderness;
        this.suppressionWindow = b.suppressionWindow;
    }

    /** Builds config from environment variables (falling back to -D system properties, then defaults). */
    public static RuleConfig fromEnvironment() {
        Builder b = new Builder();
        b.bootstrapServers = str("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        b.measurementsTopic = str("KAFKA_TOPIC_MEASUREMENTS", "river.measurements.v1");
        b.alertsTopic = str("KAFKA_TOPIC_ALERTS", "river.alerts.v1");
        b.consumerGroupId = str("KAFKA_CONSUMER_GROUP", "jalrakshak-stream-processor");

        b.levelHighThresholdM = dbl("RULE_LEVEL_HIGH_THRESHOLD_M", 4.0);
        b.rainIntenseThresholdMmPerHour = dbl("RULE_RAIN_INTENSE_THRESHOLD_MM_H", 20.0);
        b.levelAndRainLevelThresholdM = dbl("RULE_LEVEL_AND_RAIN_LEVEL_THRESHOLD_M", 3.0);
        b.levelAndRainRainThresholdMmPerHour = dbl("RULE_LEVEL_AND_RAIN_RAIN_THRESHOLD_MM_H", 15.0);

        b.risingRateThresholdM = dbl("RULE_RISING_RATE_THRESHOLD_M", 0.5);
        b.risingWindowSize = minutes("RULE_RISING_WINDOW_MINUTES", 15);
        b.risingWindowSlide = minutes("RULE_RISING_SLIDE_MINUTES", 5);

        b.spikeThresholdM = dbl("RULE_SPIKE_THRESHOLD_M", 1.0);
        b.spikeWarmupReadings = intVal("RULE_SPIKE_WARMUP_READINGS", 3);
        b.spikeBaselineWindow = minutes("RULE_SPIKE_BASELINE_WINDOW_MINUTES", 60);

        b.staleTimeout = minutes("RULE_STALE_TIMEOUT_MINUTES", 30);

        b.watermarkOutOfOrderness = minutes("WATERMARK_OUT_OF_ORDERNESS_MINUTES", 5);

        b.suppressionWindow = minutes("ALERT_SUPPRESSION_WINDOW_MINUTES", 60);

        RuleConfig config = new RuleConfig(b);
        config.validate();
        return config;
    }

    /** Fails fast on obviously invalid configuration rather than running silently with nonsense values. */
    public void validate() {
        requirePositive("RULE_LEVEL_HIGH_THRESHOLD_M", levelHighThresholdM);
        requirePositive("RULE_RAIN_INTENSE_THRESHOLD_MM_H", rainIntenseThresholdMmPerHour);
        requirePositive("RULE_LEVEL_AND_RAIN_LEVEL_THRESHOLD_M", levelAndRainLevelThresholdM);
        requirePositive("RULE_LEVEL_AND_RAIN_RAIN_THRESHOLD_MM_H", levelAndRainRainThresholdMmPerHour);
        requirePositive("RULE_RISING_RATE_THRESHOLD_M", risingRateThresholdM);
        requirePositiveDuration("RULE_RISING_WINDOW_MINUTES", risingWindowSize);
        requirePositiveDuration("RULE_RISING_SLIDE_MINUTES", risingWindowSlide);
        if (risingWindowSlide.compareTo(risingWindowSize) > 0) {
            throw new IllegalStateException("RULE_RISING_SLIDE_MINUTES must not exceed RULE_RISING_WINDOW_MINUTES");
        }
        requirePositive("RULE_SPIKE_THRESHOLD_M", spikeThresholdM);
        if (spikeWarmupReadings < 1) {
            throw new IllegalStateException("RULE_SPIKE_WARMUP_READINGS must be >= 1");
        }
        requirePositiveDuration("RULE_SPIKE_BASELINE_WINDOW_MINUTES", spikeBaselineWindow);
        requirePositiveDuration("RULE_STALE_TIMEOUT_MINUTES", staleTimeout);
        requirePositiveDuration("WATERMARK_OUT_OF_ORDERNESS_MINUTES", watermarkOutOfOrderness);
        requirePositiveDuration("ALERT_SUPPRESSION_WINDOW_MINUTES", suppressionWindow);
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            throw new IllegalStateException("KAFKA_BOOTSTRAP_SERVERS must not be blank");
        }
    }

    private static void requirePositive(String name, double value) {
        if (!(value > 0)) {
            throw new IllegalStateException(name + " must be > 0, was " + value);
        }
    }

    private static void requirePositiveDuration(String name, Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(name + " must be a positive duration, was " + value);
        }
    }

    private static String str(String env, String def) {
        String v = System.getenv(env);
        if (v == null) v = System.getProperty(env);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static double dbl(String env, double def) {
        String v = str(env, null);
        return v == null ? def : Double.parseDouble(v);
    }

    private static int intVal(String env, int def) {
        String v = str(env, null);
        return v == null ? def : Integer.parseInt(v);
    }

    private static Duration minutes(String env, int defMinutes) {
        String v = str(env, null);
        long minutes = v == null ? defMinutes : Long.parseLong(v);
        return Duration.ofMinutes(minutes);
    }

    private static final class Builder {
        String bootstrapServers;
        String measurementsTopic;
        String alertsTopic;
        String consumerGroupId;
        double levelHighThresholdM;
        double rainIntenseThresholdMmPerHour;
        double levelAndRainLevelThresholdM;
        double levelAndRainRainThresholdMmPerHour;
        double risingRateThresholdM;
        Duration risingWindowSize;
        Duration risingWindowSlide;
        double spikeThresholdM;
        int spikeWarmupReadings;
        Duration spikeBaselineWindow;
        Duration staleTimeout;
        Duration watermarkOutOfOrderness;
        Duration suppressionWindow;
    }
}