package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.SaveAttemptResponseCommand;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.entity.AttemptResponse;
import com.ieltspath.assessment.domain.exception.AttemptExpiredException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.repository.AttemptResponseRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.vo.AttemptChannel;
import com.ieltspath.assessment.domain.vo.AttemptMode;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveAttemptResponseUseCaseTest {
    @Test
    void savesNextRevision() {
        Fixture fixture = new Fixture(AttemptType.QUIZ, false);
        when(fixture.responses.findByAttemptItemId(fixture.itemId)).thenReturn(Optional.of(
                new AttemptResponse(UUID.randomUUID(), fixture.itemId, "{}", 1, 2, Instant.now(), null)));
        when(fixture.responses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = fixture.useCase.execute(fixture.command());

        assertEquals(3, result.revision());
        verify(fixture.responses).save(any());
    }

    @Test
    void acceptsAnswerSaveAfterOfficialPracticeDeadline() {
        Fixture fixture = new Fixture(AttemptType.OFFICIAL_PRACTICE, true);
        when(fixture.responses.findByAttemptItemId(fixture.itemId)).thenReturn(Optional.empty());
        when(fixture.responses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = fixture.useCase.execute(fixture.command());

        assertEquals(1, result.revision());
        verify(fixture.responses).save(any());
    }

    @Test
    void rejectsAnswerSaveAfterNonPracticeDeadline() {
        Fixture fixture = new Fixture(AttemptType.TOPIC_GATE, true);

        assertThrows(AttemptExpiredException.class, () -> fixture.useCase.execute(fixture.command()));

        verify(fixture.responses, org.mockito.Mockito.never()).save(any());
    }

    private final class Fixture {
        private final UUID userId = UUID.randomUUID();
        private final UUID attemptId = UUID.randomUUID();
        private final UUID itemId = UUID.randomUUID();
        private final AssessmentAttemptRepository attempts = mock(AssessmentAttemptRepository.class);
        private final AttemptItemRepository items = mock(AttemptItemRepository.class);
        private final AttemptResponseRepository responses = mock(AttemptResponseRepository.class);
        private final AttemptSectionRepository sections = mock(AttemptSectionRepository.class);
        private final SaveAttemptResponseUseCase useCase = new SaveAttemptResponseUseCase(attempts, items, responses, sections);

        private Fixture(AttemptType type, boolean expired) {
            Instant now = Instant.now();
            var attempt = new AssessmentAttempt(attemptId, userId, UUID.randomUUID(), type, AttemptMode.TIMED,
                    AttemptChannel.WEB, AttemptStatus.IN_PROGRESS, now.minusSeconds(120), null,
                    expired ? now.minusSeconds(60) : now.plusSeconds(60), 0, now.minusSeconds(120), now.minusSeconds(120));
            when(attempts.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));
            when(items.findByIdAndAttemptId(itemId, attemptId)).thenReturn(Optional.of(
                    new AttemptItem(itemId, UUID.randomUUID(), UUID.randomUUID(), 1, "{}", null, null)));
        }

        private SaveAttemptResponseCommand command() {
            return new SaveAttemptResponseCommand(userId, attemptId, itemId, "{\"answer\":\"A\"}", 1, 0);
        }
    }
}
