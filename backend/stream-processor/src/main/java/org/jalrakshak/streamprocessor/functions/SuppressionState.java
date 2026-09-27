package org.jalrakshak.streamprocessor.functions;

import java.io.Serializable;

public class SuppressionState implements Serializable {
    private String alertId;
    private long firstTriggeredAtEpochMilli;
    private long lastOccurredAtEpochMilli;
    private int repeatCount;

    public SuppressionState() {
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public long getFirstTriggeredAtEpochMilli() {
        return firstTriggeredAtEpochMilli;
    }

    public void setFirstTriggeredAtEpochMilli(long firstTriggeredAtEpochMilli) {
        this.firstTriggeredAtEpochMilli = firstTriggeredAtEpochMilli;
    }

    public long getLastOccurredAtEpochMilli() {
        return lastOccurredAtEpochMilli;
    }

    public void setLastOccurredAtEpochMilli(long lastOccurredAtEpochMilli) {
        this.lastOccurredAtEpochMilli = lastOccurredAtEpochMilli;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }
}