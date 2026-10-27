package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.ExaminerAssignmentStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcExaminerAssignmentStore implements ExaminerAssignmentStore {
    /** A human job with the attempt it belongs to, if any. QA samples are staff work and never part of this. */
    private static final String JOB = """
            SELECT j.id,j.user_id,j.status,s.attempt_id,j.requested_examiner_id,j.requested_at,j.examiner_changes,j.skill
            FROM grading_jobs j
            LEFT JOIN learner_submissions l ON l.id=j.submission_id
            LEFT JOIN attempt_items i ON i.id=l.attempt_item_id
            LEFT JOIN attempt_sections s ON s.id=i.attempt_section_id
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcExaminerAssignmentStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<JobRef> lockJob(UUID jobId) {
        return jdbc.query(JOB + """
                WHERE j.id=:id AND j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE' FOR UPDATE OF j
                """, new MapSqlParameterSource("id", jobId), JdbcExaminerAssignmentStore::jobRef).stream().findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<JobRef> lockOpenGroup(JobRef job) {
        if (job.attemptId() == null) return List.of(job);
        return jdbc.query(JOB + """
                WHERE s.attempt_id=:attempt AND j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE'
                  AND j.status IN ('QUEUED','PROCESSING') ORDER BY j.id FOR UPDATE OF j
                """, new MapSqlParameterSource("attempt", job.attemptId()), JdbcExaminerAssignmentStore::jobRef);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockExaminer(UUID examinerId) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))",
                new MapSqlParameterSource("key", "examiner-capacity:" + examinerId), rs -> { });
    }

    @Override
    @Transactional(readOnly = true)
    public int receivedSince(UUID examinerId, Instant since) {
        Integer received = jdbc.queryForObject("""
                SELECT count(*) FROM grading_jobs
                WHERE grading_mode='HUMAN' AND requested_examiner_id=:id AND requested_at>=:since
                  AND COALESCE(reason,'')<>'QA_SAMPLE'
                """, new MapSqlParameterSource("id", examinerId).addValue("since", java.sql.Timestamp.from(since)),
                Integer.class);
        return received == null ? 0 : received;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, Integer> receivedSince(java.util.Collection<UUID> examinerIds, Instant since) {
        java.util.Map<UUID, Integer> received = new java.util.HashMap<>();
        if (examinerIds.isEmpty()) return received;
        jdbc.query("""
                SELECT requested_examiner_id AS examiner, count(*) AS received FROM grading_jobs
                WHERE grading_mode='HUMAN' AND requested_examiner_id IN (:ids) AND requested_at>=:since
                  AND COALESCE(reason,'')<>'QA_SAMPLE'
                GROUP BY requested_examiner_id
                """, new MapSqlParameterSource("ids", examinerIds).addValue("since", java.sql.Timestamp.from(since)),
                row -> { received.put(row.getObject("examiner", UUID.class), row.getInt("received")); });
        return received;
    }

    /** The open human jobs of an examiner, QA samples apart, as the dashboard counts the late ones. */
    private static final String OVERDUE_OPEN = """
            FROM grading_jobs
            WHERE grading_mode='HUMAN' AND status IN ('QUEUED','PROCESSING') AND requested_at<=:before
              AND COALESCE(reason,'')<>'QA_SAMPLE'
            """;

    @Override
    @Transactional(readOnly = true)
    public int overdueOpen(UUID examinerId, Instant overdueBefore) {
        Integer late = jdbc.queryForObject("SELECT count(*) " + OVERDUE_OPEN + " AND requested_examiner_id=:id",
                new MapSqlParameterSource("id", examinerId).addValue("before", java.sql.Timestamp.from(overdueBefore)),
                Integer.class);
        return late == null ? 0 : late;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<UUID, Integer> overdueOpen(java.util.Collection<UUID> examinerIds, Instant overdueBefore) {
        java.util.Map<UUID, Integer> late = new java.util.HashMap<>();
        if (examinerIds.isEmpty()) return late;
        jdbc.query("SELECT requested_examiner_id AS examiner, count(*) AS late " + OVERDUE_OPEN
                        + " AND requested_examiner_id IN (:ids) GROUP BY requested_examiner_id",
                new MapSqlParameterSource("ids", examinerIds).addValue("before", java.sql.Timestamp.from(overdueBefore)),
                row -> { late.put(row.getObject("examiner", UUID.class), row.getInt("late")); });
        return late;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void cancel(UUID jobId) {
        var args = new MapSqlParameterSource("id", jobId);
        jdbc.update("UPDATE grading_jobs SET status='CANCELLED' WHERE id=:id AND status IN ('QUEUED','PROCESSING')", args);
        jdbc.update("""
                UPDATE human_reviews SET draft_payload=NULL,draft_saved_at=NULL
                WHERE grading_job_id=:id AND status<>'COMPLETED'
                """, args);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void assign(List<UUID> jobIds, UUID examinerId, boolean replacing) {
        if (jobIds.isEmpty()) return;
        var args = new MapSqlParameterSource("ids", jobIds).addValue("examiner", examinerId)
                .addValue("bump", replacing ? 1 : 0);
        jdbc.update("""
                UPDATE grading_jobs SET requested_examiner_id=:examiner,requested_at=CURRENT_TIMESTAMP,
                    assigned_examiner_id=NULL,claimed_at=NULL,examiner_changes=examiner_changes+:bump,
                    overdue_notified_at=NULL,due_soon_notified_at=NULL
                WHERE id IN (:ids) AND grading_mode='HUMAN' AND status IN ('QUEUED','PROCESSING')
                """, args);
        // The new examiner starts from nothing: the claim and the unfinished review of the previous one are dropped.
        jdbc.update("""
                UPDATE human_reviews SET status='QUEUED',grader_user_id=NULL,assigned_at=NULL,started_at=NULL,
                    completed_at=NULL,draft_payload=NULL,draft_saved_at=NULL
                WHERE grading_job_id IN (:ids) AND status<>'COMPLETED'
                """, args);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyJob> listMine(UUID learnerId) {
        return jdbc.query("""
                SELECT j.id,j.human_review_submission_id,j.skill,j.status,COALESCE(h.source,a.attempt_type) AS source,
                       j.requested_examiner_id,j.requested_at,j.examiner_changes,j.created_at,
                       CASE WHEN j.status='COMPLETED' THEN h.band END AS band,j.completed_at
                FROM grading_jobs j
                LEFT JOIN human_review_submissions h ON h.id=j.human_review_submission_id
                LEFT JOIN learner_submissions l ON l.id=j.submission_id
                LEFT JOIN attempt_items i ON i.id=l.attempt_item_id
                LEFT JOIN attempt_sections s ON s.id=i.attempt_section_id
                LEFT JOIN assessment_attempts a ON a.id=s.attempt_id
                WHERE j.user_id=:learner AND j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE'
                ORDER BY j.created_at DESC,j.id
                """, new MapSqlParameterSource("learner", learnerId), (row, index) -> new MyJob(
                row.getObject("id", UUID.class), row.getObject("human_review_submission_id", UUID.class),
                row.getString("skill"), row.getString("status"),
                row.getString("source"), row.getObject("requested_examiner_id", UUID.class),
                instant(row, "requested_at"), row.getInt("examiner_changes"), instant(row, "created_at"),
                row.getBigDecimal("band"), instant(row, "completed_at")));
    }

    private static JobRef jobRef(ResultSet row, int index) throws SQLException {
        return new JobRef(row.getObject("id", UUID.class), row.getObject("user_id", UUID.class),
                row.getString("status"), row.getObject("attempt_id", UUID.class),
                row.getObject("requested_examiner_id", UUID.class), instant(row, "requested_at"),
                row.getInt("examiner_changes"), row.getString("skill"));
    }

    private static java.time.Instant instant(ResultSet row, String column) throws SQLException {
        var timestamp = row.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
