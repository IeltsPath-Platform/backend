package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.AssessmentAttemptResult;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * The learner's most recent placement attempt in any status, so a client can resume or show its result. Empty when the
 * learner never started one, which is the normal state of a new account rather than an error.
 */
@Service
public class GetCurrentPlacementAttemptUseCase {
    private final AssessmentAttemptRepository attempts;

    public GetCurrentPlacementAttemptUseCase(AssessmentAttemptRepository attempts) {
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public Optional<AssessmentAttemptResult> execute(UUID userId) {
        return attempts.findByUserId(userId).stream()
                .filter(attempt -> attempt.getAttemptType() == AttemptType.PLACEMENT)
                .max(Comparator.comparing(AssessmentAttempt::getCreatedAt))
                .map(a -> new AssessmentAttemptResult(a.getId(), a.getUserId(), a.getPackageVersionId(),
                        a.getAttemptType(), a.getMode(), a.getChannel(), a.getStatus(), a.getStartedAt(),
                        a.getSubmittedAt(), a.getExpiresAt(), a.getRowVersion(), a.getCreatedAt(), a.getUpdatedAt()));
    }
}
