package com.ieltspath.learning.domain.vo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A graded first essay as the progress view reads it back: the stored grade, never the essay text. */
public record WritingProgressEssay(UUID submissionId, String source, Instant gradedAt, BigDecimal overallBand,
                                   String promptVersion, List<WritingGrade.Criterion> criteria,
                                   List<WritingGrade.Correction> corrections) {
    public WritingProgressEssay {
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
        corrections = corrections == null ? List.of() : List.copyOf(corrections);
    }
}
