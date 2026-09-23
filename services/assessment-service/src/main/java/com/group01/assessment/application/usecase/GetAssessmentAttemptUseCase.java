package com.group01.assessment.application.usecase;
import com.group01.assessment.application.result.AssessmentAttemptResult;
import com.group01.assessment.domain.exception.AssessmentNotFoundException;
import com.group01.assessment.domain.repository.AssessmentAttemptRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.UUID;
@Service public class GetAssessmentAttemptUseCase {
 private final AssessmentAttemptRepository repository; public GetAssessmentAttemptUseCase(AssessmentAttemptRepository repository){this.repository=repository;}
 @Transactional(readOnly=true) public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found")); return new AssessmentAttemptResult(a.getId(),a.getUserId(),a.getPackageVersionId(),a.getAttemptType(),a.getMode(),a.getChannel(),a.getStatus(),a.getStartedAt(),a.getSubmittedAt(),a.getExpiresAt(),a.getRowVersion(),a.getCreatedAt(),a.getUpdatedAt());}
}
