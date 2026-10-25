package com.ieltspath.content.application.service;

import com.ieltspath.content.domain.aggregate.ErrorType;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.ContentStatus;
import com.ieltspath.content.domain.vo.ErrorCategory;
import com.ieltspath.content.domain.vo.ErrorReviewStatus;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import com.ieltspath.content.domain.vo.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrapKnowledgePointSyncTest {
    @Mock private KnowledgePointRepository knowledgePoints;
    @Mock private ErrorTypeRepository errorTypes;

    private TrapKnowledgePointSync sync;
    private KnowledgePoint parent;
    private ErrorType errorType;

    @BeforeEach
    void setUp() {
        sync = new TrapKnowledgePointSync(knowledgePoints, errorTypes);
        parent = point(UUID.randomUUID(), "READING_EVIDENCE", "Evidence", ContentStatus.ACTIVE);
        errorType = ErrorType.create("KEYWORD_MATCH_ONLY", "Keyword match", ErrorCategory.READING_STRATEGY, 2,
                parent.getId(), "Check the claim against the full sentence.");
        when(knowledgePoints.save(any(KnowledgePoint.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsAChildForAnActiveErrorWithAnActiveParent() {
        when(knowledgePoints.findById(parent.getId())).thenReturn(Optional.of(parent));
        when(knowledgePoints.findByCode("TRAP_KEYWORD_MATCH_ONLY")).thenReturn(Optional.empty());

        sync.sync(errorType);

        assertThat(errorType.getTrapKnowledgePointId()).isNotNull();
        verify(knowledgePoints).save(org.mockito.ArgumentMatchers.argThat(point ->
                point.getCode().equals("TRAP_KEYWORD_MATCH_ONLY")
                        && point.getName().equals(errorType.getName())
                        && point.getDescription().equals(errorType.getFixHint())
                        && point.getTopicId().equals(parent.getTopicId())
                        && point.getKind() == parent.getKind()
                        && point.getLearningType() == parent.getLearningType()
                        && point.getSkill() == parent.getSkill()
                        && point.getStatus() == ContentStatus.ACTIVE));
    }

    @Test
    void updatesTheChildNameAndFixHintWhenTheErrorTypeChanges() {
        when(knowledgePoints.findById(parent.getId())).thenReturn(Optional.of(parent));
        KnowledgePoint child = point(UUID.randomUUID(), "TRAP_KEYWORD_MATCH_ONLY", "Old name", ContentStatus.ACTIVE);
        when(knowledgePoints.findByCode("TRAP_KEYWORD_MATCH_ONLY")).thenReturn(Optional.of(child));
        errorType.update("Revised name", ErrorCategory.READING_STRATEGY, 2, parent.getId(), "Read for meaning.",
                ContentStatus.ACTIVE, ErrorReviewStatus.APPROVED);

        sync.sync(errorType);

        assertThat(child.getName()).isEqualTo("Revised name");
        assertThat(child.getDescription()).isEqualTo("Read for meaning.");
        assertThat(errorType.getTrapKnowledgePointId()).isEqualTo(child.getId());
    }

    @Test
    void deactivatesTheChildWhenItsErrorTypeBecomesInactive() {
        when(knowledgePoints.findById(parent.getId())).thenReturn(Optional.of(parent));
        KnowledgePoint child = point(UUID.randomUUID(), "TRAP_KEYWORD_MATCH_ONLY", "Keyword match", ContentStatus.ACTIVE);
        when(knowledgePoints.findByCode("TRAP_KEYWORD_MATCH_ONLY")).thenReturn(Optional.of(child));
        errorType.update(errorType.getName(), errorType.getCategory(), errorType.getSeverity(), parent.getId(),
                errorType.getFixHint(), ContentStatus.INACTIVE, null);

        sync.sync(errorType);

        assertThat(child.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        assertThat(errorType.getTrapKnowledgePointId()).isEqualTo(child.getId());
    }

    @Test
    void deactivatesTheChildWhenTheErrorTypeLosesItsParent() {
        KnowledgePoint child = point(UUID.randomUUID(), "TRAP_KEYWORD_MATCH_ONLY", "Keyword match", ContentStatus.ACTIVE);
        when(knowledgePoints.findByCode("TRAP_KEYWORD_MATCH_ONLY")).thenReturn(Optional.of(child));
        errorType.update(errorType.getName(), errorType.getCategory(), errorType.getSeverity(), null,
                errorType.getFixHint(), ContentStatus.ACTIVE, null);

        sync.sync(errorType);

        assertThat(child.getStatus()).isEqualTo(ContentStatus.INACTIVE);
        assertThat(errorType.getTrapKnowledgePointId()).isEqualTo(child.getId());
    }

    @Test
    void deactivatesTheChildWhenItsParentIsInactive() {
        parent.deactivate();
        when(knowledgePoints.findById(parent.getId())).thenReturn(Optional.of(parent));
        KnowledgePoint child = point(UUID.randomUUID(), "TRAP_KEYWORD_MATCH_ONLY", "Keyword match", ContentStatus.ACTIVE);
        when(knowledgePoints.findByCode("TRAP_KEYWORD_MATCH_ONLY")).thenReturn(Optional.of(child));

        sync.sync(errorType);

        assertThat(child.getStatus()).isEqualTo(ContentStatus.INACTIVE);
    }

    @Test
    void reactivatesTheChildWhenItsParentBecomesActiveAgain() {
        when(knowledgePoints.findById(parent.getId())).thenReturn(Optional.of(parent));
        KnowledgePoint child = point(UUID.randomUUID(), "TRAP_KEYWORD_MATCH_ONLY", "Keyword match", ContentStatus.INACTIVE);
        when(knowledgePoints.findByCode("TRAP_KEYWORD_MATCH_ONLY")).thenReturn(Optional.of(child));
        when(errorTypes.findByKnowledgePointId(parent.getId())).thenReturn(List.of(errorType));

        sync.syncForKnowledgePoint(parent.getId());

        assertThat(child.getStatus()).isEqualTo(ContentStatus.ACTIVE);
        assertThat(errorType.getTrapKnowledgePointId()).isEqualTo(child.getId());
        verify(errorTypes).save(errorType);
    }

    private static KnowledgePoint point(UUID id, String code, String name, ContentStatus status) {
        Instant now = Instant.now();
        return new KnowledgePoint(id, UUID.randomUUID(), code, name, KnowledgePointKind.STRATEGY,
                LearningType.PROCEDURE, Skill.READING, "Description", CefrLevel.B1, "Can do", null, null,
                status, now, now);
    }
}
