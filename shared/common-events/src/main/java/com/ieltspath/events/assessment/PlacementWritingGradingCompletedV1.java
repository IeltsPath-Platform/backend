package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Learning's answer for a placement attempt's essays: a band and short comments each; the detail stays in Learning. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlacementWritingGradingCompletedV1(UUID eventId, String eventType, Instant occurredAt, String source,
                                                 Data data) {
    public static final String EVENT_TYPE = "PlacementWritingGradingCompleted.v1";
    public static final String SOURCE = "learning-service";
    public static final String GRADED = "GRADED";
    public static final String FAILED = "FAILED";

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Data(UUID attemptId, UUID userId, List<Item> items) {
        public Data { items = items == null ? List.of() : List.copyOf(items); }
    }

    /** {@code band} and the comments are set only when {@code status} is {@code GRADED}. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Item(UUID attemptItemId, String status, BigDecimal band, String summary, List<Criterion> criteria,
                       List<String> focus, String promptVersion, String failureCode) {
        public Item {
            criteria = criteria == null ? List.of() : List.copyOf(criteria);
            focus = focus == null ? List.of() : List.copyOf(focus);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Criterion(String code, BigDecimal band, String comment) { }
}
