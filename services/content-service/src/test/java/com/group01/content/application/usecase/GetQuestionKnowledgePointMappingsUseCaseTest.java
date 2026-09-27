package com.group01.content.application.usecase;

import com.group01.content.domain.entity.QuestionKnowledgePoint;
import com.group01.content.domain.repository.QuestionKnowledgePointRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class GetQuestionKnowledgePointMappingsUseCaseTest {
    @Mock QuestionKnowledgePointRepository repository;

    @Test
    void returnsCanonicalMappingsWithWeights() {
        UUID questionVersionId = UUID.randomUUID();
        UUID knowledgePointId = UUID.randomUUID();
        when(repository.findByQuestionVersionIds(Set.of(questionVersionId))).thenReturn(List.of(
                new QuestionKnowledgePoint(questionVersionId, knowledgePointId, new BigDecimal("0.75"))));

        var result = new GetQuestionKnowledgePointMappingsUseCase(repository)
                .execute(List.of(questionVersionId, questionVersionId));

        assertEquals(1, result.size());
        assertEquals(knowledgePointId, result.getFirst().knowledgePointId());
        assertEquals(new BigDecimal("0.75"), result.getFirst().weight());
        verify(repository).findByQuestionVersionIds(Set.of(questionVersionId));
    }

    @Test
    void rejectsEmptyRequest() {
        var useCase = new GetQuestionKnowledgePointMappingsUseCase(repository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute(List.of()));
        verifyNoInteractions(repository);
    }
}
