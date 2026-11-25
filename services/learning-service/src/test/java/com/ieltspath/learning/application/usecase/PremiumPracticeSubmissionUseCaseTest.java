package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.AccessClient;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient.Item;
import com.ieltspath.learning.application.service.EssaySubmissionFlow;
import com.ieltspath.learning.application.service.FreeWritingPrompts;
import com.ieltspath.learning.application.service.SpeakingAvailability;
import com.ieltspath.learning.application.service.SpeakingPracticeSettings;
import com.ieltspath.learning.application.service.SpeakingTestQuestions;
import com.ieltspath.learning.application.service.WritingSubmissionViewAssembler;
import com.ieltspath.learning.domain.repository.SpeakingSubmissionRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.vo.SpeakingScope;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PremiumPracticeSubmissionUseCaseTest {
    @Test
    void freeWritingSubmissionRejectsTaggedPromptBeforeStartingGrading() {
        var prompts = mock(FreeWritingPrompts.class);
        var flow = mock(EssaySubmissionFlow.class);
        var essays = mock(WritingSubmissionRepository.class);
        var lock = mock(LearnerLock.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        var view = mock(WritingSubmissionViewAssembler.class);
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID packageVersionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Item prompt = new Item(questionId, 1, "Write an essay", List.of(), Map.of("type", "ESSAY"), null, null,
                BigDecimal.ONE, List.of());
        when(flow.validate("An essay with words")).thenReturn(4);
        when(essays.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(prompts.resolve(packageVersionId, questionId))
                .thenReturn(new FreeWritingPrompts.ResolvedPrompt(prompt, "PREMIUM_PRACTICE"));
        when(flow.premiumStatus()).thenReturn(false);

        var useCase = new SubmitFreeWritingUseCase(prompts, lock, essays, flow, transactionManager, view);
        LearningRequestException exception = assertThrows(LearningRequestException.class,
                () -> useCase.execute(userId, packageVersionId, questionId, requestId, "An essay with words"));

        assertEquals("PREMIUM_REQUIRED", exception.getCode());
        verifyNoInteractions(lock, transactionManager, view);
    }

    @Test
    void freeSpeakingSubmissionRejectsTaggedTestBeforeOpeningSubmission() {
        var submissions = mock(SpeakingSubmissionRepository.class);
        var questions = mock(SpeakingTestQuestions.class);
        var points = mock(AccessClient.class);
        var lock = mock(LearnerLock.class);
        var settings = mock(SpeakingPracticeSettings.class);
        var availability = mock(SpeakingAvailability.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        UUID packageVersionId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(availability.isAvailable()).thenReturn(true);
        when(submissions.findByRequestId(requestId)).thenReturn(Optional.empty());
        when(questions.resolve(packageVersionId, List.of())).thenReturn(
                new SpeakingTestQuestions.Resolved(SpeakingScope.FULL, List.of(), "PREMIUM_PRACTICE"));
        when(points.premium()).thenReturn(false);

        var useCase = new CreateSpeakingSubmissionUseCase(submissions, questions, points, lock, settings,
                availability, transactionManager);
        LearningRequestException exception = assertThrows(LearningRequestException.class,
                () -> useCase.execute(UUID.randomUUID(), requestId, packageVersionId, List.of(), true));

        assertEquals("PREMIUM_REQUIRED", exception.getCode());
        verifyNoInteractions(lock, transactionManager);
    }
}
