package com.ieltspath.content.application.usecase;

import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import com.ieltspath.content.domain.exception.PrerequisiteCycleException;
import com.ieltspath.content.domain.exception.PrerequisiteLevelException;
import com.ieltspath.content.domain.exception.InvalidPrerequisiteException;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.exception.InvalidErrorTypeException;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.KnowledgePrerequisiteRepository;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplaceKnowledgePointPrerequisitesUseCaseTest {
    @Mock private KnowledgePointRepository knowledgePoints;
    @Mock private KnowledgePrerequisiteRepository prerequisites;
    @Mock private ErrorTypeRepository errorTypes;

    @Test
    void refusesATrapPointAsTheDependentPoint() {
        UUID id = UUID.randomUUID();
        when(errorTypes.containsTrapKnowledgePoints(List.of(id))).thenReturn(true);

        assertThatThrownBy(() -> useCase().execute(id, List.of()))
                .isInstanceOf(InvalidErrorTypeException.class)
                .hasMessage("TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE");
        verify(prerequisites).lockTree();
    }

    @Test
    void refusesATrapPointAsAPrerequisite() {
        KnowledgePoint dependent = point("PARENT", CefrLevel.B2);
        UUID trapId = UUID.randomUUID();
        when(knowledgePoints.findById(dependent.getId())).thenReturn(Optional.of(dependent));
        when(errorTypes.containsTrapKnowledgePoints(List.of(dependent.getId()))).thenReturn(false);
        when(errorTypes.containsTrapKnowledgePoints(List.of(trapId))).thenReturn(true);

        assertThatThrownBy(() -> useCase().execute(dependent.getId(), List.of(trapId)))
                .isInstanceOf(InvalidErrorTypeException.class)
                .hasMessage("TRAP_KNOWLEDGE_POINT_NOT_ASSIGNABLE");
    }

    private ReplaceKnowledgePointPrerequisitesUseCase useCase() {
        return new ReplaceKnowledgePointPrerequisitesUseCase(knowledgePoints, prerequisites, errorTypes);
    }

    private static KnowledgePoint point(String code, CefrLevel level) {
        return KnowledgePoint.create(UUID.randomUUID(), code, code, KnowledgePointKind.STRATEGY,
                LearningType.CONCEPT, null, null, level, "Can do", null, null);
    }

    @Test
    void unknownTooHighAndCyclicPrerequisitesAreRefused() {
        KnowledgePoint dependent = point("B1_POINT", CefrLevel.B1);
        KnowledgePoint higher = point("C1_POINT", CefrLevel.C1);
        KnowledgePoint lower = point("A2_POINT", CefrLevel.A2);
        UUID unknown = UUID.randomUUID();
        when(knowledgePoints.findById(dependent.getId())).thenReturn(Optional.of(dependent));
        when(knowledgePoints.findByIds(List.of(unknown))).thenReturn(List.of());
        when(knowledgePoints.findByIds(List.of(higher.getId()))).thenReturn(List.of(higher));
        when(knowledgePoints.findByIds(List.of(lower.getId()))).thenReturn(List.of(lower));
        // The lower point already depends on the dependent one, so making it a prerequisite closes a cycle.
        when(prerequisites.findAll()).thenReturn(List.of(
                new KnowledgePrerequisiteRepository.Edge(dependent.getId(), lower.getId())));

        assertThatThrownBy(() -> useCase().execute(dependent.getId(), List.of(unknown)))
                .isInstanceOf(InvalidPrerequisiteException.class);
        assertThatThrownBy(() -> useCase().execute(dependent.getId(), List.of(higher.getId())))
                .isInstanceOf(PrerequisiteLevelException.class);
        assertThatThrownBy(() -> useCase().execute(dependent.getId(), List.of(lower.getId())))
                .isInstanceOf(PrerequisiteCycleException.class);
        verify(prerequisites, never()).replace(any(), any());
    }
}
