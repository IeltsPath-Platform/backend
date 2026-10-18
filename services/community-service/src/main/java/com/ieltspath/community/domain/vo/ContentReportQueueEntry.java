package com.ieltspath.community.domain.vo;

import java.time.Instant;
import java.util.UUID;

public record ContentReportQueueEntry(UUID id, UUID reporterId, ReportContentType contentType, UUID contentId,
                                      UUID contentAuthorId, String contentPreview, String reason,
                                      ReportStatus status, Instant createdAt) { }
