package com.ieltspath.community.api.dto.response;

import com.ieltspath.community.application.result.ContentReportResult;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ContentReportResponse(UUID id, ReportContentType contentType, UUID contentId, String reason,
                                    ReportStatus status, Instant createdAt) {
    public static ContentReportResponse from(ContentReportResult result) {
        return new ContentReportResponse(result.id(), result.contentType(), result.contentId(), result.reason(),
                result.status(), result.createdAt());
    }
}
