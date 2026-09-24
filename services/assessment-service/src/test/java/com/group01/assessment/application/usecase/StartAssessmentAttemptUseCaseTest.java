package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.StartAssessmentAttemptCommand;
import com.group01.assessment.application.port.KnowledgeMappingProvider;
import com.group01.assessment.application.port.LearningGoalProvider;
import com.group01.assessment.domain.aggregate.AssessmentAttempt;
import com.group01.assessment.domain.entity.AttemptItem;
import com.group01.assessment.domain.entity.AttemptItemKnowledgePoint;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.AttemptChannel;
import com.group01.assessment.domain.vo.AttemptMode;
import com.group01.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartAssessmentAttemptUseCaseTest {
    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptSectionRepository sections;
    @Mock AttemptItemRepository items;
    @Mock AttemptItemKnowledgePointRepository knowledgeSnapshot;
    @Mock LearningGoalProvider learningGoals;
    @Mock KnowledgeMappingProvider knowledgeMappings;

    private final UUID userId = UUID.randomUUID();
    private final UUID questionVersionId = UUID.randomUUID();

    @Test
    @SuppressWarnings("unchecked")
    void capturesActiveGoalAndContentKnowledgeSnapshotAtStart() {
        UUID goalId = UUID.randomUUID();
        UUID knowledgePointId = UUID.randomUUID();
        when(learningGoals.findActiveGoalId(userId)).thenReturn(Optional.of(goalId));
        when(knowledgeMappings.findByQuestionVersionIds(Set.of(questionVersionId))).thenReturn(Map.of(questionVersionId,
                List.of(new KnowledgeMappingProvider.KnowledgePointWeight(knowledgePointId, new BigDecimal("0.50")))));

        useCase().execute(command());

        ArgumentCaptor<AssessmentAttempt> attempt = ArgumentCaptor.forClass(AssessmentAttempt.class);
        verify(attempts).save(attempt.capture());
        assertEquals(goalId, attempt.getValue().getLearningGoalId());
        ArgumentCaptor<List<AttemptItem>> savedItems = ArgumentCaptor.forClass(List.class);
        verify(items).saveAll(savedItems.capture());
        ArgumentCaptor<List<AttemptItemKnowledgePoint>> snapshot = ArgumentCaptor.forClass(List.class);
        verify(knowledgeSnapshot).saveAll(snapshot.capture());
        assertEquals(List.of(new AttemptItemKnowledgePoint(savedItems.getValue().getFirst().id(), knowledgePointId,
                new BigDecimal("0.50"))), snapshot.getValue());
    }

    @Test
    void learnerWithoutActiveGoalStartsAnUnattributedAttempt() {
        when(learningGoals.findActiveGoalId(userId)).thenReturn(Optional.empty());
        when(knowledgeMappings.findByQuestionVersionIds(any())).thenReturn(Map.of());

        useCase().execute(command());

        ArgumentCaptor<AssessmentAttempt> attempt = ArgumentCaptor.forClass(AssessmentAttempt.class);
        verify(attempts).save(attempt.capture());
        assertNull(attempt.getValue().getLearningGoalId());
        verify(knowledgeSnapshot, never()).saveAll(any());
    }

    @Test
    void dependencyFailureDoesNotStartAnAttempt() {
        when(learningGoals.findActiveGoalId(userId)).thenThrow(new IllegalStateException("User Service unavailable"));

        assertThrows(IllegalStateException.class, () -> useCase().execute(command()));

        verifyNoInteractions(attempts, sections, items, knowledgeSnapshot);
    }

    private StartAssessmentAttemptCommand command() {
        return new StartAssessmentAttemptCommand(userId, UUID.randomUUID(), AttemptType.QUIZ, AttemptMode.STANDARD,
                AttemptChannel.WEB, null, List.of(new StartAssessmentAttemptCommand.SectionInput(UUID.randomUUID(), 0,
                "{}", List.of(new StartAssessmentAttemptCommand.ItemInput(questionVersionId, 0, "{}", null, null)))));
    }

    private StartAssessmentAttemptUseCase useCase() {
        return new StartAssessmentAttemptUseCase(attempts, sections, items, knowledgeSnapshot, learningGoals,
                knowledgeMappings);
    }
}
