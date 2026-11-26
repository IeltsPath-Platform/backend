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
    private final com.ieltspath.assessment.application.service.GradingRequestAnnouncer announcer;
    public CreateGradingJobUseCase(GradingJobRepository jobs,LearnerSubmissionRepository submissions,
            com.ieltspath.assessment.application.service.ExaminerAssignmentService assignments,
            com.ieltspath.assessment.application.service.GradingRequestAnnouncer announcer){this.jobs=jobs;this.submissions=submissions;this.assignments=assignments;this.announcer=announcer;}
    @Transactional public GradingJobResult execute(CreateGradingJobCommand c){
        var submission=submissions.findById(c.submissionId()).orElseThrow(()->new AssessmentNotFoundException("Không tìm thấy bài nộp của học viên."));
        if(!submission.userId().equals(c.userId())) throw new AssessmentAccessDeniedException("Bài nộp này không thuộc về tài khoản hiện tại.");
        if(c.skill()!=Skill.WRITING&&c.skill()!=Skill.SPEAKING) throw new InvalidAssessmentStateException("Chỉ hỗ trợ chấm bài kỹ năng Writing hoặc Speaking.");
        if(c.gradingMode()==GradingMode.AI&&c.pointCostSnapshot()==null) throw new InvalidAssessmentStateException("Chấm bằng AI yêu cầu thông tin mức điểm quy đổi.");
        if(c.gradingMode()==GradingMode.HUMAN&&c.examinerId()==null) throw new InvalidAssessmentStateException("Vui lòng chọn giám khảo trước khi gửi chấm.");
        if(c.gradingMode()==GradingMode.AI&&c.examinerId()!=null) throw new InvalidAssessmentStateException("Chế độ chấm AI không hỗ trợ chọn giám khảo.");
        if(c.gradingMode()==GradingMode.HUMAN) assignments.requireAvailable(c.examinerId(),1);
        var now=Instant.now();
        var saved=jobs.save(new GradingJob(UUID.randomUUID(),c.submissionId(),c.userId(),c.skill(),c.gradingMode(),GradingJobStatus.QUEUED,c.pointCostSnapshot(),c.idempotencyKey(),now,null,
                c.gradingMode()==GradingMode.HUMAN?c.examinerId():null,c.gradingMode()==GradingMode.HUMAN?now:null));
        // The examiner the learner chose is told in the same transaction, like every other way of choosing one.
        if(c.gradingMode()==GradingMode.HUMAN) announcer.announce(c.examinerId(),saved.id(),c.skill().name(),now);
        return new GradingJobResult(saved.id(),saved.submissionId(),saved.userId(),saved.skill(),saved.gradingMode(),saved.status(),saved.pointCostSnapshot(),saved.idempotencyKey(),saved.createdAt(),saved.completedAt());
    }
}
