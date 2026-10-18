package com.ieltspath.assessment.infrastructure.persistence.adapter;

import com.ieltspath.assessment.application.port.ExaminerQaRepository;
import com.ieltspath.assessment.domain.exception.AssessmentNotFoundException;
import com.ieltspath.assessment.domain.exception.AssessmentAccessDeniedException;
import com.ieltspath.assessment.domain.exception.InvalidAssessmentStateException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcExaminerQaRepository implements ExaminerQaRepository {
    private final JdbcTemplate jdbc;

    public JdbcExaminerQaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueueSample(UUID submissionId) {
        List<UUID> jobIds = jdbc.query("""
                INSERT INTO grading_jobs(id,submission_id,user_id,skill,grading_mode,status,idempotency_key,reason)
                SELECT gen_random_uuid(),l.id,l.user_id,l.skill,'HUMAN','QUEUED','qa-sample:'||l.id,'QA_SAMPLE'
                FROM learner_submissions l
                JOIN essay_grade_comparisons c ON c.submission_id=l.id
                WHERE l.id=? AND l.status='SUBMITTED' AND l.skill='WRITING'
                  AND c.examiner_band IS NOT NULL AND c.examiner_id IS NOT NULL
                ON CONFLICT(idempotency_key) DO NOTHING
                RETURNING id
                """, (rs, row) -> rs.getObject("id", UUID.class), submissionId);
        if (!jobIds.isEmpty()) {
            jdbc.update("INSERT INTO human_reviews(id,grading_job_id,status) VALUES (gen_random_uuid(),?,'QUEUED')",
                    jobIds.getFirst());
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void saveReview(UUID jobId, UUID reviewerId, BigDecimal reviewerBand, String criteria, String note) {
        int inserted = jdbc.update("""
                INSERT INTO examiner_qa_reviews(submission_id,examiner_id,examiner_band,reviewer_id,reviewer_band,reviewer_criteria,note)
                SELECT j.submission_id,c.examiner_id,c.examiner_band,?,?::numeric,?::jsonb,?
                FROM grading_jobs j JOIN essay_grade_comparisons c ON c.submission_id=j.submission_id
                WHERE j.id=? AND j.grading_mode='HUMAN' AND j.reason='QA_SAMPLE' AND j.status IN ('QUEUED','PROCESSING')
                  AND j.assigned_examiner_id=? AND c.examiner_band IS NOT NULL AND c.examiner_id<>?
                ON CONFLICT(submission_id) DO NOTHING
                """, reviewerId, reviewerBand, criteria, note, jobId, reviewerId, reviewerId);
        if (inserted == 0) {
            boolean exists = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM grading_jobs WHERE id=? AND reason='QA_SAMPLE')", Boolean.class, jobId));
            if (!exists) throw new AssessmentNotFoundException("QA grading job not found");
            boolean assigned = Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM grading_jobs WHERE id=? AND assigned_examiner_id=?)", Boolean.class,
                    jobId, reviewerId));
            if (!assigned) throw new AssessmentAccessDeniedException("QA job is not assigned to this reviewer");
            throw new InvalidAssessmentStateException("QA review was already submitted or the reviewer is not independent");
        }
        jdbc.update("UPDATE grading_jobs SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE id=?", jobId);
        jdbc.update("UPDATE human_reviews SET status='COMPLETED',completed_at=CURRENT_TIMESTAMP WHERE grading_job_id=?",
                jobId);
    }

    @Override
    public List<ExaminerQuality> quality() {
        return jdbc.query("""
                SELECT examiner_id, count(*) AS reviewed_essays,
                       round(avg(abs(examiner_band-reviewer_band)),2) AS average_difference,
                       round(avg(CASE WHEN abs(examiner_band-reviewer_band)>1 THEN 1.0 ELSE 0.0 END),4) AS over_one_rate
                FROM examiner_qa_reviews GROUP BY examiner_id ORDER BY examiner_id
                """, (rs, row) -> new ExaminerQuality(rs.getObject("examiner_id", UUID.class),
                rs.getLong("reviewed_essays"), rs.getBigDecimal("average_difference"),
                rs.getBigDecimal("over_one_rate")));
    }
}
