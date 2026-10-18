package com.ieltspath.community.domain.repository;

import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ContentReportQueueEntry;
import com.ieltspath.community.domain.vo.ReportContentType;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

public interface ContentReportRepository {
    ContentReport save(ContentReport report);
    Optional<ContentReport> findByIdForUpdate(UUID id);
    Optional<ContentReport> findById(UUID id);
    List<ContentReport> findPendingByContentForUpdate(ReportContentType contentType, UUID contentId);
    int actionPendingForContent(ReportContentType contentType, UUID contentId, UUID adminId, Instant reviewedAt);
    ContentReport update(ContentReport report);
    CommunityPage<ContentReportQueueEntry> findPending(int page, int size);
}
