package com.ieltspath.assessment.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.GateEssayJobStore;
import com.ieltspath.assessment.domain.aggregate.AssessmentAttempt;
import com.ieltspath.assessment.domain.entity.AttemptItem;
import com.ieltspath.assessment.domain.repository.AttemptItemRepository;
import com.ieltspath.assessment.domain.service.AnswerSpecGrader;
import com.ieltspath.assessment.domain.vo.AttemptType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * On submit of a topic or course test that has essays: each essay the learner sent through
 * {@code POST /api/assessments/submissions} gets a free AI grading job; an essay never sent scores zero. When no essay
 * was sent the attempt is graded at once. Mock tests and other types keep the human path.
 */
@Service
public class EnqueueGateEssayGradingService {
    private static final Set<AttemptType> GATES = Set.of(AttemptType.TOPIC_GATE, AttemptType.COURSE_GATE);

    private final AttemptItemRepository attemptItems;
    private final GateEssayJobStore jobs;
    private final AutoGradeAttemptService autoGrader;
    private final ObjectMapper json;
    private final AnswerSpecGrader grader = new AnswerSpecGrader();

    public EnqueueGateEssayGradingService(AttemptItemRepository attemptItems, GateEssayJobStore jobs,
                                          AutoGradeAttemptService autoGrader, ObjectMapper json) {
        this.attemptItems = attemptItems;
        this.jobs = jobs;
        this.autoGrader = autoGrader;
        this.json = json;
    }

    public static String aiKey(UUID attemptItemId) {
        return "gate-essay:" + attemptItemId + ":ai";
    }

    public static String humanKey(UUID attemptItemId) {
        return "gate-essay:" + attemptItemId + ":human";
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueOrGrade(AssessmentAttempt attempt) {
        if (!GATES.contains(attempt.getAttemptType())) {
            autoGrader.gradeIfObjective(attempt);
            return;
        }
        List<UUID> essays = new ArrayList<>();
        for (AttemptItem item : attemptItems.findByAttemptId(attempt.getId())) {
            Optional<AnswerSnapshot> answer = AnswerSnapshot.parse(json, item.answerSnapshot());
            if (answer.isEmpty()) {
                autoGrader.gradeIfObjective(attempt);
                return;
            }
            if (answer.get().gradableEssay()) essays.add(item.id());
            else if (!answer.get().autoGradable(grader)) {
                autoGrader.gradeIfObjective(attempt);
                return;
            }
        }
        if (essays.isEmpty()) {
            autoGrader.gradeIfObjective(attempt);
            return;
        }
        Map<UUID, UUID> submitted = jobs.submittedEssays(essays);
        if (submitted.isEmpty()) {
            Map<UUID, Boolean> none = new HashMap<>();
            essays.forEach(item -> none.put(item, false));
            autoGrader.grade(attempt, none);
            return;
        }
        jobs.enqueueAi(submitted, attempt.getUserId());
    }
}
