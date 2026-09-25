package org.jalrakshak.streamprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Stream processing application.
 * This will eventually use Apache Flink for event-time window processing.
 */
@SpringBootApplication
public class StreamProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamProcessorApplication.class, args);
    }
}