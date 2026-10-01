package com.group01.learning.application.usecase;

import com.group01.learning.application.LessonEvidenceReference;
import com.group01.learning.application.ReviewReevaluation;
import com.group01.learning.application.command.AssessmentResult;
import com.group01.learning.application.command.AssessmentResult.ItemResult;
import com.group01.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.group01.learning.application.port.AssessmentResultStore;
import com.group01.learning.application.port.LearningProgressStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Applies one completed result version in a single transaction: mastery evidence, the final-test rule and review
 * insertion. Versions are idempotent per attempt; a regrade replaces the attempt's earlier evidence.
 */
@Service
public class ApplyAssessmentResultUseCase {
    private static final Logger log = LoggerFactory.getLogger(ApplyAssessmentResultUseCase.class);
    private static final Set<String> REVIEWED_TYPES = Set.of("TOPIC_GATE", "MOCK", "OFFICIAL_PRACTICE", "QUIZ");
    private static final BigDecimal PASS_PERCENT = BigDecimal.valueOf(70);

    private final LearningProgressStore progress;
    private final AssessmentResultStore results;
    private final ReviewReevaluation reviews;

    public ApplyAssessmentResultUseCase(LearningProgressStore progress, AssessmentResultStore results,
                                        ReviewReevaluation reviews) {
        this.progress = progress;
        this.results = results;
        this.reviews = reviews;
    }

    @Transactional
    public void apply(AssessmentResult result) {
        UUID userId = result.userId();
        progress.lockUser(userId);
        Optional<Integer> applied = results.appliedVersion(userId, result.attemptId());
        if (applied.isPresent() && applied.get() >= result.resultVersion()) {
            log.info("Assessment result already applied: eventId={}, attemptId={}, version={}",
                    result.eventId(), result.attemptId(), result.resultVersion());
            return;
        }
        if (applied.isPresent()) results.removeAttemptEvidence(userId, result.attemptId());
        results.recordVersion(userId, result.attemptId(), result.resultVersion());
        if ("PLACEMENT".equals(result.assessmentType())) return;

        List<AssessmentResultStore.Evidence> evidence = new ArrayList<>();
        Set<UUID> considered = new LinkedHashSet<>();
        Set<UUID> wrong = new HashSet<>();
        for (ItemResult item : result.items()) {
            for (KnowledgePointJudgment judgment : item.knowledgePoints()) {
                UUID kpId = judgment.knowledgePointId();
                considered.add(kpId);
                Boolean correct = item.correctnessFor(judgment);
                if (correct == null) continue;
                if (!correct) wrong.add(kpId);
                evidence.add(new AssessmentResultStore.Evidence(kpId, correct, LessonEvidenceReference.forAssessment(
                        result.resultId(), result.resultVersion(), item.itemResultId(), kpId)));
            }
        }
        results.appendEvidence(userId, result.attemptId(), result.resultVersion(), evidence);

        if ("TOPIC_GATE".equals(result.assessmentType())) applyTopicGate(result);
        if (REVIEWED_TYPES.contains(result.assessmentType())) reviews.execute(userId, considered, wrong);
    }

    /** Only the first completed attempt after an assignment consumes it; 70% or more passes the topic. */
    private void applyTopicGate(AssessmentResult result) {
        if (result.packageVersionId() == null) {
            log.info("Topic gate result without package version: eventId={}", result.eventId());
            return;
        }
        var assignment = results.findOpenAssignment(result.userId(), result.packageVersionId(), result.completedAt());
        if (assignment.isEmpty()) {
            log.info("Topic gate result without an open assignment: eventId={}", result.eventId());
            return;
        }
        BigDecimal score = result.items().stream().map(ItemResult::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = result.items().stream().map(ItemResult::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(max, 4, RoundingMode.HALF_UP);
        results.consumeAssignment(assignment.get().assignmentId(), result.attemptId(), percent.doubleValue());
        if (percent.compareTo(PASS_PERCENT) >= 0) results.passTopic(result.userId(), assignment.get().topicId());
    }
}
