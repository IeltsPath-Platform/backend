package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A placement attempt's Speaking recordings are ready for free automated grading by Learning. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SpeakingGradingRequestedV1(UUID eventId, String eventType, Instant occurredAt, String source, Data data) {
    public static final String EVENT_TYPE = "SpeakingGradingRequested.v1";
    public static final String SOURCE = "assessment-service";

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Data(UUID attemptId, UUID userId, List<Item> items) {
        public Data { items = items == null ? List.of() : List.copyOf(items); }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Item(UUID attemptItemId, int partNo, int questionNo, String question, String objectKey) { }
}
