package com.ieltspath.learning.infrastructure.persistence;

import com.ieltspath.learning.domain.repository.HumanReviewOutcomeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcHumanReviewOutcomeRepository implements HumanReviewOutcomeRepository {
    private final JdbcTemplate jdbc;

    public JdbcHumanReviewOutcomeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean insertIfAbsent(UUID reviewSubmissionId, UUID userId, UUID submissionId, String studyPointsJson,
                                  BigDecimal overallBand, boolean passed) {
        return jdbc.update("""
                INSERT INTO human_review_outcomes (review_submission_id, user_id, submission_id, study_points,
                                                   overall_band, passed)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?)
                ON CONFLICT DO NOTHING
                """, reviewSubmissionId, userId, submissionId, studyPointsJson, overallBand, passed) == 1;
    }

    @Override
    public Optional<ExaminerResult> resultOf(UUID submissionId) {
        return jdbc.query("SELECT overall_band, passed FROM human_review_outcomes WHERE submission_id = ? AND passed IS NOT NULL",
                (row, index) -> new ExaminerResult(row.getBigDecimal(1), row.getBoolean(2)), submissionId).stream().findFirst();
    }

    @Override
    public Optional<String> studyPointsOf(UUID submissionId) {
        return jdbc.query("SELECT study_points::text FROM human_review_outcomes WHERE submission_id = ?",
                (row, index) -> row.getString(1), submissionId).stream().findFirst();
    }
}
