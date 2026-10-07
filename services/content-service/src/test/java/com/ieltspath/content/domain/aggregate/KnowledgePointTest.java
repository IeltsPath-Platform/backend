package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgePointTest {
    @Test
    void activeKnowledgePointRequiresLearningType() {
        assertThrows(IllegalArgumentException.class, () -> KnowledgePoint.create(
                UUID.randomUUID(), "KP-1", "Example", KnowledgePointKind.GRAMMAR,
                null, null, null
        ));
    }

    @Test
    void inactiveLegacyKnowledgePointCanRemainUnclassifiedUntilRepublished() {
        KnowledgePoint knowledgePoint = new KnowledgePoint(
                UUID.randomUUID(), UUID.randomUUID(), "KP-LEGACY", "Legacy",
                KnowledgePointKind.VOCABULARY, null, null, null,
                com.ieltspath.content.domain.vo.ContentStatus.INACTIVE, null, null
        );

        assertEquals(com.ieltspath.content.domain.vo.ContentStatus.INACTIVE, knowledgePoint.getStatus());
        assertThrows(IllegalArgumentException.class, () -> knowledgePoint.update(
                "Legacy", KnowledgePointKind.VOCABULARY, null, null, null,
                com.ieltspath.content.domain.vo.ContentStatus.ACTIVE
        ));
    }

    @Test
    void createsWithCanonicalLearningType() {
        KnowledgePoint knowledgePoint = KnowledgePoint.create(
                UUID.randomUUID(), "KP-1", "Example", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, null, null
        );

        assertEquals(LearningType.CONCEPT, knowledgePoint.getLearningType());
    }
}
