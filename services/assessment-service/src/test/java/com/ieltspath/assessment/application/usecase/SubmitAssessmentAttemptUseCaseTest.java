package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AttemptSectionRepository;
import com.ieltspath.assessment.domain.vo.*;
import org.junit.jupiter.api.Test;

import com.ieltspath.assessment.domain.entity.AttemptSection;
import com.ieltspath.assessment.domain.exception.AttemptExpiredException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubmitAssessmentAttemptUseCaseTest {
    @Test
    void repeatedSubmitDoesNotEnqueueGateEssayWorkTwice() {
        UUID userId = UUID.randomUUID();
        UUID attemptId = UUID.randomUUID();
        Instant now = Instant.now();
        AssessmentAttempt attempt = new AssessmentAttempt(attemptId, userId, UUID.randomUUID(),
                AttemptType.TOPIC_GATE, AttemptMode.STANDARD, AttemptChannel.WEB, AttemptStatus.IN_PROGRESS,
                now, null, null, 0, now, now);
        AssessmentAttemptRepository repository = mock(AssessmentAttemptRepository.class);
        EnqueueGateEssayGradingService gateGrading = mock(EnqueueGateEssayGradingService.class);
        when(repository.findByIdAndUserId(attemptId, userId)).thenReturn(Optional.of(attempt));
        when(repository.save(attempt)).thenReturn(attempt);
        SubmitAssessmentAttemptUseCase submit = new SubmitAssessmentAttemptUseCase(repository, gateGrading,
                mock(AttemptSectionRepository.class));
        SubmitAssessmentAttemptCommand command = new SubmitAssessmentAttemptCommand(userId, attemptId);

        assertEquals(AttemptStatus.SUBMITTED, submit.execute(command).status());
        assertEquals(AttemptStatus.SUBMITTED, submit.execute(command).status());

        verify(repository, times(1)).save(attempt);
        verify(gateGrading, times(1)).enqueueOrGrade(attempt);
    }

    @Test
    void sectionTimeIsStoredOnlyWhenTheSubmitGoesThrough() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        UUID sectionId = UUID.randomUUID();
        UUID otherAttemptSection = UUID.randomUUID();
        AssessmentAttempt open = new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(),
                AttemptType.OFFICIAL_PRACTICE, AttemptMode.TIMED, AttemptChannel.WEB, AttemptStatus.IN_PROGRESS,
                now.minusSeconds(60), null, now.plusSeconds(600), 0, now, now);
        AssessmentAttempt late = new AssessmentAttempt(UUID.randomUUID(), userId, UUID.randomUUID(),
                AttemptType.OFFICIAL_PRACTICE, AttemptMode.TIMED, AttemptChannel.WEB, AttemptStatus.IN_PROGRESS,
                now.minusSeconds(900), null, now.minusSeconds(300), 0, now, now);
        AssessmentAttemptRepository repository = mock(AssessmentAttemptRepository.class);
        AttemptSectionRepository sections = mock(AttemptSectionRepository.class);
        when(repository.findByIdAndUserId(open.getId(), userId)).thenReturn(Optional.of(open));
        when(repository.findByIdAndUserId(late.getId(), userId)).thenReturn(Optional.of(late));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sections.findByAttemptId(open.getId())).thenReturn(List.of(section(open.getId(), sectionId)));
        SubmitAssessmentAttemptUseCase submit = new SubmitAssessmentAttemptUseCase(repository,
                mock(EnqueueGateEssayGradingService.class), sections);

        submit.execute(new SubmitAssessmentAttemptCommand(userId, open.getId(), List.of(
                new SubmitAssessmentAttemptCommand.SectionTime(sectionId, 95),
                new SubmitAssessmentAttemptCommand.SectionTime(otherAttemptSection, 10))));
        assertThrows(AttemptExpiredException.class, () -> submit.execute(new SubmitAssessmentAttemptCommand(userId,
                late.getId(), List.of(new SubmitAssessmentAttemptCommand.SectionTime(sectionId, 95)))));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<AttemptSection>> saved = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(sections, times(1)).saveAll(saved.capture());
        assertEquals(1, saved.getValue().size());
        assertEquals(Integer.valueOf(95), saved.getValue().getFirst().timeSpentSeconds());
        verify(sections, never()).save(any());
    }

    private static AttemptSection section(UUID attemptId, UUID sectionId) {
        return new AttemptSection(sectionId, attemptId, UUID.randomUUID(), 1, "{}", null, null, 1, 0);
    }
}
