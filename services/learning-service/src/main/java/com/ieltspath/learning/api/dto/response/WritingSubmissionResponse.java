package com.ieltspath.learning.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ieltspath.learning.application.result.WritingSubmissionResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Learner view of one essay. A GRADED essay carries the grade; PAYMENT_PENDING carries only {@code code}; GRADING and
 * FAILED carry {@code failureCode}. Chart facts and the answer spec are never part of it.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WritingSubmissionResponse(UUID submissionId, String status, String task, Integer wordCount,
                                        BigDecimal overallBand, Boolean passed, List<Criterion> criteria,
                                        List<Correction> corrections, String summary, Integer pointsCharged,
                                        String sampleAnswer, String code, String failureCode) {
    public record Criterion(String code, BigDecimal band, List<String> strengths, List<String> improvements) {}

    public record Correction(String excerpt, String suggestion, String category) {}

    public static WritingSubmissionResponse from(WritingSubmissionResult result) {
        var grade = result.grade();
        return new WritingSubmissionResponse(result.submissionId(), result.status(),
                "GRADED".equals(result.status()) ? result.task() : null, result.wordCount(), result.overallBand(),
                result.passed(),
                grade == null ? null : grade.criteria().stream().map(criterion -> new Criterion(criterion.code(),
                        criterion.band(), criterion.strengths(), criterion.improvements())).toList(),
                grade == null ? null : grade.corrections().stream().map(correction -> new Correction(
                        correction.excerpt(), correction.suggestion(), correction.category())).toList(),
                grade == null ? null : grade.summary(), result.pointsCharged(), result.sampleAnswer(), result.code(),
                result.failureCode());
    }
}
