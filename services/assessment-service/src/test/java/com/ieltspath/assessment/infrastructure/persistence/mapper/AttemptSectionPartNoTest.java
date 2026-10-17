package com.ieltspath.assessment.infrastructure.persistence.mapper;

import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.infrastructure.persistence.entity.AttemptSectionJpaEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttemptSectionPartNoTest {
    @Test
    void partNumberSurvivesStartCompleteAndPersistenceMapping() {
        Integer partNo = 2;
        AttemptSection section = new AttemptSection(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 2,
                "{}", null, null, partNo);

        AttemptSection started = section.start(Instant.parse("2026-10-15T10:00:00Z"));
        AttemptSection completed = started.complete(Instant.parse("2026-10-15T10:20:00Z"));
        AssessmentPersistenceMapper mapper = new AssessmentPersistenceMapper();
        AttemptSectionJpaEntity entity = mapper.toEntity(completed);
        AttemptSection restored = mapper.toDomain(entity);

        assertEquals(partNo, started.partNo());
        assertEquals(partNo, completed.partNo());
        assertEquals(partNo, entity.getPartNo());
        assertEquals(partNo, restored.partNo());
    }
}
