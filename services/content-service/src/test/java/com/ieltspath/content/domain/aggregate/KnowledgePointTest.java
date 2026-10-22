package com.ieltspath.content.domain.aggregate;

import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgePointTest {
    private static KnowledgePoint active(String code, KnowledgePointKind kind, LearningType learningType, Skill skill) {
        return KnowledgePoint.create(UUID.randomUUID(), code, "Example", kind, learningType, skill, null,
                CefrLevel.B1, "Do something.", null, null);
    }

    @Test
    void activeKnowledgePointRequiresLearningType() {
        assertThatThrownBy(() -> active("KP-1", KnowledgePointKind.GRAMMAR, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activeKnowledgePointRequiresCefrLevelAndCanDo() {
        assertThatThrownBy(() -> KnowledgePoint.create(UUID.randomUUID(), "KP-1", "Example",
                KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, null, null, null, "Do something.", null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cefrLevel");
        assertThatThrownBy(() -> KnowledgePoint.create(UUID.randomUUID(), "KP-1", "Example",
                KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, null, null, CefrLevel.B1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KnowledgePoint.create(UUID.randomUUID(), "KP-1", "Example",
                KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, null, null, CefrLevel.B1, "   ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
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
    void activationRequiresCefrLevelAndCanDoUntilTheyAreFilledIn() {
        KnowledgePoint draft = new KnowledgePoint(
                UUID.randomUUID(), UUID.randomUUID(), "S60_DRAFT", "Draft", KnowledgePointKind.STRATEGY,
                LearningType.PROCEDURE, Skill.SPEAKING, null, ContentStatus.INACTIVE, null, null);

        assertThatThrownBy(draft::activate).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cefrLevel");
        assertThat(draft.getStatus()).isEqualTo(ContentStatus.INACTIVE);

        draft.update(draft.getTopicId(), draft.getCode(), draft.getName(), draft.getKind(), draft.getLearningType(),
                draft.getSkill(), null, CefrLevel.B2, "Deliver a long turn.", null, null);
        draft.activate();
        assertThat(draft.getStatus()).isEqualTo(ContentStatus.ACTIVE);
    }

    @Test
    void createsWithCanonicalLearningType() {
        KnowledgePoint knowledgePoint = active("KP-1", KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, null);

        assertThat(knowledgePoint.getLearningType()).isEqualTo(LearningType.CONCEPT);
        assertThat(knowledgePoint.getCefrLevel()).isEqualTo(CefrLevel.B1);
    }

    @Test
    void updateChangesTopicAndCodeWithoutAcceptingOrChangingStatus() {
        UUID newTopicId = UUID.randomUUID();
        KnowledgePoint knowledgePoint = active("R55_OLD_CODE", KnowledgePointKind.GRAMMAR, LearningType.CONCEPT,
                Skill.READING);

        knowledgePoint.update(newTopicId, "L60_NEW_CODE", "Updated", KnowledgePointKind.STRATEGY,
                LearningType.PROCEDURE, Skill.LISTENING, "Updated description", CefrLevel.B2, "Updated can-do.",
                null, null);

        assertThat(knowledgePoint.getTopicId()).isEqualTo(newTopicId);
        assertThat(knowledgePoint.getCode()).isEqualTo("L60_NEW_CODE");
        assertThat(knowledgePoint.getName()).isEqualTo("Updated");
        assertThat(knowledgePoint.getKind()).isEqualTo(KnowledgePointKind.STRATEGY);
        assertThat(knowledgePoint.getLearningType()).isEqualTo(LearningType.PROCEDURE);
        assertThat(knowledgePoint.getSkill()).isEqualTo(Skill.LISTENING);
        assertThat(knowledgePoint.getDescription()).isEqualTo("Updated description");
        assertThat(knowledgePoint.getCefrLevel()).isEqualTo(CefrLevel.B2);
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.ACTIVE);
    }

    @Test
    void updatingAnActivePointCannotClearCefrLevelOrCanDo() {
        KnowledgePoint knowledgePoint = active("R55_CLEAR", KnowledgePointKind.GRAMMAR, LearningType.CONCEPT,
                Skill.READING);

        assertThatThrownBy(() -> knowledgePoint.update(knowledgePoint.getTopicId(), "R55_CLEAR", "Example",
                KnowledgePointKind.GRAMMAR, LearningType.CONCEPT, Skill.READING, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(knowledgePoint.getCefrLevel()).isEqualTo(CefrLevel.B1);
        assertThat(knowledgePoint.getCanDo()).isEqualTo("Do something.");
    }

    @Test
    void deactivateAndActivateChangeStatus() {
        KnowledgePoint knowledgePoint = active("R55_STATUS_TEST", KnowledgePointKind.GRAMMAR, LearningType.CONCEPT,
                Skill.READING);

        knowledgePoint.deactivate();
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.INACTIVE);

        knowledgePoint.activate();
        assertThat(knowledgePoint.getStatus()).isEqualTo(ContentStatus.ACTIVE);
    }
}
