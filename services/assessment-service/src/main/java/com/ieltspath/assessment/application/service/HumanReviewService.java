package com.ieltspath.assessment.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.HumanReviewStore;
import com.ieltspath.assessment.application.command.CompleteHumanReviewCommand;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HumanReviewService {
    private final HumanReviewStore store;
    private final ObjectMapper json;

    public HumanReviewService(HumanReviewStore store, ObjectMapper json) {
        this.store = store;
        this.json = json;
    }

    @Transactional
    public HumanReviewStore.Review create(HumanReviewStore.CreateReview review) {
        requireObject(review.promptSnapshot(), "promptSnapshot");
        requireObject(review.rubric(), "rubric");
        return store.create(review);
    }

    @Transactional(readOnly = true)
    public HumanReviewStore.ReviewResult get(UUID reviewId, UUID learnerId) {
        return store.find(reviewId, learnerId).orElseThrow(
                () -> new AssessmentNotFoundException("Human review not found"));
    }

    @Transactional
    public void complete(UUID jobId, UUID examinerId, CompleteHumanReviewCommand request) {
        requireHalfBand(request.band(), "band");
        for (var criterion : request.criteria()) requireHalfBand(criterion.band(), "criterion band");
        JsonNode criteria = json.valueToTree(request.criteria());
        store.complete(jobId, examinerId, request.band(), criteria,
                request.errorCodes() == null ? List.of() : request.errorCodes(), request.feedback());
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
