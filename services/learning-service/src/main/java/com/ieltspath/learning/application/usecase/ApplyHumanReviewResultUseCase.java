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
import org.springframework.transaction.annotation.Transactional;

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
 * reviews (never blocking), and the study points are kept for the learner to read. Applying is idempotent per essay, and all of
 * it happens in one transaction under the learner's lock. Every read happens before the first write, so a Content outage leaves
 * nothing half applied.
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
    private final HumanReviewEvidenceRule rule = new HumanReviewEvidenceRule();
    private final int limit;

    public ApplyHumanReviewResultUseCase(LearnerLock lock, WritingSubmissionRepository essays,
                                         HumanReviewRequestRepository requests, PracticeAttemptRepository attempts,
                                         LearningContentClient content, ErrorTrapCatalog traps, LessonAccess access,
                                         KnowledgeEvidenceRepository evidence, MasteryProfileUpdater profiles,
                                         ErrorTrapReviews errorTrapReviews, WritingCriterionReviews criterionReviews,
                                         ReviewItemRepository reviews, HumanReviewOutcomeRepository outcomes,
                                         ObjectMapper json,
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
        this.limit = limit;
    }

    /**
     * @throws IllegalArgumentException when the essay is not the learner's, or the review is not the one asked for it; the
     *                                  event is then refused rather than retried
     */
    @Transactional
    public void execute(HumanReviewResult result) {
        UUID learnerId = result.learnerId();
        UUID essayId = result.sourceSubmissionId();
        UUID reviewSubmissionId = result.reviewSubmissionId();
        lock.lock(learnerId);
        if (outcomes.studyPointsOf(essayId).isPresent()) return;

        WritingSubmission essay = essays.findOwned(essayId, learnerId)
                .orElseThrow(() -> new IllegalArgumentException("the source submission is not the learner's essay"));
        if (requests.findReviewId(essayId).filter(reviewSubmissionId::equals).isEmpty()) {
            throw new IllegalArgumentException("the review was not asked for this essay");
        }

        // Reads first: nothing is written until every answer from Content and the catalog is in hand.
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
        // authority for them (as for the AI's error evidence), and they are left out of the catalog check: the learner's
        // catalog query does not list child points.
        Set<UUID> criterionPoints = new LinkedHashSet<>(outcome.correctByKnowledgePoint().keySet());
        Set<UUID> known = criterionPoints.isEmpty() ? Set.of() : access.knownKnowledgePoints(learnerId, criterionPoints);
        Map<UUID, Boolean> judged = new LinkedHashMap<>();
        outcome.correctByKnowledgePoint().forEach((kp, correct) -> {
            if (known.contains(kp)) judged.put(kp, correct);
        });
        List<TrapOutcome> marked = outcome.traps();
        List<FailedWritingCriterion> failed = outcome.failedCriteria().stream()
                .filter(f -> known.contains(f.knowledgePointId())).toList();

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

        // Optional reviews: the marked errors first (the examiner has seen them), then the weakest criteria.
        List<ErrorTrapReviews.MarkedErrorTrap> markedTraps = marked.stream()
                .map(t -> new ErrorTrapReviews.MarkedErrorTrap(t.code(), t.trapKnowledgePointId(), t.count())).toList();
        int trapReviews = errorTrapReviews.createForMarkedCodes(learnerId, essay.lessonId(), markedTraps, limit);
        criterionReviews.create(learnerId, essay.lessonId(), failed, Math.max(0, limit - trapReviews));

        ArrayNode studyPoints = studyPoints(learnerId, rubric, marked, trapCatalog, failed);
        outcomes.insertIfAbsent(reviewSubmissionId, learnerId, essayId, studyPoints.toString());
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
     * when the point now has an optional review. Names come from Content; if Content cannot answer, the names are left out.
     */
    private ArrayNode studyPoints(UUID learnerId, LearningContentClient.Rubric rubric, List<TrapOutcome> marked,
                                  ErrorTrapCatalog.ErrorTraps trapCatalog, List<FailedWritingCriterion> failed) {
        Map<UUID, UUID> reviewByPoint = new HashMap<>();
        for (PendingReview pending : reviews.findPending(learnerId)) {
            reviewByPoint.putIfAbsent(pending.knowledgePointId(), pending.reviewId());
        }
        Map<UUID, LearningContentClient.KnowledgePoint> details = knowledgePointDetails();

        ArrayNode points = json.createArrayNode();
        for (TrapOutcome trap : marked) {
            UUID parent = trapCatalog.parentByTrapKp().get(trap.trapKnowledgePointId());
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
