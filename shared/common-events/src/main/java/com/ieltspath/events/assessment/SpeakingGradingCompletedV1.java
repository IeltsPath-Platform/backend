package com.ieltspath.events.assessment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A placement Speaking grade, or a terminal failure which lets Assessment complete a partial result. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SpeakingGradingCompletedV1(UUID eventId, String eventType, Instant occurredAt, String source, Data data) {
    public static final String EVENT_TYPE = "SpeakingGradingCompleted.v1";
    public static final String SOURCE = "learning-service";

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Data(UUID attemptId, UUID userId, String status, BigDecimal band,
                       List<Criterion> criteria, String summary, String rubricCode, Integer rubricVersion,
                       String failureCode) {
        public Data { criteria = criteria == null ? List.of() : List.copyOf(criteria); }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Criterion(String code, BigDecimal band, String comment) { }
}
