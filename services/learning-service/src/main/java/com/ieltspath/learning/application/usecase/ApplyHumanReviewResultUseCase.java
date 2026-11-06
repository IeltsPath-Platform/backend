package com.ieltspath.learning.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.learning.application.command.HumanReviewResult;
import com.ieltspath.learning.application.port.LearnerLock;
import com.ieltspath.learning.application.port.LearningContentClient;
import com.ieltspath.learning.application.service.ErrorTrapCatalog;
import com.ieltspath.learning.application.service.ErrorTrapReviews;
import com.ieltspath.learning.application.service.LessonAccess;
import com.ieltspath.learning.application.service.LessonEvidenceReference;
import com.ieltspath.learning.application.service.MasteryProfileUpdater;
import com.ieltspath.learning.application.service.WritingCriterionReviews;
import com.ieltspath.learning.domain.aggregate.PracticeAttempt;
import com.ieltspath.learning.domain.aggregate.WritingSubmission;
import com.ieltspath.learning.domain.repository.HumanReviewOutcomeRepository;
import com.ieltspath.learning.domain.repository.HumanReviewRequestRepository;
import com.ieltspath.learning.domain.repository.KnowledgeEvidenceRepository;
import com.ieltspath.learning.domain.repository.PracticeAttemptRepository;
import com.ieltspath.learning.domain.repository.ReviewItemRepository;
import com.ieltspath.learning.domain.repository.WritingSubmissionRepository;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.ErrorMark;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.Mark;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.Outcome;
import com.ieltspath.learning.domain.service.HumanReviewEvidenceRule.TrapOutcome;
import com.ieltspath.learning.domain.vo.EvidenceSource;
import com.ieltspath.learning.domain.vo.FailedWritingCriterion;
import com.ieltspath.learning.domain.vo.KnowledgeEvidence;
import com.ieltspath.learning.domain.vo.PendingReview;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Applies an examiner's finished standalone Writing review the way the AI grade is applied: criterion bands become evidence for
 * their knowledge points, and each approved error code becomes a wrong answer on its trap point. The examiner's grade replaces
 * the AI's criterion evidence for the same essay, so the essay is not counted twice. Then the weakest points get optional
 * reviews (never blocking), and the study points are kept for the learner to read. Applying is idempotent per essay.
 *
 * <p>Two steps. First every read from Content and the catalog happens with no lock and no transaction, so a slow Content holds
 * nothing and a Content outage just makes the message retry. Then one short transaction under the learner's lock writes
 * everything from the data already read; it makes no Content call.
 *
 * <p>An essay of the practice library (free writing, no lesson) is outside the learner's path, as it is when the AI grades it:
 * the learner reads the study points, but nothing is recorded as evidence, no mastery profile changes and no review is made.
 */
@Service
public class ApplyHumanReviewResultUseCase {
    private static final String CRITERION_CODE_PREFIX = "DEMO_WRITING_CRITERION_";

    private final LearnerLock lock;
    private final WritingSubmissionRepository essays;
    private final HumanReviewRequestRepository requests;
    private final PracticeAttemptRepository attempts;
    private final LearningContentClient content;
    private final ErrorTrapCatalog traps;
    private final LessonAccess access;
    private final KnowledgeEvidenceRepository evidence;
    private final MasteryProfileUpdater profiles;
    private final ErrorTrapReviews errorTrapReviews;
    private final WritingCriterionReviews criterionReviews;
    private final ReviewItemRepository reviews;
    private final HumanReviewOutcomeRepository outcomes;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    private final HumanReviewEvidenceRule rule = new HumanReviewEvidenceRule();
    private final int limit;

    public ApplyHumanReviewResultUseCase(LearnerLock lock, WritingSubmissionRepository essays,
                                         HumanReviewRequestRepository requests, PracticeAttemptRepository attempts,
                                         LearningContentClient content, ErrorTrapCatalog traps, LessonAccess access,
                                         KnowledgeEvidenceRepository evidence, MasteryProfileUpdater profiles,
                                         ErrorTrapReviews errorTrapReviews, WritingCriterionReviews criterionReviews,
                                         ReviewItemRepository reviews, HumanReviewOutcomeRepository outcomes,
                                         ObjectMapper json, PlatformTransactionManager transactionManager,
                                         @Value("${learning.human-review.review-limit:2}") int limit) {
        if (limit < 0) throw new IllegalArgumentException("human-review.review-limit must not be negative");
        this.lock = lock;
        this.essays = essays;
        this.requests = requests;
        this.attempts = attempts;
        this.content = content;
        this.traps = traps;
        this.access = access;
        this.evidence = evidence;
        this.profiles = profiles;
        this.errorTrapReviews = errorTrapReviews;
        this.criterionReviews = criterionReviews;
        this.reviews = reviews;
        this.outcomes = outcomes;
        this.json = json;
        this.transaction = new TransactionTemplate(transactionManager);
        this.limit = limit;
    }

    /**
     * @throws IllegalArgumentException when the essay is not the learner's, or the review is not the one asked for it; the
     *                                  event is then refused rather than retried
     */
    public void execute(HumanReviewResult result) {
        // Applied before: nothing to read and nothing to write, so a redelivered event makes no Content call at all.
        if (outcomes.studyPointsOf(result.sourceSubmissionId()).isPresent()) return;
        Reads reads = readContent(result);
        transaction.executeWithoutResult(status -> apply(result, reads));
    }

    /** Everything the review needs from Content and the catalog, read before the lock and the transaction. */
    private record Reads(WritingSubmission essay, Outcome outcome, Set<UUID> known, List<FailedWritingCriterion> failed,
                         ErrorTrapCatalog.ErrorTraps trapCatalog, Map<UUID, LearningContentClient.KnowledgePoint> details,
                         BigDecimal overallBand, boolean passed, Map<UUID, Integer> availability,
                         List<ErrorTrapReviews.PlannedTrap> trapPlan) { }

    private Reads readContent(HumanReviewResult result) {
        UUID learnerId = result.learnerId();
        UUID essayId = result.sourceSubmissionId();
        WritingSubmission essay = essays.findOwned(essayId, learnerId)
                .orElseThrow(() -> new IllegalArgumentException("the source submission is not the learner's essay"));

        LearningContentClient.Rubric rubric = content.getActiveRubric(essay.prompt().task());
        Map<String, UUID> pointByRubric = new LinkedHashMap<>();
        for (LearningContentClient.RubricCriterion criterion : rubric.criteria()) {
            if (criterion.knowledgePointId() != null) pointByRubric.putIfAbsent(criterion.code(), criterion.knowledgePointId());
        }
        Set<String> approved = content.activeErrorTypes().stream().map(LearningContentClient.ErrorType::code)
                .collect(Collectors.toSet());
        ErrorTrapCatalog.ErrorTraps trapCatalog = traps.load();
        List<Mark> marks = result.criteria().stream().map(c -> new Mark(c.criterion(), c.band())).toList();
        List<ErrorMark> errors = result.errorCodes().stream().map(e -> new ErrorMark(e.code(), e.count())).toList();
        Outcome outcome = rule.derive(essay.prompt().task(), essay.prompt().passBand(), marks, pointByRubric, errors,
                trapCatalog.trapKpByCode(), approved);

        // Criterion points must be in the learner's catalog. Trap points come from the local error-trap catalog, which is the
        // authority for them (as for the AI's error evidence), and they are left out of the catalog check.
        Set<UUID> criterionPoints = new LinkedHashSet<>(outcome.correctByKnowledgePoint().keySet());
        Set<UUID> known = criterionPoints.isEmpty() ? Set.of() : access.knownKnowledgePoints(learnerId, criterionPoints);
        List<FailedWritingCriterion> failed = outcome.failedCriteria().stream()
                .filter(f -> known.contains(f.knowledgePointId())).map(f -> f.fromSubmission(essayId)).toList();

        BigDecimal overall = HumanReviewEvidenceRule.overallBand(marks);
        BigDecimal passBand = essay.prompt().passBand();
        boolean passed = passBand != null && overall != null && overall.compareTo(passBand) >= 0;

        Map<UUID, Integer> availability = Map.of();
        List<ErrorTrapReviews.PlannedTrap> trapPlan = List.of();
        if (!essay.isFree()) {
            availability = criterionReviews.availabilityOf(learnerId, failed);
            trapPlan = errorTrapReviews.plan(learnerId, essay.lessonId(), outcome.traps().stream()
                    .map(t -> new ErrorTrapReviews.MarkedErrorTrap(t.code(), t.trapKnowledgePointId(), t.count())).toList());
        }
        return new Reads(essay, outcome, known, failed, trapCatalog, knowledgePointDetails(), overall, passed,
                availability, trapPlan);
    }

    /** The writes, under the learner's lock. Makes no Content call: everything it needs was read in {@link #readContent}. */
    private void apply(HumanReviewResult result, Reads reads) {
        UUID learnerId = result.learnerId();
        UUID essayId = result.sourceSubmissionId();
        UUID reviewSubmissionId = result.reviewSubmissionId();
        lock.lock(learnerId);
        if (outcomes.studyPointsOf(essayId).isPresent()) return;
        if (requests.findReviewId(essayId).filter(reviewSubmissionId::equals).isEmpty()) {
            throw new IllegalArgumentException("the review was not asked for this essay");
        }

        WritingSubmission essay = reads.essay();
        Outcome outcome = reads.outcome();
        Set<UUID> known = reads.known();
        List<FailedWritingCriterion> failed = reads.failed();
        List<TrapOutcome> marked = outcome.traps();

        if (essay.isFree()) {
            // Practice outside the course moves no mastery: the learner only reads what to study.
            outcomes.insertIfAbsent(reviewSubmissionId, learnerId, essayId,
                    studyPoints(learnerId, marked, reads, failed).toString(), reads.overallBand(), reads.passed());
            return;
        }

        Map<UUID, Boolean> judged = new LinkedHashMap<>();
        outcome.correctByKnowledgePoint().forEach((kp, correct) -> {
            if (known.contains(kp)) judged.put(kp, correct);
        });

        // The examiner's criterion result replaces the AI's for this essay, so one essay is not counted twice.
        List<UUID> aiReferences = aiReferences(essay, learnerId, judged.keySet());
        Set<UUID> lost = aiReferences.isEmpty() ? Set.of() : evidence.removeWritingEvidence(learnerId, aiReferences);

        List<KnowledgeEvidence> appended = new ArrayList<>();
        judged.forEach((kp, correct) -> appended.add(KnowledgeEvidence.of(kp, correct, EvidenceSource.HUMAN_REVIEW,
                LessonEvidenceReference.forHumanReview(reviewSubmissionId, kp))));
        for (TrapOutcome trap : marked) {
            appended.add(KnowledgeEvidence.of(trap.trapKnowledgePointId(), false, EvidenceSource.ERROR_TRAP,
                    LessonEvidenceReference.forHumanReview(reviewSubmissionId, trap.trapKnowledgePointId())));
        }
        evidence.append(learnerId, appended);
        Set<UUID> affected = new LinkedHashSet<>(lost);
        appended.forEach(item -> affected.add(item.knowledgePointId()));
        if (!affected.isEmpty()) profiles.rebuild(learnerId, affected);

        // A criterion the examiner finds weak again while an optional review from ANOTHER essay still waits is no longer a
        // suggestion: that review blocks the lessons of its skill until it is done. A review this same essay raised is not
        // a second grade, so it stays optional.
        reviews.promoteCriterionReviewsToBlocking(learnerId,
                failed.stream().map(FailedWritingCriterion::knowledgePointId).collect(Collectors.toSet()), essayId);

        // A criterion the examiner grades at or above the pass band is no longer weak: its waiting optional Writing review is
        // closed as done. A blocking review is not closed, because the learner still has to do it.
        Set<UUID> failedPoints = failed.stream().map(FailedWritingCriterion::knowledgePointId).collect(Collectors.toSet());
        reviews.closeOptionalCriterionReviews(learnerId, outcome.criterionKnowledgePoints().stream()
                .filter(known::contains).filter(point -> !failedPoints.contains(point)).toList());

        // Optional reviews: the marked errors first (the examiner has seen them), then the weakest criteria.
        int trapReviews = errorTrapReviews.createPlanned(learnerId, reads.trapPlan(), limit);
        criterionReviews.createWithAvailability(learnerId, essay.lessonId(), failed, Math.max(0, limit - trapReviews),
                reads.availability());

        ArrayNode studyPoints = studyPoints(learnerId, marked, reads, failed);
        outcomes.insertIfAbsent(reviewSubmissionId, learnerId, essayId, studyPoints.toString(), reads.overallBand(),
                reads.passed());
    }

    /** The AI's criterion evidence for this essay: lesson essays by their own id, practice essays by the attempt's request. */
    private List<UUID> aiReferences(WritingSubmission essay, UUID learnerId, Set<UUID> points) {
        List<UUID> references = new ArrayList<>();
        UUID attemptRequest = essay.practiceAttemptId() == null ? null
                : attempts.findOwned(learnerId, essay.practiceAttemptId()).map(PracticeAttempt::requestId).orElse(null);
        for (UUID point : points) {
            references.add(LessonEvidenceReference.forWriting(essay.id(), point));
            if (attemptRequest != null) {
                references.add(LessonEvidenceReference.forPracticeSet(attemptRequest, essay.prompt().questionVersionId(), point));
            }
        }
        return references;
    }

    /**
     * What the learner reads on the review: each marked error (its grammar point) and each weak criterion, with the review id
     * when the point now has an optional review. Names come from Content read beforehand; if Content could not answer, the names
     * are left out.
     */
    private ArrayNode studyPoints(UUID learnerId, List<TrapOutcome> marked, Reads reads, List<FailedWritingCriterion> failed) {
        Map<UUID, UUID> reviewByPoint = new HashMap<>();
        for (PendingReview pending : reviews.findPending(learnerId)) {
            reviewByPoint.putIfAbsent(pending.knowledgePointId(), pending.reviewId());
        }
        Map<UUID, LearningContentClient.KnowledgePoint> details = reads.details();

        ArrayNode points = json.createArrayNode();
        for (TrapOutcome trap : marked) {
            UUID parent = reads.trapCatalog().parentByTrapKp().get(trap.trapKnowledgePointId());
            if (parent == null) continue;
            ObjectNode point = json.createObjectNode().put("kind", "ERROR_CODE").put("errorCode", trap.code())
                    .put("count", trap.count());
            describe(point, parent, details, reviewByPoint);
            points.add(point);
        }
        for (FailedWritingCriterion criterion : failed) {
            String code = criterion.code().startsWith(CRITERION_CODE_PREFIX)
                    ? criterion.code().substring(CRITERION_CODE_PREFIX.length()) : criterion.code();
            ObjectNode point = json.createObjectNode().put("kind", "CRITERION").put("criterionCode", code)
                    .put("band", criterion.band());
            describe(point, criterion.knowledgePointId(), details, reviewByPoint);
            points.add(point);
        }
        return points;
    }

    private static void describe(ObjectNode point, UUID knowledgePointId,
                                 Map<UUID, LearningContentClient.KnowledgePoint> details, Map<UUID, UUID> reviewByPoint) {
        point.put("knowledgePointId", knowledgePointId.toString());
        LearningContentClient.KnowledgePoint detail = details.get(knowledgePointId);
        if (detail != null) {
            if (detail.code() != null) point.put("code", detail.code());
            if (detail.name() != null) point.put("name", detail.name());
            if (detail.hasPracticeSet()) point.put("practiceKnowledgePointId", knowledgePointId.toString());
        }
        UUID review = reviewByPoint.get(knowledgePointId);
        if (review != null) point.put("reviewItemId", review.toString());
    }

    private Map<UUID, LearningContentClient.KnowledgePoint> knowledgePointDetails() {
        try {
            Map<UUID, LearningContentClient.KnowledgePoint> found = new HashMap<>();
            for (LearningContentClient.Topic topic : content.getTopicSequence()) {
                if (topic.knowledgePoints() == null) continue;
                for (LearningContentClient.KnowledgePoint point : topic.knowledgePoints()) {
                    if (point != null && point.id() != null) found.putIfAbsent(point.id(), point);
                }
            }
            return found;
        } catch (RuntimeException unavailable) {
            return Map.of();
        }
    }
}
