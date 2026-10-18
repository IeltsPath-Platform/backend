package com.ieltspath.learning.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ieltspath.events.assessment.AssessmentCompletedV2;
import com.ieltspath.learning.application.command.AssessmentResult;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentCompletedContractTest {
    @Test
    void parsesTheSharedProducerRecordWithoutChangingTheConsumerParser() throws Exception {
        ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        UUID eventId = UUID.randomUUID(), user = UUID.randomUUID(), pkg = UUID.randomUUID(),
                attempt = UUID.randomUUID(), result = UUID.randomUUID(), question = UUID.randomUUID(),
                item = UUID.randomUUID(), kp = UUID.randomUUID(), essay = UUID.randomUUID(), essayQuestion = UUID.randomUUID();
        Instant at = Instant.parse("2026-10-01T09:10:00Z");
        var record = new AssessmentCompletedV2(eventId, AssessmentCompletedV2.EVENT_TYPE, at,
                AssessmentCompletedV2.SOURCE, new AssessmentCompletedV2.Data(user, null, pkg, attempt, result, 2,
                "TOPIC_GATE", "COMPLETED", at, new BigDecimal("6.5"), List.of(
                new AssessmentCompletedV2.ItemResult(item, question, false, BigDecimal.ZERO, BigDecimal.ONE,
                        List.of(new AssessmentCompletedV2.KnowledgePointMapping(kp, BigDecimal.ONE, "FAIL", "PARAPHRASE_TRAP")),
                        4200L, "PARAPHRASE_TRAP"),
                new AssessmentCompletedV2.ItemResult(essay, essayQuestion, null, BigDecimal.ONE, BigDecimal.TEN, List.of()))));

        AssessmentResult parsed = new AssessmentCompletedParser(json).parse(json.writeValueAsBytes(record));

        assertEquals(new AssessmentResult(eventId, user, pkg, attempt, result, 2, "TOPIC_GATE", at, List.of(
                new AssessmentResult.ItemResult(item, question, false, BigDecimal.ZERO, BigDecimal.ONE,
                        List.of(new AssessmentResult.KnowledgePointJudgment(kp, "FAIL")), 4200L, "PARAPHRASE_TRAP"),
                new AssessmentResult.ItemResult(essay, essayQuestion, null, BigDecimal.ONE, BigDecimal.TEN, List.of())),
                new BigDecimal("6.5")), parsed);
    }
}
