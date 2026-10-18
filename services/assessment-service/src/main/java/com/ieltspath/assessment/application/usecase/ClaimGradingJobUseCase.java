package com.ieltspath.assessment.application.usecase;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.domain.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Service
public class ClaimGradingJobUseCase {
    private final ExaminerQueueStore store;
    public ClaimGradingJobUseCase(ExaminerQueueStore store) { this.store=store; }
    @Transactional
    public ExaminerQueueStore.QueueEntry execute(UUID examiner,UUID jobId) {
        var job=store.find(jobId).orElseThrow(()->new AssessmentNotFoundException("Grading job not found"));
        if (job.attemptId()!=null && !"QA_SAMPLE".equals(job.reason())) {
            var siblings=store.lockAttempt(job.attemptId());
            if (siblings.stream().filter(j -> !"QA_SAMPLE".equals(j.reason())).anyMatch(j -> !"COMPLETED".equals(j.status()) && j.assignedExaminerId()!=null
                    && !examiner.equals(j.assignedExaminerId()) && j.claimedAt()!=null
                    && !j.claimedAt().isBefore(Instant.now().minus(Duration.ofHours(24))))) {
                throw new JobAlreadyClaimedException();
            }
        }
        if (!store.claim(jobId,examiner)) throw new JobAlreadyClaimedException();
        return store.find(jobId).orElseThrow();
    }
}
