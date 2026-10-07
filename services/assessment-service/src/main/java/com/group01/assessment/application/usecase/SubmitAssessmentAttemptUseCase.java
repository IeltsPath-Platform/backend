package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.exception.AttemptExpiredException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import com.group01.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Submits an attempt and, when every item is objective, grades it in the same transaction. A repeated submit
 * returns the attempt unchanged and writes nothing. A late submit commits the EXPIRED state and then fails.
 */
@Service
public class SubmitAssessmentAttemptUseCase {
    private final AssessmentAttemptRepository repository;
    private final EnqueueGateEssayGradingService gateEssayGrading;

    public SubmitAssessmentAttemptUseCase(AssessmentAttemptRepository repository,
                                          EnqueueGateEssayGradingService gateEssayGrading) {
        this.repository = repository;
        this.gateEssayGrading = gateEssayGrading;
    }

    @Transactional(noRollbackFor = AttemptExpiredException.class)
    public AssessmentAttemptResult execute(SubmitAssessmentAttemptCommand c) {
        var attempt = repository.findByIdAndUserId(c.attemptId(), c.userId())
                .orElseThrow(() -> new AssessmentNotFoundException("Assessment attempt not found"));
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            return AssessmentAttemptResult.from(attempt);
        }
        try {
            attempt.submit(Instant.now());
        } catch (AttemptExpiredException expired) {
            repository.save(attempt);
            throw expired;
        }
        var saved = repository.save(attempt);
        gateEssayGrading.enqueueOrGrade(saved);
        return AssessmentAttemptResult.from(saved);
    }
}
