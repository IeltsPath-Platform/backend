package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAttemptStructureUseCaseTest {
    @Mock AssessmentAttemptRepository attempts;
    @Mock AttemptSectionRepository sections;
    @Mock AttemptItemRepository items;

    private final UUID userId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();

    @Test
    void ownerGetsQuestionAndKnowledgeSnapshotsWithoutAnswer() throws Exception {
        UUID sectionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID questionVersionId = UUID.randomUUID();
        when(attempts.findByIdAndUserId(attemptId, userId))
                .thenReturn(Optional.of(mock(AssessmentAttempt.class)));
        when(sections.findByAttemptId(attemptId)).thenReturn(List.of(
                new AttemptSection(sectionId, attemptId, UUID.randomUUID(), 1, "{\"title\":\"Reading\"}")));
        when(items.findByAttemptId(attemptId)).thenReturn(List.of(
                new AttemptItem(itemId, sectionId, questionVersionId, 1,
                        "{\"stem\":\"Question\"}", "{\"correct\":\"A\"}", "{\"kp\":\"KP1\"}")));

        var result = new GetAttemptStructureUseCase(attempts, sections, items).execute(userId, attemptId);
        var item = result.sections().getFirst().items().getFirst();
        assertEquals(itemId, item.id());
        assertEquals(questionVersionId, item.questionVersionId());
        assertEquals("{\"stem\":\"Question\"}", item.questionSnapshot());
        assertEquals("{\"kp\":\"KP1\"}", item.knowledgeSnapshot());
        assertFalse(new ObjectMapper().writeValueAsString(result).contains("answerSnapshot"));
    }

    @Test
    void anotherUsersAttemptIsNotVisible() {
        when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class,
                () -> new GetAttemptStructureUseCase(attempts, sections, items).execute(userId, attemptId));
        verifyNoInteractions(sections, items);
    }
}
