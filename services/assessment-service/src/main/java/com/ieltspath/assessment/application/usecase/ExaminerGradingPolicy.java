package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.ieltspath.assessment.application.port.EssayGradeComparisonStore;
import com.ieltspath.assessment.application.service.ExaminerQaService;
import com.ieltspath.assessment.domain.entity.ItemResult;
import com.ieltspath.assessment.domain.exception.*;
import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

/** Assignment rows stay locked until result details/finalization commit, excluding a concurrent reclaim. */
@Component
public class ExaminerGradingPolicy {
    private final ExaminerQueueStore queue;
    private final EssayGradeComparisonStore comparisons;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper json;
    private final ExaminerQaService examinerQa;
    public ExaminerGradingPolicy(ExaminerQueueStore queue,EssayGradeComparisonStore comparisons,
                                  CurrentUserProvider currentUser,ObjectMapper json, ExaminerQaService examinerQa) {
        this.queue=queue;this.comparisons=comparisons;this.currentUser=currentUser;this.json=json;
        this.examinerQa=examinerQa;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void prepareRegrade(UUID attempt) { queue.reopenCompleted(attempt); }

    @Transactional(propagation=Propagation.MANDATORY)
    public void requireAssignment(UUID attempt) {
        var jobs=queue.lockAttempt(attempt).stream().filter(job -> !"QA_SAMPLE".equals(job.reason())).toList();
        if (jobs.isEmpty()) return;
        UUID examiner=currentUser.requireUserId();
        if (jobs.stream().anyMatch(j -> !examiner.equals(j.assignedExaminerId())))
            throw new AssessmentAccessDeniedException("Only the assigned examiner may grade this attempt");
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void complete(UUID attempt,UUID result,List<ItemResult> items) {
        var jobs=queue.lockAttempt(attempt).stream().filter(job -> !"QA_SAMPLE".equals(job.reason())).toList();
        if (jobs.isEmpty() && items.stream().noneMatch(this::hasEssayBand)) return;
        UUID examiner=currentUser.requireUserId();
        if (jobs.stream().anyMatch(j -> !examiner.equals(j.assignedExaminerId())))
            throw new AssessmentAccessDeniedException("Only the assigned examiner may finalize this attempt");
        var byItem=items.stream().collect(Collectors.toMap(ItemResult::attemptItemId,x->x));
        for (var job:jobs) {
            if (!"COURSE_GATE".equals(job.attemptType()) || !"WRITING".equals(job.skill())) continue;
            var item=byItem.get(job.attemptItemId());
            try {
                var feedback=json.readTree(item==null?"{}":item.feedbackSnapshot());
                var band=feedback==null?null:feedback.get("band");
                if (band==null || !band.isNumber() || band.decimalValue().signum()<0
                        || band.decimalValue().compareTo(java.math.BigDecimal.valueOf(9))>0
                        || band.decimalValue().multiply(java.math.BigDecimal.valueOf(2)).stripTrailingZeros().scale()>0)
                    throw new InvalidAssessmentStateException("Each submitted course essay requires feedbackSnapshot.band (0..9 in half bands)");
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new InvalidAssessmentStateException("Essay feedback must be valid JSON with a band");
            }
        }
        comparisons.recordExaminer(attempt,result,examiner,jobs.stream().map(ExaminerQueueStore.QueueEntry::submissionId).distinct().toList());
        queue.completeAttempt(attempt,examiner);
        examinerQa.sampleCompletedEssays(jobs.stream().filter(job -> "WRITING".equals(job.skill()))
                .map(ExaminerQueueStore.QueueEntry::submissionId).distinct().toList());
    }
    private boolean hasEssayBand(ItemResult item) {
        try {
            var feedback=json.readTree(item.feedbackSnapshot());
            return feedback!=null && feedback.path("band").isNumber();
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) { return false; }
    }
}
