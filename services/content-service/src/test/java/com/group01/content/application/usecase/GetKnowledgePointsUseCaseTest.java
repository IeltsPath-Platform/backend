package com.group01.content.application.usecase;

import com.group01.content.application.result.KnowledgePointResult;
import com.group01.content.domain.aggregate.KnowledgePoint;
import com.group01.content.domain.repository.KnowledgePointRepository;
import com.group01.content.domain.vo.KnowledgePointKind;
import com.group01.content.domain.vo.LearningType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetKnowledgePointsUseCaseTest {
    @Mock KnowledgePointRepository knowledgePoints;

    @Test
    void listsEveryKnowledgePointWhenNoTopicIsGiven() {
        KnowledgePoint first = kp("KP-A");
        KnowledgePoint second = kp("KP-B");
        when(knowledgePoints.findAll()).thenReturn(List.of(first, second));

        List<KnowledgePointResult> results = new GetKnowledgePointsUseCase(knowledgePoints).execute(null);

        assertThat(results).extracting(KnowledgePointResult::code).containsExactly("KP-A", "KP-B");
        verify(knowledgePoints, never()).findByTopicId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void filtersByTopicWhenOneIsGiven() {
        UUID topicId = UUID.randomUUID();
        when(knowledgePoints.findByTopicId(topicId)).thenReturn(List.of(kp("KP-T")));

        List<KnowledgePointResult> results = new GetKnowledgePointsUseCase(knowledgePoints).execute(topicId);

        assertThat(results).extracting(KnowledgePointResult::code).containsExactly("KP-T");
        verify(knowledgePoints, never()).findAll();
    }

    private static KnowledgePoint kp(String code) {
        return KnowledgePoint.create(UUID.randomUUID(), code, code, KnowledgePointKind.GRAMMAR,
                LearningType.PROCEDURE, null, null);
    }
}
