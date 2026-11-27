package com.ieltspath.learning.domain.vo;

import java.time.Instant;
import java.util.UUID;

public record AiGradingReport(
        UUID id,
        UUID userId,
        UUID submissionId,
        SubmissionType submissionType,
        String reason,
        ReportStatus status,
        Instant createdAt
) {
    public enum SubmissionType {
        WRITING,
        SPEAKING
    }

    public enum ReportStatus {
        OPEN,
        REVIEWED,
        DISMISSED
    }
}
