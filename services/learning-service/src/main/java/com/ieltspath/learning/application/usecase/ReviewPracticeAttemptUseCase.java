package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.result.PracticeAttemptView;
import com.ieltspath.learning.application.service.PracticeAttemptViewAssembler;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.vo.PracticeSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Reopens a submitted practice attempt read-only: the questions and essays of the package version it was pinned to,
 * with the saved result. The answers in the result were already shown when the attempt was submitted.
 */
@Service
public class ReviewPracticeAttemptUseCase {
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final PracticeAttemptViewAssembler view;

    public ReviewPracticeAttemptUseCase(PracticeAttemptRepository attempts, LearningContentClient content,
                                        PracticeAttemptViewAssembler view) {
        this.attempts = attempts;
        this.content = content;
        this.view = view;
    }

    @Transactional(readOnly = true)
    public Result execute(UUID userId, UUID attemptId) {
        var attempt = attempts.findOwned(userId, attemptId)
                .orElseThrow(() -> new LearningRequestException(404, "NOT_FOUND", "Practice attempt was not found"));
        if (!attempt.submitted()) {
            throw new LearningRequestException(409, "ATTEMPT_NOT_SUBMITTED", "Practice attempt is still open");
        }
        return new Result(view.assemble(attempt, content.getPackageVersion(attempt.packageVersionId())),
                attempt.response());
    }

    public record Result(PracticeAttemptView attempt, PracticeSubmission result) {}
}
