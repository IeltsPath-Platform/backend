package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ieltspath.assessment.application.command.CompleteHumanReviewCommand;
import com.ieltspath.assessment.application.exception.ContentUnavailableException;
import com.ieltspath.assessment.application.exception.UnknownErrorCodeException;
import com.ieltspath.assessment.application.port.ErrorTypeCatalog;
import com.ieltspath.assessment.application.port.HumanReviewStore;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class HumanReviewService {
    private static final int MAX_DRAFT_BYTES = 204_800;

    private final HumanReviewStore store;
    private final ObjectMapper json;
    private final ErrorTypeCatalog errorTypes;
    private final ExaminerAssignmentService assignments;
    private final GradingRequestAnnouncer announcer;
    private final GradingCompletedAnnouncer completedAnnouncer;

    public HumanReviewService(HumanReviewStore store, ObjectMapper json, ErrorTypeCatalog errorTypes,
                              ExaminerAssignmentService assignments, GradingRequestAnnouncer announcer,
                              GradingCompletedAnnouncer completedAnnouncer) {
        this.store = store;
        this.json = json;
        this.errorTypes = errorTypes;
        this.assignments = assignments;
        this.announcer = announcer;
        this.completedAnnouncer = completedAnnouncer;
    }

    @Transactional
    public HumanReviewStore.Review create(HumanReviewStore.CreateReview review) {
        requireObject(review.promptSnapshot(), "promptSnapshot");
        requireObject(review.rubric(), "rubric");
        // A repeated request keeps the examiner chosen the first time and is not held to the limit again.
        boolean repeated = store.exists(review.requestId());
        if (!repeated) assignments.requireAvailable(review.examinerId(), 1);
        HumanReviewStore.Review created = store.create(review);
        // Only the first request tells the examiner; a repeated one finds the job already announced.
        if (!repeated) {
            announcer.announce(review.examinerId(), store.jobIdOf(created.reviewSubmissionId()), "WRITING", Instant.now());
        }
        return created;
    }

    /**
     * What the learner may read. An error code whose catalog entry is still in review is not shown, so the code is
     * dropped from {@code errorCodes} and from each marked error; the examiner's own excerpt and suggestion stay.
     */
    @Transactional(readOnly = true)
    public HumanReviewStore.ReviewResult get(UUID reviewId, UUID learnerId) {
        HumanReviewStore.ReviewResult result = store.find(reviewId, learnerId).orElseThrow(
                () -> new AssessmentNotFoundException("Human review not found"));
        if (!"DONE".equals(result.status())) return result;
        Set<String> approved = approvedCodes();
        List<String> codes = result.errorCodes() == null ? null
                : result.errorCodes().stream().filter(approved::contains).toList();
        return new HumanReviewStore.ReviewResult(result.reviewSubmissionId(), result.status(), result.band(),
                result.criteria(), codes, result.feedback(), withoutUnapprovedCodes(result.annotations(), approved),
                result.examinerId());
    }

    /**
     * The learner gives up a late review whose examiner can no longer be changed. Idempotent: a review already cancelled
     * answers again with its status. A review that was graded in the meantime stays graded (409).
     */
    @Transactional
    public HumanReviewStore.ReviewResult cancel(UUID reviewId, UUID learnerId) {
        store.find(reviewId, learnerId).orElseThrow(() -> new AssessmentNotFoundException("Human review not found"));
        assignments.cancel(learnerId, store.jobIdOf(reviewId));
        return get(reviewId, learnerId);
    }

    @Transactional
    public void complete(UUID jobId, UUID examinerId, CompleteHumanReviewCommand request) {
        requireHalfBand(request.band(), "band");
        for (var criterion : request.criteria()) requireHalfBand(criterion.band(), "criterion band");
        List<String> errorCodes = request.errorCodes() == null ? List.of() : request.errorCodes();
        List<CompleteHumanReviewCommand.Annotation> annotations =
                request.annotations() == null ? List.of() : request.annotations();
        requireKnownCodes(errorCodes, annotations);
        requireAnnotationsQuoteTheEssay(jobId, annotations);
        JsonNode criteria = json.valueToTree(request.criteria());
        JsonNode marked = annotations.isEmpty() ? null : json.valueToTree(annotations);
        // Only the call that finished the review tells the learner; a repeated request finds it already announced.
        store.complete(jobId, examinerId, request.band(), criteria, errorCodes, request.feedback(), marked)
                .ifPresent(done -> completedAnnouncer.announce(done.learnerId(), jobId, done.reviewSubmissionId(),
                        done.skill(), Instant.now()));
    }

    /** Keeps an unfinished review. The shape is the examiner's own, so only its type and size are checked. */
    @Transactional
    public void saveDraft(UUID jobId, UUID examinerId, JsonNode payload) {
        requireObject(payload, "draft");
        if (payload.toString().getBytes(StandardCharsets.UTF_8).length > MAX_DRAFT_BYTES) {
            throw new InvalidAssessmentStateException("draft must not be larger than 200 KB");
        }
        store.saveDraft(jobId, examinerId, payload);
    }

    @Transactional(readOnly = true)
    public Optional<HumanReviewStore.Draft> draft(UUID jobId, UUID examinerId) {
        return store.draft(jobId, examinerId);
    }

    /** A grader may record any active code, reviewed or not. Only learners wait for the review. */
    private void requireKnownCodes(List<String> errorCodes, List<CompleteHumanReviewCommand.Annotation> annotations) {
        Set<String> wanted = new LinkedHashSet<>(errorCodes);
        annotations.forEach(annotation -> wanted.add(annotation.errorCode()));
        if (wanted.isEmpty()) return;
        Set<String> known = errorTypes.staffCodes();
        List<String> unknown = wanted.stream().filter(code -> !known.contains(code)).toList();
        if (!unknown.isEmpty()) throw new UnknownErrorCodeException(unknown);
    }

    private void requireAnnotationsQuoteTheEssay(UUID jobId, List<CompleteHumanReviewCommand.Annotation> annotations) {
        if (annotations.isEmpty()) return;
        String essay = store.essay(jobId).orElseThrow(
                () -> new AssessmentNotFoundException("Standalone human review not found"));
        for (int index = 0; index < annotations.size(); index++) {
            var annotation = annotations.get(index);
            if (annotation.start() < 0 || annotation.end() <= annotation.start() || annotation.end() > essay.length()
                    || !essay.substring(annotation.start(), annotation.end()).equals(annotation.excerpt())) {
                throw new InvalidAssessmentStateException(
                        "annotations[" + index + "] must quote the essay between start and end");
            }
        }
    }

    /** A learner read must not fail because the catalog is down: with no catalog, no code is shown. */
    private Set<String> approvedCodes() {
        try {
            return errorTypes.activeCodes();
        } catch (ContentUnavailableException exception) {
            return Set.of();
        }
    }

    private JsonNode withoutUnapprovedCodes(JsonNode annotations, Set<String> approved) {
        if (annotations == null || !annotations.isArray()) return annotations;
        ArrayNode visible = json.createArrayNode();
        for (JsonNode annotation : annotations) {
            ObjectNode copy = annotation.deepCopy();
            if (!approved.contains(copy.path("errorCode").asText())) copy.remove("errorCode");
            visible.add(copy);
        }
        return visible;
    }

    private static void requireHalfBand(java.math.BigDecimal band, String field) {
        if (band == null || band.signum() < 0 || band.compareTo(java.math.BigDecimal.valueOf(9)) > 0
                || band.multiply(java.math.BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0) {
            throw new InvalidAssessmentStateException(field + " must be an IELTS half-band from 0 to 9");
        }
    }

    private static void requireObject(JsonNode node, String field) {
        if (node == null || !node.isObject()) throw new InvalidAssessmentStateException(field + " must be a JSON object");
    }
}
