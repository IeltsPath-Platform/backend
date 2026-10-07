package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.repository.AssessmentAttemptRepository;
import com.ieltspath.assessment.domain.repository.AssessmentResultRepository;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.vo.GradingJobStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Completes the result of a topic or course test once the LLM has graded every essay that was sent: an essay passes
 * when its band reaches the item's pass band and an essay never sent scores zero. Nothing happens while a job is
 * still open, when a job failed (an examiner grades the attempt instead) or when a result already exists.
 */
@Component
public class GateResultAssembler {
    private final AssessmentAttemptRepository attempts;
    private final AssessmentResultRepository results;
    private final AttemptItemRepository attemptItems;
    private final GateEssayJobStore jobs;
    private final AutoGradeAttemptService autoGrader;
    private final ObjectMapper json;

    public GateResultAssembler(AssessmentAttemptRepository attempts, AssessmentResultRepository results,
                               AttemptItemRepository attemptItems, GateEssayJobStore jobs,
                               AutoGradeAttemptService autoGrader, ObjectMapper json) {
        this.attempts = attempts;
        this.results = results;
        this.attemptItems = attemptItems;
        this.jobs = jobs;
        this.autoGrader = autoGrader;
        this.json = json;
    }

    /** @return true when this call completed the result. */
    @Transactional
    public boolean completeIfGraded(UUID attemptId) {
        var attempt = attempts.findById(attemptId).orElse(null);
        if (attempt == null || results.findLatestForUpdateByAttemptId(attemptId).isPresent()) return false;
        List<GateEssayJobStore.JobState> states = jobs.aiJobs(attemptId);
        if (states.isEmpty() || states.stream()
                .anyMatch(state -> !GradingJobStatus.COMPLETED.name().equals(state.status()))) {
            return false;
        }
        Map<UUID, GateEssayJobStore.JobState> byItem = new HashMap<>();
        states.forEach(state -> byItem.put(state.attemptItemId(), state));
        Map<UUID, Boolean> essayPassed = new HashMap<>();
        for (AttemptItem item : attemptItems.findByAttemptId(attemptId)) {
            AnswerSnapshot.parse(json, item.answerSnapshot()).filter(AnswerSnapshot::gradableEssay).ifPresent(answer -> {
                var state = byItem.get(item.id());
                essayPassed.put(item.id(), state != null && state.band() != null
                        && state.band().compareTo(answer.passBand()) >= 0);
            });
        }
        return autoGrader.grade(attempt, essayPassed);
    }
}
