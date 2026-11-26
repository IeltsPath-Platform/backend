package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.AssessmentAttemptResult;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CancelPlacementAttemptUseCase {
    private final AssessmentAttemptRepository attempts;

    public CancelPlacementAttemptUseCase(AssessmentAttemptRepository attempts) { this.attempts = attempts; }

    @Transactional
    public AssessmentAttemptResult execute(UUID userId, UUID attemptId) {
        var attempt = attempts.findByIdAndUserId(attemptId, userId)
                .orElseThrow(() -> new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));
        attempt.cancel(Instant.now());
        return AssessmentAttemptResult.from(attempts.save(attempt));
    }
}
