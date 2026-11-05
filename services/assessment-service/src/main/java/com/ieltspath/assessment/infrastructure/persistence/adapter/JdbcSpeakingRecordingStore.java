package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.SpeakingRecordingStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSpeakingRecordingStore implements SpeakingRecordingStore {
    private final NamedParameterJdbcTemplate jdbc;

    public JdbcSpeakingRecordingStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Recording> findById(UUID submissionId) {
        return jdbc.query("""
                        SELECT id,user_id,audio_reference,recording_deleted_at
                        FROM learner_submissions WHERE id=:id AND skill='SPEAKING'
                        """, new MapSqlParameterSource("id", submissionId), JdbcSpeakingRecordingStore::recording)
                .stream().findFirst();
    }

    @Override
    public boolean isAssignedExaminer(UUID submissionId, UUID examinerId) {
        Boolean assigned = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM grading_jobs
                    WHERE submission_id=:submission AND assigned_examiner_id=:examiner
                      AND skill='SPEAKING' AND grading_mode='HUMAN' AND COALESCE(reason,'')<>'QA_SAMPLE'
                )
                """, new MapSqlParameterSource("submission", submissionId).addValue("examiner", examinerId), Boolean.class);
        return Boolean.TRUE.equals(assigned);
    }

    @Override
    public List<Recording> findExpired(Instant cutoff, int limit) {
        return jdbc.query("""
                SELECT id,user_id,audio_reference,recording_deleted_at
                FROM learner_submissions
                WHERE skill='SPEAKING' AND submitted_at<:cutoff AND recording_deleted_at IS NULL
                  AND audio_reference LIKE 'speaking/%'
                ORDER BY submitted_at,id LIMIT :limit
                """, new MapSqlParameterSource("cutoff", java.sql.Timestamp.from(cutoff)).addValue("limit", limit),
                JdbcSpeakingRecordingStore::recording);
    }

    @Override
    public void markDeleted(UUID submissionId, Instant deletedAt) {
        jdbc.update("""
                UPDATE learner_submissions SET recording_deleted_at=:deletedAt
                WHERE id=:id AND skill='SPEAKING' AND recording_deleted_at IS NULL
                """, new MapSqlParameterSource("id", submissionId)
                .addValue("deletedAt", java.sql.Timestamp.from(deletedAt)));
    }

    private static Recording recording(ResultSet row, int index) throws SQLException {
        var deletedAt = row.getTimestamp("recording_deleted_at");
        return new Recording(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getString("audio_reference"), deletedAt == null ? null : deletedAt.toInstant());
    }
}
