package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.CreateAssessmentResultCommand;
import com.group01.assessment.application.result.AssessmentResultResult;
import com.group01.assessment.domain.entity.AssessmentResult;
import com.group01.assessment.domain.exception.*;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.AttemptStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreateAssessmentResultUseCase {
    private final AssessmentAttemptRepository attempts; private final AssessmentResultRepository results;
    public CreateAssessmentResultUseCase(AssessmentAttemptRepository attempts, AssessmentResultRepository results){this.attempts=attempts;this.results=results;}
    @Transactional
    public AssessmentResultResult execute(CreateAssessmentResultCommand c){
        var attempt=attempts.findByIdAndUserId(c.attemptId(),c.userId()).orElseThrow(()->new AssessmentNotFoundException("Assessment attempt not found"));
        if(attempt.getStatus()!=AttemptStatus.SUBMITTED) throw new InvalidAssessmentStateException("Only submitted attempts can have a result");
        int version=results.findLatestByAttemptId(c.attemptId()).map(r->r.resultVersion()+1).orElse(1);
        var saved=results.save(new AssessmentResult(UUID.randomUUID(),c.attemptId(),version,"COMPLETED",c.overallBand(),Instant.now()));
        return new AssessmentResultResult(saved.id(),saved.attemptId(),saved.resultVersion(),saved.status(),saved.overallBand(),saved.completedAt());
    }
}
