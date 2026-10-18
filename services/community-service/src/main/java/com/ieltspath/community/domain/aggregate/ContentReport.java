package com.ieltspath.community.domain.aggregate;

import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportDecision;
import com.ieltspath.community.domain.vo.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public class ContentReport {
    private final UUID id;
    private final UUID reporterId;
    private final ReportContentType contentType;
    private final UUID contentId;
    private final String reason;
    private final Instant createdAt;
    private ReportStatus status;
    private UUID reviewedBy;
    private Instant reviewedAt;

    public ContentReport(UUID id, UUID reporterId, ReportContentType contentType, UUID contentId, String reason,
                         ReportStatus status, Instant createdAt, UUID reviewedBy, Instant reviewedAt) {
        if (id == null || reporterId == null || contentType == null || contentId == null || status == null || createdAt == null)
            throw new CommunityException("Report fields must not be null");
        this.id = id;
        this.reporterId = reporterId;
        this.contentType = contentType;
        this.contentId = contentId;
        this.reason = requireReason(reason);
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        if (status == ReportStatus.PENDING && (reviewedBy != null || reviewedAt != null)
                || status != ReportStatus.PENDING && (reviewedBy == null || reviewedAt == null))
            throw new CommunityException("Pending report review fields are inconsistent");
    }

    public static ContentReport create(UUID reporterId, ReportContentType contentType, UUID contentId, String reason) {
        return new ContentReport(UUID.randomUUID(), reporterId, contentType, contentId, reason,
                ReportStatus.PENDING, Instant.now(), null, null);
    }

    private static String requireReason(String value) {
        if (value == null || value.isBlank()) throw new CommunityException("Report reason is required");
        String normalized = value.trim();
        if (normalized.length() > 1000) throw new CommunityException("Report reason must be at most 1000 characters");
        return normalized;
    }

    public void resolve(UUID adminId, ReportDecision decision) {
        if (adminId == null || decision == null) throw new CommunityException("Report decision fields must not be null");
        if (status != ReportStatus.PENDING) throw new CommunityException("Only pending reports can be resolved");
        status = decision == ReportDecision.DISMISS ? ReportStatus.DISMISSED : ReportStatus.ACTIONED;
        reviewedBy = adminId;
        reviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getReporterId() { return reporterId; }
    public ReportContentType getContentType() { return contentType; }
    public UUID getContentId() { return contentId; }
    public String getReason() { return reason; }
    public ReportStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
}
