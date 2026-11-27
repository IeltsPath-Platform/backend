package com.ieltspath.community.application.result;

import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentReportResult(UUID id, ReportContentType contentType, UUID contentId, String reason,
                                  ReportStatus status, Instant createdAt, Instant decidedAt) {
    public ContentReportResult(UUID id, ReportContentType contentType, UUID contentId, String reason,
                               ReportStatus status, Instant createdAt) {
        this(id, contentType, contentId, reason, status, createdAt, null);
    }

    public static ContentReportResult from(ContentReport report) {
        return new ContentReportResult(report.getId(), report.getContentType(), report.getContentId(),
                report.getReason(), report.getStatus(), report.getCreatedAt(), report.getReviewedAt());
    }
}
