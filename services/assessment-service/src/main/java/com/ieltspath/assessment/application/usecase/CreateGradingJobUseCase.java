package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.command.CreateGradingJobCommand;
import com.ieltspath.assessment.application.result.GradingJobResult;
import com.ieltspath.assessment.domain.entity.GradingJob;
import com.ieltspath.assessment.domain.exception.*;
import com.ieltspath.assessment.domain.repository.*;
import com.ieltspath.assessment.domain.vo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.UUID;

@Service public class CreateGradingJobUseCase {
    private final GradingJobRepository jobs; private final LearnerSubmissionRepository submissions;
    private final com.ieltspath.assessment.application.service.ExaminerAssignmentService assignments;
    public CreateGradingJobUseCase(GradingJobRepository jobs,LearnerSubmissionRepository submissions,
            com.ieltspath.assessment.application.service.ExaminerAssignmentService assignments){this.jobs=jobs;this.submissions=submissions;this.assignments=assignments;}
    @Transactional public GradingJobResult execute(CreateGradingJobCommand c){
        var submission=submissions.findById(c.submissionId()).orElseThrow(()->new AssessmentNotFoundException("Learner submission not found"));
        if(!submission.userId().equals(c.userId())) throw new AssessmentAccessDeniedException("Submission does not belong to current user");
        if(c.skill()!=Skill.WRITING&&c.skill()!=Skill.SPEAKING) throw new InvalidAssessmentStateException("Only writing or speaking submissions can be graded");
        if(c.gradingMode()==GradingMode.AI&&c.pointCostSnapshot()==null) throw new InvalidAssessmentStateException("AI grading requires a point cost snapshot");
        if(c.gradingMode()==GradingMode.HUMAN&&c.examinerId()==null) throw new InvalidAssessmentStateException("Human grading requires the examiner the learner chose");
        if(c.gradingMode()==GradingMode.AI&&c.examinerId()!=null) throw new InvalidAssessmentStateException("AI grading has no examiner");
        if(c.gradingMode()==GradingMode.HUMAN) assignments.requireAvailable(c.examinerId(),1);
        var now=Instant.now();
        var saved=jobs.save(new GradingJob(UUID.randomUUID(),c.submissionId(),c.userId(),c.skill(),c.gradingMode(),GradingJobStatus.QUEUED,c.pointCostSnapshot(),c.idempotencyKey(),now,null,
                c.gradingMode()==GradingMode.HUMAN?c.examinerId():null,c.gradingMode()==GradingMode.HUMAN?now:null));
        return new GradingJobResult(saved.id(),saved.submissionId(),saved.userId(),saved.skill(),saved.gradingMode(),saved.status(),saved.pointCostSnapshot(),saved.idempotencyKey(),saved.createdAt(),saved.completedAt());
    }
}
