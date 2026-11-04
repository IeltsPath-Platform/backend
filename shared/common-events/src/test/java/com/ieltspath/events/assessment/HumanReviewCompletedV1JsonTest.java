package com.ieltspath.events.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HumanReviewCompletedV1JsonTest {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void preservesFieldNamesAndCarriesNoEssayOrComment() throws Exception {
        var event = new HumanReviewCompletedV1(id(1), HumanReviewCompletedV1.EVENT_TYPE,
                Instant.parse("2026-11-05T10:00:00Z"), id(2), id(3), id(4),
                List.of(new HumanReviewCompletedV1.Criterion("coherence", new BigDecimal("5.0"))),
                List.of(new HumanReviewCompletedV1.ErrorCodeCount("LINKER_MISUSE", 2)));

        byte[] body = JSON.writeValueAsBytes(event);

        assertEquals(JSON.readTree("""
                {"eventId":"30000000-0000-4000-8000-000000000001",
                 "eventType":"assessment.human-review-completed.v1",
                 "occurredAt":"2026-11-05T10:00:00Z",
                 "learnerId":"30000000-0000-4000-8000-000000000002",
                 "reviewSubmissionId":"30000000-0000-4000-8000-000000000003",
                 "sourceSubmissionId":"30000000-0000-4000-8000-000000000004",
                 "criteria":[{"criterion":"coherence","band":5.0}],
                 "errorCodes":[{"code":"LINKER_MISUSE","count":2}]}
                """), JSON.readTree(body));
        assertEquals(event, JSON.readValue(body, HumanReviewCompletedV1.class));
    }

    @Test
    void neverSerialisesEssayCommentFeedbackOrExaminerFields() throws Exception {
        var event = new HumanReviewCompletedV1(id(1), HumanReviewCompletedV1.EVENT_TYPE, Instant.now(), id(2), id(3),
                id(4), List.of(), List.of());

        String text = JSON.writeValueAsString(event);

        for (String forbidden : List.of("essay", "comment", "feedback", "excerpt", "suggestion", "examinerId",
                "source\"", "task\"")) {
            assertFalse(text.contains(forbidden), "event must not carry " + forbidden);
        }
    }

    private static UUID id(int suffix) {
        return UUID.fromString("30000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
