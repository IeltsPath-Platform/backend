package com.group01.assessment.application.usecase;

import com.group01.assessment.application.command.CreateGradingJobCommand;
import com.group01.assessment.application.result.GradingJobResult;
import com.group01.assessment.domain.entity.GradingJob;
import com.group01.assessment.domain.exception.*;
import com.group01.assessment.domain.repository.*;
import com.group01.assessment.domain.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.UUID;

@Service public class CreateGradingJobUseCase {
    private final GradingJobRepository jobs; private final LearnerSubmissionRepository submissions;
    public CreateGradingJobUseCase(GradingJobRepository jobs,LearnerSubmissionRepository submissions){this.jobs=jobs;this.submissions=submissions;}
    @Transactional public GradingJobResult execute(CreateGradingJobCommand c){
        var submission=submissions.findById(c.submissionId()).orElseThrow(()->new AssessmentNotFoundException("Learner submission not found"));
        if(!submission.userId().equals(c.userId())) throw new AssessmentAccessDeniedException("Submission does not belong to current user");
        if(c.skill()!=Skill.WRITING&&c.skill()!=Skill.SPEAKING) throw new InvalidAssessmentStateException("Only writing or speaking submissions can be graded");
        if(c.gradingMode()==GradingMode.AI&&c.pointCostSnapshot()==null) throw new InvalidAssessmentStateException("AI grading requires a point cost snapshot");
        var saved=jobs.save(new GradingJob(UUID.randomUUID(),c.submissionId(),c.userId(),c.skill(),c.gradingMode(),GradingJobStatus.QUEUED,c.pointCostSnapshot(),c.idempotencyKey(),Instant.now(),null));
        return new GradingJobResult(saved.id(),saved.submissionId(),saved.userId(),saved.skill(),saved.gradingMode(),saved.status(),saved.pointCostSnapshot(),saved.idempotencyKey(),saved.createdAt(),saved.completedAt());
    }
}
