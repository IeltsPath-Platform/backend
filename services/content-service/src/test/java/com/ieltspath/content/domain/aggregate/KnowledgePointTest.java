package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgePointTest {
    @Test
    void activeKnowledgePointRequiresLearningType() {
        assertThatThrownBy(() -> KnowledgePoint.create(
                UUID.randomUUID(), "KP-1", "Example", KnowledgePointKind.GRAMMAR,
                null, null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activationRequiresLearningTypeAndLeavesLegacyPointInactive() {
        KnowledgePoint knowledgePoint = new KnowledgePoint(
                UUID.randomUUID(), UUID.randomUUID(), "KP-LEGACY", "Legacy",
                KnowledgePointKind.VOCABULARY, null, null, null, ContentStatus.INACTIVE, null, null
        );

        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        assertThatThrownBy(knowledgePoint::activate).isInstanceOf(IllegalArgumentException.class);
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.INACTIVE);
    }

    @Test
    void createsWithCanonicalLearningType() {
        KnowledgePoint knowledgePoint = KnowledgePoint.create(
                UUID.randomUUID(), "KP-1", "Example", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, null, null
        );

        assertThat(knowledgePoint.getLearningType()).isEqualTo(LearningType.CONCEPT);
    }

    @Test
    void updateChangesTopicAndCodeWithoutAcceptingOrChangingStatus() {
        UUID newTopicId = UUID.randomUUID();
        KnowledgePoint knowledgePoint = KnowledgePoint.create(
                UUID.randomUUID(), "R55_OLD_CODE", "Example", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, Skill.READING, null);

        knowledgePoint.update(newTopicId, "L60_NEW_CODE", "Updated", KnowledgePointKind.STRATEGY,
                LearningType.PROCEDURE, Skill.LISTENING, "Updated description");

        assertThat(knowledgePoint.getTopicId()).isEqualTo(newTopicId);
        assertThat(knowledgePoint.getCode()).isEqualTo("L60_NEW_CODE");
        assertThat(knowledgePoint.getName()).isEqualTo("Updated");
        assertThat(knowledgePoint.getKind()).isEqualTo(KnowledgePointKind.STRATEGY);
        assertThat(knowledgePoint.getLearningType()).isEqualTo(LearningType.PROCEDURE);
        assertThat(knowledgePoint.getSkill()).isEqualTo(Skill.LISTENING);
        assertThat(knowledgePoint.getDescription()).isEqualTo("Updated description");
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.ACTIVE);
    }

    @Test
    void deactivateAndActivateChangeStatus() {
        KnowledgePoint knowledgePoint = KnowledgePoint.create(
                UUID.randomUUID(), "R55_STATUS_TEST", "Example", KnowledgePointKind.GRAMMAR,
                LearningType.CONCEPT, Skill.READING, null);

        knowledgePoint.deactivate();
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.INACTIVE);

        knowledgePoint.activate();
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.ACTIVE);
    }
}
