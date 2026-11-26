package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.DeletedUserStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcDeletedUserStore implements DeletedUserStore {
    private final JdbcTemplate jdbc;

    public JdbcDeletedUserStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean isDeleted(UUID userId) {
        if (userId == null) return false;
        Integer found = jdbc.query(
                "SELECT 1 FROM deleted_users WHERE user_id = ?",
                rs -> rs.next() ? 1 : null,
                userId);
        return found != null;
    }

    @Override
    public void recordDeletion(UUID eventId, UUID userId, Instant occurredAt) {
        Timestamp ts = Timestamp.from(occurredAt);
        jdbc.update("""
                INSERT INTO deleted_users (user_id, deleted_at)
                VALUES (?, ?)
                ON CONFLICT (user_id) DO NOTHING
                """, userId, ts);
        jdbc.update("""
                INSERT INTO user_deletion_jobs (event_id, user_id, status, created_at)
                VALUES (?, ?, 'PENDING', ?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, userId, ts);
    }

    public record Job(UUID eventId, UUID userId, int attempts) { }

    public record SpeakingSubmissionRef(UUID id, String mediaReference) { }

    public List<Job> claimPendingJobs(int limit, long leaseSeconds, int maxAttempts) {
        return jdbc.query("""
                UPDATE user_deletion_jobs
                SET status = 'PROCESSING',
                    claimed_at = CURRENT_TIMESTAMP,
                    attempts = attempts + 1
                WHERE event_id IN (
                    SELECT event_id
                    FROM user_deletion_jobs
                    WHERE (status = 'PENDING'
                           OR (status = 'PROCESSING' AND claimed_at < CURRENT_TIMESTAMP - make_interval(secs => ?)))
                      AND attempts < ?
                    ORDER BY created_at ASC
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                RETURNING event_id, user_id, attempts
                """, (rs, rowNum) -> new Job(
                UUID.fromString(rs.getString("event_id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getInt("attempts")), leaseSeconds, maxAttempts, limit);
    }

    public List<SpeakingSubmissionRef> findSpeakingSubmissionsBatch(UUID userId, int limit) {
        return jdbc.query("""
                SELECT id, media_reference
                FROM learner_submissions
                WHERE user_id = ? AND skill = 'SPEAKING' AND media_reference IS NOT NULL
                ORDER BY id ASC
                LIMIT ?
                """, (rs, rowNum) -> new SpeakingSubmissionRef(
                UUID.fromString(rs.getString("id")),
                rs.getString("media_reference")), userId, limit);
    }

    public void clearMediaReferenceByIds(List<UUID> ids) {
        if (ids.isEmpty()) return;
        for (UUID id : ids) {
            jdbc.update("UPDATE learner_submissions SET media_reference = NULL WHERE id = ?", id);
        }
    }

    public void deleteUserAssessmentData(UUID userId) {
        jdbc.update("""
                DELETE FROM item_result_knowledge_judgments
                WHERE item_result_id IN (
                    SELECT ir.id FROM item_results ir
                    JOIN assessment_results ar ON ar.id = ir.result_id
                    JOIN assessment_attempts aa ON aa.id = ar.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM error_analysis_items
                WHERE result_id IN (
                    SELECT ar.id FROM assessment_results ar
                    JOIN assessment_attempts aa ON aa.id = ar.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM item_results
                WHERE result_id IN (
                    SELECT ar.id FROM assessment_results ar
                    JOIN assessment_attempts aa ON aa.id = ar.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM skill_scores
                WHERE result_id IN (
                    SELECT ar.id FROM assessment_results ar
                    JOIN assessment_attempts aa ON aa.id = ar.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM assessment_results
                WHERE attempt_id IN (SELECT id FROM assessment_attempts WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM attempt_responses
                WHERE attempt_item_id IN (
                    SELECT ai.id FROM attempt_items ai
                    JOIN attempt_sections asec ON asec.id = ai.section_id
                    JOIN assessment_attempts aa ON aa.id = asec.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM attempt_item_knowledge_points
                WHERE attempt_item_id IN (
                    SELECT ai.id FROM attempt_items ai
                    JOIN attempt_sections asec ON asec.id = ai.section_id
                    JOIN assessment_attempts aa ON aa.id = asec.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM attempt_items
                WHERE section_id IN (
                    SELECT asec.id FROM attempt_sections asec
                    JOIN assessment_attempts aa ON aa.id = asec.attempt_id
                    WHERE aa.user_id = ?
                )
                """, userId);
        jdbc.update("""
                DELETE FROM attempt_sections
                WHERE attempt_id IN (SELECT id FROM assessment_attempts WHERE user_id = ?)
                """, userId);
        jdbc.update("DELETE FROM placement_speaking_grades WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM assessment_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM human_reviews WHERE grader_user_id = ?", userId);
        jdbc.update("""
                DELETE FROM human_reviews
                WHERE submission_id IN (SELECT id FROM learner_submissions WHERE user_id = ?)
                """, userId);
        jdbc.update("""
                DELETE FROM essay_grade_comparisons
                WHERE grading_job_id IN (SELECT id FROM grading_jobs WHERE user_id = ?)
                """, userId);
        jdbc.update("UPDATE essay_grade_comparisons SET examiner_id = NULL WHERE examiner_id = ?", userId);
        jdbc.update("DELETE FROM grading_jobs WHERE user_id = ?", userId);
        jdbc.update("UPDATE grading_jobs SET assigned_examiner_id = NULL WHERE assigned_examiner_id = ?", userId);
        jdbc.update("UPDATE grading_jobs SET requested_examiner_id = NULL, requested_at = NULL WHERE requested_examiner_id = ?", userId);
        jdbc.update("DELETE FROM learner_submissions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM human_review_submissions WHERE learner_id = ?", userId);
        jdbc.update("DELETE FROM video_practice_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM llm_usage_daily WHERE user_id = ?", userId);
    }

    public void markJobDone(UUID eventId) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'DONE', completed_at = CURRENT_TIMESTAMP, last_error = NULL
                WHERE event_id = ?
                """, eventId);
    }

    public void markJobFailed(UUID eventId, String errorCode) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'FAILED', last_error = ?, completed_at = CURRENT_TIMESTAMP
                WHERE event_id = ?
                """, errorCode, eventId);
    }

    public void recordJobError(UUID eventId, String errorCode) {
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET last_error = ?
                WHERE event_id = ?
                """, errorCode, eventId);
    }
}
