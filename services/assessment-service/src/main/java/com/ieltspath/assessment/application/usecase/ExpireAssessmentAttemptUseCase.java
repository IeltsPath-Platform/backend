package com.ieltspath.assessment.application.usecase;
import com.ieltspath.assessment.application.result.AssessmentAttemptResult;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException; import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.Instant; import java.util.UUID;
@Service public class ExpireAssessmentAttemptUseCase {
 private final AssessmentAttemptRepository repository; public ExpireAssessmentAttemptUseCase(AssessmentAttemptRepository repository){this.repository=repository;}
 @Transactional public AssessmentAttemptResult execute(UUID userId,UUID id){var a=repository.findByIdAndUserId(id,userId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found")); a.expire(Instant.now()); var saved=repository.save(a); return AssessmentAttemptResult.from(saved);}
}
