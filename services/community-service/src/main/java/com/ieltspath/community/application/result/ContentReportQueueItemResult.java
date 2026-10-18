package com.ieltspath.community.application.result;

import com.ieltspath.community.domain.vo.ContentReportQueueEntry;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentReportQueueItemResult(UUID id, UUID reporterId, String reporterDisplayName,
                                           ReportContentType contentType, UUID contentId,
                                           UUID contentAuthorId, String contentAuthorDisplayName,
                                           String contentPreview, String reason, ReportStatus status,
                                           Instant createdAt) {
    public static ContentReportQueueItemResult from(ContentReportQueueEntry entry, String reporterDisplayName,
                                                    String contentAuthorDisplayName) {
        return new ContentReportQueueItemResult(entry.id(), entry.reporterId(), reporterDisplayName,
                entry.contentType(), entry.contentId(), entry.contentAuthorId(), contentAuthorDisplayName,
                entry.contentPreview(), entry.reason(), entry.status(), entry.createdAt());
    }
}
