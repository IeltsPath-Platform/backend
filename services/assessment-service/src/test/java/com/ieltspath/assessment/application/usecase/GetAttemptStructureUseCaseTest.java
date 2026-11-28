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

        var result = new GetAttemptStructureUseCase(attempts, sections, items, new ObjectMapper()).execute(userId, attemptId);
        var item = result.sections().getFirst().items().getFirst();
        assertEquals(itemId, item.id());
        assertEquals(questionVersionId, item.questionVersionId());
        assertEquals("{\"stem\":\"Question\"}", item.questionSnapshot());
        assertEquals("{\"kp\":\"KP1\"}", item.knowledgeSnapshot());
        assertFalse(new ObjectMapper().writeValueAsString(result).contains("answerSnapshot"));
    }

    @Test
    void givesOnlyTheLengthOfASpokenAnswer() throws Exception {
        UUID sectionId = UUID.randomUUID();
        when(attempts.findByIdAndUserId(attemptId, userId))
                .thenReturn(Optional.of(mock(AssessmentAttempt.class)));
        when(sections.findByAttemptId(attemptId))
                .thenReturn(List.of(new AttemptSection(sectionId, attemptId, UUID.randomUUID(), 1, "{}")));
        when(items.findByAttemptId(attemptId)).thenReturn(List.of(
                answered(sectionId, 1,
                        "{\"answerSpec\":{\"type\":\"SPEAKING\",\"durationSeconds\":120,\"secret\":\"x\"}}"),
                answered(sectionId, 2, "{\"answerSpec\":{\"type\":\"FILL\",\"durationSeconds\":30}}"),
                answered(sectionId, 3, "{\"answerSpec\":{\"type\":\"SPEAKING\"}}"),
                answered(sectionId, 4, null)));

        var result = new GetAttemptStructureUseCase(attempts, sections, items, new ObjectMapper())
                .execute(userId, attemptId);

        assertEquals(java.util.Arrays.asList(120, null, null, null), result.sections().getFirst().items().stream()
                .map(value -> value.answerSeconds()).toList());
        assertFalse(new ObjectMapper().findAndRegisterModules().writeValueAsString(result).contains("secret"));
    }

    private static AttemptItem answered(UUID sectionId, int sortOrder, String answerSnapshot) {
        return new AttemptItem(UUID.randomUUID(), sectionId, UUID.randomUUID(), sortOrder, "{}", answerSnapshot, "[]");
    }

    @Test
    void anotherUsersAttemptIsNotVisible() {
        when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.empty());

        assertThrows(AssessmentNotFoundException.class,
                () -> new GetAttemptStructureUseCase(attempts, sections, items, new ObjectMapper()).execute(userId, attemptId));
        verifyNoInteractions(sections, items);
    }
}
