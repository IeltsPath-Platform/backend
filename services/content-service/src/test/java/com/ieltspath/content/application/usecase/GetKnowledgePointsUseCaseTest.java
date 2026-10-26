package com.ieltspath.content.application.usecase;

import com.ieltspath.content.application.result.KnowledgePointResult;
import com.ieltspath.content.domain.aggregate.KnowledgePoint;
import com.ieltspath.content.domain.repository.KnowledgePointRepository;
import com.ieltspath.content.domain.repository.ErrorTypeRepository;
import com.ieltspath.content.domain.vo.CefrLevel;
import com.ieltspath.content.domain.vo.KnowledgePointKind;
import com.ieltspath.content.domain.vo.LearningType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetKnowledgePointsUseCaseTest {
    @Mock KnowledgePointRepository knowledgePoints;
    @Mock ErrorTypeRepository errorTypes;

    @Test
    void listsEveryKnowledgePointWhenNoTopicIsGiven() {
        KnowledgePoint first = kp("KP-A");
        KnowledgePoint second = kp("KP-B");
        when(knowledgePoints.findAll()).thenReturn(List.of(first, second));
        when(errorTypes.findTrapKnowledgePointIds(List.of(first.getId(), second.getId()))).thenReturn(Set.of());

        List<KnowledgePointResult> results = new GetKnowledgePointsUseCase(knowledgePoints, errorTypes).execute(null);

        assertThat(results).extracting(KnowledgePointResult::code).containsExactly("KP-A", "KP-B");
        verify(knowledgePoints, never()).findByTopicId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void filtersByTopicWhenOneIsGiven() {
        UUID topicId = UUID.randomUUID();
        when(knowledgePoints.findByTopicId(topicId)).thenReturn(List.of(kp("KP-T")));
        UUID pointId = knowledgePoints.findByTopicId(topicId).getFirst().getId();
        when(errorTypes.findTrapKnowledgePointIds(List.of(pointId))).thenReturn(Set.of());

        List<KnowledgePointResult> results = new GetKnowledgePointsUseCase(knowledgePoints, errorTypes).execute(topicId);

        assertThat(results).extracting(KnowledgePointResult::code).containsExactly("KP-T");
        verify(knowledgePoints, never()).findAll();
    }

    @Test
    void doesNotReturnTrapChildrenToTheAuthorPicker() {
        KnowledgePoint parent = kp("READING_EVIDENCE");
        KnowledgePoint trap = kp("TRAP_KEYWORD_MATCH_ONLY");
        when(knowledgePoints.findAll()).thenReturn(List.of(parent, trap));
        when(errorTypes.findTrapKnowledgePointIds(List.of(parent.getId(), trap.getId()))).thenReturn(Set.of(trap.getId()));

        List<KnowledgePointResult> results = new GetKnowledgePointsUseCase(knowledgePoints, errorTypes).execute(null);

        assertThat(results).extracting(KnowledgePointResult::code).containsExactly("READING_EVIDENCE");
    }

    private static KnowledgePoint kp(String code) {
        return KnowledgePoint.create(UUID.randomUUID(), code, code, KnowledgePointKind.GRAMMAR,
                LearningType.PROCEDURE, null, null, CefrLevel.B1, "Do something.", null, null);
    }
}
