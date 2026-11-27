package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.AiGradingReportRepository;
import com.ieltspath.learning.domain.vo.AiGradingReport;
import com.ieltspath.learning.domain.vo.AiGradingReport.ReportStatus;
import com.ieltspath.learning.domain.vo.AiGradingReport.SubmissionType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAiGradingReportRepository implements AiGradingReportRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAiGradingReportRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<UUID> findSubmissionOwner(SubmissionType type, UUID submissionId) {
        String sql = switch (type) {
            case WRITING -> "SELECT user_id FROM lesson_writing_submissions WHERE id = :id";
            case SPEAKING -> "SELECT user_id FROM speaking_submissions WHERE id = :id";
        };
        var rows = jdbc.query(sql, new MapSqlParameterSource("id", submissionId),
                (rs, rowNum) -> rs.getObject("user_id", UUID.class));
        return rows.stream().findFirst();
    }

    @Override
    public SaveResult saveIdempotent(UUID id, UUID userId, UUID submissionId, SubmissionType type, String reason) {
        var args = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("submissionId", submissionId)
                .addValue("type", type.name())
                .addValue("reason", reason);
        int inserted = jdbc.update("""
                INSERT INTO ai_grading_reports (id, user_id, submission_id, submission_type, reason, status)
                VALUES (:id, :userId, :submissionId, :type, :reason, 'OPEN')
                ON CONFLICT (user_id, submission_id) DO NOTHING
                """, args);
        AiGradingReport report = jdbc.queryForObject("""
                SELECT id, user_id, submission_id, submission_type, reason, status, created_at
                FROM ai_grading_reports
                WHERE user_id = :userId AND submission_id = :submissionId
                """, args, (rs, rowNum) -> toReport(rs));
        return new SaveResult(report, inserted > 0);
    }

    @Override
    public Page findPage(ReportStatus status, SubmissionType type, int page, int size) {
        var args = new MapSqlParameterSource()
                .addValue("limit", size)
                .addValue("offset", (long) page * size);
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null) {
            where.append(" AND status = :status");
            args.addValue("status", status.name());
        }
        if (type != null) {
            where.append(" AND submission_type = :type");
            args.addValue("type", type.name());
        }
        Long totalObj = jdbc.queryForObject("SELECT count(*) FROM ai_grading_reports" + where, args, Long.class);
        long total = totalObj == null ? 0L : totalObj;
        var items = jdbc.query(
                "SELECT id, user_id, submission_id, submission_type, reason, status, created_at FROM ai_grading_reports"
                        + where + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset",
                args,
                (rs, rowNum) -> toReport(rs));
        int totalPages = size == 0 ? 0 : (int) ((total + size - 1) / size);
        return new Page(items, page, size, total, totalPages);
    }

    private static AiGradingReport toReport(ResultSet rs) throws SQLException {
        return new AiGradingReport(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("submission_id", UUID.class),
                SubmissionType.valueOf(rs.getString("submission_type")),
                rs.getString("reason"),
                ReportStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
