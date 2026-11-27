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
        // A job that crashed on its last attempt would stay PROCESSING for ever; once its lease is over it is FAILED.
        jdbc.update("""
                UPDATE user_deletion_jobs
                SET status = 'FAILED', completed_at = CURRENT_TIMESTAMP, last_error = COALESCE(last_error, 'LEASE_EXPIRED')
                WHERE status = 'PROCESSING' AND attempts >= ?
                  AND claimed_at < CURRENT_TIMESTAMP - make_interval(secs => ?)
                """, maxAttempts, leaseSeconds);
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

    /**
     * Speaking answers whose recording has not been dealt with yet. A recording is marked like the retention job does
     * ({@code recording_deleted_at}), because the row check requires a Speaking answer to keep its reference.
     */
    public List<SpeakingSubmissionRef> findSpeakingSubmissionsBatch(UUID userId, int limit) {
        return jdbc.query("""
                SELECT id, audio_reference
                FROM learner_submissions
                WHERE user_id = ? AND skill = 'SPEAKING' AND recording_deleted_at IS NULL
                ORDER BY id ASC
                LIMIT ?
                """, (rs, rowNum) -> new SpeakingSubmissionRef(
                UUID.fromString(rs.getString("id")),
                rs.getString("audio_reference")), userId, limit);
    }

    /** Only references under this prefix are objects in the recordings bucket, as for the retention job. */
    public static boolean isStoredRecording(String reference) {
        return reference != null && reference.startsWith("speaking/");
    }

    public void markRecordingsDeleted(List<UUID> ids) {
        if (ids.isEmpty()) return;
        jdbc.update(con -> {
            var statement = con.prepareStatement("""
                    UPDATE learner_submissions SET recording_deleted_at = CURRENT_TIMESTAMP
                    WHERE id = ANY (?) AND recording_deleted_at IS NULL
                    """);
            statement.setArray(1, con.createArrayOf("uuid", ids.toArray()));
            return statement;
        });
    }

    /** Every table with a user column that {@link #deleteUserAssessmentData} handles; checked against the schema by a test. */
    public static final java.util.Set<String> HANDLED_TABLES = java.util.Set.of(
            "assessment_attempts", "learner_submissions", "grading_jobs", "human_reviews", "human_review_submissions",
            "essay_grade_comparisons", "examiner_qa_reviews", "ai_gold_essays", "placement_speaking_grades",
            "video_practice_attempts", "llm_usage_daily");

    /** Stands in for a deleted examiner or reviewer in columns that cannot be empty. */
    public static final UUID DELETED_USER = new UUID(0L, 0L);

    /**
     * Removes the learner's own work (attempts, answers, essays, recordings, gold-set copies of their essays) and takes
     * the user out of work they did for others as an examiner: finished reviews stay without a grader, unfinished ones
     * go back to the queue. Statements run children first so every foreign key holds.
     */
    public void deleteUserAssessmentData(UUID userId) {
        // Events about the learner carry their scores and bands; the table has no user column, the payload names them.
        jdbc.update("""
                DELETE FROM outbox_events
                WHERE payload->>'learnerId' = ? OR payload #>> '{data,userId}' = ?
                """, userId.toString(), userId.toString());
        String submissions = "SELECT id FROM learner_submissions WHERE user_id = ?";
        String reviewSubmissions = "SELECT id FROM human_review_submissions WHERE learner_id = ?";
        String jobs = "SELECT id FROM grading_jobs WHERE user_id = ? OR submission_id IN (" + submissions
                + ") OR human_review_submission_id IN (" + reviewSubmissions + ")";

        // The learner's essays and answers, with every copy and review of them.
        jdbc.update("DELETE FROM examiner_qa_reviews WHERE submission_id IN (" + submissions + ")", userId);
        jdbc.update("DELETE FROM ai_eval_items WHERE gold_id IN (SELECT id FROM ai_gold_essays WHERE comparison_submission_id IN ("
                + submissions + "))", userId);
        jdbc.update("DELETE FROM ai_gold_essays WHERE comparison_submission_id IN (" + submissions + ")", userId);
        jdbc.update("DELETE FROM essay_grade_comparisons WHERE submission_id IN (" + submissions + ")", userId);
        jdbc.update("DELETE FROM human_reviews WHERE grading_job_id IN (" + jobs + ")", userId, userId, userId);
        jdbc.update("DELETE FROM grading_jobs WHERE id IN (" + jobs + ")", userId, userId, userId);
        jdbc.update("DELETE FROM learner_submissions WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM human_review_submissions WHERE learner_id = ?", userId);

        // The learner's attempts: results and items reach each other both ways, so both paths are cleared.
        String attempts = "SELECT id FROM assessment_attempts WHERE user_id = ?";
        String results = "SELECT id FROM assessment_results WHERE attempt_id IN (" + attempts + ")";
        String items = "SELECT i.id FROM attempt_items i JOIN attempt_sections s ON s.id = i.attempt_section_id"
                + " WHERE s.attempt_id IN (" + attempts + ")";
        String itemResults = "SELECT id FROM item_results WHERE result_id IN (" + results + ") OR attempt_item_id IN ("
                + items + ")";
        jdbc.update("DELETE FROM item_result_knowledge_judgments WHERE item_result_id IN (" + itemResults + ")", userId, userId);
        jdbc.update("DELETE FROM error_analysis_items WHERE item_result_id IN (" + itemResults + ")", userId, userId);
        jdbc.update("DELETE FROM item_results WHERE id IN (" + itemResults + ")", userId, userId);
        jdbc.update("DELETE FROM skill_scores WHERE result_id IN (" + results + ")", userId);
        jdbc.update("DELETE FROM assessment_results WHERE attempt_id IN (" + attempts + ")", userId);
        jdbc.update("DELETE FROM attempt_responses WHERE attempt_item_id IN (" + items + ")", userId);
        jdbc.update("DELETE FROM attempt_item_knowledge_points WHERE attempt_item_id IN (" + items + ")", userId);
        jdbc.update("DELETE FROM attempt_items WHERE id IN (" + items + ")", userId);
        jdbc.update("DELETE FROM attempt_sections WHERE attempt_id IN (" + attempts + ")", userId);
        jdbc.update("DELETE FROM placement_speaking_grades WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM assessment_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM video_practice_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM llm_usage_daily WHERE user_id = ?", userId);

        // Work the user did as an examiner: unfinished reviews go back to the queue, finished ones keep their grade.
        jdbc.update("""
                UPDATE human_reviews SET status = 'QUEUED', grader_user_id = NULL, assigned_at = NULL, started_at = NULL,
                    completed_at = NULL, draft_payload = NULL, draft_saved_at = NULL
                WHERE grader_user_id = ? AND status IN ('ASSIGNED', 'IN_REVIEW')
                """, userId);
        jdbc.update("UPDATE human_reviews SET grader_user_id = NULL WHERE grader_user_id = ?", userId);
        jdbc.update("""
                UPDATE grading_jobs SET assigned_examiner_id = NULL, claimed_at = NULL
                WHERE assigned_examiner_id = ? AND status IN ('QUEUED', 'PROCESSING')
                """, userId);
        jdbc.update("UPDATE grading_jobs SET assigned_examiner_id = NULL WHERE assigned_examiner_id = ?", userId);
        jdbc.update("""
                UPDATE grading_jobs SET requested_examiner_id = NULL, requested_at = NULL WHERE requested_examiner_id = ?
                """, userId);
        jdbc.update("UPDATE essay_grade_comparisons SET examiner_id = NULL WHERE examiner_id = ?", userId);
        // QA rows need both people and two different ones: a row whose other side is already gone is dropped.
        jdbc.update("""
                DELETE FROM examiner_qa_reviews
                WHERE (examiner_id = ? AND reviewer_id = ?) OR (reviewer_id = ? AND examiner_id = ?)
                """, userId, DELETED_USER, userId, DELETED_USER);
        jdbc.update("UPDATE examiner_qa_reviews SET examiner_id = ? WHERE examiner_id = ?", DELETED_USER, userId);
        jdbc.update("UPDATE examiner_qa_reviews SET reviewer_id = ? WHERE reviewer_id = ?", DELETED_USER, userId);
        jdbc.update("UPDATE ai_gold_essays SET created_by = ? WHERE created_by = ?", DELETED_USER, userId);
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
