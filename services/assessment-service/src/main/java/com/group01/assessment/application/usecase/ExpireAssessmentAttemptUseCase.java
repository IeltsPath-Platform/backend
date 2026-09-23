package com.group01.assessment.application.usecase;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException; import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.Instant; import java.util.UUID;
@Service public class ExpireAssessmentAttemptUseCase {
 private final AssessmentAttemptRepository repository; public ExpireAssessmentAttemptUseCase(AssessmentAttemptRepository repository){this.repository=repository;}
 @Transactional public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found")); a.expire(Instant.now()); var saved=repository.save(a); return new AssessmentAttemptResult(saved.getId(),saved.getUserId(),saved.getPackageVersionId(),saved.getAttemptType(),saved.getMode(),saved.getChannel(),saved.getStatus(),saved.getStartedAt(),saved.getSubmittedAt(),saved.getExpiresAt(),saved.getRowVersion(),saved.getCreatedAt(),saved.getUpdatedAt());}
}
