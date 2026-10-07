package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.result.AssessmentResultResult;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AssessmentResult;
import com.ieltspath.assessment.domain.exception.*;
import com.ieltspath.assessment.domain.repository.*;
import com.ieltspath.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Opens the next result version for grading. Creating a result does not finalize it: the version stays DRAFT
 * until {@link FinalizeAssessmentResultUseCase} completes it, and a regrade opens a new version.
 */
@Service
public class CreateAssessmentResultUseCase {
    private final AssessmentAttemptRepository attempts; private final AssessmentResultRepository results;
    public CreateAssessmentResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results){this.attempts=attempts;this.results=results;}
    /** Grader entry (EXAMINER/ADMIN, enforced by the controller): any learner's attempt. */
    @Transactional
    public AssessmentResultResult executeForGrader(UUID attemptId, Double overallBand){
        var attempt=attempts.findById(attemptId).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found"));
        return openNextVersion(attempt,overallBand);
    }
    private AssessmentResultResult openNextVersion(AssessmentAttempt attempt, Double overallBand){
        if(attempt.getStatus()!=AttemptStatus.SUBMITTED) throw new InvalidAssessmentStateException("Only submitted attempts can have a result");
        var latest=results.findLatestByAttemptId(attempt.getId());
        if(latest.isPresent()&&latest.get().isGradable()) throw new InvalidAssessmentStateException("The latest result version is still being graded");
        int version=latest.map(r->r.resultVersion()+1).orElse(1);
        var saved=results.save(new AssessmentResult(UUID.randomUUID(),attempt.getId(),version,AssessmentResult.DRAFT,overallBand,null));
        return new AssessmentResultResult(saved.id(),saved.attemptId(),saved.resultVersion(),saved.status(),saved.overallBand(),saved.completedAt());
    }
}
