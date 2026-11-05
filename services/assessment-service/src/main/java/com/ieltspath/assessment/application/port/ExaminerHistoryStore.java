package com.ieltspath.assessment.application.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What an examiner graded, so they can read it again. Only the examiner who graded a job sees it, and grading stays blind:
 * nothing here names the learner.
 */
public interface ExaminerHistoryStore {
    /** The jobs the examiner finished, newest first. QA samples and work without a structured review are not listed. */
    Page completed(UUID examinerId, int page, int size);

    /** The examiner's finished review of one job; empty when it is not theirs, not finished, or has no structured review. */
    Optional<GradedReview> review(UUID examinerId, UUID jobId);

    record Page(List<Item> content, int page, int size, long totalElements) {}

    record Item(UUID jobId, String source, String skill, String task, BigDecimal band, Instant completedAt) {}

    record GradedReview(UUID jobId, String source, String skill, String task, String prompt, List<PromptImage> images,
                        String essay, BigDecimal band, JsonNode criteria, List<String> errorCodes, String feedback,
                        JsonNode annotations, Instant completedAt) {
        @Override public String toString() { return "GradedReview[jobId=" + jobId + "]"; }
    }
}
