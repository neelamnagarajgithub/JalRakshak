package org.jalrakshak.streamprocessor.functions;

import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.jalrakshak.shared.MeasurementEvent;
import org.jalrakshak.streamprocessor.serde.MeasurementIngestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngestSplitFunction extends ProcessFunction<MeasurementIngestResult, MeasurementEvent> {

    private static final Logger logger = LoggerFactory.getLogger(IngestSplitFunction.class);

    public static final OutputTag<String> MALFORMED_TAG =
            new OutputTag<String>("malformed-measurements") {};

    @Override
    public void processElement(MeasurementIngestResult result, Context ctx, Collector<MeasurementEvent> out) {
        if (result.isValid()) {
            out.collect(result.getEvent());
        } else {
            logger.warn("Routing malformed measurement record to side output: {}", result.getError());
            ctx.output(MALFORMED_TAG, result.getError() + " | raw=" + result.getRawPayload());
        }
    }
}