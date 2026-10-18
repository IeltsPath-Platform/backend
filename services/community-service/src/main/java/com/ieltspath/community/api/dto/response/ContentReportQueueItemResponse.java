package com.ieltspath.community.api.dto.response;

import com.ieltspath.community.domain.vo.ContentReportQueueEntry;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentReportQueueItemResponse(UUID id, UUID reporterId, String reporterDisplayName,
                                             ReportContentType contentType, UUID contentId,
                                             UUID contentAuthorId, String contentAuthorDisplayName,
                                             String contentPreview, String reason,
                                             ReportStatus status, Instant createdAt) {
    public static ContentReportQueueItemResponse from(com.ieltspath.community.application.result.ContentReportQueueItemResult entry) {
        return new ContentReportQueueItemResponse(entry.id(), entry.reporterId(), entry.reporterDisplayName(),
                entry.contentType(), entry.contentId(), entry.contentAuthorId(), entry.contentAuthorDisplayName(),
                entry.contentPreview(), entry.reason(), entry.status(), entry.createdAt());
    }
}
