package com.ieltspath.learning.application.usecase;

import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.domain.repository.AiGradingReportRepository;
import com.ieltspath.learning.domain.vo.AiGradingReport.ReportStatus;
import com.ieltspath.learning.domain.vo.AiGradingReport.SubmissionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AiGradingReportUseCase {
    private static final int MAX_REASON_LENGTH = 1000;

    private final AiGradingReportRepository reports;

    public AiGradingReportUseCase(AiGradingReportRepository reports) {
        this.reports = reports;
    }

    @Transactional
    public AiGradingReportRepository.SaveResult report(UUID userId, UUID submissionId, SubmissionType type, String reason) {
        if (submissionId == null || type == null) {
            throw new LearningRequestException(400, "INVALID_REQUEST", "Thiếu thông tin bài nộp cần báo cáo.");
        }
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_REASON_LENGTH) {
            throw new LearningRequestException(400, "INVALID_REASON",
                    "Lý do báo cáo phải từ 1 đến 1000 ký tự.");
        }
        UUID ownerId = reports.findSubmissionOwner(type, submissionId)
                .orElseThrow(() -> new LearningRequestException(404, "SUBMISSION_NOT_FOUND",
                        "Không tìm thấy bài nộp."));
        if (!ownerId.equals(userId)) {
            throw new LearningRequestException(403, "NOT_OWNER",
                    "Bạn không có quyền báo cáo bài nộp này.");
        }
        return reports.saveIdempotent(UUID.randomUUID(), userId, submissionId, type, normalized);
    }

    @Transactional(readOnly = true)
    public AiGradingReportRepository.Page list(ReportStatus status, SubmissionType type, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new LearningRequestException(400, "INVALID_PAGINATION", "Tham số phân trang không hợp lệ.");
        }
        return reports.findPage(status, type, page, size);
    }
}
