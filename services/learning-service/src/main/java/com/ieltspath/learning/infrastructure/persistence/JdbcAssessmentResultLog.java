package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.application.port.AssessmentResultLog;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcAssessmentResultLog implements AssessmentResultLog {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAssessmentResultLog(NamedParameterJdbcTemplate jdbc) {
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
}
