package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieltspath.assessment.application.port.HumanReviewStore;
import com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import com.ieltspath.assessment.domain.exception.RevisionConflictException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHumanReviewStore implements HumanReviewStore {
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcHumanReviewStore(NamedParameterJdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional
    public Review create(CreateReview review) {
        UUID id = UUID.randomUUID();
        List<UUID> inserted = jdbc.query("""
                INSERT INTO human_review_submissions(id,request_id,learner_id,source_submission_id,source,task,
                    prompt_snapshot,essay_text,rubric_snapshot)
                VALUES (:id,:requestId,:learnerId,:sourceSubmissionId,:source,:task,CAST(:prompt AS jsonb),
                    :essay,CAST(:rubric AS jsonb))
                ON CONFLICT DO NOTHING RETURNING id
                """, params(review, id), (row, index) -> row.getObject("id", UUID.class));
        if (inserted.isEmpty()) return resolveConflict(review);

        jdbc.update("""
                INSERT INTO grading_jobs(id,submission_id,human_review_submission_id,user_id,skill,grading_mode,
                    status,idempotency_key,reason,requested_examiner_id,requested_at)
                VALUES (:jobId,NULL,:submissionId,:learnerId,'WRITING','HUMAN','QUEUED',:key,'HUMAN_CREDIT',
                    :examiner,CURRENT_TIMESTAMP)
                """, new MapSqlParameterSource().addValue("jobId", UUID.randomUUID()).addValue("submissionId", id)
                .addValue("learnerId", review.learnerId()).addValue("key", "human-review:" + review.requestId())
                .addValue("examiner", review.examinerId()));
        jdbc.update("INSERT INTO human_reviews(id,grading_job_id,status) VALUES (:id,:jobId,'QUEUED')",
                new MapSqlParameterSource("id", UUID.randomUUID()).addValue("jobId", jobId(id)));
        return new Review(review.requestId(), id, "PENDING");
    }

    private Review resolveConflict(CreateReview review) {
        List<StoredRequest> byRequest = jdbc.query("""
                SELECT id,request_id,learner_id,source_submission_id,source,task,prompt_snapshot::text AS prompt,
                       essay_text,rubric_snapshot::text AS rubric
                FROM human_review_submissions WHERE request_id=:requestId
                """, new MapSqlParameterSource("requestId", review.requestId()), (row, index) -> new StoredRequest(
                row.getObject("id", UUID.class), row.getObject("request_id", UUID.class),
                row.getObject("learner_id", UUID.class), row.getObject("source_submission_id", UUID.class),
                row.getString("source"), row.getString("task"), row.getString("prompt"), row.getString("essay_text"),
                row.getString("rubric")));
        if (!byRequest.isEmpty()) {
            StoredRequest stored = byRequest.getFirst();
            if (!same(stored, review)) throw new RevisionConflictException("requestId was already used for another review");
            return new Review(stored.requestId(), stored.id(), "PENDING");
        }
        boolean sourceExists = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM human_review_submissions WHERE source_submission_id=:sourceId)
                """, new MapSqlParameterSource("sourceId", review.sourceSubmissionId()), Boolean.class));
        if (sourceExists) throw new RevisionConflictException("Essay already has a human review request");
        throw new RevisionConflictException("Human review request conflicts with existing data");
    }

    private boolean same(StoredRequest stored, CreateReview request) {
        return stored.learnerId().equals(request.learnerId())
                && stored.sourceSubmissionId().equals(request.sourceSubmissionId())
                && stored.source().equals(request.source()) && stored.task().equals(request.task())
                && stored.essay().equals(request.essay())
                && jsonEquals(stored.prompt(), request.promptSnapshot())
                && jsonEquals(stored.rubric(), request.rubric());
    }

    private boolean jsonEquals(String stored, JsonNode requested) {
        try {
            return json.readTree(stored).equals(requested);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored human review JSON is invalid", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID requestId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM human_review_submissions WHERE request_id=:id)",
                new MapSqlParameterSource("id", requestId), Boolean.class));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResult> find(UUID reviewId, UUID learnerId) {
        return jdbc.query("""
                SELECT h.id,j.status,j.requested_examiner_id,h.band,h.criteria::text AS criteria,h.error_codes,h.feedback,
                       h.annotations::text AS annotations
                FROM human_review_submissions h JOIN grading_jobs j ON j.human_review_submission_id=h.id
                WHERE h.id=:id AND h.learner_id=:learnerId
                """, new MapSqlParameterSource("id", reviewId).addValue("learnerId", learnerId), (row, index) -> {
            String status = "COMPLETED".equals(row.getString("status")) ? "DONE" : "PENDING";
            UUID examiner = row.getObject("requested_examiner_id", UUID.class);
            if ("PENDING".equals(status)) return new ReviewResult(row.getObject("id", UUID.class), status,
                    null, null, null, null, null, examiner);
            try {
                Array codes = row.getArray("error_codes");
                String marked = row.getString("annotations");
                return new ReviewResult(row.getObject("id", UUID.class), status, row.getBigDecimal("band"),
                        json.readTree(row.getString("criteria")), codes(codes), row.getString("feedback"),
                        marked == null ? null : json.readTree(marked), examiner);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored human review criteria are invalid", exception);
            }
        }).stream().findFirst();
    }

    @Override
    @Transactional
    public Optional<Completed> complete(UUID jobId, UUID examinerId, BigDecimal band, JsonNode criteria,
                                        List<String> errorCodes, String feedback, JsonNode annotations) {
        Job job = jdbc.query("""
                SELECT human_review_submission_id,assigned_examiner_id,status,user_id,skill
                FROM grading_jobs WHERE id=:id AND grading_mode='HUMAN' FOR UPDATE
                """, new MapSqlParameterSource("id", jobId), (row, index) -> new Job(
                row.getObject("human_review_submission_id", UUID.class),
                row.getObject("assigned_examiner_id", UUID.class), row.getString("status"),
                row.getObject("user_id", UUID.class), row.getString("skill")))
                .stream().findFirst().orElseThrow(() -> new AssessmentNotFoundException("Human review job not found"));
        if (job.submissionId() == null) throw new AssessmentNotFoundException("Standalone human review not found");
        if ("COMPLETED".equals(job.status())) {
            if (alreadyCompleted(job.submissionId(), band, criteria, errorCodes, feedback, annotations)) return Optional.empty();
            throw new RevisionConflictException("Human review has already been completed");
        }
        if (!examinerId.equals(job.examinerId())) {
            throw new AssessmentAccessDeniedException("Claim the human review before completing it");
        }
        int changed = jdbc.update("""
                UPDATE human_review_submissions SET band=:band,criteria=CAST(:criteria AS jsonb),
                    error_codes=CAST(:codes AS varchar[]),feedback=:feedback,annotations=CAST(:annotations AS jsonb),
                    completed_at=CURRENT_TIMESTAMP
                WHERE id=:id AND band IS NULL
                """, new MapSqlParameterSource("band", band).addValue("criteria", criteria.toString())
                .addValue("codes", stringArray(errorCodes)).addValue("feedback", feedback)
                .addValue("annotations", annotations == null ? null : annotations.toString(), Types.VARCHAR)
                .addValue("id", job.submissionId()));
        if (changed != 1) throw new RevisionConflictException("Human review has already been completed");
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=:id",
                new MapSqlParameterSource("id", jobId));
        // A completed review keeps no draft: the draft goes in the same statement that sets COMPLETED.
        jdbc.update("""
                UPDATE human_reviews SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP,
                    draft_payload=NULL,draft_saved_at=NULL
                WHERE grading_job_id=:id AND grader_user_id=:examiner
                """, new MapSqlParameterSource("id", jobId).addValue("examiner", examinerId));
        return Optional.of(new Completed(job.learnerId(), job.submissionId(), job.skill()));
    }

    private boolean alreadyCompleted(UUID submissionId, BigDecimal band, JsonNode criteria, List<String> errors,
                                     String feedback, JsonNode annotations) {
        return Boolean.TRUE.equals(jdbc.query("""
                SELECT band=:band AND criteria=CAST(:criteria AS jsonb) AND error_codes=CAST(:codes AS varchar[])
                    AND feedback=:feedback AND annotations IS NOT DISTINCT FROM CAST(:annotations AS jsonb)
                FROM human_review_submissions WHERE id=:id
                """, new MapSqlParameterSource("band", band).addValue("criteria", criteria.toString())
                .addValue("codes", stringArray(errors)).addValue("feedback", feedback)
                .addValue("annotations", annotations == null ? null : annotations.toString(), Types.VARCHAR)
                .addValue("id", submissionId),
                (row, index) -> row.getBoolean(1)).stream().findFirst().orElse(false));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> essay(UUID jobId) {
        return jdbc.query("""
                SELECT h.essay_text FROM grading_jobs j
                JOIN human_review_submissions h ON h.id=j.human_review_submission_id
                WHERE j.id=:id AND j.grading_mode='HUMAN'
                """, new MapSqlParameterSource("id", jobId), (row, index) -> row.getString("essay_text"))
                .stream().findFirst();
    }

    @Override
    @Transactional
    public void saveDraft(UUID jobId, UUID examinerId, JsonNode payload) {
        int changed = jdbc.update("""
                UPDATE human_reviews r SET draft_payload=CAST(:payload AS jsonb),draft_saved_at=CURRENT_TIMESTAMP,
                    status='IN_REVIEW',started_at=COALESCE(r.started_at,CURRENT_TIMESTAMP)
                FROM grading_jobs j
                WHERE r.grading_job_id=j.id AND j.id=:id AND j.grading_mode='HUMAN'
                  AND j.human_review_submission_id IS NOT NULL AND j.assigned_examiner_id=:examiner
                  AND j.status IN ('QUEUED','PROCESSING') AND r.status<>'COMPLETED'
                """, new MapSqlParameterSource("payload", payload.toString()).addValue("id", jobId)
                .addValue("examiner", examinerId));
        if (changed == 1) return;
        throw draftRefusal(jobId, "saving a draft");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Draft> draft(UUID jobId, UUID examinerId) {
        List<Draft> rows = jdbc.query("""
                SELECT r.draft_payload::text AS payload,r.draft_saved_at
                FROM human_reviews r JOIN grading_jobs j ON j.id=r.grading_job_id
                WHERE j.id=:id AND j.grading_mode='HUMAN' AND j.human_review_submission_id IS NOT NULL
                  AND j.assigned_examiner_id=:examiner
                """, new MapSqlParameterSource("id", jobId).addValue("examiner", examinerId), (row, index) -> {
            String payload = row.getString("payload");
            if (payload == null) return null;
            try {
                return new Draft(json.readTree(payload), row.getTimestamp("draft_saved_at").toInstant());
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Stored human review draft is invalid", exception);
            }
        });
        if (rows.isEmpty()) throw draftRefusal(jobId, "reading a draft");
        return Optional.ofNullable(rows.getFirst());
    }

    /** Tells apart a job that does not exist, one that is finished, and one the examiner has not claimed. */
    private RuntimeException draftRefusal(UUID jobId, String action) {
        List<String> states = jdbc.query("""
                SELECT status FROM grading_jobs
                WHERE id=:id AND grading_mode='HUMAN' AND human_review_submission_id IS NOT NULL
                """, new MapSqlParameterSource("id", jobId), (row, index) -> row.getString("status"));
        if (states.isEmpty()) return new AssessmentNotFoundException("Standalone human review not found");
        if ("COMPLETED".equals(states.getFirst())) {
            return new RevisionConflictException("Human review has already been completed");
        }
        return new AssessmentAccessDeniedException("Claim the human review before " + action);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID jobIdOf(UUID reviewSubmissionId) {
        return jobId(reviewSubmissionId);
    }

    private UUID jobId(UUID submissionId) {
        return jdbc.queryForObject("SELECT id FROM grading_jobs WHERE human_review_submission_id=:id",
                new MapSqlParameterSource("id", submissionId), UUID.class);
    }

    private MapSqlParameterSource params(CreateReview review, UUID id) {
        return new MapSqlParameterSource().addValue("id", id).addValue("requestId", review.requestId())
                .addValue("learnerId", review.learnerId()).addValue("sourceSubmissionId", review.sourceSubmissionId())
                .addValue("source", review.source()).addValue("task", review.task())
                .addValue("prompt", write(review.promptSnapshot())).addValue("essay", review.essay())
                .addValue("rubric", write(review.rubric()));
    }

    private String write(JsonNode node) {
        try {
            return json.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Review snapshot is not valid JSON", exception);
        }
    }

    private static String stringArray(List<String> values) {
        return values.stream().map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }

    private static List<String> codes(Array array) throws SQLException {
        if (array == null) return List.of();
        try {
            return Arrays.stream((Object[]) array.getArray()).map(Object::toString).toList();
        } finally {
            array.free();
        }
    }

    private record StoredRequest(UUID id, UUID requestId, UUID learnerId, UUID sourceSubmissionId, String source,
                                 String task, String prompt, String essay, String rubric) {}
    private record Job(UUID submissionId, UUID examinerId, String status, UUID learnerId, String skill) {}
}
