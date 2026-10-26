package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.ExaminerQueueStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Repository
public class JdbcExaminerQueueStore implements ExaminerQueueStore {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;
    public JdbcExaminerQueueStore(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }
    private static final String FROM = """
            FROM grading_jobs j
            LEFT JOIN learner_submissions l ON l.id=j.submission_id
            LEFT JOIN human_review_submissions h ON h.id=j.human_review_submission_id
            LEFT JOIN attempt_items i ON i.id=l.attempt_item_id
            LEFT JOIN attempt_sections s ON s.id=i.attempt_section_id
            LEFT JOIN assessment_attempts a ON a.id=s.attempt_id
            LEFT JOIN human_reviews hr ON hr.grading_job_id=j.id
            """;
    private static final String SELECT = """
            SELECT j.id, COALESCE(j.submission_id,j.human_review_submission_id) AS submission_id,
                   a.id AS attempt_id, i.id AS attempt_item_id, a.attempt_type,
                   COALESCE(h.source,a.attempt_type) AS source,j.skill,
                   COALESCE(i.answer_snapshot->'answerSpec'->>'task', l.prompt_snapshot->>'task',h.task) AS task,
                   j.status, COALESCE(j.reason,'HUMAN_CREDIT') AS reason, j.assigned_examiner_id,
                   j.claimed_at, j.created_at,
                   GREATEST(0, EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP-COALESCE(j.requested_at,j.created_at))))::bigint AS wait_seconds,
                   COALESCE(j.requested_at,j.created_at) <= CURRENT_TIMESTAMP - INTERVAL '48 hours' AS overdue,
                   j.requested_examiner_id, (hr.draft_payload IS NOT NULL) AS has_draft
            """;
    @Override public QueuePage list(String mode, String status, int page, int size) {
        return listByReason(mode, status, page, size, null);
    }
    @Override public QueuePage listByReason(String mode, String status, int page, int size, String reason) {
        return page(null, mode, status, page, size, reason);
    }
    @Override public QueuePage listFor(UUID examinerId, String mode, String status, int page, int size) {
        return page(examinerId, mode, status, page, size, null);
    }
    /** An examiner sees only the jobs the learner chose them for; no examiner means every job (administrators). */
    private QueuePage page(UUID examiner, String mode, String status, int page, int size, String reason) {
        var args = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("mode", mode)
                .addValue("status", status).addValue("reason", reason).addValue("limit", size)
                .addValue("offset", (long)page*size).addValue("examiner", examiner == null ? null : examiner.toString());
        String scope = " AND (CAST(:examiner AS VARCHAR) IS NULL OR (CAST(requested_examiner_id AS VARCHAR)=CAST(:examiner AS VARCHAR) AND COALESCE(reason,'')<>'QA_SAMPLE'))";
        var rows = jdbc.query(SELECT+FROM+" WHERE j.grading_mode=:mode AND j.status=:status AND (CAST(:reason AS VARCHAR) IS NULL OR j.reason=CAST(:reason AS VARCHAR))"
                + scope.replace("requested_examiner_id", "j.requested_examiner_id").replace("COALESCE(reason", "COALESCE(j.reason")
                + " ORDER BY COALESCE(j.requested_at,j.created_at),j.id LIMIT :limit OFFSET :offset", args, JdbcExaminerQueueStore::entry);
        Long total = jdbc.queryForObject("SELECT count(*) FROM grading_jobs WHERE grading_mode=:mode AND status=:status AND (CAST(:reason AS VARCHAR) IS NULL OR reason=CAST(:reason AS VARCHAR))" + scope, args, Long.class);
        return new QueuePage(rows, page, size, total == null ? 0 : total);
    }
    @Override public QueuePage claimedQaBy(UUID reviewerId, int page, int size) {
        var args = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource().addValue("reviewer", reviewerId)
                .addValue("limit", size).addValue("offset", (long) page * size);
        String open = " WHERE j.grading_mode='HUMAN' AND j.reason='QA_SAMPLE' AND j.assigned_examiner_id=:reviewer"
                + " AND j.status IN ('QUEUED','PROCESSING')";
        var rows = jdbc.query(SELECT + FROM + open + " ORDER BY j.claimed_at,j.id LIMIT :limit OFFSET :offset", args,
                JdbcExaminerQueueStore::entry);
        Long total = jdbc.queryForObject("SELECT count(*) FROM grading_jobs j" + open, args, Long.class);
        return new QueuePage(rows, page, size, total == null ? 0 : total);
    }
    @Override public Optional<QueueEntry> find(UUID jobId) {
        return jdbc.query(SELECT+FROM+" WHERE j.id=:id AND j.grading_mode='HUMAN'", Map.of("id",jobId), JdbcExaminerQueueStore::entry).stream().findFirst();
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public boolean claim(UUID jobId, UUID examinerId) {
        var args = Map.of("id",jobId,"examiner",examinerId);
        List<UUID> changed = jdbc.query("""
                WITH target AS (
                  SELECT j.id,s.attempt_id,j.reason FROM grading_jobs j
                  LEFT JOIN learner_submissions l ON l.id=j.submission_id
                  LEFT JOIN attempt_items i ON i.id=l.attempt_item_id
                  LEFT JOIN attempt_sections s ON s.id=i.attempt_section_id
                  WHERE j.id=:id AND j.grading_mode='HUMAN' AND j.status IN ('QUEUED','PROCESSING')
                    AND (CASE WHEN COALESCE(j.reason,'')='QA_SAMPLE'
                              THEN (j.assigned_examiner_id IS NULL OR j.claimed_at < CURRENT_TIMESTAMP-INTERVAL '24 hours')
                              ELSE (j.requested_examiner_id=:examiner
                                    AND (j.assigned_examiner_id IS NULL OR j.assigned_examiner_id=:examiner)) END)
                )
                UPDATE grading_jobs j SET assigned_examiner_id=:examiner,claimed_at=CURRENT_TIMESTAMP
                FROM target t
                WHERE (j.id=t.id OR (t.attempt_id IS NOT NULL AND EXISTS (
                    SELECT 1 FROM learner_submissions l JOIN attempt_items i ON i.id=l.attempt_item_id
                    JOIN attempt_sections s ON s.id=i.attempt_section_id
                    WHERE l.id=j.submission_id AND s.attempt_id=t.attempt_id)))
                  AND j.grading_mode='HUMAN' AND j.status IN ('QUEUED','PROCESSING')
                  AND (CASE WHEN COALESCE(j.reason,'')='QA_SAMPLE'
                            THEN (j.assigned_examiner_id IS NULL OR j.claimed_at < CURRENT_TIMESTAMP-INTERVAL '24 hours')
                            ELSE (j.requested_examiner_id=:examiner
                                  AND (j.assigned_examiner_id IS NULL OR j.assigned_examiner_id=:examiner)) END)
                  AND (j.id=t.id OR (COALESCE(t.reason,'')<>'QA_SAMPLE' AND COALESCE(j.reason,'')<>'QA_SAMPLE'))
                  AND (COALESCE(t.reason,'')<>'QA_SAMPLE' OR NOT EXISTS(
                    SELECT 1 FROM essay_grade_comparisons c WHERE c.submission_id=j.submission_id AND c.examiner_id=:examiner))
                RETURNING j.id
                """,args,(r,n)->r.getObject("id",UUID.class));
        if (!changed.isEmpty()) jdbc.update("""
                INSERT INTO human_reviews(id,grading_job_id,grader_user_id,status,assigned_at)
                SELECT gen_random_uuid(),id,:examiner,'ASSIGNED',CURRENT_TIMESTAMP FROM grading_jobs WHERE id IN (:ids)
                ON CONFLICT(grading_job_id) DO UPDATE SET
                draft_payload=CASE WHEN human_reviews.grader_user_id IS DISTINCT FROM :examiner THEN NULL
                    ELSE human_reviews.draft_payload END,
                draft_saved_at=CASE WHEN human_reviews.grader_user_id IS DISTINCT FROM :examiner THEN NULL
                    ELSE human_reviews.draft_saved_at END,
                status=CASE WHEN human_reviews.grader_user_id IS NOT DISTINCT FROM :examiner THEN human_reviews.status
                    ELSE 'ASSIGNED' END,
                assigned_at=CASE WHEN human_reviews.grader_user_id IS NOT DISTINCT FROM :examiner
                    THEN COALESCE(human_reviews.assigned_at,CURRENT_TIMESTAMP) ELSE CURRENT_TIMESTAMP END,
                started_at=CASE WHEN human_reviews.grader_user_id IS NOT DISTINCT FROM :examiner
                    THEN human_reviews.started_at ELSE NULL END,
                completed_at=NULL,grader_user_id=:examiner
                """,Map.of("ids",changed,"examiner",examinerId));
        return changed.contains(jobId);
    }
    @Override public Optional<Essay> essay(UUID id) {
        return jdbc.query("""
                SELECT j.id,a.id AS attempt_id,i.id AS attempt_item_id,
                  COALESCE(i.question_snapshot->>'stem',l.prompt_snapshot->>'stem',h.prompt_snapshot->>'stem') AS prompt,
                  COALESCE(l.text_payload,h.essay_text) AS essay,
                  COALESCE(i.answer_snapshot->'answerSpec'->>'task',l.prompt_snapshot->>'task',h.task) AS task,
                  h.rubric_snapshot::text AS rubric,h.source
                """+FROM+" WHERE j.id=:id AND j.grading_mode='HUMAN'",Map.of("id",id), (r,n) -> {
            JsonNode rubric = null;
            try {
                if (r.getString("rubric") != null) rubric = json.readTree(r.getString("rubric"));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Stored rubric snapshot is invalid", exception);
            }
            return new Essay(id,r.getObject("attempt_id",UUID.class),r.getObject("attempt_item_id",UUID.class),
                    r.getString("prompt"),r.getString("essay"),r.getString("task"),rubric,r.getString("source"));
        }).stream().findFirst();
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public List<QueueEntry> lockAttempt(UUID attempt) {
        return jdbc.query(SELECT+FROM+" WHERE a.id=:attempt AND j.grading_mode='HUMAN' AND j.status IN ('QUEUED','PROCESSING') ORDER BY j.id FOR UPDATE OF j",Map.of("attempt",attempt),JdbcExaminerQueueStore::entry);
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void reopenCompleted(UUID attempt) {
        var args=Map.of("attempt",attempt);
        jdbc.update("""
                UPDATE grading_jobs j SET status='QUEUED',assigned_examiner_id=NULL,claimed_at=NULL,completed_at=NULL
                FROM learner_submissions l,attempt_items i,attempt_sections s
                WHERE j.submission_id=l.id AND l.attempt_item_id=i.id AND i.attempt_section_id=s.id
                  AND s.attempt_id=:attempt AND j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE' AND j.status='COMPLETED'
                """,args);
        jdbc.update("""
                UPDATE human_reviews h SET status='QUEUED',grader_user_id=NULL,assigned_at=NULL,started_at=NULL,completed_at=NULL
                FROM grading_jobs j,learner_submissions l,attempt_items i,attempt_sections s
                WHERE h.grading_job_id=j.id AND j.submission_id=l.id AND l.attempt_item_id=i.id
                  AND i.attempt_section_id=s.id AND s.attempt_id=:attempt AND j.grading_mode='HUMAN'
                  AND COALESCE(j.reason,'')<>'QA_SAMPLE'
                  AND j.status='QUEUED' AND j.assigned_examiner_id IS NULL
                """,args);
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void completeAttempt(UUID attempt, UUID examiner) {
        var args=Map.of("attempt",attempt,"examiner",examiner);
        jdbc.update("""
                UPDATE grading_jobs j SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP
                FROM learner_submissions l,attempt_items i,attempt_sections s
                WHERE j.submission_id=l.id AND l.attempt_item_id=i.id AND i.attempt_section_id=s.id
                AND s.attempt_id=:attempt AND j.grading_mode='HUMAN' AND j.assigned_examiner_id=:examiner
                AND COALESCE(j.reason,'')<>'QA_SAMPLE' AND j.status IN ('QUEUED','PROCESSING')
                """,args);
        jdbc.update("""
                UPDATE human_reviews h SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP
                FROM grading_jobs j,learner_submissions l,attempt_items i,attempt_sections s
                WHERE h.grading_job_id=j.id AND j.submission_id=l.id AND l.attempt_item_id=i.id
                AND i.attempt_section_id=s.id AND s.attempt_id=:attempt AND j.assigned_examiner_id=:examiner
                AND j.grading_mode='HUMAN' AND COALESCE(j.reason,'')<>'QA_SAMPLE' AND j.status='COMPLETED'
                """,args);
    }
    private static QueueEntry entry(ResultSet r,int n) throws SQLException {
        var claimed=r.getTimestamp("claimed_at");
        return new QueueEntry(r.getObject("id",UUID.class),r.getObject("submission_id",UUID.class),
                r.getObject("attempt_id",UUID.class),r.getObject("attempt_item_id",UUID.class),r.getString("attempt_type"),
                r.getString("source"),r.getString("skill"),r.getString("task"),r.getString("status"),r.getString("reason"),
                r.getObject("assigned_examiner_id",UUID.class),claimed==null?null:claimed.toInstant(),
                r.getTimestamp("created_at").toInstant(),r.getLong("wait_seconds"),r.getBoolean("overdue"),
                r.getObject("requested_examiner_id",UUID.class),r.getBoolean("has_draft"));
    }
}
