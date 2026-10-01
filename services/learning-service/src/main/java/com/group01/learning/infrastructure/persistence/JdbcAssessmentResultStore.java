package com.group01.learning.infrastructure.persistence;

import com.group01.learning.application.port.AssessmentResultStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAssessmentResultStore implements AssessmentResultStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAssessmentResultStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Integer> appliedVersion(UUID userId, UUID attemptId) {
        return jdbc.queryForList("""
                SELECT result_version FROM assessment_result_versions WHERE user_id = :userId AND attempt_id = :attemptId
                """, Map.of("userId", userId, "attemptId", attemptId), Integer.class).stream().findFirst();
    }

    @Override
    public void recordVersion(UUID userId, UUID attemptId, int resultVersion) {
        jdbc.update("""
                INSERT INTO assessment_result_versions (user_id, attempt_id, result_version, processed_at)
                VALUES (:userId, :attemptId, :version, clock_timestamp())
                ON CONFLICT (user_id, attempt_id) DO UPDATE
                SET result_version = EXCLUDED.result_version, processed_at = EXCLUDED.processed_at
                WHERE assessment_result_versions.result_version < EXCLUDED.result_version
                """, Map.of("userId", userId, "attemptId", attemptId, "version", resultVersion));
    }

    @Override
    public void removeAttemptEvidence(UUID userId, UUID attemptId) {
        jdbc.update("DELETE FROM kp_evidence WHERE user_id = :userId AND attempt_id = :attemptId AND source = 'assessment'",
                Map.of("userId", userId, "attemptId", attemptId));
    }

    @Override
    public void appendEvidence(UUID userId, UUID attemptId, int resultVersion, List<Evidence> evidence) {
        if (evidence.isEmpty()) return;
        MapSqlParameterSource[] rows = evidence.stream().map(item -> new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("userId", userId)
                .addValue("kpId", item.knowledgePointId())
                .addValue("correct", item.correct())
                .addValue("reference", item.sourceReferenceId())
                .addValue("attemptId", attemptId)
                .addValue("version", resultVersion)).toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate("""
                INSERT INTO kp_evidence (id, user_id, kp_id, correct, source, source_reference_id, attempt_id, result_version)
                VALUES (:id, :userId, :kpId, :correct, 'assessment', :reference, :attemptId, :version)
                ON CONFLICT (user_id, source, source_reference_id) DO NOTHING
                """, rows);
    }

    @Override
    public Optional<OpenAssignment> findOpenAssignment(UUID userId, UUID packageVersionId, Instant completedAt) {
        return jdbc.query("""
                SELECT id, topic_id FROM topic_test_assignments
                WHERE user_id = :userId AND package_version_id = :versionId AND consumed_at IS NULL
                  AND assigned_at <= :completedAt
                ORDER BY assigned_at DESC LIMIT 1
                FOR UPDATE
                """, Map.of("userId", userId, "versionId", packageVersionId, "completedAt", Timestamp.from(completedAt)),
                (row, index) -> new OpenAssignment(row.getObject("id", UUID.class),
                        row.getObject("topic_id", UUID.class))).stream().findFirst();
    }

    @Override
    public void consumeAssignment(UUID assignmentId, UUID attemptId, double percent) {
        jdbc.update("""
                UPDATE topic_test_assignments
                SET consumed_attempt_id = :attemptId, consumed_at = clock_timestamp(), percent = :percent
                WHERE id = :id AND consumed_at IS NULL
                """, Map.of("id", assignmentId, "attemptId", attemptId, "percent", percent));
    }

    @Override
    public void passTopic(UUID userId, UUID topicId) {
        jdbc.update("""
                INSERT INTO topic_progress (user_id, topic_id, passed_at) VALUES (:userId, :topicId, clock_timestamp())
                ON CONFLICT (user_id, topic_id) DO UPDATE
                SET passed_at = clock_timestamp(), updated_at = clock_timestamp()
                WHERE topic_progress.passed_at IS NULL
                """, Map.of("userId", userId, "topicId", topicId));
    }
}
