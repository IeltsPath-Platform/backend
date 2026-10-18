package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.BandStatisticsStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcBandStatisticsStore implements BandStatisticsStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcBandStatisticsStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Choose a version before checking its scores; an ineligible regrade must not revive an older result.
    private static final String SERIES = """
        WITH newest AS (
          SELECT DISTINCT ON (a.id) a.id AS attempt_id, a.submitted_at, r.id AS result_id,
                 r.result_version, r.completed_at AS graded_at, r.overall_band,
                 EXISTS(SELECT 1 FROM assessment_results pending WHERE pending.attempt_id = a.id
                        AND pending.result_version > r.result_version
                        AND pending.status IN ('DRAFT', 'PROCESSING')) AS regrade_pending
          FROM assessment_attempts a
          JOIN assessment_results r ON r.attempt_id = a.id AND r.status = 'COMPLETED'
          WHERE a.user_id = :user AND a.attempt_type = :kind
            AND a.submitted_at >= :from AND a.submitted_at < :until
          ORDER BY a.id, r.result_version DESC
        ), eligible AS (
          SELECT n.*, s.band, s.source
          FROM newest n
          JOIN LATERAL (
            SELECT CASE WHEN :skill = 'OVERALL' THEN
                            CASE WHEN n.overall_band BETWEEN 0 AND 9 THEN n.overall_band END
                        ELSE max(ss.band) END AS band,
                   string_agg(DISTINCT ss.grading_source, ',' ORDER BY ss.grading_source) AS source
            FROM skill_scores ss
            WHERE ss.result_id = n.result_id
              AND (:skill = 'OVERALL' OR ss.skill = :skill)
              AND ss.band BETWEEN 0 AND 9 AND ss.grading_source IN ('AUTO', 'AI', 'HUMAN')
            HAVING count(*) = CASE WHEN :skill = 'OVERALL' THEN 4 ELSE 1 END
          ) s ON s.band IS NOT NULL
          WHERE NOT (:kind = 'PLACEMENT' AND :skill IN ('SPEAKING', 'OVERALL'))
        )
        """;

    @Override
    public Page history(UUID user, Instant from, Instant until, String kind, String skill, Cursor cursor, int limit) {
        var parameters = new MapSqlParameterSource()
                .addValue("user", user).addValue("from", Timestamp.from(from))
                .addValue("until", Timestamp.from(until)).addValue("kind", kind)
                .addValue("skill", skill).addValue("limit", limit);
        var summary = jdbc.query(SERIES + """
            SELECT (SELECT band::double precision FROM eligible ORDER BY submitted_at, attempt_id LIMIT 1) AS baseline,
                   (SELECT band::double precision FROM eligible ORDER BY submitted_at DESC, attempt_id DESC LIMIT 1) AS latest,
                   min(submitted_at) AS data_from, count(*) AS eligible_count
            FROM eligible
            """, parameters, (rs, row) -> new Page(List.of(), (Double) rs.getObject("baseline"),
                (Double) rs.getObject("latest"),
                rs.getTimestamp("data_from") == null ? null : rs.getTimestamp("data_from").toInstant(),
                rs.getLong("eligible_count"))).getFirst();

        String after = "";
        if (cursor != null) {
            after = " WHERE (submitted_at, attempt_id) < (:cursorTime, :cursorId)";
            parameters.addValue("cursorTime", Timestamp.from(cursor.submittedAt()))
                    .addValue("cursorId", cursor.attemptId());
        }
        var points = jdbc.query(SERIES + "SELECT * FROM eligible" + after
                        + " ORDER BY submitted_at DESC, attempt_id DESC LIMIT :limit", parameters,
                (rs, row) -> new Point(rs.getObject("attempt_id", UUID.class), rs.getObject("result_id", UUID.class),
                        rs.getInt("result_version"), rs.getTimestamp("submitted_at").toInstant(),
                        rs.getTimestamp("graded_at") == null ? null : rs.getTimestamp("graded_at").toInstant(),
                        rs.getDouble("band"), rs.getBoolean("regrade_pending"), rs.getString("source"),
                        "PLACEMENT".equals(kind) ? "PLACEMENT_ESTIMATE" : "GRADED"));
        return new Page(points, summary.baseline(), summary.latest(), summary.dataFrom(), summary.eligibleCount());
    }
}
