package com.ieltspath.community.infrastructure.adapter;

import com.ieltspath.community.domain.aggregate.ContentReport;
import com.ieltspath.community.domain.exception.CommunityException;
import com.ieltspath.community.domain.repository.ContentReportRepository;
import com.ieltspath.community.domain.vo.CommunityPage;
import com.ieltspath.community.domain.vo.ContentReportQueueEntry;
import com.ieltspath.community.domain.vo.ReportContentType;
import com.ieltspath.community.domain.vo.ReportStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ContentReportRepositoryAdapter implements ContentReportRepository {
    private final JdbcTemplate jdbc;

    @Override
    public ContentReport save(ContentReport report) {
        try {
            jdbc.update("""
                    INSERT INTO community_reports(id,reporter_id,content_type,content_id,reason,status,created_at)
                    VALUES (?,?,?,?,?,'PENDING',?)
                    """, report.getId(), report.getReporterId(), report.getContentType().name(), report.getContentId(),
                    report.getReason(), Timestamp.from(report.getCreatedAt()));
        } catch (org.springframework.dao.DuplicateKeyException duplicate) {
            throw new com.ieltspath.community.domain.exception.CommunityConflictException("You have already reported this content");
        }
        return report;
    }

    @Override
    public Optional<ContentReport> findByIdForUpdate(UUID id) {
        return jdbc.query("""
                SELECT id,reporter_id,content_type,content_id,reason,status,created_at,reviewed_by,reviewed_at
                FROM community_reports WHERE id=? FOR UPDATE
                """, (rs, row) -> toReport(rs), id).stream().findFirst();
    }

    @Override
    public Optional<ContentReport> findById(UUID id) {
        return jdbc.query("""
                SELECT id,reporter_id,content_type,content_id,reason,status,created_at,reviewed_by,reviewed_at
                FROM community_reports WHERE id=?
                """, (rs, row) -> toReport(rs), id).stream().findFirst();
    }

    @Override
    public List<ContentReport> findPendingByContentForUpdate(ReportContentType contentType, UUID contentId) {
        return jdbc.query("""
                SELECT id,reporter_id,content_type,content_id,reason,status,created_at,reviewed_by,reviewed_at
                FROM community_reports
                WHERE status='PENDING' AND content_type=? AND content_id=?
                ORDER BY created_at,id
                FOR UPDATE
                """, (rs, row) -> toReport(rs), contentType.name(), contentId);
    }

    @Override
    public int actionPendingForContent(ReportContentType contentType, UUID contentId, UUID adminId, Instant reviewedAt) {
        return jdbc.update("""
                UPDATE community_reports SET status='ACTIONED',reviewed_by=?,reviewed_at=?
                WHERE status='PENDING' AND content_type=? AND content_id=?
                """, adminId, Timestamp.from(reviewedAt), contentType.name(), contentId);
    }

    @Override
    public ContentReport update(ContentReport report) {
        int changed = jdbc.update("""
                UPDATE community_reports SET status=?,reviewed_by=?,reviewed_at=?
                WHERE id=? AND status='PENDING'
                """, report.getStatus().name(), report.getReviewedBy(), Timestamp.from(report.getReviewedAt()), report.getId());
        if (changed != 1) throw new CommunityException("Report is no longer pending");
        return report;
    }

    @Override
    public CommunityPage<ContentReportQueueEntry> findPending(int page, int size) {
        long total = jdbc.queryForObject("SELECT count(*) FROM community_reports WHERE status='PENDING'", Long.class);
        var entries = jdbc.query("""
                SELECT r.id,r.reporter_id,r.content_type,r.content_id,r.reason,r.status,r.created_at,
                       CASE WHEN r.content_type='POST' THEN p.author_id ELSE c.author_id END AS content_author_id,
                       CASE WHEN r.content_type='POST' THEN left(coalesce(nullif(p.title,''),p.body),300)
                            ELSE left(c.body,300) END AS content_preview
                FROM community_reports r
                LEFT JOIN posts p ON r.content_type='POST' AND p.id=r.content_id
                LEFT JOIN comments c ON r.content_type='COMMENT' AND c.id=r.content_id
                WHERE r.status='PENDING'
                ORDER BY r.created_at ASC,r.id ASC
                LIMIT ? OFFSET ?
                """, (rs, row) -> new ContentReportQueueEntry(rs.getObject("id", UUID.class),
                rs.getObject("reporter_id", UUID.class), ReportContentType.valueOf(rs.getString("content_type")),
                rs.getObject("content_id", UUID.class), rs.getObject("content_author_id", UUID.class),
                rs.getString("content_preview"), rs.getString("reason"), ReportStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant()), size, (long) page * size);
        return new CommunityPage<>(entries, total);
    }

    private static ContentReport toReport(ResultSet rs) throws SQLException {
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        return new ContentReport(rs.getObject("id", UUID.class), rs.getObject("reporter_id", UUID.class),
                ReportContentType.valueOf(rs.getString("content_type")), rs.getObject("content_id", UUID.class),
                rs.getString("reason"), ReportStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(), rs.getObject("reviewed_by", UUID.class),
                reviewedAt == null ? null : reviewedAt.toInstant());
    }
}
