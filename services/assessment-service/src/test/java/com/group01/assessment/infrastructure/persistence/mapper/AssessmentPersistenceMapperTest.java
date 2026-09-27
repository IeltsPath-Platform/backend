package com.group01.assessment.infrastructure.persistence.mapper;

import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.entity.ErrorAnalysisItem;
import com.group01.assessment.domain.entity.ItemResult;
import com.group01.assessment.domain.entity.ItemResultKnowledgeJudgment;
import com.group01.assessment.domain.entity.OutboxEvent;
import com.group01.assessment.domain.entity.VideoPracticeAttempt;
import com.group01.assessment.domain.vo.PracticeStatus;
import com.group01.assessment.domain.vo.QualitativeJudgment;
import com.group01.assessment.domain.vo.PracticeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentPersistenceMapperTest {
    private final AssessmentPersistenceMapper mapper = new AssessmentPersistenceMapper();

    @Test
    void mapsV5ItemResultFields() {
        ItemResult domain = new ItemResult(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                4.25, 5.0, true, 1200L, "{\"feedback\":\"ok\"}");

        assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
    }

    @Test
    void mapsAttemptItemKnowledgePointSnapshot() {
        AttemptItemKnowledgePoint domain = new AttemptItemKnowledgePoint(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("0.75"));

        assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
    }

    @Test
    void mapsExplicitKnowledgeJudgment() {
        ItemResultKnowledgeJudgment domain = new ItemResultKnowledgeJudgment(UUID.randomUUID(), UUID.randomUUID(),
                QualitativeJudgment.PASS);

        assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
    }

    @Test
    void mapsOutboxEventForRelay() {
        OutboxEvent domain = new OutboxEvent(UUID.randomUUID(), "AssessmentResult", UUID.randomUUID().toString(),
                "AssessmentCompleted.v2", "{\"event_type\":\"AssessmentCompleted.v2\"}",
                Instant.parse("2026-09-24T10:00:00Z"), null, 2, "broker unavailable");

        assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
    }

    @Test
    void mapsV5ErrorAnalysisFields() {
        ErrorAnalysisItem domain = new ErrorAnalysisItem(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "LEXICAL", "word choice");

        assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
    }

    @Test
    void mapsV5VideoPracticeSnapshotAndResultFields() {
        VideoPracticeAttempt domain = new VideoPracticeAttempt(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                PracticeType.DICTATION, "reference words", "response words", null,
                new BigDecimal("85.00"), PracticeStatus.COMPLETED,
                Instant.parse("2026-09-24T10:00:00Z"), Instant.parse("2026-09-24T10:01:00Z"),
                Instant.parse("2026-09-24T10:00:00Z"), "{\"accuracy\":85}");

        assertEquals(domain, mapper.toDomain(mapper.toEntity(domain)));
    }
}
