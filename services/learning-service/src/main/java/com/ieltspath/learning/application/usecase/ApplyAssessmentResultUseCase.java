package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.command.AssessmentResult;
import com.ieltspath.learning.application.command.AssessmentResult.ItemResult;
import com.ieltspath.learning.application.command.AssessmentResult.KnowledgePointJudgment;
import com.ieltspath.learning.application.port.AssessmentResultLog;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.QuestionEventLog;
import com.ieltspath.learning.application.service.CourseOutcomeService;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.domain.vo.QuestionAttemptEvent;
import com.ieltspath.learning.domain.vo.QuestionEventSource;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.ReviewReevaluation;
import com.ieltspath.learning.domain.aggregate.LearnerCurriculum;
import com.ieltspath.learning.domain.aggregate.TopicTestAssignment;
import com.ieltspath.learning.domain.aggregate.LearnerPlacement;
import com.ieltspath.learning.domain.aggregate.CourseTestAssignment;
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
 * Applies one completed result version in a single transaction: mastery evidence, the topic and course test records,
 * review insertion and what a test means for the course. Versions are idempotent per attempt; a regrade replaces the attempt's earlier evidence.
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
    @org.springframework.beans.factory.annotation.Autowired
    private com.ieltspath.learning.domain.repository.PracticeTestAssignmentRepository practiceAssignments;
    private final QuestionEventLog questionEvents;
    private final MasteryProfileUpdater masteryProfiles;
    private final CourseOutcomeService outcomes;
    private final Clock clock = Clock.systemUTC();

    public ApplyAssessmentResultUseCase(LearnerLock lock, KnowledgeEvidenceRepository evidence,
                                        LearnerCurriculumRepository curricula, AssessmentResultLog results,
                                        TopicTestAssignmentRepository assignments, ReviewReevaluation reviews,
                                        LearnerPlacementRepository placements, CourseProgressRepository courseProgress,
                                        CourseTestAssignmentRepository courseAssignments,
                                        QuestionEventLog questionEvents, MasteryProfileUpdater masteryProfiles,
                                        CourseOutcomeService outcomes) {
        this.outcomes = outcomes;
        this.questionEvents = questionEvents;
        this.masteryProfiles = masteryProfiles;
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
        Set<UUID> regradedKps = Set.of();
        if (applied.isPresent()) {
            regradedKps = evidence.removeAssessmentEvidence(userId, result.attemptId());
            questionEvents.removeAssessmentEvents(userId, result.attemptId());
        }
        results.recordVersion(userId, result.attemptId(), result.resultVersion());
        // Every assessment type logs its items, placement included. An item the grader did not judge keeps a null correct.
        List<QuestionAttemptEvent> items = new ArrayList<>();
        for (ItemResult item : result.items()) {
            items.add(new QuestionAttemptEvent(userId, item.questionVersionId(),
                    item.knowledgePoints().stream().map(KnowledgePointJudgment::knowledgePointId).distinct().toList(),
                    item.isCorrect(), item.score(), item.maxScore(), item.durationMs(), item.errorCode(),
                    QuestionEventSource.ASSESSMENT, item.itemResultId(), result.attemptId(), true));
        }
        questionEvents.append(items);
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
        if (applied.isPresent()) {
            // A regrade replaced earlier evidence, including points the new result no longer mentions.
            Set<UUID> affected = new HashSet<>(regradedKps);
            judged.forEach(item -> affected.add(item.knowledgePointId()));
            masteryProfiles.rebuild(userId, affected);
        } else {
            masteryProfiles.recordEvidence(userId, judged);
        }
        if ("OFFICIAL_PRACTICE".equals(result.assessmentType())) {
            if (applied.isEmpty() && !applyPracticeTest(result)) {
                // A free practice test counts for nothing, but a wrong answer can still offer an optional review.
                reviews.executeOptional(userId, considered, wrong, "FREE_PRACTICE_TEST");
            }
            // A practice test never creates a blocking review.
            return;
        }
        // The gates record the test; what it means for the course needs the reviews of the wrong points, so it runs last.
        Runnable followUp = () -> { };
        if ("TOPIC_GATE".equals(result.assessmentType())) followUp = applyTopicGate(result, wrong);
        if ("COURSE_GATE".equals(result.assessmentType())) followUp = applyCourseGate(result, wrong);
        if (REVIEWED_TYPES.contains(result.assessmentType())) reviews.execute(userId, considered, wrong);
        followUp.run();
    }

    /** True when the result belongs to a course milestone assignment; false for a free practice test. */
    private boolean applyPracticeTest(AssessmentResult result) {
        if (result.packageVersionId() == null) return false;
        var open = practiceAssignments.findOpenForAttempt(result.userId(), result.packageVersionId(), result.completedAt());
        if (open.isEmpty()) return false;
        BigDecimal score = result.items().stream().map(ItemResult::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = result.items().stream().map(ItemResult::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(max, 4, RoundingMode.HALF_UP);
        var assignment = open.get();
        if (assignment.consume(result.attemptId(), result.completedAt(), percent)) practiceAssignments.save(assignment);
        return true;
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

    /**
     * Only the first completed attempt after an assignment consumes it. The first topic test ever taken finishes the topic
     * and is its mid-course score, whatever the percent; 70% or more also passes the topic. Returns the follow-up that
     * needs the reviews of this result.
     */
    private Runnable applyTopicGate(AssessmentResult result, Set<UUID> wrong) {
        if (result.packageVersionId() == null) {
            log.info("Topic gate result without package version: eventId={}", result.eventId());
            return () -> { };
        }
        var open = assignments.findOpenForAttempt(result.userId(), result.packageVersionId(), result.completedAt());
        if (open.isEmpty()) {
            log.info("Topic gate result without an open assignment: eventId={}", result.eventId());
            return () -> { };
        }
        BigDecimal score = result.items().stream().map(ItemResult::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = result.items().stream().map(ItemResult::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(max, 4, RoundingMode.HALF_UP);
        TopicTestAssignment assignment = open.get();
        boolean passed = assignment.consume(result.attemptId(), percent);
        assignments.save(assignment);
        LearnerCurriculum curriculum = curricula.find(result.userId());
        boolean firstTest = curriculum.recordFirstTest(assignment.topicId(), clock.instant(), percent.doubleValue());
        boolean newlyPassed = passed && curriculum.pass(assignment.topicId(), clock.instant());
        if (firstTest || newlyPassed) curricula.save(curriculum);
        return () -> outcomes.afterTopicTest(result.userId(), assignment.topicId(), result.attemptId(),
                percent.doubleValue(), firstTest, wrong);
    }

    /**
     * The course test is consumed here; whether the learner passes the course is decided by the total score, not by this
     * test alone, once the reviews of its wrong points exist.
     */
    private Runnable applyCourseGate(AssessmentResult result, Set<UUID> wrong) {
        if (result.packageVersionId() == null) {
            log.info("Course gate result without package version: eventId={}", result.eventId());
            return () -> { };
        }
        var open = courseAssignments.findOpenForAttempt(result.userId(), result.packageVersionId(), result.completedAt());
        if (open.isEmpty()) {
            log.info("Course gate result without an open assignment: eventId={}", result.eventId());
            return () -> { };
        }
        BigDecimal score = result.items().stream().map(ItemResult::score).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal max = result.items().stream().map(ItemResult::maxScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal percent = max.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(max, 4, RoundingMode.HALF_UP);
        CourseTestAssignment assignment = open.get();
        assignment.consume(result.attemptId(), percent);
        courseAssignments.save(assignment);
        return () -> outcomes.afterCourseTest(result.userId(), assignment.courseId(), result.attemptId(), wrong);
    }
}
