package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A placement attempt's essays are ready to be graded by Learning's essay grader, free for the learner. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PlacementWritingGradingRequestedV1(UUID eventId, String eventType, Instant occurredAt, String source,
                                                 Data data) {
    public static final String EVENT_TYPE = "PlacementWritingGradingRequested.v1";
    public static final String SOURCE = "assessment-service";

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Data(UUID attemptId, UUID userId, List<Item> items) {
        public Data { items = items == null ? List.of() : List.copyOf(items); }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Item(UUID attemptItemId, UUID questionVersionId, String stem, String task, Integer minWords,
                       BigDecimal passBand, String chartFacts, List<UUID> knowledgePointIds, String essayText) {
        public Item { knowledgePointIds = knowledgePointIds == null ? List.of() : List.copyOf(knowledgePointIds); }
    }
}
