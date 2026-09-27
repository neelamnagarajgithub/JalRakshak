package org.jalrakshak.streamprocessor.serde;

import lombok.Builder;
import lombok.Getter;
import org.jalrakshak.shared.MeasurementEvent;

import java.io.Serializable;

@Getter
@Builder
public class MeasurementIngestResult implements Serializable {
    private final MeasurementEvent event;
    private final String rawPayload;
    private final String error;

    public boolean isValid() {
        return error == null && event != null;
    }

    public static MeasurementIngestResult valid(MeasurementEvent event) {
        return MeasurementIngestResult.builder().event(event).build();
    }

    public static MeasurementIngestResult malformed(String rawPayload, String error) {
        return MeasurementIngestResult.builder().rawPayload(rawPayload).error(error).build();
    }
}