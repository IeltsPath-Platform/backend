package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.port.AssessmentResultLog;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.ReviewReevaluation;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.aggregate.TopicTestAssignment;
import com.ieltspath.learning.domain.aggregate.LearnerPlacement;
import com.ieltspath.learning.domain.aggregate.CourseTestAssignment;
import com.ieltspath.learning.domain.aggregate.CourseProgress;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.LearnerCurriculumRepository;
import com.ieltspath.learning.domain.repository.TopicTestAssignmentRepository;
import com.ieltspath.learning.domain.repository.LearnerPlacementRepository;
import com.ieltspath.learning.domain.repository.CourseProgressRepository;
import com.ieltspath.learning.domain.repository.CourseTestAssignmentRepository;
import com.ieltspath.learning.domain.vo.BandLevel;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
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
    private static final Set<String> REVIEWED_TYPES = Set.of("TOPIC_GATE", "COURSE_GATE", "MOCK",
            "OFFICIAL_PRACTICE", "QUIZ");

    private final LearnerLock lock;
    private final KnowledgeEvidenceRepository evidence;
    private final LearnerCurriculumRepository curricula;
    private final AssessmentResultLog results;
    private final TopicTestAssignmentRepository assignments;
    private final ReviewReevaluation reviews;
    private final LearnerPlacementRepository placements;
    private final CourseProgressRepository courseProgress;
    private final CourseTestAssignmentRepository courseAssignments;
    private final Clock clock = Clock.systemUTC();

    public ApplyAssessmentResultUseCase(LearnerLock lock, KnowledgeEvidenceRepository evidence,
                                        LearnerCurriculumRepository curricula, AssessmentResultLog results,
                                        TopicTestAssignmentRepository assignments, ReviewReevaluation reviews,
                                        LearnerPlacementRepository placements, CourseProgressRepository courseProgress,
                                        CourseTestAssignmentRepository courseAssignments) {
        this.lock = lock;
        this.evidence = evidence;
        this.curricula = curricula;
        this.results = results;
        this.assignments = assignments;
        this.reviews = reviews;
        this.placements = placements;
        this.courseProgress = courseProgress;
        this.courseAssignments = courseAssignments;
    }

    @Transactional
    public void execute(AssessmentResult result) {
        UUID userId = result.userId();
        lock.lock(userId);
        Optional<Integer> applied = results.appliedVersion(userId, result.attemptId());
        if (applied.isPresent() && applied.get() >= result.resultVersion()) {
            log.info("Assessment result already applied: eventId={}, attemptId={}, version={}",
                    result.eventId(), result.attemptId(), result.resultVersion());
            return;
        }
        if (applied.isPresent()) evidence.removeAssessmentEvidence(userId, result.attemptId());
        results.recordVersion(userId, result.attemptId(), result.resultVersion());
        if ("PLACEMENT".equals(result.assessmentType())) {
            applyPlacement(result);
            return;
        }
        List<KnowledgeEvidence> judged = new ArrayList<>();
        Set<UUID> considered = new LinkedHashSet<>();
        Set<UUID> wrong = new HashSet<>();
        for (ItemResult item : result.items()) {
            for (KnowledgePointJudgment judgment : item.knowledgePoints()) {
                UUID kpId = judgment.knowledgePointId();
                considered.add(kpId);
                Boolean correct = item.correctnessFor(judgment);
                if (correct == null) continue;
                if (!correct) wrong.add(kpId);
                judged.add(KnowledgeEvidence.assessment(kpId, correct, LessonEvidenceReference.forAssessment(
                        result.resultId(), result.resultVersion(), item.itemResultId(), kpId),
                        result.attemptId(), result.resultVersion()));
            }
        }
        evidence.append(userId, judged);
        if ("TOPIC_GATE".equals(result.assessmentType())) applyTopicGate(result);
        if ("COURSE_GATE".equals(result.assessmentType())) applyCourseGate(result);
        if (REVIEWED_TYPES.contains(result.assessmentType())) reviews.execute(userId, considered, wrong);
    }

    private void applyPlacement(AssessmentResult result) {
        if (result.overallBand() == null) return;
        BandLevel band = new BandLevel(result.overallBand());
        var existing = placements.find(result.userId());
        if (existing.isEmpty()) {
            placements.save(LearnerPlacement.create(result.userId(), band, result.attemptId(), result.completedAt()));
        } else if (existing.get().record(band, result.attemptId(), result.completedAt())) {
            placements.save(existing.get());
        }
    }

    /** Only the first completed attempt after an assignment consumes it; 70% or more passes the topic. */
    private void applyTopicGate(AssessmentResult result) {
        if (result.packageVersionId() == null) {
            log.info("Topic gate result without package version: eventId={}", result.eventId());
            return;
        }
        var open = assignments.findOpenForAttempt(result.userId(), result.packageVersionId(), result.completedAt());
        if (open.isEmpty()) {
            log.info("Topic gate result without an open assignment: eventId={}", result.eventId());
            return;
        }
        BigDecimal score = result.items().stream().map(ItemResult::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = result.items().stream().map(ItemResult::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(max, 4, RoundingMode.HALF_UP);
        TopicTestAssignment assignment = open.get();
        boolean passed = assignment.consume(result.attemptId(), percent);
        assignments.save(assignment);
        if (passed) {
            LearnerCurriculum curriculum = curricula.find(result.userId());
            if (curriculum.pass(assignment.topicId(), clock.instant())) curricula.save(curriculum);
        }
    }

    private void applyCourseGate(AssessmentResult result) {
        if (result.packageVersionId() == null) {
            log.info("Course gate result without package version: eventId={}", result.eventId());
            return;
        }
        var open = courseAssignments.findOpenForAttempt(result.userId(), result.packageVersionId(), result.completedAt());
        if (open.isEmpty()) {
            log.info("Course gate result without an open assignment: eventId={}", result.eventId());
            return;
        }
        BigDecimal score = result.items().stream().map(ItemResult::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = result.items().stream().map(ItemResult::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(max, 4, RoundingMode.HALF_UP);
        CourseTestAssignment assignment = open.get();
        boolean passed = assignment.consume(result.attemptId(), percent);
        courseAssignments.save(assignment);
        if (passed) {
            CourseProgress progress = courseProgress.findAll(result.userId()).get(assignment.courseId());
            if (progress == null) {
                progress = CourseProgress.restore(result.userId(), assignment.courseId(), null);
            }
            if (progress.pass(clock.instant())) courseProgress.save(progress);
        }
    }
}
