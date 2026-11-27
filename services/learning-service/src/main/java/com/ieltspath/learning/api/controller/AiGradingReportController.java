package com.ieltspath.learning.api.controller;

import com.ieltspath.commonsecurity.currentuser.CurrentUserProvider;
import com.ieltspath.learning.application.exception.LearningRequestException;
import com.ieltspath.learning.application.usecase.AiGradingReportUseCase;
import com.ieltspath.learning.domain.vo.AiGradingReport;
import com.ieltspath.learning.domain.vo.AiGradingReport.ReportStatus;
import com.ieltspath.learning.domain.vo.AiGradingReport.SubmissionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/learning")
public class AiGradingReportController {
    private final CurrentUserProvider currentUser;
    private final AiGradingReportUseCase useCase;

    public AiGradingReportController(CurrentUserProvider currentUser, AiGradingReportUseCase useCase) {
        this.currentUser = currentUser;
        this.useCase = useCase;
    }

    @PostMapping("/ai-grading-reports")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AiGradingReportResponse> submit(@Valid @RequestBody CreateAiGradingReportRequest request) {
        var saved = useCase.report(
                currentUser.requireUserId(),
                request.submissionId(),
                request.submissionType(),
                request.reason());
        HttpStatus status = saved.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(AiGradingReportResponse.from(saved.report()));
    }

    @GetMapping("/admin/ai-grading-reports")
    @PreAuthorize("hasAnyRole('ADMIN', 'EXAMINER')")
    public AiGradingReportPageResponse list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "submissionType", required = false) String submissionType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        ReportStatus parsedStatus = parseStatus(status);
        SubmissionType parsedType = parseType(submissionType);
        var result = useCase.list(parsedStatus, parsedType, page, size);
        return new AiGradingReportPageResponse(
                result.content().stream().map(AiGradingReportResponse::from).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages());
    }

    private static ReportStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ReportStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new LearningRequestException(400, "INVALID_STATUS", "Trạng thái báo cáo không hợp lệ.");
        }
    }

    private static SubmissionType parseType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return SubmissionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new LearningRequestException(400, "INVALID_SUBMISSION_TYPE", "Loại bài nộp không hợp lệ.");
        }
    }

    public record CreateAiGradingReportRequest(
            @NotNull(message = "Mã bài nộp không được để trống.") UUID submissionId,
            @NotNull(message = "Loại bài nộp không được để trống.") SubmissionType submissionType,
            @NotBlank(message = "Lý do báo cáo không được để trống.")
            @Size(max = 1000, message = "Lý do báo cáo tối đa 1000 ký tự.") String reason
    ) { }

    public record AiGradingReportResponse(
            UUID id,
            UUID userId,
            UUID submissionId,
            SubmissionType submissionType,
            String reason,
            ReportStatus status,
            Instant createdAt
    ) {
        static AiGradingReportResponse from(AiGradingReport report) {
            return new AiGradingReportResponse(
                    report.id(), report.userId(), report.submissionId(),
                    report.submissionType(), report.reason(), report.status(), report.createdAt());
        }
    }

    public record AiGradingReportPageResponse(
            List<AiGradingReportResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) { }
}
