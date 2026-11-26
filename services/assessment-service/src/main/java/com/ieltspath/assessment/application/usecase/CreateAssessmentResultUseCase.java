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
    private final ExaminerGradingPolicy examinerPolicy;
    public CreateAssessmentResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                                          ExaminerGradingPolicy examinerPolicy){this.attempts=attempts;this.results=results;this.examinerPolicy=examinerPolicy;}
    /** Grader entry (EXAMINER, enforced by the controller): any learner's attempt. */
    @Transactional
    public AssessmentResultResult executeForGrader(UUID attemptId, Double overallBand){
        var attempt=attempts.findById(attemptId).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy lượt làm bài thi. Vui lòng kiểm tra lại."));
        examinerPolicy.requireAssignment(attemptId);
        return openNextVersion(attempt,overallBand);
    }
    private AssessmentResultResult openNextVersion(AssessmentAttempt attempt, Double overallBand){
        if(attempt.getStatus()!=AttemptStatus.SUBMITTED) throw new InvalidAssessmentStateException("Chỉ có thể tạo kết quả cho lượt làm bài đã nộp.");
        var latest=results.findLatestByAttemptId(attempt.getId());
        if(latest.isPresent()&&latest.get().isGradable()) throw new InvalidAssessmentStateException("Phiên bản kết quả gần nhất vẫn đang được chấm.");
        int version=latest.map(r->r.resultVersion()+1).orElse(1);
        var saved=results.save(new AssessmentResult(UUID.randomUUID(),attempt.getId(),version,AssessmentResult.DRAFT,overallBand,null));
        if (latest.isPresent() && latest.get().isCompleted()) examinerPolicy.prepareRegrade(attempt.getId());
        return new AssessmentResultResult(saved.id(),saved.attemptId(),saved.resultVersion(),saved.status(),saved.overallBand(),saved.completedAt());
    }
}
