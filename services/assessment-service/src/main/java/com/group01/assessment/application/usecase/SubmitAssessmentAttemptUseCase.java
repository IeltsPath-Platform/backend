package com.group01.assessment.application.usecase;
import com.group01.assessment.application.command.SubmitAssessmentAttemptCommand;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.Instant;
@Service public class SubmitAssessmentAttemptUseCase {
 private final AssessmentAttemptRepository repository; public SubmitAssessmentAttemptUseCase(AssessmentAttemptRepository repository){this.repository=repository;}
 @Transactional public AssessmentAttemptResult execute(SubmitAssessmentAttemptCommand c){var a=repository.findByIdAndUserId(c.attemptId(),c.userId()).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found")); a.submit(Instant.now()); var saved=repository.save(a); return new AssessmentAttemptResult(saved.getId(),saved.getUserId(),saved.getPackageVersionId(),saved.getAttemptType(),saved.getMode(),saved.getChannel(),saved.getStatus(),saved.getStartedAt(),saved.getSubmittedAt(),saved.getExpiresAt(),saved.getRowVersion(),saved.getCreatedAt(),saved.getUpdatedAt());}
}
