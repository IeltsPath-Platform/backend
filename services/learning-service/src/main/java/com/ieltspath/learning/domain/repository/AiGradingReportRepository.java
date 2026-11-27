package com.ieltspath.learning.domain.repository;

import com.ieltspath.learning.domain.vo.AiGradingReport;
import com.ieltspath.learning.domain.vo.AiGradingReport.ReportStatus;
import com.ieltspath.learning.domain.vo.AiGradingReport.SubmissionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGradingReportRepository {
    Optional<UUID> findSubmissionOwner(SubmissionType type, UUID submissionId);

    SaveResult saveIdempotent(UUID id, UUID userId, UUID submissionId, SubmissionType type, String reason);

    Page findPage(ReportStatus status, SubmissionType type, int page, int size);

    record SaveResult(AiGradingReport report, boolean created) { }

    record Page(List<AiGradingReport> content, int page, int size, long totalElements, int totalPages) { }
}
