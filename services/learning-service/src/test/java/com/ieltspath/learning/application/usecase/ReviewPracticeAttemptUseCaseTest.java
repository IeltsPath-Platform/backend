package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.port.LearningContentClient.PackageVersion;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.service.PracticeAttemptViewAssembler;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.vo.LearningSkill;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReviewPracticeAttemptUseCaseTest {
    private static final UUID USER = UUID.randomUUID();
    private static final UUID ATTEMPT = UUID.randomUUID();

    private final PracticeAttemptRepository attempts = mock(PracticeAttemptRepository.class);
    private final LearningContentClient content = mock(LearningContentClient.class);
    private final PracticeAttemptViewAssembler assembler = mock(PracticeAttemptViewAssembler.class);
    private final ReviewPracticeAttemptUseCase review = new ReviewPracticeAttemptUseCase(attempts, content, assembler);

    private static PracticeAttempt attempt(Instant submittedAt, PracticeSubmission response) {
        return PracticeAttempt.restore(ATTEMPT, USER, UUID.randomUUID(), LearningSkill.WRITING, UUID.randomUUID(),
                UUID.randomUUID(), Instant.now(), submittedAt, submittedAt == null ? null : UUID.randomUUID(), response);
    }

    @Test
    void aSubmittedAttemptComesBackWithItsPinnedVersionAndSavedResult() {
        var saved = new PracticeSubmission(ATTEMPT, 1, 1, 1.0, true, true, List.of(), null, List.of());
        PracticeAttempt submitted = attempt(Instant.now(), saved);
        PackageVersion version = mock(PackageVersion.class);
        PracticeAttemptView view = new PracticeAttemptView(ATTEMPT, submitted.packageId(),
                submitted.packageVersionId(), null, null, List.of());
        when(attempts.findOwned(USER, ATTEMPT)).thenReturn(Optional.of(submitted));
        when(content.getPackageVersion(submitted.packageVersionId())).thenReturn(version);
        when(assembler.assemble(submitted, version)).thenReturn(view);

        var result = review.execute(USER, ATTEMPT);

        assertThat(result.attempt()).isSameAs(view);
        assertThat(result.result()).isSameAs(saved);
        verify(content).getPackageVersion(submitted.packageVersionId());
    }

    @Test
    void anAttemptThatIsStillOpenCannotBeReviewed() {
        when(attempts.findOwned(USER, ATTEMPT)).thenReturn(Optional.of(attempt(null, null)));

        assertThatThrownBy(() -> review.execute(USER, ATTEMPT))
                .isInstanceOfSatisfying(LearningRequestException.class, error -> {
                    assertThat(error.getStatus()).isEqualTo(409);
                    assertThat(error.getCode()).isEqualTo("ATTEMPT_NOT_SUBMITTED");
                });
        verifyNoInteractions(content);
    }

    @Test
    void anAttemptOfAnotherLearnerIsNotFound() {
        when(attempts.findOwned(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> review.execute(USER, ATTEMPT))
                .isInstanceOfSatisfying(LearningRequestException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(404));
        verifyNoInteractions(content);
    }
}
