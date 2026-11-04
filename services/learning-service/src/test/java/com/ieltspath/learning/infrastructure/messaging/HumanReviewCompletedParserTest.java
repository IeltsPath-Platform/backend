package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.learning.application.command.HumanReviewResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HumanReviewCompletedParserTest {
    private final HumanReviewCompletedParser parser = new HumanReviewCompletedParser(new ObjectMapper());

    private static final String VALID = """
            {"eventId":"30000000-0000-4000-8000-000000000001",
             "eventType":"assessment.human-review-completed.v1",
             "occurredAt":"2026-11-05T10:00:00Z",
             "learnerId":"30000000-0000-4000-8000-000000000002",
             "reviewSubmissionId":"30000000-0000-4000-8000-000000000003",
             "sourceSubmissionId":"30000000-0000-4000-8000-000000000004",
             "criteria":[{"criterion":"coherence","band":5.0}],
             "errorCodes":[{"code":"LINKER_MISUSE","count":2}]}
            """;

    @Test
    void readsTheContractIntoTheResult() {
        HumanReviewResult result = parser.parse(VALID.getBytes(StandardCharsets.UTF_8));

        assertEquals("30000000-0000-4000-8000-000000000002", result.learnerId().toString());
        assertEquals("30000000-0000-4000-8000-000000000004", result.sourceSubmissionId().toString());
        assertEquals(List.of(new HumanReviewResult.Criterion("coherence", new BigDecimal("5.0"))), result.criteria());
        assertEquals(List.of(new HumanReviewResult.ErrorCount("LINKER_MISUSE", 2)), result.errorCodes());
    }

    @Test
    void refusesAnotherEventTypeOrAMalformedBody() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("not json".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                VALID.replace("assessment.human-review-completed.v1", "assessment.grading-completed.v1")
                        .getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void refusesAMarkCountBelowOne() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                VALID.replace("\"count\":2", "\"count\":0").getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void refusesAnIdThatIsNotAUuid() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(
                VALID.replace("30000000-0000-4000-8000-000000000002", "not-a-uuid").getBytes(StandardCharsets.UTF_8)));
    }
}
