package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.RemediationFlagRepository;
import com.ieltspath.learning.domain.vo.RemediationFlag;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcRemediationFlagRepository implements RemediationFlagRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRemediationFlagRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(RemediationFlag flag) {
        jdbc.update("""
                INSERT INTO remediation_flags (id, user_id, course_id, topic_id, kind, source_attempt_id, review_ids)
                VALUES (:id, :userId, :courseId, :topicId, :kind, :attemptId, CAST(:reviewIds AS uuid[]))
                ON CONFLICT (source_attempt_id, kind) DO NOTHING
                """, new MapSqlParameterSource().addValue("id", flag.id()).addValue("userId", flag.userId())
                .addValue("courseId", flag.courseId()).addValue("topicId", flag.topicId())
                .addValue("kind", flag.kind().name()).addValue("attemptId", flag.sourceAttemptId())
                .addValue("reviewIds", JdbcCourseOutcomeRepository.uuidArray(flag.reviewIds())));
    }

    @Override
    public List<RemediationFlag> findByCourse(UUID userId, UUID courseId) {
        return jdbc.query("""
                SELECT f.id, f.topic_id, f.kind, f.source_attempt_id, f.review_ids, f.created_at,
                       EXISTS (SELECT 1 FROM review_items r WHERE r.id = ANY (f.review_ids)
                               AND r.status = 'PENDING') AS is_open
                FROM remediation_flags f WHERE f.user_id = :userId AND f.course_id = :courseId
                ORDER BY f.created_at, f.id
                """, new MapSqlParameterSource("userId", userId).addValue("courseId", courseId), (row, index) ->
                new RemediationFlag(row.getObject("id", UUID.class), userId, courseId,
                        row.getObject("topic_id", UUID.class), RemediationFlag.Kind.valueOf(row.getString("kind")),
                        row.getObject("source_attempt_id", UUID.class), SqlArrays.uuids(row.getArray("review_ids")),
                        row.getTimestamp("created_at").toInstant(), row.getBoolean("is_open")));
    }
}
