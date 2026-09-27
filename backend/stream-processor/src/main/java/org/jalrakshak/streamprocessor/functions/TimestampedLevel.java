package org.jalrakshak.streamprocessor.functions;

import java.io.Serializable;

public class TimestampedLevel implements Serializable {
    private long observedAtEpochMilli;
    private double waterLevelM;

    public TimestampedLevel() {
    }

    public TimestampedLevel(long observedAtEpochMilli, double waterLevelM) {
        this.observedAtEpochMilli = observedAtEpochMilli;
        this.waterLevelM = waterLevelM;
    }

    public long getObservedAtEpochMilli() {
        return observedAtEpochMilli;
    }

    public void setObservedAtEpochMilli(long observedAtEpochMilli) {
        this.observedAtEpochMilli = observedAtEpochMilli;
    }

    public double getWaterLevelM() {
        return waterLevelM;
    }

    public void setWaterLevelM(double waterLevelM) {
        this.waterLevelM = waterLevelM;
    }
}